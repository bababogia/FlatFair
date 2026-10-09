package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.ui.components.StatItem
import com.example.flatfair.ui.viewmodel.ChoresViewModel
import com.example.flatfair.ui.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoommateProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    choresViewModel: ChoresViewModel = hiltViewModel()
) {
    val roommate by userViewModel.getUserById(userId).collectAsState(initial = null)
    val stats by userViewModel.getStatsForUser(userId).collectAsState(initial = null)
    val completedChores by choresViewModel.completedChores.collectAsState()
    
    val roommateChores = remember(userId, completedChores) {
        completedChores.filter { it.assignedUserId == userId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roommate Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = roommate?.name ?: "Loading...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Chores", value = stats?.choresCompleted?.toString() ?: "0")
                    StatItem(label = "Earned", value = stats?.totalCreditsEarned?.toString() ?: "0")
                    StatItem(label = "Balance", value = roommate?.creditBalance?.toString() ?: "0")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Recent Completions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (roommateChores.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No recent chores visible.", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(roommateChores) { chore ->
                        ListItem(
                            headlineContent = { Text(chore.title) },
                            supportingContent = { Text(chore.category) },
                            trailingContent = {
                                Text("+${chore.totalReward}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            },
                            leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
