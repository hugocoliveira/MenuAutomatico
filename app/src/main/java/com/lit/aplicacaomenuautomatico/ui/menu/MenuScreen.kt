package com.lit.aplicacaomenuautomatico.ui.menu

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Android / Activity (back press)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.activity.ComponentActivity       // Activity base — necessária para acessar o dispatcher
import androidx.activity.OnBackPressedCallback   // callback registrado no dispatcher da Activity
import androidx.activity.compose.LocalActivity   // provê a Activity atual dentro de um Composable

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose (animação)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.animation.AnimatedContent      // anima a transição de conteúdo quando a key muda
import androidx.compose.animation.core.tween           // curva de animação com duração em ms
import androidx.compose.animation.fadeIn               // animação de entrada por opacidade
import androidx.compose.animation.fadeOut              // animação de saída por opacidade
import androidx.compose.animation.slideInHorizontally  // slide de entrada (da direita)
import androidx.compose.animation.slideOutHorizontally // slide de saída (para a esquerda)
import androidx.compose.animation.togetherWith         // combina animação de entrada com saída

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose (layout)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.foundation.layout.Arrangement  // define espaçamento entre filhos do LazyColumn
import androidx.compose.foundation.layout.Box          // container para empilhar o spinner sobre o conteúdo
import androidx.compose.foundation.layout.PaddingValues // padding interno do LazyColumn
import androidx.compose.foundation.layout.fillMaxSize  // modifier: ocupa 100% da área disponível
import androidx.compose.foundation.layout.fillMaxWidth // modifier: ocupa 100% da largura disponível
import androidx.compose.foundation.layout.padding      // modifier: aplica margens internas (do Scaffold)
import androidx.compose.foundation.lazy.LazyColumn     // lista virtualizada — só renderiza itens visíveis
import androidx.compose.foundation.lazy.items          // DSL do LazyColumn para listas tipadas

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material Icons
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // seta voltar (espelhada em RTL)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Material3 (componentes de tela)
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.material3.CircularProgressIndicator  // spinner durante carregamento do menu
import androidx.compose.material3.ExperimentalMaterial3Api   // API experimental (TopAppBar)
import androidx.compose.material3.Icon                       // exibe ícones vetoriais Material
import androidx.compose.material3.IconButton                 // botão de ícone (botão Voltar da TopAppBar)
import androidx.compose.material3.MaterialTheme              // acessa tipografia e cores do tema
import androidx.compose.material3.Scaffold                   // estrutura de tela: TopAppBar + conteúdo + Snackbar
import androidx.compose.material3.SnackbarHost               // host do Snackbar dentro do Scaffold
import androidx.compose.material3.SnackbarHostState          // estado do Snackbar (texto e visibilidade)
import androidx.compose.material3.Text                       // exibe textos (título da TopAppBar)
import androidx.compose.material3.TopAppBar                  // barra superior com título e ícone
import androidx.compose.material3.TopAppBarDefaults          // cores padrão da TopAppBar

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose runtime e estado
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.runtime.Composable         // marca a função como Composable
import androidx.compose.runtime.DisposableEffect   // efeito com cleanup — registra/remove o back callback
import androidx.compose.runtime.LaunchedEffect     // efeito de corrotina — exibe Snackbar ao detectar erro
import androidx.compose.runtime.getValue           // delegate para leitura de State<T>
import androidx.compose.runtime.remember            // memoriza instâncias entre recomposições

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — utilitários de UI
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.ui.Alignment               // alinha o spinner ao centro do Box
import androidx.compose.ui.Modifier                // encadeia modificadores de layout e aparência
import androidx.compose.ui.platform.LocalContext   // provê o Context atual (passado ao ViewModel)
import androidx.compose.ui.unit.dp                 // unidade densidade-independente (espaçamentos)
import androidx.lifecycle.compose.collectAsStateWithLifecycle // coleta StateFlow respeitando ciclo de vida

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — componentes e tema do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.ui.menu.components.ExitConfirmDialog // diálogo de confirmação de saída
import com.lit.aplicacaomenuautomatico.ui.menu.components.MenuItemCard      // card de cada item da lista
import com.lit.aplicacaomenuautomatico.ui.theme.OnPrimary                   // branco — texto sobre fundo Primary
import com.lit.aplicacaomenuautomatico.ui.theme.Primary                     // azul #051FC7 — cor da TopAppBar

