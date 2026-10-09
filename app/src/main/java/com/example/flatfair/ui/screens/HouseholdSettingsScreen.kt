package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.ui.viewmodel.HouseholdUiState
import com.example.flatfair.ui.viewmodel.HouseholdViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: HouseholdViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val household by viewModel.currentHousehold.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showStipendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is HouseholdUiState.Success) {
            // If it was a leave success, we might want to navigate back to a different place,
            // but for now, navigating back is fine as the AppNavigation will redirect to HouseholdSelection.
            onNavigateBack()
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Household") },
            text = { Text("Are you sure you want to leave ${household?.name}? You will need a new invite code to join again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.leaveHousehold()
                        showLeaveDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStipendDialog) {
        var stipendAmount by remember { mutableStateOf("100") }
        AlertDialog(
            onDismissRequest = { showStipendDialog = false },
            title = { Text("Distribute Stipend") },
            text = {
                Column {
                    Text("Enter the amount of credits to give to EACH member of the household.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = stipendAmount,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) stipendAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.distributeStipend(stipendAmount.toIntOrNull() ?: 0)
                        showStipendDialog = false
                    },
                    enabled = stipendAmount.isNotBlank() && (stipendAmount.toIntOrNull() ?: 0) > 0
                ) {
                    Text("Distribute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStipendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(household?.name ?: "Household Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Invite Roommates",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Share this code with your flatmates so they can join this household.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val inviteCode = household?.inviteCode ?: "......"
                                Text(
                                    text = inviteCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(inviteCode))
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Copy")
                                }
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                item {
                    Text("Admin Actions", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    OutlinedCard(
                        onClick = { showStipendDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text("Distribute Weekly Stipend") },
                            supportingContent = { Text("Give credits to everyone for basic tasks") },
                            leadingContent = { Icon(Icons.Default.Star, contentDescription = null) }
                        )
                    }
                }
            }

            item {
                Text("Members", style = MaterialTheme.typography.titleMedium)
            }

            items(members) { member ->
                val isMe = member.id == user?.id
                val memberIsAdmin = household?.adminIds?.contains(member.id) == true
                
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(if (isMe) "${member.name} (You)" else member.name) },
                        supportingContent = { if (memberIsAdmin) Text("Admin") },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                if (uiState is HouseholdUiState.Error) {
                    Text(
                        text = (uiState as HouseholdUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { showLeaveDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is HouseholdUiState.Loading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (uiState is HouseholdUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Leave Household")
                    }
                }
            }
        }
    }
}
