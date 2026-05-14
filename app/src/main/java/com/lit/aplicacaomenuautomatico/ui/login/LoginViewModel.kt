package com.lit.aplicacaomenuautomatico.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository
import com.lit.aplicacaomenuautomatico.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Estados possíveis da tela de login.
 * Modelado como sealed class para que a UI trate cada estado de forma exaustiva.
 */
sealed class LoginUiState {
    /** Estado inicial — formulário vazio, sem ação em andamento */
    object Ocioso : LoginUiState()

    /** Autenticando — exibe indicador de carregamento e desabilita o botão */
    object Carregando : LoginUiState()

    /** Login bem-sucedido — a UI deve navegar para a tela de menu */
    object Sucesso : LoginUiState()

    /**
     * Falha na autenticação — exibe mensagem de erro ao usuário.
     * @param mensagem Descrição amigável do erro (ex.: "Usuário ou senha inválidos")
     */
    data class Erro(val mensagem: String) : LoginUiState()
}

/**
 * ViewModel da tela de login.
 * Gerencia o estado da UI e orquestra a autenticação com o SAP via repositório.
 * Após login bem-sucedido, salva as credenciais e agenda a sincronização periódica.
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
     * Executado em Dispatchers.IO para não bloquear a Main thread durante a chamada de rede.
     *
     * @param username Usuário SAP digitado pelo operador
     * @param password Senha SAP digitada pelo operador
     */
    fun login(username: String, password: String) {
        // Valida campos antes de fazer a chamada de rede
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Erro("Informe usuário e senha para continuar.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LoginUiState.Carregando

            menuRepository.sincronizarDoServidor(username, password).fold(
                onSuccess = {
                    // Salva credenciais de forma segura para uso pelo WorkManager
                    menuRepository.salvarCredenciais(username, password)
                    // Agenda sincronização periódica em background (60 minutos)
                    agendarSyncPeriodico()
                    _uiState.value = LoginUiState.Sucesso
                },
                onFailure = { erro ->
                    // Traduz exceções HTTP para mensagens amigáveis ao operador
                    _uiState.value = LoginUiState.Erro(traduzirErro(erro))
                }
            )
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
     * Usa [ExistingPeriodicWorkPolicy.KEEP] para não recriar o trabalho se já estiver agendado
     * (ex.: se o usuário fizer login novamente com outro usuário).
     * A constraint de rede garante que o Worker só execute com internet disponível.
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
     * Evita expor stack traces ou mensagens técnicas na UI.
     *
     * @param erro Exceção capturada durante a autenticação
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
