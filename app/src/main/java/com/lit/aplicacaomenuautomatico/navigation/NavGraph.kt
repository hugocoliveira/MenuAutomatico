package com.lit.aplicacaomenuautomatico.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lit.aplicacaomenuautomatico.ui.login.LoginScreen
import com.lit.aplicacaomenuautomatico.ui.login.LoginViewModel
import com.lit.aplicacaomenuautomatico.ui.menu.MenuScreen
import com.lit.aplicacaomenuautomatico.ui.menu.MenuViewModel

/**
 * Define as rotas de navegação do app.
 * O app tem apenas duas telas: Login e Menu.
 * A navegação entre sub-menus é gerenciada internamente pelo [MenuViewModel]
 * com um back stack próprio — não usa rotas separadas por nível de menu.
 */
sealed class Rota(val caminho: String) {
    /** Tela de autenticação — sempre a primeira tela em condições normais */
    object Login : Rota("login")

    /** Tela de menu dinâmico — exibe todos os níveis de hierarquia */
    object Menu : Rota("menu")
}

/**
 * NavHost principal do app. Define as composables associadas a cada rota e
 * gerencia a transição de Login → Menu após autenticação bem-sucedida.
 *
 * @param navController Controlador de navegação — gerenciado pelo [MainActivity]
 * @param startDestination Rota inicial determinada pela lógica de primeiro acesso e conectividade
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Rota.Login.caminho) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSucesso = {
                    // Navega para o Menu e remove o Login do back stack
                    // para que o botão Voltar no Menu não retorne ao Login
                    navController.navigate(Rota.Menu.caminho) {
                        popUpTo(Rota.Login.caminho) { inclusive = true }
                    }
                }
            )
        }

        composable(Rota.Menu.caminho) {
            val viewModel: MenuViewModel = hiltViewModel()
            MenuScreen(viewModel = viewModel)
        }
    }
}
