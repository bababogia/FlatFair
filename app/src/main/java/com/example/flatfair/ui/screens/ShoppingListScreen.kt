package com.example.flatfair.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.ShoppingItem
import com.example.flatfair.ui.viewmodel.HouseholdViewModel
import com.example.flatfair.ui.viewmodel.ShoppingViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingViewModel = hiltViewModel(),
    householdViewModel: HouseholdViewModel = hiltViewModel()
) {
    val items by viewModel.shoppingItems.collectAsState()
    val members by householdViewModel.householdMembers.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToLogExpense by remember { mutableStateOf<ShoppingItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Shopping List") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        if (showAddItemDialog) {
            AddShoppingItemDialog(
                onDismiss = { showAddItemDialog = false },
                onConfirm = { name, qty, urgent ->
                    viewModel.addItem(name, qty, urgent)
                    showAddItemDialog = false
                }
            )
        }

        if (itemToLogExpense != null) {
            LogShoppingExpenseDialog(
                item = itemToLogExpense!!,
                members = members.filter { it.id != user?.id },
                onDismiss = { itemToLogExpense = null },
                onConfirm = { amount, splitWith ->
                    viewModel.buyAndCreateExpense(itemToLogExpense!!, amount, splitWith)
                    itemToLogExpense = null
                }
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Your shopping list is empty.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val (boughtItems, pendingItems) = items.partition { it.isBought }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pendingItems.isNotEmpty()) {
                    item { Text("To Buy", style = MaterialTheme.typography.titleMedium) }
                    items(pendingItems) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggleBought = { viewModel.toggleItemBought(item) },
                            onLogExpense = { itemToLogExpense = item },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }

                if (boughtItems.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { Text("Recently Bought", style = MaterialTheme.typography.titleMedium) }
                    items(boughtItems) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggleBought = { viewModel.toggleItemBought(item) },
                            onLogExpense = null, // Already bought
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    onToggleBought: () -> Unit,
    onLogExpense: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (item.isBought) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isBought,
                onCheckedChange = { onToggleBought() }
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isUrgent && !item.isBought) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (item.isBought) TextDecoration.LineThrough else null
                    )
                    if (item.isUrgent && !item.isBought) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Urgent",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (item.quantity.isNotBlank()) {
                    Text(
                        text = item.quantity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = "Added by ${item.addedByUserName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }

            if (onLogExpense != null) {
                IconButton(onClick = onLogExpense) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "Buy & Log Expense", tint = MaterialTheme.colorScheme.primary)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun LogShoppingExpenseDialog(
    item: ShoppingItem,
    members: List<com.example.flatfair.domain.model.User>,
    onDismiss: () -> Unit,
    onConfirm: (Double, List<String>) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>().apply { addAll(members.map { it.id }) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Expense for ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                    label = { Text("Total Amount Paid ($)") },
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
                    onConfirm(amountDouble, selectedMembers.toList()) 
                },
                enabled = amount.isNotBlank() && selectedMembers.isNotEmpty()
            ) {
                Text("Confirm Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddShoppingItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Milk)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (e.g. 2L)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                    Text("Mark as Urgent")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, quantity, isUrgent) },
                enabled = name.isNotBlank()
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