// ═════════════════════════════════════════════════════════════════════════════
// COMPOSABLE: MenuScreen
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Tela principal de menu do app.
 * Reutilizada para todos os níveis da hierarquia (MAIN, submenus, etc.) —
 * a navegação entre níveis é gerenciada internamente pelo [MenuViewModel].
 *
 * Estrutura visual (Scaffold):
 *  - TopAppBar azul com título do menu atual e botão Voltar (exceto no MAIN)
 *  - Área de conteúdo: spinner de carregamento ou lista de itens animada
 *  - SnackbarHost para mensagens de erro ao lançar apps externos
 *
 * Back press:
 *  Registrado diretamente no dispatcher da Activity (não via LocalOnBackPressedDispatcherOwner)
 *  para garantir interceptação correta mesmo com o NavHost ativo.
 *
 * @param viewModel ViewModel com o estado e a lógica de navegação do menu
 */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar ainda é experimental no Material3
@Suppress("UnusedContentLambdaTargetStateParameter") // suprime aviso do AnimatedContent
@Composable
fun MenuScreen(viewModel: MenuViewModel) {

    // ─── ESTADO E UTILITÁRIOS ─────────────────────────────────────────────────
    // Coleta o StateFlow do ViewModel respeitando o ciclo de vida (para em background)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context          = LocalContext.current            // contexto passado ao ViewModel para lançar apps
    val snackbarHostState = remember { SnackbarHostState() } // estado do Snackbar (texto, fila de mensagens)

    // ─── EFEITO: SNACKBAR DE ERRO ─────────────────────────────────────────────
    // Observa o campo erroLancarApp — quando não-null, exibe o Snackbar e limpa o estado.
    LaunchedEffect(uiState.erroLancarApp) {
        uiState.erroLancarApp?.let { erro ->
            snackbarHostState.showSnackbar(erro) // showSnackbar é suspend — aguarda a animação
            viewModel.limparErroLancarApp()       // limpa após exibir para não reaparecer
        }
    }

    // ─── INTERCEPTAÇÃO DO BOTÃO VOLTAR ────────────────────────────────────────
    // Registrado no dispatcher da Activity diretamente — evita conflito com o NavHost
    // que pode escopar o dispatcher internamente em versões mais novas do Navigation Compose.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        val callback = object : OnBackPressedCallback(true) { // true = callback sempre ativo
            override fun handleOnBackPressed() {
                if (!viewModel.voltarMenuAnterior()) {
                    // Back stack interno vazio — está no MAIN, exibe diálogo de confirmação
                    viewModel.mostrarDialogSaida()
                }
                // Se voltarMenuAnterior() retornou true, já navegou para o pai — nada mais a fazer
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(callback) // registra o interceptor
        onDispose { callback.remove() }                          // remove ao sair do Composable
    }

    // ─── DIÁLOGO DE CONFIRMAÇÃO DE SAÍDA ─────────────────────────────────────
    // Exibido quando o usuário pressiona Voltar no menu MAIN
    if (uiState.mostrarDialogSaida) {
        ExitConfirmDialog(
            onConfirmar = { activity?.finish() },       // encerra o processo do app
            onCancelar  = { viewModel.ocultarDialogSaida() } // fecha o diálogo e volta ao menu
        )
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ESTRUTURA PRINCIPAL: Scaffold
    // Scaffold organiza: TopAppBar (topo) + conteúdo (centro) + SnackbarHost (baixo)
    // ═════════════════════════════════════════════════════════════════════════
    Scaffold(
        // ─── TOP APP BAR ──────────────────────────────────────────────────────
        topBar = {
            TopAppBar(
                title = {
                    // Título: nome do menu atual (SText do item pai ou "Menu Principal" no MAIN)
                    Text(
                        text  = uiState.tituloAtual,
                        color = OnPrimary              // branco — contraste sobre fundo Primary azul
                    )
                },
                // Botão Voltar visível apenas quando não está no menu raiz (MAIN)
                navigationIcon = {
                    if (uiState.menuAtual != "MAIN") { // MAIN não tem pai — sem botão Voltar
                        IconButton(
                            onClick = {
                                val voltou = viewModel.voltarMenuAnterior()
                                if (!voltou) viewModel.mostrarDialogSaida() // segurança: nunca deveria ocorrer
                            }
                        ) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Filled.ArrowBack, // espelha em RTL
                                contentDescription = "Voltar ao menu anterior",
                                tint               = OnPrimary // ícone branco sobre TopAppBar azul
                            )
                        }
                    }
                },
                // Define a cor de fundo da TopAppBar como Primary (azul #051FC7)
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Primary,   // fundo azul da TopAppBar
                    titleContentColor = OnPrimary  // cor padrão do conteúdo (título e ícones)
                )
            )
        },
        // ─── SNACKBAR HOST ────────────────────────────────────────────────────
        snackbarHost = { SnackbarHost(snackbarHostState) } // posiciona o Snackbar no rodapé do Scaffold

    ) { innerPadding ->
        // innerPadding = espaço reservado pela TopAppBar e pelo Snackbar — deve ser aplicado ao conteúdo

        Box(
            modifier = Modifier
                .fillMaxSize()           // ocupa toda a área de conteúdo do Scaffold
                .padding(innerPadding)   // respeita o espaço da TopAppBar e do Snackbar
        ) {
            if (uiState.carregando) {
                // ─── INDICADOR DE CARREGAMENTO ────────────────────────────────
                // Exibido enquanto o Flow do banco ainda não emitiu a primeira lista
                CircularProgressIndicator(
                    color    = Primary,                     // spinner azul primário
                    modifier = Modifier.align(Alignment.Center) // centralizado na área de conteúdo
                )
            } else {
                // ─── LISTA ANIMADA DE MENUS ───────────────────────────────────
                // AnimatedContent anima a troca de conteúdo quando o menu muda.
                // targetState = uiState.menuAtual — muda ao navegar entre menus.
                AnimatedContent(
                    targetState  = uiState.menuAtual, // muda ao navegar; dispara a animação
                    transitionSpec = {
                        // Avançando (entrando no submenu): slide da direita para a esquerda
                        // Voltando: o back stack já trocou o estado — a direção é a mesma
                        (slideInHorizontally(
                            animationSpec  = tween(300),          // 300ms entrada
                            initialOffsetX = { width -> width }   // começa fora da tela à direita
                        ) + fadeIn(tween(300))) togetherWith      // combina slide + fade de entrada
                        (slideOutHorizontally(
                            animationSpec  = tween(300),
                            targetOffsetX  = { width -> -width }  // sai para a esquerda
                        ) + fadeOut(tween(150)))                   // fade de saída mais rápido (150ms)
                    },
                    label = "menu_transition" // label para ferramentas de diagnóstico de animação
                ) { _ ->
                    // ─── LAZY COLUMN: lista de itens ──────────────────────────
                    // LazyColumn virtualiza a lista — só renderiza itens visíveis na tela.
                    // Crítico para menus com muitos itens (sem LazyColumn, Column com forEach
                    // renderizaria tudo de uma vez causando jank).
                    LazyColumn(
                        modifier         = Modifier.fillMaxWidth(), // ocupa toda a largura
                        contentPadding   = PaddingValues(horizontal = 8.dp, vertical = 6.dp), // margem interna
                        verticalArrangement = Arrangement.spacedBy(6.dp) // 6dp entre cada card
                    ) {
                        // key garante que o Compose identifique corretamente cada item ao animar
                        // e não reutilize um card do item errado durante a transição
                        items(
                            items = uiState.itens,
                            key   = { item -> "${item.mmenu}_${item.sequence}" } // chave única por item
                        ) { item ->
                            MenuItemCard(
                                item    = item,
                                onClick = {
                                    if (item.isSubmenu) {
                                        // Type = "1": navega para o submenu filho
                                        viewModel.navegarParaSubmenu(item)
                                    } else {
                                        // Type = "2": lança o app externo instalado no dispositivo
                                        viewModel.lancarAppExterno(context, item)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
