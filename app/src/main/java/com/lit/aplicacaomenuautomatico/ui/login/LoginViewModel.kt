package com.lit.aplicacaomenuautomatico.ui.login

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lit.aplicacaomenuautomatico.BuildConfig
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository
import com.lit.aplicacaomenuautomatico.worker.SyncWorker
import com.updater.lib.AppUpdateChecker
import com.updater.lib.UpdateConfig
import com.updater.lib.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Resultado de verificação de status de um app externo.
 *
 * @param nomeApp Nome amigável exibido no diálogo (ex.: "Entrada Fornecimento")
 * @param packageId Package ID do app (ex.: "com.entrada.fornecimento")
 * @param estaInstalado true = app instalado mas desatualizado | false = app não instalado
 * @param updateInfo Informações da versão disponível no GitHub (download ou atualização)
 */
data class AppUpdateResult(
    val nomeApp: String,
    val packageId: String,
    val estaInstalado: Boolean,
    val updateInfo: UpdateInfo
)

/**
 * Estados possíveis da tela de login.
 * Modelado como sealed class para que a UI trate cada estado de forma exaustiva.
 */
sealed class LoginUiState {
    /** Estado inicial — formulário vazio, sem ação em andamento */
    object Ocioso : LoginUiState()

    /** Autenticando contra o SAP — exibe indicador de carregamento */
    object Carregando : LoginUiState()

    /**
     * Login SAP concluído — verificando atualizações dos 3 apps antes de liberar o menu.
     * Exibe indicador de carregamento com texto "Verificando atualizações..."
     */
    object VerificandoAtualizacoes : LoginUiState()

    /**
     * Um ou mais apps têm atualização obrigatória disponível.
     * O usuário não pode prosseguir para o menu sem instalar.
     *
     * @param atualizacoes Lista de apps com atualização pendente
     */
    data class AtualizacaoObrigatoria(val atualizacoes: List<AppUpdateResult>) : LoginUiState()

    /** Tudo OK — a UI deve navegar para a tela de menu */
    object Sucesso : LoginUiState()

    /**
     * Falha na autenticação — exibe mensagem de erro ao usuário.
     * @param mensagem Descrição amigável do erro
     */
    data class Erro(val mensagem: String) : LoginUiState()
}

