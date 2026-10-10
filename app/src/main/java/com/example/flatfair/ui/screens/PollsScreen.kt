package com.example.flatfair.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Household Polls") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Poll")
            }
        }
    ) { padding ->
        if (showAddDialog) {
            AddPollDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { question, options ->
                    viewModel.createPoll(question, options)
                    showAddDialog = false
                }
            )
        }

        if (polls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HowToVote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No active polls.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(polls) { poll ->
                    PollCard(
                        poll = poll,
                        currentUserId = user?.id ?: "",
                        onVote = { optionIndex -> viewModel.vote(poll.id, optionIndex) },
                        onClose = { viewModel.closePoll(poll.id, poll.question) },
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
    onDelete: () -> Unit
) {
    val userVote = poll.votes[currentUserId]

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (poll.isClosed) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poll.question,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Asked by ${poll.createdByUserName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                if (poll.createdByUserId == currentUserId) {
                    Row {
                        if (!poll.isClosed) {
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Check, contentDescription = "Close Poll", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val totalVotes = poll.votes.size
            poll.options.forEachIndexed { index, option ->
                val voteCount = poll.votes.values.count { it == index }
                val percentage = if (totalVotes > 0) voteCount.toFloat() / totalVotes else 0f
                
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !poll.isClosed) { onVote(index) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadioButton(
                                selected = userVote == index,
                                onClick = { if (!poll.isClosed) onVote(index) },
                                enabled = !poll.isClosed
                            )
                            Text(text = option, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            text = "$voteCount",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (userVote == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            if (poll.isClosed) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Poll Closed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun AddPollDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var option1 by remember { mutableStateOf("") }
    var option2 by remember { mutableStateOf("") }
    var option3 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Poll") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option1,
                    onValueChange = { option1 = it },
                    label = { Text("Option 1") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option2,
                    onValueChange = { option2 = it },
                    label = { Text("Option 2") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = option3,
                    onValueChange = { option3 = it },
                    label = { Text("Option 3 (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val options = listOfNotNull(
                        option1.takeIf { it.isNotBlank() },
                        option2.takeIf { it.isNotBlank() },
                        option3.takeIf { it.isNotBlank() }
                    )
                    onConfirm(question, options) 
                },
                enabled = question.isNotBlank() && option1.isNotBlank() && option2.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
