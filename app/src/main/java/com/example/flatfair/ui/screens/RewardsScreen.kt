package com.example.flatfair.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.Reward
import com.example.flatfair.ui.viewmodel.RewardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: RewardViewModel = hiltViewModel()
) {
    val rewards by viewModel.rewards.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var rewardToEdit by remember { mutableStateOf<Reward?>(null) }
    var rewardToRedeem by remember { mutableStateOf<Reward?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Household Rewards", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Reward") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        if (showAddDialog) {
            AddEditRewardDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, desc, cost ->
                    viewModel.addReward(title, desc, cost)
                    showAddDialog = false
                }
            )
        }

        if (rewardToEdit != null) {
            AddEditRewardDialog(
                reward = rewardToEdit,
                onDismiss = { rewardToEdit = null },
                onConfirm = { title, desc, cost ->
                    viewModel.updateReward(rewardToEdit!!.copy(
                        title = title,
                        description = desc,
                        cost = cost
                    ))
                    rewardToEdit = null
                }
            )
        }

        if (rewardToRedeem != null) {
            AlertDialog(
                onDismissRequest = { rewardToRedeem = null },
                title = { Text("Confirm Redemption") },
                text = { Text("Are you sure you want to spend ${rewardToRedeem?.cost} credits on '${rewardToRedeem?.title}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.redeemReward(rewardToRedeem!!)
                            rewardToRedeem = null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Redeem")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rewardToRedeem = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(modifier = Modifier.padding(padding)) {
            // Refined Balance Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = Color(0xFFFFD700).copy(alpha = 0.2f)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFB8860B),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Your Credit Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${user?.creditBalance ?: 0} Credits",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (rewards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(30.dp)
                                    .fillMaxSize(),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "No Rewards Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Admins haven't added any rewards to the household store.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(rewards, key = { it.id }) { reward ->
                        RewardCard(
                            reward = reward,
                            canAfford = (user?.creditBalance ?: 0) >= reward.cost,
                            isAdmin = isAdmin,
                            onRedeem = { rewardToRedeem = reward },
                            onEdit = { rewardToEdit = reward },
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (!reward.isAvailable) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (reward.isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (canAfford && reward.isAvailable) Color(0xFFB8860B) else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${reward.cost} Credits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (canAfford && reward.isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (isAdmin) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (reward.isAvailable) "Make Unavailable" else "Make Available") },
                                onClick = {
                                    onToggleAvailability()
                                    showMenu = false
                                },
                                leadingIcon = { 
                                    Switch(
                                        checked = reward.isAvailable, 
                                        onCheckedChange = null,
                                        modifier = Modifier.graphicsLayer(scaleX = 0.6f, scaleY = 0.6f)
                                    ) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Reward") },
                                onClick = {
                                    onEdit()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Reward") },
                                onClick = {
                                    onDelete()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            if (reward.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reward.isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRedeem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = canAfford && reward.isAvailable,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (reward.isAvailable) "Redeem Now" else "Currently Unavailable",
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (!canAfford && reward.isAvailable) {
                Text(
                    text = "You need ${reward.cost - (0)} more credits",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AddEditRewardDialog(
    reward: Reward? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf(reward?.title ?: "") }
    var description by remember { mutableStateOf(reward?.description ?: "") }
    var cost by remember { mutableStateOf(reward?.cost?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reward == null) "New Household Reward" else "Edit Reward", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (reward == null) {
                    Text(
                        "Add a treat for your roommates to redeem with credits.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reward Name") },
                    placeholder = { Text("e.g. Cinema Ticket") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) cost = it },
                    label = { Text("Credit Cost") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    prefix = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, cost.toIntOrNull() ?: 0) },
                enabled = title.isNotBlank() && cost.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (reward == null) "Add Reward" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
