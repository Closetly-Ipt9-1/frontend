package com.m306.closetly.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Explore : BottomNavItem(
        route = Routes.EXPLORE,
        title = "Explore",
        icon = Icons.Default.Home
    )

    object FitCreator : BottomNavItem(
        route = Routes.FIT_CREATOR,
        title = "Fit Creator",
        icon = Icons.Default.AddCircle
    )

    object Closet : BottomNavItem(
        route = Routes.CLOSET,
        title = "Closet",
        icon = Icons.Default.Search
    )

    object Profile : BottomNavItem(
        route = Routes.PROFILE,
        title = "Profile",
        icon = Icons.Default.Person
    )
}