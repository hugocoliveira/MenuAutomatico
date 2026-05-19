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
    /** Código interno do grupo de menu atual (ex.: "MAIN", "INB00") — usado para carregar dados */
    val menuAtual: String = "MAIN",
    /** Título de exibição na TopAppBar — SText do item pai que levou a este nível */
    val tituloAtual: String = "Menu Principal",
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
     * Pilha de menus visitados — cada entrada guarda o par (mmenu, título) para restaurar
     * tanto o conteúdo quanto o título correto ao pressionar Voltar.
     * Começa vazia pois o MAIN é o ponto de entrada e não tem pai.
     * Exemplo após navegar MAIN → INB00 → GR00: [("MAIN","Menu Principal"), ("INB00","Inbound")]
     *
     * Usar ArrayDeque (não mutableListOf) porque removeLast() é método concreto do
     * ArrayDeque do Kotlin e funciona em qualquer API Android. Em Kotlin 2.x,
     * mutableListOf().removeLast() é compilado como chamada de interface em
     * java.util.List, que só existe a partir do Java 21 / Android API 35.
     */
    private val backStack = ArrayDeque<Pair<String, String>>()

    /** Job da coleta do Flow atual — cancelado ao navegar para outro menu */
    private var jobColeta: Job? = null

    init {
        // Carrega o menu raiz MAIN ao inicializar o ViewModel
        carregarMenu("MAIN", "Menu Principal")
    }

    /**
     * Carrega os itens de um grupo de menu do banco local via Flow reativo.
     * Cancela qualquer coleta anterior antes de iniciar a nova para evitar vazamento de recursos.
     *
     * @param mmenu Código do grupo de menu a carregar (ex.: "MAIN", "INB00")
     * @param titulo Título de exibição na TopAppBar — SText do item pai ou "Menu Principal" para o MAIN
     */
    fun carregarMenu(mmenu: String, titulo: String = "Menu Principal") {
        // Cancela o Flow anterior para não acumular coletas em background
        jobColeta?.cancel()

        _uiState.update { it.copy(carregando = true, menuAtual = mmenu, tituloAtual = titulo) }

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
        // Empilha o menu atual (código + título) antes de navegar para o filho
        backStack.add(_uiState.value.menuAtual to _uiState.value.tituloAtual)
        // O submenu filho é identificado pelo Transacao do item pai; o SText vira o título
        carregarMenu(item.transacao, item.sText)
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
        try {
            val cn = android.content.ComponentName(
                item.componente,
                "${item.componente}.MainActivity"
            )
            val intent = Intent().apply {
                component = cn
                // Código da transação SAP para o app externo processar
                putExtra("transacao", item.transacao)
                // Extra de segurança: sub-apps fecham imediatamente sem este extra
                putExtra("origem", "com.lit.aplicacaomenuautomatico")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            _uiState.update {
                it.copy(erroLancarApp = "Aplicativo '${item.componente}' não encontrado no dispositivo.")
            }
        }
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
        if (backStack.isEmpty()) {
            // Já está no menu raiz — não há para onde voltar dentro dos menus
            return false
        }
        // Desempilha o menu anterior e restaura código + título
        val (mmenuAnterior, tituloAnterior) = backStack.removeLast()
        carregarMenu(mmenuAnterior, tituloAnterior)
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
