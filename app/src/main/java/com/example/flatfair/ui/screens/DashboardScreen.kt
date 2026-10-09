package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.ui.viewmodel.ActivityViewModel
import com.example.flatfair.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToChores: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    activityViewModel: ActivityViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val activeChores by viewModel.activeChores.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val pendingExpenses by viewModel.pendingExpensesCount.collectAsState()
    val pendingVerifications by viewModel.pendingVerificationsCount.collectAsState()
    val availableBids by viewModel.availableBidsCount.collectAsState()
    val recentActivities by activityViewModel.recentActivities.collectAsState()

    val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("FlatFair") },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToHistory,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Dorm Credits", style = MaterialTheme.typography.titleMedium)
                        Text("${user?.creditBalance ?: 0}", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            "Tap to view history",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Alerts Section
            if (pendingVerifications > 0 || availableBids > 0 || pendingExpenses > 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pendingVerifications > 0) {
                            AlertCard(
                                title = "Pending Verifications",
                                text = "There are $pendingVerifications chores waiting for your approval",
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                onClick = onNavigateToChores
                            )
                        }
                        if (availableBids > 0) {
                            AlertCard(
                                title = "New Bidding Opportunities",
                                text = "$availableBids chores are available for bidding",
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = onNavigateToChores
                            )
                        }
                        if (pendingExpenses > 0) {
                            AlertCard(
                                title = "Unsettled Expenses",
                                text = "You have $pendingExpenses expenses to settle",
                                color = MaterialTheme.colorScheme.errorContainer,
                                onClick = onNavigateToExpenses
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Active Chores", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onNavigateToChores) {
                        Text("View All")
                    }
                }
            }

            if (activeChores.isEmpty()) {
                item {
                    Text(
                        "No chores assigned to you yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(activeChores) { chore ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(chore.title) },
                            supportingContent = { Text("Due: ${chore.deadline ?: "N/A"}") },
                            trailingContent = { 
                                SuggestionChip(onClick = {}, label = { Text("${chore.baseReward} pts") })
                            }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onNavigateToChores,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Open Bidding Pool")
                }
            }

            if (recentActivities.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onNavigateToActivity) {
                            Text("More")
                        }
                    }
                }
                
                items(recentActivities.take(3)) { activity ->
                    ListItem(
                        headlineContent = { Text("${activity.userName} ${activity.action}") },
                        supportingContent = { Text(activity.targetName) },
                        trailingContent = { Text(dateFormatter.format(Date(activity.timestamp)), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Household Leaderboard", style = MaterialTheme.typography.titleLarge)
            }

            itemsIndexed(leaderboard) { index, member ->
                val isMe = member.id == user?.id
                ListItem(
                    headlineContent = { 
                        Text(
                            text = if (isMe) "${member.name} (You)" else member.name,
                            fontWeight = if (isMe) FontWeight.Bold else null
                        ) 
                    },
                    supportingContent = { Text("${member.creditBalance} credits") },
                    leadingContent = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp)) {
                            if (index == 0) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                            } else {
                                Text("#${index + 1}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    },
                    colors = if (isMe) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else ListItemDefaults.colors()
                )
                if (index < leaderboard.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AlertCard(
    title: String,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(title) },
            supportingContent = { Text(text) },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
            trailingContent = { Text("View", fontWeight = FontWeight.Bold) }
        )
    }
}
