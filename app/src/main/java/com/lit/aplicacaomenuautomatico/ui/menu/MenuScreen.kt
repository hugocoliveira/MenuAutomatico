package com.lit.aplicacaomenuautomatico.ui.menu

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lit.aplicacaomenuautomatico.ui.menu.components.ExitConfirmDialog
import com.lit.aplicacaomenuautomatico.ui.menu.components.MenuItemCard
import com.lit.aplicacaomenuautomatico.ui.theme.OnPrimary
import com.lit.aplicacaomenuautomatico.ui.theme.Primary

/**
 * Tela principal de menu do app.
 * Reutilizada para todos os níveis da hierarquia de menus (MAIN, submenus, etc.).
 * A navegação entre níveis é gerenciada pelo [MenuViewModel] com um back stack interno.
 *
 * Funcionalidades:
 *  - TopAppBar azul com título do menu atual e botão Voltar (exceto no MAIN)
 *  - Lista reativa de itens via LazyColumn (performance para listas grandes)
 *  - Animação de slide ao navegar entre menus
 *  - BackHandler intercepta o botão Voltar antes do Navigation Compose
 *  - Snackbar para erros ao lançar apps externos
 *  - Diálogo de confirmação de saída no menu raiz
 *
 * @param viewModel ViewModel com o estado e a lógica de navegação
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UnusedContentLambdaTargetStateParameter")
@Composable
fun MenuScreen(viewModel: MenuViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Exibe o Snackbar quando há erro ao lançar app externo
    LaunchedEffect(uiState.erroLancarApp) {
        uiState.erroLancarApp?.let { erro ->
            snackbarHostState.showSnackbar(erro)
            viewModel.limparErroLancarApp()
        }
    }

    // Registra o callback diretamente no dispatcher da Activity, sem usar
    // LocalOnBackPressedDispatcherOwner (que o NavHost pode escopar internamente em
    // versões mais novas do Navigation Compose, impedindo a interceptação correta).
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.voltarMenuAnterior()) {
                    viewModel.mostrarDialogSaida()
                }
            }
        }
        activity?.onBackPressedDispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    // Diálogo de confirmação de saída (exibido ao pressionar Voltar no MAIN)
    if (uiState.mostrarDialogSaida) {
        ExitConfirmDialog(
            onConfirmar = {
                // Encerra o app ao confirmar saída
                activity?.finish()
            },
            onCancelar = {
                viewModel.ocultarDialogSaida()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.tituloAtual,
                        color = OnPrimary
                    )
                },
                // Botão Voltar visível em todos os níveis exceto no MAIN (raiz)
                navigationIcon = {
                    if (uiState.menuAtual != "MAIN") {
                        IconButton(
                            onClick = {
                                val voltou = viewModel.voltarMenuAnterior()
                                if (!voltou) viewModel.mostrarDialogSaida()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar ao menu anterior",
                                tint = OnPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.carregando) {
                // Indicador de carregamento centralizado enquanto os dados são lidos do SQLite
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // AnimatedContent anima a transição entre menus com slide horizontal
                // A key é o código do menu — único por nível, muda ao navegar
                AnimatedContent(
                    targetState = uiState.menuAtual,
                    transitionSpec = {
                        // Avançando: slide da direita para a esquerda
                        // Voltando: slide da esquerda para a direita (controlado pelo back stack)
                        (slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { width -> width }
                        ) + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { width -> -width }
                        ) + fadeOut(tween(150)))
                    },
                    label = "menu_transition"
                ) { _ ->
                    // LazyColumn para performance — não renderiza itens fora da tela
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // key garante que o Compose não reutilize incorretamente itens ao animar
                        items(
                            items = uiState.itens,
                            key = { item -> "${item.mmenu}_${item.sequence}" }
                        ) { item ->
                            MenuItemCard(
                                item = item,
                                onClick = {
                                    if (item.isSubmenu) {
                                        // Type=1: navega para o submenu filho
                                        viewModel.navegarParaSubmenu(item)
                                    } else {
                                        // Type=2: lança o app externo instalado no dispositivo
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
