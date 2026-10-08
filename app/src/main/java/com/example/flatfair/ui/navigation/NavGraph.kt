package com.example.flatfair.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flatfair.ui.screens.DashboardScreen
import com.example.flatfair.ui.screens.ChoresScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Chores : Screen("chores")
}

@Composable
fun FlatFairNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToChores = { navController.navigate(Screen.Chores.route) }
            )
        }
        composable(Screen.Chores.route) {
            ChoresScreen()
        }
    }
}
