package com.m306.closetly.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.m306.closetly.auth.ui.LoginScreen
import com.m306.closetly.auth.ui.RegisterScreen
import com.m306.closetly.closet.ui.ClosetScreen
import com.m306.closetly.explore.ui.ExploreScreen
import com.m306.closetly.fitcreator.ui.FitCreatorScreen
import com.m306.closetly.profile.ui.ProfileScreen
import com.m306.closetly.profile.ui.EditProfileScreen
import com.m306.closetly.auth.func.AuthManager
import com.m306.closetly.profile.ui.SavedOutfitsScreen


@Composable
fun ClosetlyApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomItems = listOf(
        BottomNavItem.Explore,
        BottomNavItem.FitCreator,
        BottomNavItem.Closet,
        BottomNavItem.Profile
    )

    val showBottomBar = currentDestination?.route in setOf(
        Routes.EXPLORE,
        Routes.FIT_CREATOR,
        Routes.CLOSET,
        Routes.PROFILE
    )
    val startDestination =
        if (AuthManager.getCurrentUserId() != null) Routes.EXPLORE else Routes.LOGIN


    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == item.route } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                androidx.compose.material3.Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(item.title)
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.EXPLORE) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Routes.REGISTER) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRegisterSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.EXPLORE) {
                ExploreScreen()
            }

            composable(Routes.FIT_CREATOR) {
                FitCreatorScreen()
            }

            composable(Routes.CLOSET) {
                ClosetScreen()
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditClick = {
                        navController.navigate(Routes.EDIT_PROFILE) {
                            launchSingleTop = true
                        }
                    },
                    onSavedOutfitsClick = {
                        navController.navigate(Routes.SAVED_OUTFITS) {
                            launchSingleTop = true
                        }
                    },
                    onLogoutClick = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onSaveSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.SAVED_OUTFITS) {
                SavedOutfitsScreen()
            }
        }
    }
}