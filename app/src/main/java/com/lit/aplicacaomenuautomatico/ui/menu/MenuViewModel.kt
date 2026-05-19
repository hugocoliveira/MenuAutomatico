package com.lit.aplicacaomenuautomatico.ui.menu

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository
import com.lit.aplicacaomenuautomatico.domain.model.MenuApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado da tela de menu.
 * Contém todos os dados necessários para renderizar a UI sem lógica no Composable.
 */
data class MenuUiState(
    /** Nome do grupo de menu sendo exibido atualmente (ex.: "MAIN", "INB00") */
    val tituloAtual: String = "MAIN",
    /** Lista de itens do menu atual — vazia durante carregamento */
    val itens: List<MenuApp> = emptyList(),
    /** True enquanto os dados estão sendo carregados do banco */
    val carregando: Boolean = true,
    /** True quando o usuário está no MAIN e pressionou Voltar — exibe diálogo de saída */
    val mostrarDialogSaida: Boolean = false,
    /** Mensagem de erro ao tentar lançar app externo — exibida via Snackbar, null = sem erro */
    val erroLancarApp: String? = null
)

/**
 * ViewModel da tela de menu.
 * Gerencia:
 *  - Carregamento dos itens do SQLite via Flow reativo
 *  - Back stack interno de menus visitados (independente do Navigation Compose)
 *  - Lançamento de apps externos via Intent
 *  - Controle do diálogo de confirmação de saída
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    /** Estado reativo da UI — observado pela MenuScreen */
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    /**
     * Pilha de menus visitados — permite voltar ao menu pai sem nova consulta ao SAP.
     * Começa vazia pois o MAIN é o ponto de entrada e não tem pai.
     * Exemplo após navegar MAIN → INB00 → GR00: ["MAIN", "INB00"]
     *
     * Usar ArrayDeque (não mutableListOf) porque removeLast() é método concreto do
     * ArrayDeque do Kotlin e funciona em qualquer API Android. Em Kotlin 2.x,
     * mutableListOf().removeLast() é compilado como chamada de interface em
     * java.util.List, que só existe a partir do Java 21 / Android API 35.
     */
    private val backStackMenus = ArrayDeque<String>()

    /** Job da coleta do Flow atual — cancelado ao navegar para outro menu */
    private var jobColeta: Job? = null

    init {
        // Carrega o menu raiz MAIN ao inicializar o ViewModel
        carregarMenu("MAIN")
    }

    /**
     * Carrega os itens de um grupo de menu do banco local via Flow reativo.
     * Cancela qualquer coleta anterior antes de iniciar a nova para evitar vazamento de recursos.
     *
     * @param mmenu Código do grupo de menu a carregar (ex.: "MAIN", "INB00")
     */
    fun carregarMenu(mmenu: String) {
        // Cancela o Flow anterior para não acumular coletas em background
        jobColeta?.cancel()

        _uiState.update { it.copy(carregando = true, tituloAtual = mmenu) }

        jobColeta = viewModelScope.launch {
            menuRepository.getItensPorMenu(mmenu).collect { itens ->
                _uiState.update { it.copy(itens = itens, carregando = false) }
            }
        }
    }

    /**
     * Navega para o submenu filho de um item com Type=1.
     * Empilha o menu atual no back stack antes de navegar,
     * permitindo retorno correto ao pressionar Voltar.
     *
     * @param item Item de menu com type="1" que foi tocado pelo usuário
     */
    fun navegarParaSubmenu(item: MenuApp) {
        // Empilha o menu atual antes de navegar para o filho
        backStackMenus.add(_uiState.value.tituloAtual)
        // O submenu filho é identificado pelo campo Transacao do item pai
        carregarMenu(item.transacao)
    }

    /**
     * Lança o app externo associado a um item com Type=2.
     * Verifica se o app está instalado antes de tentar lançar.
     * Em caso de app não encontrado, atualiza o estado com mensagem de erro para o Snackbar.
     *
     * @param context Contexto necessário para acessar o PackageManager e iniciar a Intent
     * @param item Item de menu com type="2" que foi tocado pelo usuário
     */
    fun lancarAppExterno(context: Context, item: MenuApp) {
        val pm = context.packageManager

        // Tenta via getLaunchIntentForPackage (apps com ícone no launcher)
        var intentLaunch = pm.getLaunchIntentForPackage(item.componente)

        // Fallback: intent explícito para apps sem ícone no launcher (exported=true sem LAUNCHER)
        if (intentLaunch == null) {
            intentLaunch = pm.getLaunchIntentForPackage(item.componente)
                ?: runCatching {
                    val cn = android.content.ComponentName(item.componente, "${item.componente}.MainActivity")
                    pm.getActivityInfo(cn, 0) // lança NameNotFoundException se não existir
                    Intent().apply { component = cn }
                }.getOrNull()
        }

        if (intentLaunch == null) {
            _uiState.update {
                it.copy(
                    erroLancarApp = "Aplicativo '${item.componente}' não encontrado no dispositivo."
                )
            }
            return
        }

        // Passa o código da transação SAP como extra para o app externo processar
        intentLaunch.putExtra("transacao", item.transacao)
        // Extra de segurança: sub-apps rejeitam abertura direta sem este extra
        intentLaunch.putExtra("origem", "com.lit.aplicacaomenuautomatico")
        intentLaunch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intentLaunch)
    }

    /**
     * Tenta retornar ao menu pai desempilhando o back stack interno.
     * Não usa o back stack do Navigation Compose — a navegação entre menus
     * é gerenciada inteiramente por este ViewModel.
     *
     * @return true se voltou para o menu pai com sucesso,
     *         false se já estava no MAIN (sem pai — deve mostrar diálogo de saída)
     */
    fun voltarMenuAnterior(): Boolean {
        if (backStackMenus.isEmpty()) {
            // Já está no menu raiz — não há para onde voltar dentro dos menus
            return false
        }
        // Desempilha o menu anterior e o carrega
        val menuAnterior = backStackMenus.removeLast()
        carregarMenu(menuAnterior)
        return true
    }

    /** Exibe o diálogo de confirmação de saída (chamado quando Voltar no MAIN) */
    fun mostrarDialogSaida() {
        _uiState.update { it.copy(mostrarDialogSaida = true) }
    }

    /** Fecha o diálogo de saída sem encerrar o app */
    fun ocultarDialogSaida() {
        _uiState.update { it.copy(mostrarDialogSaida = false) }
    }

    /** Limpa a mensagem de erro do Snackbar após ela ter sido exibida */
    fun limparErroLancarApp() {
        _uiState.update { it.copy(erroLancarApp = null) }
    }
}
