package com.example.flatfair.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.Poll
import com.example.flatfair.ui.viewmodel.PollViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PollViewModel = hiltViewModel()
) {
    val polls by viewModel.polls.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var pollToEdit by remember { mutableStateOf<Poll?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Household Polls", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Poll") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        if (showAddDialog) {
            AddEditPollDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { question, options ->
                    viewModel.createPoll(question, options)
                    showAddDialog = false
                }
            )
        }

        if (pollToEdit != null) {
            AddEditPollDialog(
                poll = pollToEdit,
                onDismiss = { pollToEdit = null },
                onConfirm = { question, options ->
                    viewModel.updatePoll(pollToEdit!!.copy(
                        question = question,
                        options = options
                    ))
                    pollToEdit = null
                }
            )
        }

        if (polls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "No Polls Active",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Start a poll to decide on dinner, chores, or household rules!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(polls, key = { it.id }) { poll ->
                    PollCard(
                        poll = poll,
                        currentUserId = user?.id ?: "",
                        onVote = { optionIndex -> viewModel.vote(poll.id, optionIndex) },
                        onClose = { viewModel.closePoll(poll.id, poll.question) },
                        onEdit = { pollToEdit = poll },
                        onDelete = { viewModel.deletePoll(poll.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun PollCard(
    poll: Poll,
    currentUserId: String,
    onVote: (Int) -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val userVote = poll.votes[currentUserId]
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (poll.isClosed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (poll.isClosed) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (poll.isClosed) "CLOSED" else "ACTIVE",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = if (poll.isClosed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = poll.question,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (poll.isClosed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Asked by ${poll.createdByUserName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                if (poll.createdByUserId == currentUserId) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (!poll.isClosed) {
                                DropdownMenuItem(
                                    text = { Text("Close Poll") },
                                    onClick = {
                                        onClose()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = {
                                        onEdit()
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete") },
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

            Spacer(modifier = Modifier.height(24.dp))

            val totalVotes = poll.votes.size
            poll.options.forEachIndexed { index, option ->
                val voteCount = poll.votes.values.count { it == index }
                val percentage = if (totalVotes > 0) voteCount.toFloat() / totalVotes else 0f
                val isSelected = userVote == index
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable(enabled = !poll.isClosed) { onVote(index) }
                ) {
                    // Progress Bar Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage)
                            .matchParentSize()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            )
                    )
                    
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (poll.isClosed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${(percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$voteCount votes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            
            AnimatedVisibility(visible = poll.isClosed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VOTING HAS ENDED",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditPollDialog(
    poll: Poll? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var question by remember { mutableStateOf(poll?.question ?: "") }
    val options = remember {
        mutableStateListOf<String>().apply {
            if (poll != null) addAll(poll.options)
            else {
                addAll(listOf("", "")) // Start with 2 empty options
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (poll == null) "Create Household Poll" else "Edit Poll", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("What are we voting for?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                
                Text(
                    "Options",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { options[index] = it },
                            label = { Text("Option ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        if (options.size > 2) {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                if (options.size < 6) {
                    TextButton(
                        onClick = { options.add("") },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val filteredOptions = options.filter { it.isNotBlank() }
                    onConfirm(question, filteredOptions) 
                },
                enabled = question.isNotBlank() && options.filter { it.isNotBlank() }.size >= 2,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (poll == null) "Launch Poll" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
