package com.devg2studios.seguromovilultima.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devg2studios.seguromovilultima.ui.debug.DebugToolsScreen
import com.devg2studios.seguromovilultima.ui.views.home.HomeScreen
import com.devg2studios.seguromovilultima.ui.views.auth.login.LoginScreen
import com.devg2studios.seguromovilultima.ui.views.apagadoFalso.FakeShutdownSettingsScreen
import com.devg2studios.seguromovilultima.ui.views.auth.register.RegisterScreen
import com.devg2studios.seguromovilultima.ui.views.codigoIncorrecto.FailedAttemptSettingsScreen
import com.devg2studios.seguromovilultima.ui.views.permisos.SetupScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val FAILED_ATTEMPT_SETTINGS = "failed_attempt_settings"
    const val SETUP = "setup"
    const val DEBUG_TOOLS = "debug_tools"
    const val FAKE_SHUTDOWN_SETTINGS = "fake_shutdown_settings"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startViewModel: AppStartViewModel = hiltViewModel()
) {
    val startDestination = if (startViewModel.isLoggedIn()) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.FAILED_ATTEMPT_SETTINGS) {
            FailedAttemptSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETUP) {
            SetupScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DEBUG_TOOLS) {
            DebugToolsScreen(
                onBack = { navController.popBackStack() },
                onGoToPermissions = { navController.navigate(Routes.SETUP) }
            )
        }
        composable(Routes.FAKE_SHUTDOWN_SETTINGS) {
            FakeShutdownSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onGoToLogin = { navController.popBackStack() }
            )
        }
    }
}