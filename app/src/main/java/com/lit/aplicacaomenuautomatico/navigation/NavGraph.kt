package com.lit.aplicacaomenuautomatico.navigation

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.compose.runtime.Composable        // marca a função como Composable (UI declarativa)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Hilt Navigation Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.hilt.navigation.compose.hiltViewModel // cria/obtém ViewModel injetado pelo Hilt no escopo da rota

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — Navigation Compose
// ─────────────────────────────────────────────────────────────────────────────
import androidx.navigation.NavHostController             // controlador que gerencia o back stack do Navigation
import androidx.navigation.compose.NavHost              // container que hospeda as rotas e faz a composição
import androidx.navigation.compose.composable           // define um destino de navegação no NavHost

// ─────────────────────────────────────────────────────────────────────────────
// IMPORTS — telas do projeto
// ─────────────────────────────────────────────────────────────────────────────
import com.lit.aplicacaomenuautomatico.ui.login.LoginScreen    // tela de autenticação
import com.lit.aplicacaomenuautomatico.ui.login.LoginViewModel  // ViewModel da tela de login
import com.lit.aplicacaomenuautomatico.ui.menu.MenuScreen       // tela de menu dinâmico
import com.lit.aplicacaomenuautomatico.ui.menu.MenuViewModel    // ViewModel da tela de menu

// ═════════════════════════════════════════════════════════════════════════════
// ROTAS: sealed class Rota
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Define as rotas de navegação do app como uma sealed class tipada.
 * Evita strings mágicas espalhadas pelo código — toda referência a rotas usa [Rota.X.caminho].
 *
 * O app tem apenas duas telas:
 *  - [Login] — autenticação SAP, sempre a primeira tela em condições normais
 *  - [Menu]  — menu dinâmico multinível; a navegação entre submenus é interna ao [MenuViewModel]
 *
 * Nota: a navegação entre submenus não cria rotas separadas no NavHost —
 * é gerenciada por um back stack próprio no [MenuViewModel], o que simplifica
 * as animações e evita overhead do Navigation Compose para cada nível de menu.
 *
 * @param caminho String usada como identificador da rota no [NavHost]
 */
sealed class Rota(val caminho: String) {
    /** Tela de autenticação — startDestination quando há rede ou banco vazio */
    object Login : Rota("login")

    /** Tela de menu dinâmico — startDestination no modo offline (banco com dados, sem rede) */
    object Menu : Rota("menu")
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPOSABLE: NavGraph
// ═════════════════════════════════════════════════════════════════════════════

/**
 * NavHost principal do app.
 * Associa cada rota ao seu Composable e gerencia a transição Login → Menu após autenticação.
 *
 * Cada rota tem seu próprio ViewModel criado via [hiltViewModel] — o ViewModel
 * é escopado ao back stack entry da rota e descartado quando a rota é removida do back stack.
 *
 * @param navController Controlador de navegação criado e retido pelo [MainActivity]
 * @param startDestination Rota inicial determinada pela lógica de primeiro acesso
 *                         (banco vazio → Login | offline com cache → Menu)
 */
@Composable
fun NavGraph(
    navController: NavHostController, // gerencia o back stack de rotas e as transições
    startDestination: String          // "login" ou "menu" — definido pelo MainActivity
) {
    NavHost(
        navController    = navController,    // controlador que executa navigate() e popBackStack()
        startDestination = startDestination  // rota exibida ao abrir o app
    ) {

        // ─── DESTINO: Login ───────────────────────────────────────────────────
        composable(Rota.Login.caminho) {
            val viewModel: LoginViewModel = hiltViewModel() // ViewModel escopado a esta entrada do back stack

            LoginScreen(
                viewModel     = viewModel,
                onLoginSucesso = {
                    // Navega para o Menu e remove o Login do back stack com popUpTo + inclusive.
                    // Sem isso, pressionar Voltar no Menu retornaria ao Login — comportamento indesejado.
                    navController.navigate(Rota.Menu.caminho) {
                        popUpTo(Rota.Login.caminho) { inclusive = true } // remove Login do back stack
                    }
                }
            )
        }

        // ─── DESTINO: Menu ────────────────────────────────────────────────────
        composable(Rota.Menu.caminho) {
            val viewModel: MenuViewModel = hiltViewModel() // ViewModel escopado a esta entrada do back stack

            MenuScreen(viewModel = viewModel)
            // Sem onBack aqui — o back handler é registrado dentro do MenuScreen
            // diretamente no dispatcher da Activity para interceptar corretamente.
        }
    }
}
