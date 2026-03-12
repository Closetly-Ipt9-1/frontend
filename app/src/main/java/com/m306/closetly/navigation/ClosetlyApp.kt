package com.m306.closetly.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.m306.closetly.auth.ui.LoginScreen
import com.m306.closetly.home.ui.HomeScreen
import com.m306.closetly.auth.ui.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ClosetlyApp() {

    val navController = rememberNavController()
    val user = FirebaseAuth.getInstance().currentUser
    val startdestination = if (user != null) {
        Routes.HOME
    } else {
        Routes.LOGIN
    }


    NavHost(
        navController = navController,
        startDestination = startdestination

    ) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen()
        }

        composable(Routes.REGISTER){
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN)
                },
                onRegisterSuccess = {
                    navController.navigate(Routes.LOGIN)
                },
            )
        }
    }
}