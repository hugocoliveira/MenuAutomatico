package com.lit.aplicacaomenuautomatico

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository
import com.lit.aplicacaomenuautomatico.navigation.NavGraph
import com.lit.aplicacaomenuautomatico.navigation.Rota
import com.lit.aplicacaomenuautomatico.ui.theme.AplicacaoMenuAutomaticoTheme
import com.lit.aplicacaomenuautomatico.ui.theme.Primary
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity principal e única do aplicativo.
 * Responsável por:
 *  1. Determinar a rota inicial (Login ou Menu) com base no estado do banco e conectividade
 *  2. Configurar o NavHost com a rota correta
 *
 * Regra de rota inicial:
 *  - Banco vazio (primeiro acesso) → Login obrigatório (rede necessária)
 *  - Banco com dados + sem rede   → Menu direto (modo offline com último cache)
 *  - Banco com dados + com rede   → Login (para sincronizar dados atualizados)
 *
 * @AndroidEntryPoint habilita a injeção de dependências pelo Hilt nesta Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Repositório injetado para verificar se o banco local tem dados
     * antes de renderizar qualquer tela (lógica de primeiro acesso).
     */
    @Inject
    lateinit var menuRepository: MenuRepository

    // Launcher para solicitar permissão de notificação (Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solicitarPermissaoNotificacao()
        enableEdgeToEdge()

        setContent {
            AplicacaoMenuAutomaticoTheme {
                val navController = rememberNavController()

                // Determina a rota inicial de forma assíncrona (isBancoVazio é suspend)
                // Enquanto calcula, exibe um indicador de carregamento central
                val rotaInicial by produceState<String?>(initialValue = null) {
                    val bancoVazio = menuRepository.isBancoVazio()
                    val temRede = verificarConectividade()

                    value = when {
                        // Primeiro acesso: nunca houve dados — login e rede são obrigatórios
                        bancoVazio -> Rota.Login.caminho
                        // Tem dados mas sem rede: entra direto no menu offline
                        !temRede -> Rota.Menu.caminho
                        // Tem dados e tem rede: solicita login para sincronizar
                        else -> Rota.Login.caminho
                    }
                }

                if (rotaInicial == null) {
                    // Aguarda a resolução assíncrona da rota exibindo loading central
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    // Rota resolvida — inicializa o NavGraph com a tela correta
                    NavGraph(
                        navController = navController,
                        startDestination = rotaInicial!!
                    )
                }
            }
        }
    }

    /**
     * Solicita permissão POST_NOTIFICATIONS no Android 13+.
     * Sem essa permissão, notificações de atualização OTA são silenciosamente bloqueadas.
     */
    private fun solicitarPermissaoNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Verifica se o dispositivo tem conexão de rede ativa com capacidade de internet.
     * Usa [NetworkCapabilities] — compatível com minSdk 24.
     *
     * @return true se há rede disponível, false caso contrário
     */
    private fun verificarConectividade(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
