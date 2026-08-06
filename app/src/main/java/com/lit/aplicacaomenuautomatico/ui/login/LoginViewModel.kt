package com.lit.aplicacaomenuautomatico.ui.login

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / sistema
// ─────────────────────────────────────────────────────────────────────────────
import android.content.Context              // acesso a recursos do sistema (PackageManager, assets)
import android.content.pm.PackageManager   // verifica se um app está instalado no dispositivo

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Jetpack / AndroidX
// ─────────────────────────────────────────────────────────────────────────────
import androidx.lifecycle.ViewModel         // base do ViewModel — sobrevive a rotações de tela
import androidx.lifecycle.viewModelScope    // escopo de corrotina ligado ao ciclo de vida do ViewModel

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — WorkManager (sincronização em background)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.work.Constraints                    // define restrições para execução do Worker (rede, bateria)
import androidx.work.ExistingPeriodicWorkPolicy     // política para trabalho periódico já enfileirado (KEEP = não substitui)
import androidx.work.NetworkType                    // tipo de rede exigida (CONNECTED = qualquer conexão)
import androidx.work.PeriodicWorkRequestBuilder     // constrói requisição de trabalho periódico
import androidx.work.WorkManager                    // gerenciador de tarefas em background

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — recursos do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.BuildConfig              // acessa GITHUB_TOKEN gerado pelo build
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository // repositório: SAP OData + SQLite
import com.lit.aplicacaomenuautomatico.worker.SyncWorker        // Worker que sincroniza dados a cada 60 min

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — biblioteca de atualização OTA
// ─────────────────────────────────────────────────────────────────────────────
import com.updater.lib.AppUpdateChecker // verifica versão disponível no GitHub OTA
import com.updater.lib.UpdateConfig     // configuração do repositório GitHub (owner, repo, branch, token)
import com.updater.lib.UpdateInfo       // resultado da verificação: versionName, versionCode, apkUrl

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt (injeção de dependência)
// ─────────────────────────────────────────────────────────────────────────────
import dagger.hilt.android.lifecycle.HiltViewModel      // marca o ViewModel para injeção via Hilt
import dagger.hilt.android.qualifiers.ApplicationContext // injeta o ApplicationContext (não Activity)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Corrotinas / Flow
// ─────────────────────────────────────────────────────────────────────────────
import kotlinx.coroutines.Dispatchers           // define qual thread executa a corrotina (IO, Main, Default)
import kotlinx.coroutines.async                 // lança corrotina paralela que retorna Deferred<T>
import kotlinx.coroutines.awaitAll              // aguarda todos os Deferred em paralelo finalizarem
import kotlinx.coroutines.flow.MutableStateFlow // StateFlow mutável — atualizado internamente no ViewModel
import kotlinx.coroutines.flow.StateFlow        // StateFlow somente-leitura — exposto à UI
import kotlinx.coroutines.flow.asStateFlow      // converte MutableStateFlow em StateFlow imutável
import kotlinx.coroutines.launch                // lança corrotina no escopo do ViewModel

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Java / utilitários
// ─────────────────────────────────────────────────────────────────────────────
import java.util.concurrent.TimeUnit // unidade de tempo para o WorkManager (MINUTES)
import javax.inject.Inject           // anotação padrão JSR-330 para injeção de construtor via Hilt

// ═════════════════════════════════════════════════════════════════════════════
// DATA CLASS: AppUpdateResult
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Resultado de verificação de status de um app externo.
 * Usado para popular o diálogo de atualização obrigatória na UI.
 *
 * @param nomeApp      Nome amigável exibido no diálogo (ex.: "Entrada Fornecimento")
 * @param packageId    Package ID do app (ex.: "com.entrada.fornecimento")
 * @param estaInstalado true = app instalado mas desatualizado | false = app não instalado
 * @param updateInfo   Informações da versão disponível no GitHub OTA (versionName, apkUrl, etc.)
 */
data class AppUpdateResult(
    val nomeApp: String,        // nome legível exibido ao operador no diálogo
    val packageId: String,      // identificador único do app no sistema Android
    val estaInstalado: Boolean, // distingue "atualizar" de "instalar" — muda ícone e texto do diálogo
    val updateInfo: UpdateInfo  // contém URL do APK, versionName e versionCode para download
)

// ═════════════════════════════════════════════════════════════════════════════
// SEALED CLASS: LoginUiState
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Estados possíveis da tela de login.
 * Modelado como sealed class para que a UI trate cada estado de forma exaustiva
 * usando `when` — sem risco de estado desconhecido em runtime.
 */
sealed class LoginUiState {

    /** Estado inicial — formulário habilitado, sem ação em andamento. */
    object Ocioso : LoginUiState()

