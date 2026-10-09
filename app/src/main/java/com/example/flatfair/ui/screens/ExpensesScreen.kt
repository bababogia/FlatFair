package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.Expense
import com.example.flatfair.ui.viewmodel.ExpenseViewModel
import com.example.flatfair.ui.viewmodel.HouseholdViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpenseViewModel = hiltViewModel(),
    householdViewModel: HouseholdViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val members by householdViewModel.householdMembers.collectAsState()
    val balances by viewModel.userBalances.collectAsState()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shared Expenses") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExpenseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        if (showAddExpenseDialog) {
            AddExpenseDialog(
                members = members.filter { it.id != user?.id },
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { title, amount, splitWith ->
                    viewModel.addExpense(title, amount, splitWith)
                    showAddExpenseDialog = false
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
            if (balances.isNotEmpty()) {
                item {
                    Text("Balances", style = MaterialTheme.typography.titleLarge)
                }
                items(balances) { balance ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (balance.netAmount >= 0) 
                                Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(balance.otherUserName) },
                            supportingContent = { 
                                Text(if (balance.netAmount >= 0) "owes you" else "you owe") 
                            },
                            trailingContent = {
                                Text(
                                    "$${String.format(Locale.US, "%.2f", Math.abs(balance.netAmount))}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance.netAmount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        )
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            item {
                Text("Recent Expenses", style = MaterialTheme.typography.titleLarge)
            }

            if (expenses.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No shared expenses yet.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                items(expenses) { expense ->
                    ExpenseCard(
                        expense = expense,
                        isCurrentUserId = expense.paidByUserId == user?.id,
                        onSettle = { viewModel.settleExpense(expense) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    isCurrentUserId: Boolean,
    onSettle: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = if (expense.isSettled) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) 
                 else CardDefaults.outlinedCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (isCurrentUserId) "You paid" else "${expense.paidByUserName} paid",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "$${String.format(Locale.US, "%.2f", expense.amount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFormatter.format(Date(expense.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                if (!expense.isSettled) {
                    if (isCurrentUserId) {
                        Button(
                            onClick = onSettle,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Settle All", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Pending") },
                            colors = SuggestionChipDefaults.suggestionChipColors(labelColor = MaterialTheme.colorScheme.secondary)
                        )
                    }
                } else {
                    Text(
                        "Settled",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    members: List<com.example.flatfair.domain.model.User>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description (e.g. Toilet Paper)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("$ ") }
                )
                
                Text("Split with:", style = MaterialTheme.typography.labelLarge)
                members.forEach { member ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = selectedMembers.contains(member.id),
                            onCheckedChange = { checked ->
                                if (checked) selectedMembers.add(member.id)
                                else selectedMembers.remove(member.id)
                            }
                        )
                        Text(member.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    onConfirm(title, amountDouble, selectedMembers.toList()) 
                },
                enabled = title.isNotBlank() && amount.isNotBlank() && selectedMembers.isNotEmpty()
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
