package com.lit.aplicacaomenuautomatico

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / permissões / conectividade
// ─────────────────────────────────────────────────────────────────────────────
import android.Manifest              // constantes de permissão do sistema (POST_NOTIFICATIONS)
import android.content.Context       // acesso ao ConnectivityManager e outros serviços do sistema
import android.content.pm.PackageManager // verifica se uma permissão já foi concedida
import android.net.ConnectivityManager   // acessa informações sobre a rede ativa
import android.net.NetworkCapabilities   // consulta capacidades da rede (internet, Wi-Fi, etc.)
import android.os.Build              // verifica a versão do Android em tempo de execução
import android.os.Bundle             // estado salvo do ciclo de vida da Activity

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — AndroidX Activity / Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.activity.ComponentActivity         // Activity base com suporte a Compose e lifecycle
import androidx.activity.compose.setContent        // define o conteúdo Composable da Activity
import androidx.activity.enableEdgeToEdge          // estende conteúdo até as bordas da tela (edge-to-edge)
import androidx.activity.result.contract.ActivityResultContracts // contrato para solicitar permissões

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose (layout e estado)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.foundation.layout.Box          // container que empilha filhos em camadas (Z)
import androidx.compose.foundation.layout.fillMaxSize  // modifier: ocupa 100% do espaço disponível
import androidx.compose.material3.CircularProgressIndicator // spinner durante resolução da rota inicial
import androidx.compose.runtime.getValue               // delegate para leitura de State<T>
import androidx.compose.runtime.produceState           // cria um State<T> a partir de código assíncrono
import androidx.compose.ui.Alignment                   // alinha o spinner ao centro da tela
import androidx.compose.ui.Modifier                    // encadeia modificadores de layout

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Navigation Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.navigation.compose.rememberNavController // cria e retém o NavController no ciclo de vida

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — recursos do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository // verifica se o banco está vazio
import com.lit.aplicacaomenuautomatico.navigation.NavGraph            // host de rotas do app
import com.lit.aplicacaomenuautomatico.navigation.Rota                // sealed class com os caminhos
import com.lit.aplicacaomenuautomatico.ui.theme.AplicacaoMenuAutomaticoTheme // tema Material3
import com.lit.aplicacaomenuautomatico.ui.theme.Primary               // cor primária para o spinner

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt / injeção de dependência
// ─────────────────────────────────────────────────────────────────────────────
import dagger.hilt.android.AndroidEntryPoint // habilita injeção de dependências nesta Activity
import javax.inject.Inject                   // anotação padrão JSR-330 para injeção de campo

// ═════════════════════════════════════════════════════════════════════════════
// ACTIVITY PRINCIPAL: MainActivity
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Activity principal e única do aplicativo (single-Activity pattern).
 * Responsável por:
 *  1. Solicitar a permissão de notificação no Android 13+ (necessária para o OTA)
 *  2. Determinar assincronamente a rota inicial com base no estado do banco e da rede
 *  3. Inicializar o [NavGraph] com a rota correta
 *
 * Lógica de rota inicial:
 *  - Banco vazio (primeiro acesso)       → [Rota.Login] (rede obrigatória para sincronizar)
 *  - Banco com dados + sem rede          → [Rota.Menu]  (modo offline com último cache)
 *  - Banco com dados + com rede          → [Rota.Login] (sincroniza dados atualizados)
 *
 * @AndroidEntryPoint habilita injeção de campo pelo Hilt nesta Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ─── DEPENDÊNCIA INJETADA ─────────────────────────────────────────────────

    /**
     * Repositório injetado para verificar o estado do banco local antes de
     * renderizar qualquer tela — a query [isBancoVazio] é suspend e precisa
     * ser chamada em corrotina, o que é feito via [produceState].
     */
    @Inject
    lateinit var menuRepository: MenuRepository

    // ─── LAUNCHER DE PERMISSÃO ────────────────────────────────────────────────

    /**
     * Launcher para a solicitação de permissão POST_NOTIFICATIONS (Android 13+).
     * O callback vazio é intencional — o app funciona mesmo sem a permissão,
     * apenas as notificações do OTA serão silenciadas.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* resultado ignorado */ }

    // ─── CICLO DE VIDA ────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicita permissão de notificação antes de renderizar qualquer tela
        solicitarPermissaoNotificacao()

        // Estende o conteúdo até as bordas da tela (status bar e navigation bar transparentes)
        enableEdgeToEdge()

        setContent {
            AplicacaoMenuAutomaticoTheme {
                // NavController criado aqui e passado ao NavGraph — retido pelo rememberNavController
                val navController = rememberNavController()

                // ─── RESOLUÇÃO ASSÍNCRONA DA ROTA INICIAL ────────────────────
                // produceState executa a lambda em corrotina e expõe o resultado como State<T>.
                // initialValue = null sinaliza "ainda calculando" — exibe spinner enquanto null.
                val rotaInicial by produceState<String?>(initialValue = null) {
                    val bancoVazio = menuRepository.isBancoVazio() // suspend: consulta COUNT(*) no SQLite
                    val temRede   = verificarConectividade()        // verifica NetworkCapabilities

                    // Determina qual tela deve abrir baseado no estado do banco e da rede
                    value = when {
                        bancoVazio -> Rota.Login.caminho // primeiro acesso: rede obrigatória
                        !temRede   -> Rota.Menu.caminho  // offline com cache: entra direto no menu
                        else       -> Rota.Login.caminho // tem rede: sincroniza com login
                    }
                }

                // ─── RENDERIZAÇÃO CONDICIONAL ─────────────────────────────────
                if (rotaInicial == null) {
                    // Aguarda a resolução da rota — exibe spinner centralizado na tela
                    Box(
                        modifier          = Modifier.fillMaxSize(), // ocupa toda a tela
                        contentAlignment  = Alignment.Center        // centraliza o spinner
                    ) {
                        CircularProgressIndicator(color = Primary)  // spinner azul primário
                    }
                } else {
                    // Rota resolvida — inicializa o NavGraph com a tela correta
                    NavGraph(
                        navController    = navController,
                        startDestination = rotaInicial!! // non-null garantido pelo bloco if acima
                    )
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PRIVADA: solicitarPermissaoNotificacao
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Solicita a permissão POST_NOTIFICATIONS no Android 13+ (API 33 = TIRAMISU).
     * Em versões anteriores a permissão não existe — nenhuma ação necessária.
     * Sem esta permissão, notificações de download do OTA são bloqueadas silenciosamente.
     */
    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && // apenas Android 13+
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            // Exibe o diálogo padrão do Android pedindo permissão de notificação
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PRIVADA: verificarConectividade
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verifica se o dispositivo tem conexão de rede ativa com capacidade de internet.
     * Usa [NetworkCapabilities] (API 23+) — compatível com o minSdk 24 do app.
     *
     * @return true se há rede com acesso à internet, false caso contrário
     */
    private fun verificarConectividade(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // activeNetwork é null quando não há nenhuma interface de rede conectada
        val network = connectivityManager.activeNetwork ?: return false

        // getNetworkCapabilities é null quando a interface não tem capacidades configuradas
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // NET_CAPABILITY_INTERNET = há rota para a internet (Wi-Fi com acesso, dados móveis, VPN)
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