    /**
     * Autenticando contra o SAP OData.
     * UI: desabilita campos e botão, exibe CircularProgressIndicator + "Autenticando..."
     */
    object Carregando : LoginUiState()

    /**
     * Login SAP concluído com sucesso — verificando versões dos apps externos via GitHub OTA.
     * UI: mantém CircularProgressIndicator, muda texto para "Verificando atualizações..."
     */
    object VerificandoAtualizacoes : LoginUiState()

    /**
     * Um ou mais apps têm versão pendente (atualização ou primeira instalação).
     * UI: exibe DialogAtualizacaoObrigatoria — o operador não pode prosseguir ao menu.
     *
     * @param atualizacoes Lista com todos os apps que precisam de ação
     */
    data class AtualizacaoObrigatoria(val atualizacoes: List<AppUpdateResult>) : LoginUiState()

    /**
     * Tudo verificado e em ordem — a UI deve navegar para a tela de menu.
     * Este estado é consumido pelo LaunchedEffect em LoginScreen.
     */
    object Sucesso : LoginUiState()

    /**
     * Falha na autenticação ou na rede — exibe mensagem de erro ao usuário.
     * UI: mostra o texto com AnimatedVisibility (fadeIn) abaixo dos campos.
     *
     * @param mensagem Descrição amigável do erro (traduzida por [traduzirErro])
     */
    data class Erro(val mensagem: String) : LoginUiState()
}

