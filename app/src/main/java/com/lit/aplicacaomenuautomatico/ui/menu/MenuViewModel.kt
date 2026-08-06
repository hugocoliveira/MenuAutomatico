package com.lit.aplicacaomenuautomatico.ui.menu

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android (Intent e Context)
// ─────────────────────────────────────────────────────────────────────────────
import android.content.Context  // necessário para PackageManager e startActivity
import android.content.Intent   // cria intents para lançar apps externos

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Jetpack / ViewModel
// ─────────────────────────────────────────────────────────────────────────────
import androidx.lifecycle.ViewModel      // classe base do ViewModel — sobrevive a rotações de tela
import androidx.lifecycle.viewModelScope // escopo de corrotina ligado ao ciclo de vida do ViewModel

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — recursos do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.data.repository.MenuRepository // fonte de dados do menu (SQLite)
import com.lit.aplicacaomenuautomatico.domain.model.MenuApp           // modelo de domínio de um item de menu

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt
// ─────────────────────────────────────────────────────────────────────────────
import dagger.hilt.android.lifecycle.HiltViewModel // marca o ViewModel para injeção via Hilt
import javax.inject.Inject                          // anotação padrão JSR-330 para injeção de construtor

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Corrotinas / Flow
// ─────────────────────────────────────────────────────────────────────────────
import kotlinx.coroutines.Job                   // referência a uma corrotina — permite cancelamento
import kotlinx.coroutines.flow.MutableStateFlow // StateFlow mutável — atualizado internamente
import kotlinx.coroutines.flow.StateFlow        // StateFlow somente-leitura — exposto à UI
import kotlinx.coroutines.flow.asStateFlow      // converte MutableStateFlow em StateFlow imutável
import kotlinx.coroutines.flow.update           // atualiza o valor do StateFlow com uma função lambda
import kotlinx.coroutines.launch                // lança corrotina no escopo do ViewModel

// ═════════════════════════════════════════════════════════════════════════════
// DATA CLASS: MenuUiState
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Estado completo da tela de menu.
 * Contém todos os dados necessários para renderizar a UI sem lógica no Composable.
 * Imutável — cada mudança cria uma nova instância via copy().
 */
data class MenuUiState(
    /** Código interno do grupo de menu atualmente exibido (ex.: "MAIN", "INB00") */
    val menuAtual: String = "MAIN",

    /** Título de exibição na TopAppBar — SText do item pai ou "Menu Principal" para o MAIN */
    val tituloAtual: String = "Menu Principal",

    /** Lista de itens do menu atual — vazia durante carregamento inicial */
    val itens: List<MenuApp> = emptyList(),

    /** true enquanto os dados estão sendo lidos do banco pela primeira vez */
    val carregando: Boolean = true,

    /** true quando o usuário pressionou Voltar no MAIN — exibe ExitConfirmDialog */
    val mostrarDialogSaida: Boolean = false,

    /**
     * Mensagem de erro ao tentar lançar um app externo não instalado.
     * Exibida via Snackbar; null = sem erro pendente.
     */
    val erroLancarApp: String? = null
)

