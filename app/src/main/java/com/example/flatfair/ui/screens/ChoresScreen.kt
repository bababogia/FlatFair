package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.flatfair.ui.theme.FlatFairTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreen() {
    var bidValue by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bidding & Chores") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add chore */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Chore")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Available for Bidding",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Set your credits for tasks you want to avoid.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mock Chore Bidding Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Clean the Bathroom", style = MaterialTheme.typography.titleLarge)
                        Text("Difficulty: Hard | Due Sunday", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Your Bid: ${bidValue.toInt()} Credits")
                        Slider(
                            value = bidValue,
                            onValueChange = { bidValue = it },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                        
                        Button(
                            onClick = { /* Save bid */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Place Bid")
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Assigned to You", style = MaterialTheme.typography.titleLarge)
            }

            items(listOf("Take out Trash", "Vacuum Stairs")) { assignedChore ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(assignedChore, style = MaterialTheme.typography.titleMedium)
                            Text("Complete by Tomorrow", style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(checked = false, onCheckedChange = {})
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChoresScreenPreview() {
    FlatFairTheme {
        ChoresScreen()
    }
}
