package com.example.flatfair.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.Bid
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.ChoreFrequency
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.ui.viewmodel.ChoresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreen(
    viewModel: ChoresViewModel = hiltViewModel()
) {
    val availableChores by viewModel.availableChores.collectAsState()
    val assignedChores by viewModel.assignedChores.collectAsState()
    val choresToVerify by viewModel.choresToVerify.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    var showAddChoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Bidding & Chores") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddChoreDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Chore")
            }
        }
    ) { padding ->
        if (showAddChoreDialog) {
            AddChoreDialog(
                onDismiss = { showAddChoreDialog = false },
                onConfirm = { title, desc, diff, freq ->
                    viewModel.createChore(title, desc, diff, freq)
                    showAddChoreDialog = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAdmin && choresToVerify.isNotEmpty()) {
                item {
                    Text(
                        "Pending Verification",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(choresToVerify) { chore ->
                    VerificationCard(
                        chore = chore,
                        onApprove = { viewModel.approveChore(chore) },
                        onReject = { viewModel.rejectVerification(chore) }
                    )
                }
                item { HorizontalDivider() }
            }

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

            if (availableChores.isEmpty()) {
                item {
                    Text("No chores available for bidding.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(availableChores) { chore ->
                BiddingChoreCard(
                    chore = chore,
                    isAdmin = isAdmin,
                    onPlaceBid = { amount -> viewModel.placeBid(chore.id, amount) },
                    onResolve = { viewModel.resolveBidding(chore) },
                    getBids = { viewModel.getBidsForChore(chore.id) }
                )
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Assigned to You", style = MaterialTheme.typography.titleLarge)
            }

            if (assignedChores.isEmpty()) {
                item {
                    Text("No chores assigned to you.", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(assignedChores) { assignedChore ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(assignedChore.title, style = MaterialTheme.typography.titleMedium)
                            Text("Reward: ${assignedChore.totalReward} credits", style = MaterialTheme.typography.bodySmall)
                            if (assignedChore.frequency != ChoreFrequency.ONCE) {
                                Text(
                                    "Recurring: ${assignedChore.frequency.name.lowercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            if (assignedChore.status == ChoreStatus.WAITING_FOR_VERIFICATION) {
                                Text(
                                    "Waiting for verification...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Checkbox(
                            enabled = assignedChore.status == ChoreStatus.ASSIGNED,
                            checked = assignedChore.status == ChoreStatus.WAITING_FOR_VERIFICATION || assignedChore.status == ChoreStatus.COMPLETED,
                            onCheckedChange = { isChecked ->
                                if (isChecked) viewModel.requestVerification(assignedChore)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationCard(
    chore: Chore,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(chore.title, style = MaterialTheme.typography.titleMedium)
            Text("Done by: ${chore.assignedUserName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReject, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
fun BiddingChoreCard(
    chore: Chore,
    isAdmin: Boolean,
    onPlaceBid: (Int) -> Unit,
    onResolve: () -> Unit,
    getBids: () -> kotlinx.coroutines.flow.Flow<List<Bid>>
) {
    var bidValue by remember { mutableFloatStateOf(0f) }
    var expanded by remember { mutableStateOf(false) }
    val bids by getBids().collectAsState(initial = emptyList())
    val currentPot = bids.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(chore.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Reward: ${chore.baseReward} + Pot: $currentPot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Show less" else "Show bids"
                    )
                }
            }

            if (chore.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(chore.description, style = MaterialTheme.typography.bodyMedium)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("Current Bids to Avoid:", style = MaterialTheme.typography.labelLarge)
                    if (bids.isEmpty()) {
                        Text("No bids yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        bids.sortedByDescending { it.amount }.forEach { bid ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(bid.userName, style = MaterialTheme.typography.bodySmall)
                                Text("${bid.amount} credits", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                    
                    if (isAdmin && bids.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onResolve,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resolve & Assign")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Your Bid to AVOID: ${bidValue.toInt()} Credits")
            Slider(
                value = bidValue,
                onValueChange = { bidValue = it },
                valueRange = 0f..50f,
                steps = 49
            )
            
            Button(
                onClick = { onPlaceBid(bidValue.toInt()) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Place Bid")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, ChoreFrequency) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableFloatStateOf(1f) }
    var frequency by remember { mutableStateOf(ChoreFrequency.ONCE) }
    var expandedFreq by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedFreq,
                    onExpandedChange = { expandedFreq = !expandedFreq }
                ) {
                    OutlinedTextField(
                        value = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFreq) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFreq,
                        onDismissRequest = { expandedFreq = false }
                    ) {
                        ChoreFrequency.entries.forEach { freqOption ->
                            DropdownMenuItem(
                                text = { Text(freqOption.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    frequency = freqOption
                                    expandedFreq = false
                                }
                            )
                        }
                    }
                }

                Text("Difficulty: ${difficulty.toInt()}")
                Slider(
                    value = difficulty,
                    onValueChange = { difficulty = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, difficulty.toInt(), frequency) },
                enabled = title.isNotBlank()
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
