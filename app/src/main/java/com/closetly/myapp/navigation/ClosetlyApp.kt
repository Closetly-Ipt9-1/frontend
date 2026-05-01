package com.closetly.myapp.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.closetly.myapp.auth.ui.LoginScreen
import com.closetly.myapp.auth.ui.RegisterScreen
import com.closetly.myapp.closet.ui.ClosetScreen
import com.closetly.myapp.explore.ui.ExploreScreen
import com.closetly.myapp.fitcreator.ui.FitCreatorScreen
import com.closetly.myapp.profile.ui.ProfileScreen
import com.closetly.myapp.profile.ui.EditProfileScreen
import com.closetly.myapp.auth.func.AuthManager
import com.closetly.myapp.profile.ui.SavedOutfitsScreen
import com.closetly.myapp.profile.ui.StandardAvatarScreen
import com.closetly.myapp.profile.viewmodel.ProfileViewModel
import com.closetly.myapp.premium.ui.PremiumScreen

@Composable
fun ClosetlyApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val profileViewModel: ProfileViewModel = viewModel()

    LaunchedEffect(currentDestination?.route) {
        if (currentDestination?.route == Routes.PROFILE) {
            profileViewModel.loadAvatar()
        }
    }

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
        Routes.PROFILE,
    )
    val startDestination =
        if (AuthManager.getCurrentUserId() != null) Routes.EXPLORE else Routes.LOGIN

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets(0.dp)
                ) {
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
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

            composable(Routes.EXPLORE) { ExploreScreen() }
            composable(Routes.FIT_CREATOR) { FitCreatorScreen() }
            composable(Routes.CLOSET) { ClosetScreen() }
            composable(Routes.MANAGE_SUBSCRIPTION) { PremiumScreen() }


            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onEditClick = {
                        navController.navigate(Routes.EDIT_PROFILE) {
                            launchSingleTop = true
                        }
                    },
                    onLogoutClick = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSavedOutfitsClick = {
                        navController.navigate(Routes.SAVED_OUTFITS) {
                            launchSingleTop = true
                        }
                    },
                    onCreateAvatarClick = {
                        navController.navigate(Routes.STANDARD_AVATAR) {
                            launchSingleTop = true
                        }
                    },
                    onCustomizeAvatarClick = {
                        navController.navigate(Routes.STANDARD_AVATAR) {
                            launchSingleTop = true
                        }
                    },
                    onPremiumClick = {
                        navController.navigate(Routes.MANAGE_SUBSCRIPTION) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }

            composable(Routes.SAVED_OUTFITS) { SavedOutfitsScreen() }

            composable(Routes.STANDARD_AVATAR) {
                StandardAvatarScreen(navController)
            }
        }
    }
}