// ═════════════════════════════════════════════════════════════════════════════
// VIEWMODEL: LoginViewModel
// ═════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel da tela de login.
 * Responsável por:
 *  1. Validar e autenticar as credenciais via [MenuRepository] (SAP OData)
 *  2. Verificar em paralelo as versões dos apps externos após login bem-sucedido
 *  3. Bloquear o acesso ao menu enquanto houver apps desatualizados ou não instalados
 *  4. Agendar o WorkManager para sincronização periódica (60 min) após login
 *
 * Injetado pelo Hilt via @HiltViewModel — não deve ser instanciado diretamente.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val menuRepository: MenuRepository,     // acesso a SAP OData e banco SQLite local
    @ApplicationContext private val context: Context // contexto da aplicação (não de Activity)
) : ViewModel() {

    // ─── ESTADO REATIVO ───────────────────────────────────────────────────────
    // MutableStateFlow interno — atualizado apenas pelo ViewModel.
    // StateFlow público imutável — exposto à UI via collectAsStateWithLifecycle().
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Ocioso)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: login
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Inicia o processo de autenticação com as credenciais digitadas pelo operador.
     * Fluxo:
     *  1. Valida que os campos não estão em branco
     *  2. Emite [LoginUiState.Carregando] para a UI desabilitar o formulário
     *  3. Chama o repositório para sincronizar dados do SAP (Basic Auth)
     *  4. Em caso de sucesso: salva credenciais, agenda sync periódico e verifica apps
     *  5. Em caso de falha: emite [LoginUiState.Erro] com mensagem traduzida
     *
     * Executa em [Dispatchers.IO] para não bloquear a Main thread.
     *
     * @param username Usuário SAP digitado pelo operador
     * @param password Senha SAP digitada pelo operador
     */
    fun login(username: String, password: String) {
        // Validação dos campos — evita chamada de rede desnecessária
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Erro("Informe usuário e senha para continuar.")
            return
        }

        // Inicia corrotina no escopo do ViewModel em thread de I/O
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoginUiState.Carregando // sinaliza UI para mostrar o spinner

            // fold() trata os dois casos do Result<T>: sucesso e falha
            menuRepository.sincronizarDoServidor(username, password).fold(
                onSuccess = {
                    // Persiste credenciais para uso offline (próximas sessões sem rede)
                    menuRepository.salvarCredenciais(username, password)
                    // Agenda sync periódico — só após login bem-sucedido
                    agendarSyncPeriodico()
                    // Verifica versões dos apps antes de liberar o menu
                    verificarAtualizacoesDeTodosApps()
                },
                onFailure = { erro ->
                    // Traduz exceções técnicas em mensagens legíveis pelo operador
                    _uiState.value = LoginUiState.Erro(traduzirErro(erro))
                }
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PRIVADA: verificarAtualizacoesDeTodosApps
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verifica em paralelo o status de todos os apps relevantes:
     *  - O próprio Menu Automático (sempre verificado, independente do OData)
     *  - Apps externos cujos package IDs estão na tabela `aplicativos` (populada durante o sync)
     *
     * Para cada app:
     *  - Instalado e desatualizado → inclui na lista de pendências com [AppUpdateResult.estaInstalado] = true
     *  - Não instalado            → inclui na lista de pendências com [AppUpdateResult.estaInstalado] = false
     *  - Instalado e atualizado   → ignora (checkForUpdate retorna null)
     *
     * Resultado:
     *  - Lista vazia   → emite [LoginUiState.Sucesso] (navega ao menu)
     *  - Lista não vazia → emite [LoginUiState.AtualizacaoObrigatoria] (bloqueia acesso)
     */
    private suspend fun verificarAtualizacoesDeTodosApps() {
        // Informa a UI que a verificação de updates começou (mantém o spinner visível)
        _uiState.value = LoginUiState.VerificandoAtualizacoes

        // Token do GitHub para requests autenticados (maior rate limit).
        // Se a build não incluiu o token, usa null → request anônimo (limite menor).
        val token = BuildConfig.GITHUB_TOKEN.takeIf { it.isNotEmpty() }

        // ─── CONFIGURAÇÃO DO PRÓPRIO APP ──────────────────────────────────────
        // O Menu Automático é sempre verificado, independente do OData.
        val configProprioApp = UpdateConfig(
            githubOwner  = "hugocoliveira",
            githubRepo   = "MenuAutomatico",
            branch       = "main_MenuAutomatico",
            githubToken  = token,
            packageId    = "com.lit.aplicacaomenuautomatico"
        )

        // ─── MAPA DE APPS EXTERNOS COM OTA ───────────────────────────────────
        // Mapeamento estático: packageId → (nome amigável, UpdateConfig).
        // Só os apps aqui listados são verificados — apps sem OTA são ignorados.
        val otaExternos = mapOf(
            "com.entrada.fornecimento" to ("Entrada Fornecimento" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "EntradaFornecimento",
                branch      = "main",
                githubToken = token,
                packageId   = "com.entrada.fornecimento"
            )),
            "com.entrada.transporte" to ("Entrada Transporte" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "EntradaTransporte",
                branch      = "master",
                githubToken = token,
                packageId   = "com.entrada.transporte"
            )),
            // Novos apps adicionados em v1.49 — instalados automaticamente via OTA no primeiro login
            "br.com.lit.busca.material" to ("Busca Material" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "BuscaMaterial",
                branch      = "main",
                githubToken = token,
                packageId   = "br.com.lit.busca.material"
            )),
            "br.com.lit.busca.posicao" to ("Busca Posicao" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "BuscaPosicao",
                branch      = "main",
                githubToken = token,
                packageId   = "br.com.lit.busca.posicao"
            )),
            "br.com.lit.busca.fila" to ("Busca Por Fila" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "BuscaPorFila",
                branch      = "main",
                githubToken = token,
                packageId   = "br.com.lit.busca.fila"
            )),
            "br.com.lit.busca.uc" to ("Busca Por UC" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo  = "BuscaPorUC",
                branch      = "main",
                githubToken = token,
                packageId   = "br.com.lit.busca.uc"
            ))
        )

        // ─── LISTA DE PACKAGE IDs DO ODATA ────────────────────────────────────
        // Busca os package IDs extraídos do SAP durante o sync e gravados em SQLite.
        // Apenas apps presentes no OData E no mapa otaExternos serão verificados.
        val aplicativos = menuRepository.getAplicativos()

        // ─── MONTAGEM DA LISTA DE VERIFICAÇÕES ────────────────────────────────
        // Começa com o próprio app e adiciona os externos que têm config OTA conhecida.
        val verificacoes = mutableListOf(
            Triple("Menu Automático", "com.lit.aplicacaomenuautomatico", configProprioApp)
        )
        aplicativos.forEach { packageId ->
            // none() evita duplicatas caso o packageId já esteja em verificacoes
            if (verificacoes.none { it.second == packageId }) {
                otaExternos[packageId]?.let { (nome, config) ->
                    verificacoes.add(Triple(nome, packageId, config))
                }
            }
        }

        // ─── VERIFICAÇÕES EM PARALELO ─────────────────────────────────────────
        // async lança cada verificação em paralelo no Dispatchers.IO.
        // awaitAll() aguarda todas completarem antes de continuar.
        // Resultado: lista de AppUpdateResult apenas dos apps com pendência (filterNotNull remove os atualizados).
        val pendencias = verificacoes
            .map { (nome, packageId, config) ->
                viewModelScope.async(Dispatchers.IO) {
                    val instalado = estaInstalado(packageId) // verifica no PackageManager local
                    // checkForUpdate retorna UpdateInfo se há versão nova, null se está em dia.
                    // Funciona para apps não instalados também: versionCode local = 0 < remoto.
                    AppUpdateChecker.checkForUpdate(config, context)?.let { updateInfo ->
                        AppUpdateResult(
                            nomeApp       = nome,
                            packageId     = packageId,
                            estaInstalado = instalado, // define se é "atualizar" ou "instalar"
                            updateInfo    = updateInfo  // contém apkUrl, versionName, versionCode
                        )
                    }
                }
            }
            .awaitAll()       // aguarda todas as corrotinas paralelas terminarem
            .filterNotNull()  // remove os apps que já estão atualizados (checkForUpdate retornou null)

        // ─── DECISÃO FINAL ─────────────────────────────────────────────────────
        // Sem pendências → libera acesso ao menu. Com pendências → bloqueia com diálogo.
        _uiState.value = if (pendencias.isEmpty()) {
            LoginUiState.Sucesso                         // navega ao menu (consumido pelo LaunchedEffect)
        } else {
            LoginUiState.AtualizacaoObrigatoria(pendencias) // exibe diálogo com lista de apps pendentes
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PRIVADA: estaInstalado
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Verifica se um app está instalado no dispositivo consultando o PackageManager do sistema.
     * Usa try/catch pois a API lança exceção quando o pacote não é encontrado.
     *
     * @param packageId Package ID do app a verificar (ex.: "com.entrada.fornecimento")
     * @return true se instalado, false se não encontrado
     */
    private fun estaInstalado(packageId: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageId, 0) // lança NameNotFoundException se ausente
            true  // chegou aqui = pacote encontrado = app instalado
        } catch (e: PackageManager.NameNotFoundException) {
            false // pacote não encontrado = app não instalado
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: reVerificarAposInstalacao
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Re-executa a verificação de status de todos os apps após instalação detectada.
     * Chamada pela LoginScreen quando o BroadcastReceiver captura ACTION_PACKAGE_ADDED
     * ou ACTION_PACKAGE_REPLACED para um dos apps pendentes.
     *
     * Se todos os apps estiverem em ordem → emite [LoginUiState.Sucesso] e navega ao menu.
     * Se ainda houver pendências → emite [LoginUiState.AtualizacaoObrigatoria] atualizado.
     */
    fun reVerificarAposInstalacao() {
        viewModelScope.launch(Dispatchers.IO) {
            verificarAtualizacoesDeTodosApps() // reutiliza exatamente o mesmo fluxo de verificação
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: limparErro
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Reseta o estado para [LoginUiState.Ocioso].
     * Chamada pela LoginScreen quando o usuário começa a digitar após um erro —
     * remove a mensagem de erro e reabilita os campos imediatamente.
     */
    fun limparErro() {
        _uiState.value = LoginUiState.Ocioso // volta ao estado inicial sem mensagem de erro
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PRIVADA: agendarSyncPeriodico
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Agenda o [SyncWorker] para sincronizar dados do SAP a cada 60 minutos em background.
     * Só executa quando há conexão de rede ([NetworkType.CONNECTED]).
     *
     * [ExistingPeriodicWorkPolicy.KEEP] garante que um trabalho já agendado não seja
     * substituído — evita resetar o intervalo a cada login.
     */
    private fun agendarSyncPeriodico() {
        // Restrições: só executa com rede disponível (Wi-Fi ou dados móveis)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Trabalho periódico a cada 60 minutos usando o SyncWorker
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(60, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // Enfileira com nome único — KEEP mantém o trabalho existente se já estiver agendado
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sync_menu_automatico",              // nome único para identificar o trabalho
            ExistingPeriodicWorkPolicy.KEEP,     // não substitui se já existir (preserva o intervalo)
            syncRequest
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PRIVADA: traduzirErro
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Converte exceções técnicas de rede e HTTP em mensagens legíveis para o operador.
     * Inspeciona a mensagem da exceção para identificar o tipo de falha.
     *
     * Casos tratados:
     *  - HTTP 401 → credenciais inválidas
     *  - Falha de DNS / timeout / conexão recusada → problema de rede
     *  - Qualquer outro erro → mensagem genérica com detalhe técnico
     *
     * @param erro Exceção capturada no onFailure do Result
     * @return String com mensagem amigável para exibir na UI
     */
    private fun traduzirErro(erro: Throwable): String {
        val mensagem = erro.message ?: "" // mensagem bruta da exceção (pode ser vazia)
        return when {
            // HTTP 401: SAP rejeitou as credenciais
            mensagem.contains("401") ->
                "Usuário ou senha inválidos. Verifique e tente novamente."

            // Falhas de rede: sem DNS, timeout ou conexão recusada pelo servidor
            mensagem.contains("Unable to resolve host") ||
            mensagem.contains("timeout")               ||
            mensagem.contains("connect") ->
                "Não foi possível conectar ao servidor SAP. Verifique a rede e tente novamente."

            // Fallback: erro desconhecido — inclui mensagem técnica para facilitar diagnóstico
            else -> "Erro ao autenticar: $mensagem"
        }
    }
}
