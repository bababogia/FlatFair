package com.example.flatfair.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.domain.model.Comment
import com.example.flatfair.ui.viewmodel.ChoresViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreDetailScreen(
    choreId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoresViewModel = hiltViewModel()
) {
    val availableChores by viewModel.availableChores.collectAsState()
    val assignedChores by viewModel.assignedChores.collectAsState()
    val choresToVerify by viewModel.choresToVerify.collectAsState()
    val completedChores by viewModel.completedChores.collectAsState()
    val roommateChores by viewModel.roommateChores.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    val chore = remember(choreId, availableChores, assignedChores, choresToVerify, completedChores, roommateChores) {
        (availableChores + assignedChores + choresToVerify + completedChores + roommateChores).find { it.id == choreId }
    }

    val comments by viewModel.getCommentsForChore(choreId).collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }
    val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && chore != null) {
            viewModel.requestVerification(chore, uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chore Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Write a message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            viewModel.addComment(choreId, commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        if (chore == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ChoreHeader(chore)
                }

                item {
                    StatusCard(chore)
                }

                // Action Buttons for current user
                if (chore.assignedUserId == user?.id && chore.status == ChoreStatus.ASSIGNED) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.requestVerification(chore) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Complete")
                            }
                            FilledTonalButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Photo Proof")
                            }
                        }
                    }
                }

                if (chore.assignedUserId != null && chore.assignedUserId != user?.id && chore.status == ChoreStatus.ASSIGNED) {
                    item {
                        Button(
                            onClick = { viewModel.nudgeUser(chore) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Send a Nudge")
                        }
                    }
                }

                if (chore.proofImageUrl != null) {
                    item {
                        Text(
                            text = "Proof of Work",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = chore.proofImageUrl,
                            contentDescription = "Proof image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (chore.description.isNotEmpty()) {
                    item {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = chore.description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Activity & Comments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (comments.isEmpty()) {
                    item {
                        Text(
                            "No messages yet. Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            isMe = comment.authorId == user?.id,
                            dateString = dateFormatter.format(Date(comment.timestamp))
                        )
                    }
                }
                
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun ChoreHeader(chore: Chore) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SuggestionChip(
                onClick = {},
                label = { Text(chore.category) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
            Spacer(Modifier.width(8.dp))
            if (chore.frequency != com.example.flatfair.domain.model.ChoreFrequency.ONCE) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Recurring") },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
        Text(
            text = chore.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${chore.totalReward} Credits Reward",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun StatusCard(chore: Chore) {
    val statusColor = when (chore.status) {
        ChoreStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        ChoreStatus.ASSIGNED -> MaterialTheme.colorScheme.primary
        ChoreStatus.WAITING_FOR_VERIFICATION -> Color(0xFFF57C00)
        ChoreStatus.COMPLETED -> Color(0xFF388E3C)
        ChoreStatus.OVERDUE -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (chore.status == ChoreStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = statusColor
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = when (chore.status) {
                        ChoreStatus.PENDING -> "Status: Bidding Open"
                        ChoreStatus.ASSIGNED -> "Status: Assigned to ${chore.assignedUserName}"
                        ChoreStatus.WAITING_FOR_VERIFICATION -> "Status: Pending Approval"
                        ChoreStatus.COMPLETED -> "Status: Completed"
                        ChoreStatus.OVERDUE -> "Status: Overdue"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = when (chore.status) {
                        ChoreStatus.PENDING -> "Roommates are placing bids to avoid this task."
                        ChoreStatus.ASSIGNED -> "Work is currently in progress."
                        ChoreStatus.WAITING_FOR_VERIFICATION -> "Waiting for a household admin to verify the work."
                        ChoreStatus.COMPLETED -> "Credits have been awarded to ${chore.assignedUserName}."
                        ChoreStatus.OVERDUE -> "The deadline for this task has passed."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment, isMe: Boolean, dateString: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isMe) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (isMe) "You" else comment.authorName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = comment.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = dateString,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
