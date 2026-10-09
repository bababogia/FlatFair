package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.Reward
import com.example.flatfair.ui.viewmodel.RewardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    viewModel: RewardViewModel = hiltViewModel()
) {
    val rewards by viewModel.rewards.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Household Rewards") })
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reward")
                }
            }
        }
    ) { padding ->
        if (showAddDialog) {
            AddRewardDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, desc, cost ->
                    viewModel.addReward(title, desc, cost)
                    showAddDialog = false
                }
            )
        }

        Column(modifier = Modifier.padding(padding)) {
            // User Balance Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Your Balance: ${user?.creditBalance ?: 0} Credits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (rewards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No rewards available yet.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rewards) { reward ->
                        RewardCard(
                            reward = reward,
                            canAfford = (user?.creditBalance ?: 0) >= reward.cost,
                            isAdmin = isAdmin,
                            onRedeem = { viewModel.redeemReward(reward) },
                            onDelete = { viewModel.deleteReward(reward.id, reward.title) },
                            onToggleAvailability = { viewModel.toggleAvailability(reward.id, !reward.isAvailable) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardCard(
    reward: Reward,
    canAfford: Boolean,
    isAdmin: Boolean,
    onRedeem: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (!reward.isAvailable) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (reward.isAvailable) Color.Unspecified else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${reward.cost} Credits",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (canAfford && reward.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                if (isAdmin) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = reward.isAvailable,
                            onCheckedChange = { onToggleAvailability() },
                            modifier = Modifier.graphicsLayer(scaleX = 0.8f, scaleY = 0.8f)
                        )
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (reward.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reward.isAvailable) Color.Unspecified else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRedeem,
                modifier = Modifier.align(Alignment.End),
                enabled = canAfford && reward.isAvailable
            ) {
                Text(if (reward.isAvailable) "Redeem" else "Unavailable")
            }
        }
    }
}

@Composable
fun AddRewardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Household Reward") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create something fun for your roommates to spend their hard-earned credits on!")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reward Name (e.g. Cinema Ticket)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) cost = it },
                    label = { Text("Cost (Credits)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, cost.toIntOrNull() ?: 0) },
                enabled = title.isNotBlank() && cost.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
