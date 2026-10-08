package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.flatfair.ui.theme.FlatFairTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToChores: () -> Unit = {}
) {
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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Dorm Credits", style = MaterialTheme.typography.titleMedium)
                        Text("150", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Your Active Chores", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onNavigateToChores) {
                        Text("View All")
                    }
                }
            }

            items(listOf("Clean Kitchen", "Vacuum Living Room")) { chore ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(chore) },
                        supportingContent = { Text("Due: Friday") },
                        trailingContent = { 
                            SuggestionChip(onClick = {}, label = { Text("10 pts") })
                        }
                    )
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
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    FlatFairTheme {
        DashboardScreen()
    }
}