// ═════════════════════════════════════════════════════════════════════════════
// VIEWMODEL: MenuViewModel
// ═════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel da tela de menu.
 *
 * Gerencia:
 *  - Carregamento reativo dos itens do SQLite via Flow
 *  - Back stack interno de menus visitados (independente do Navigation Compose)
 *  - Lançamento de apps externos via Intent explícita
 *  - Controle do diálogo de confirmação de saída
 *  - Mensagens de erro ao lançar apps não instalados (Snackbar)
 *
 * A navegação entre submenus é gerenciada internamente por este ViewModel
 * e não cria novas rotas no NavHost — o [MenuScreen] é sempre o mesmo Composable.
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository // fonte de dados: lê do SQLite local
) : ViewModel() {

    // ─── ESTADO REATIVO ───────────────────────────────────────────────────────
    // MutableStateFlow interno — atualizado somente pelo ViewModel.
    // StateFlow público — exposto à UI via collectAsStateWithLifecycle().
    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    // ─── BACK STACK INTERNO ───────────────────────────────────────────────────
    /**
     * Pilha de menus visitados. Cada entrada é um par (mmenu, título).
     * Começa vazia — o MAIN é a raiz e não tem pai.
     *
     * Exemplo após navegar MAIN → INB00 → GR00:
     *  [("MAIN","Menu Principal"), ("INB00","Inbound")]
     *
     * Usar ArrayDeque (não mutableListOf) porque removeLast() em Kotlin 2.x
     * sobre MutableList é resolvido como chamada de interface java.util.List,
     * que só existe a partir do Java 21 / Android API 35.
     * ArrayDeque.removeLast() é sempre concreto e funciona em qualquer API.
     */
    private val backStack = ArrayDeque<Pair<String, String>>()

    /** Job da coleta do Flow atual — cancelado ao navegar para evitar coletas acumuladas */
    private var jobColeta: Job? = null

    // ─── MENU FIXO ───────────────────────────────────────────────────────────
    // Estrutura de menus definida diretamente no app, ignorando o retorno do SAP.
    // Para voltar ao menu dinâmico do SAP: restaurar o commit tagueado como
    // v1.58-menu-sap-original e publicar nova versão.
    private val menuFixo: Map<String, List<MenuApp>> = mapOf(
        "MAIN" to listOf(
            MenuApp(lgnum = "", mmenu = "MAIN", sequence = 1, componente = "", type = "1",
                transacao = "CONSULTA", text = "Consulta estoque/posição", sText = "Consulta estoque/posição"),
            MenuApp(lgnum = "", mmenu = "MAIN", sequence = 2, componente = "", type = "1",
                transacao = "TAREFAS",  text = "Tarefa de Depósito",       sText = "Tarefa de Depósito")
        ),
        "CONSULTA" to listOf(
            MenuApp(lgnum = "", mmenu = "CONSULTA", sequence = 1,
                componente = "br.com.lit.busca.material", type = "2",
                transacao = "CONSULTA_MAT", text = "Consulta Estoque", sText = "Consulta Estoque"),
            MenuApp(lgnum = "", mmenu = "CONSULTA", sequence = 2,
                componente = "br.com.lit.busca.posicao", type = "2",
                transacao = "CONSULTA_POS", text = "Consulta Posição", sText = "Consulta Posição")
        ),
        "TAREFAS" to listOf(
            MenuApp(lgnum = "", mmenu = "TAREFAS", sequence = 1,
                componente = "br.com.lit.busca.fila", type = "2",
                transacao = "TD_FILA", text = "TD por Fila", sText = "TD por Fila"),
            MenuApp(lgnum = "", mmenu = "TAREFAS", sequence = 2,
                componente = "br.com.lit.busca.uc", type = "2",
                transacao = "TD_UC", text = "TD por UC", sText = "TD por UC")
        )
    )

    // ─── INICIALIZAÇÃO ────────────────────────────────────────────────────────

    init {
        carregarMenu("MAIN", "Menu Principal")
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: carregarMenu
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Carrega os itens do [menuFixo] para o grupo solicitado.
     * Grupos não mapeados resultam em lista vazia.
     *
     * @param mmenu  Código do grupo de menu (ex.: "MAIN", "CONSULTA", "TAREFAS")
     * @param titulo Título exibido na TopAppBar
     */
    fun carregarMenu(mmenu: String, titulo: String = "Menu Principal") {
        jobColeta?.cancel()
        val itens = menuFixo[mmenu] ?: emptyList()
        _uiState.update {
            it.copy(carregando = false, menuAtual = mmenu, tituloAtual = titulo, itens = itens)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: navegarParaSubmenu
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Navega para o submenu filho de um item com Type = "1".
     *
     * Antes de navegar, empilha o menu atual no back stack para permitir
     * o retorno correto ao pressionar Voltar.
     * O código do submenu filho é o campo [MenuApp.transacao] do item tocado.
     * O título do submenu é o campo [MenuApp.sText] do item tocado.
     *
     * @param item Item de menu com type = "1" que foi tocado pelo usuário
     */
    fun navegarParaSubmenu(item: MenuApp) {
        // Salva o estado atual antes de navegar para o filho
        backStack.add(_uiState.value.menuAtual to _uiState.value.tituloAtual)
        // O filho é identificado pelo Transacao do item pai; SText vira o título da TopAppBar
        carregarMenu(item.transacao, item.sText)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: lancarAppExterno
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lança o app externo associado a um item com Type = "2" via Intent explícita.
     *
     * O app externo é identificado pelo package ID em [MenuApp.componente].
     * Se o app não estiver instalado, emite mensagem de erro via [erroLancarApp]
     * para o Snackbar da UI — nunca lança exceção não tratada.
     *
     * Extras passados ao app externo:
     *  - "transacao" → código SAP para o app processar (ex.: "TR01")
     *  - "origem"    → package ID deste app (sub-apps fecham sem este extra)
     *
     * @param context Contexto necessário para PackageManager e startActivity
     * @param item    Item de menu com type = "2" tocado pelo usuário
     */
    fun lancarAppExterno(context: Context, item: MenuApp) {
        try {
            // Intent explícita pelo ComponentName — não requer intent-filter no app alvo
            val cn = android.content.ComponentName(
                item.componente,                    // package ID do app externo
                "${item.componente}.MainActivity"   // convenção: todos os sub-apps têm MainActivity
            )
            val intent = Intent().apply {
                component = cn
                putExtra("transacao", item.transacao) // código SAP para o app processar
                putExtra("origem", "com.lit.aplicacaomenuautomatico") // token de autenticação entre apps
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // necessário pois o contexto não é uma Activity
            }
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            // App não instalado ou ComponentName incorreto — exibe mensagem no Snackbar
            _uiState.update {
                it.copy(erroLancarApp = "Aplicativo '${item.componente}' não encontrado no dispositivo.")
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÃO PÚBLICA: voltarMenuAnterior
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Retorna ao menu pai desempilhando o back stack interno.
     *
     * Não usa o back stack do Navigation Compose — a navegação entre menus
     * é gerenciada inteiramente por este ViewModel via [backStack].
     *
     * @return true = voltou para o pai com sucesso |
     *         false = já estava no MAIN (sem pai — caller deve exibir diálogo de saída)
     */
    fun voltarMenuAnterior(): Boolean {
        if (backStack.isEmpty()) {
            // Back stack vazio = já está no menu raiz — não há para onde voltar
            return false
        }
        // Desempilha e restaura o menu anterior (código + título)
        val (mmenuAnterior, tituloAnterior) = backStack.removeLast()
        carregarMenu(mmenuAnterior, tituloAnterior)
        return true
    }

    // ═════════════════════════════════════════════════════════════════════════
    // FUNÇÕES DE CONTROLE DO DIÁLOGO E SNACKBAR
    // ═════════════════════════════════════════════════════════════════════════

    /** Exibe o ExitConfirmDialog — chamado quando Voltar é pressionado no MAIN */
    fun mostrarDialogSaida() {
        _uiState.update { it.copy(mostrarDialogSaida = true) }
    }

    /** Fecha o ExitConfirmDialog sem encerrar o app (usuário clicou em "Cancelar") */
    fun ocultarDialogSaida() {
        _uiState.update { it.copy(mostrarDialogSaida = false) }
    }

    /** Limpa a mensagem de erro do Snackbar após ela ter sido exibida pela UI */
    fun limparErroLancarApp() {
        _uiState.update { it.copy(erroLancarApp = null) }
    }
}