/**
 * ViewModel da tela de login.
 * Gerencia o estado da UI e orquestra:
 *  1. Autenticação com o SAP via repositório
 *  2. Verificação de atualizações dos 3 apps após login bem-sucedido
 *  3. Bloqueio obrigatório para instalação de atualizações antes de acessar o menu
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    /** Estado reativo da UI — observado pela LoginScreen via collectAsStateWithLifecycle() */
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Ocioso)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Inicia o processo de login com as credenciais informadas.
     * Após autenticação bem-sucedida, verifica atualizações dos 3 apps antes de navegar ao menu.
     *
     * @param username Usuário SAP digitado pelo operador
     * @param password Senha SAP digitada pelo operador
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Erro("Informe usuário e senha para continuar.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoginUiState.Carregando

            menuRepository.sincronizarDoServidor(username, password).fold(
                onSuccess = {
                    menuRepository.salvarCredenciais(username, password)
                    agendarSyncPeriodico()
                    // Após login OK, verifica atualizações antes de liberar o menu
                    verificarAtualizacoesDeTodosApps()
                },
                onFailure = { erro ->
                    _uiState.value = LoginUiState.Erro(traduzirErro(erro))
                }
            )
        }
    }

    /**
     * Verifica em paralelo o status de todos os apps relevantes:
     *  - O próprio Menu Automático (sempre verificado)
     *  - Apps externos cujos package IDs estão na tabela aplicativos (populada durante o sync)
     *
     * Para cada app:
     *  - Se instalado → verifica se há atualização disponível no OTA
     *  - Se não instalado → verifica se o APK está disponível para download no OTA
     *
     * Se houver qualquer pendência (atualização ou instalação), emite
     * [LoginUiState.AtualizacaoObrigatoria] para bloquear o acesso ao menu.
     * Caso tudo esteja em ordem, emite [LoginUiState.Sucesso].
     */
    private suspend fun verificarAtualizacoesDeTodosApps() {
        _uiState.value = LoginUiState.VerificandoAtualizacoes

        val token = BuildConfig.GITHUB_TOKEN.takeIf { it.isNotEmpty() }

        // Config do próprio app — sempre verificada independente do OData
        val configProprioApp = UpdateConfig(
            githubOwner = "hugocoliveira",
            githubRepo = "MenuAutomatico",
            branch = "main_MenuAutomatico",
            githubToken = token,
            packageId = "com.lit.aplicacaomenuautomatico"
        )

        // Mapeamento estático: packageId → (nome amigável, config OTA)
        // Contém todos os apps externos que possuem distribuição via GitHub OTA
        val otaExternos = mapOf(
            "com.entrada.fornecimento" to ("Entrada Fornecimento" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo = "EntradaFornecimento",
                branch = "main",
                githubToken = token,
                packageId = "com.entrada.fornecimento"
            )),
            "com.entrada.transporte" to ("Entrada Transporte" to UpdateConfig(
                githubOwner = "hugocoliveira",
                githubRepo = "EntradaTransporte",
                branch = "master",
                githubToken = token,
                packageId = "com.entrada.transporte"
            ))
        )

        // Busca da tabela aplicativos os package IDs extraídos do OData durante o sync
        val aplicativos = menuRepository.getAplicativos()

        // Monta a lista de verificações: próprio app + externos com config OTA conhecida
        // Apps externos sem config OTA (desconhecidos) são ignorados — não é possível checar
        val verificacoes = mutableListOf(
            Triple("Menu Automático", "com.lit.aplicacaomenuautomatico", configProprioApp)
        )
        aplicativos.forEach { packageId ->
            otaExternos[packageId]?.let { (nome, config) ->
                verificacoes.add(Triple(nome, packageId, config))
            }
        }

        // Verificações em paralelo para reduzir o tempo de espera
        val pendencias = verificacoes
            .map { (nome, packageId, config) ->
                viewModelScope.async(Dispatchers.IO) {
                    val instalado = estaInstalado(packageId)
                    // checkForUpdate retorna UpdateInfo tanto para "atualizar" quanto para
                    // "instalar" — se não instalado, versionCode local = 0 < remoto
                    AppUpdateChecker.checkForUpdate(config, context)?.let { updateInfo ->
                        AppUpdateResult(
                            nomeApp = nome,
                            packageId = packageId,
                            estaInstalado = instalado,
                            updateInfo = updateInfo
                        )
                    }
                }
            }
            .awaitAll()
            .filterNotNull()

        _uiState.value = if (pendencias.isEmpty()) {
            LoginUiState.Sucesso
        } else {
            LoginUiState.AtualizacaoObrigatoria(pendencias)
        }
    }

    /**
     * Verifica se um app está instalado no dispositivo usando o PackageManager.
     *
     * @param packageId Package ID do app a verificar (ex.: "com.entrada.fornecimento")
     * @return true se instalado, false se não encontrado
     */
    private fun estaInstalado(packageId: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageId, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Re-executa a verificação de status de todos os apps após uma instalação detectada.
     * Chamado pela UI quando recebe broadcast de pacote instalado/atualizado.
     * Se todos os apps estiverem em ordem, emite [LoginUiState.Sucesso] e navega ao menu.
     * Se ainda houver pendências, emite [LoginUiState.AtualizacaoObrigatoria] com a lista atualizada.
     */
    fun reVerificarAposInstalacao() {
        viewModelScope.launch(Dispatchers.IO) {
            verificarAtualizacoesDeTodosApps()
        }
    }

    /**
     * Reseta o estado para Ocioso — chamado após a UI consumir um estado de Erro
     * e o usuário começar a digitar novamente.
     */
    fun limparErro() {
        _uiState.value = LoginUiState.Ocioso
    }

    /**
     * Agenda o WorkManager para sincronizar os dados do SAP a cada 60 minutos.
     */
    private fun agendarSyncPeriodico() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(60, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sync_menu_automatico",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    /**
     * Converte exceções de rede e HTTP em mensagens compreensíveis para o operador.
     */
    private fun traduzirErro(erro: Throwable): String {
        val mensagem = erro.message ?: ""
        return when {
            mensagem.contains("401") -> "Usuário ou senha inválidos. Verifique e tente novamente."
            mensagem.contains("Unable to resolve host") ||
            mensagem.contains("timeout") ||
            mensagem.contains("connect") ->
                "Não foi possível conectar ao servidor SAP. Verifique a rede e tente novamente."
            else -> "Erro ao autenticar: $mensagem"
        }
    }
}
