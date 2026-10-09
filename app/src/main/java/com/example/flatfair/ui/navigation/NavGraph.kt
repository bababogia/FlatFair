package com.example.flatfair.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flatfair.ui.screens.*
import com.example.flatfair.ui.viewmodel.AuthViewModel

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object HouseholdSelection : Screen("household_selection")
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Chores : Screen("chores", "Chores", Icons.AutoMirrored.Filled.List)
    data object Expenses : Screen("expenses", "Expenses", Icons.Default.ShoppingCart)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    data object HouseholdSettings : Screen("household_settings")
    data object TransactionHistory : Screen("transaction_history")
    data object RecentActivity : Screen("recent_activity")
    data object EditProfile : Screen("edit_profile")
}

@Composable
fun AppNavigation(viewModel: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val startDestination = if (!viewModel.isLoggedIn) {
        Screen.Login.route
    } else if (currentUser?.currentHouseholdId == null) {
        Screen.HouseholdSelection.route
    } else {
        Screen.Dashboard.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.HouseholdSelection.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.HouseholdSelection.route) {
            HouseholdSelectionScreen(
                onSelectionSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.HouseholdSelection.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            MainContainer(navController) {
                DashboardScreen(
                    onNavigateToChores = { navController.navigate(Screen.Chores.route) },
                    onNavigateToHistory = { navController.navigate(Screen.TransactionHistory.route) },
                    onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                    onNavigateToActivity = { navController.navigate(Screen.RecentActivity.route) }
                )
            }
        }
        composable(Screen.Chores.route) {
            MainContainer(navController) {
                ChoresScreen()
            }
        }
        composable(Screen.Expenses.route) {
            MainContainer(navController) {
                ExpensesScreen()
            }
        }
        composable(Screen.Profile.route) {
            MainContainer(navController) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    },
                    onNavigateToHouseholdSettings = {
                        navController.navigate(Screen.HouseholdSettings.route)
                    },
                    onNavigateToEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    }
                )
            }
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.HouseholdSettings.route) {
            HouseholdSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.RecentActivity.route) {
            ActivityLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainContainer(
    navController: androidx.navigation.NavHostController,
    content: @Composable () -> Unit
) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Chores,
        Screen.Expenses,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                        label = { screen.label?.let { Text(it) } },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
