package com.example.flatfair.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.flatfair.domain.model.Bid
import com.example.flatfair.domain.model.Chore
import com.example.flatfair.domain.model.ChoreFrequency
import com.example.flatfair.domain.model.ChoreStatus
import com.example.flatfair.ui.viewmodel.ChoresViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreen(
    onNavigateToChoreDetail: (String) -> Unit,
    viewModel: ChoresViewModel = hiltViewModel()
) {
    val availableChores by viewModel.availableChores.collectAsState()
    val assignedChores by viewModel.assignedChores.collectAsState()
    val roommateChores by viewModel.roommateChores.collectAsState()
    val choresToVerify by viewModel.choresToVerify.collectAsState()
    val completedChores by viewModel.completedChores.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddChoreDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var sortOrder by remember { mutableStateOf(SortOrder.NEWEST) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val sheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Bidding & Chores") },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chores...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Active") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("History") }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddChoreDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Chore")
            }
        }
    ) { padding ->
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState
            ) {
                FilterBottomSheetContent(
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelected = { selectedCategoryFilter = it },
                    selectedSort = sortOrder,
                    onSortSelected = { sortOrder = it },
                    onDismiss = { showFilterSheet = false }
                )
            }
        }

        if (showAddChoreDialog) {
            AddChoreDialog(
                onDismiss = { showAddChoreDialog = false },
                onConfirm = { title, desc, diff, freq, category, deadline ->
                    viewModel.createChore(title, desc, diff, freq, category, deadline)
                    showAddChoreDialog = false
                }
            )
        }

        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                ActiveChoresList(
                    availableChores = applyFilters(availableChores, searchQuery, selectedCategoryFilter, sortOrder),
                    assignedChores = applyFilters(assignedChores, searchQuery, selectedCategoryFilter, sortOrder),
                    roommateChores = applyFilters(roommateChores, searchQuery, selectedCategoryFilter, sortOrder),
                    choresToVerify = choresToVerify,
                    isAdmin = isAdmin,
                    viewModel = viewModel,
                    onChoreClick = onNavigateToChoreDetail
                )
            } else {
                CompletedChoresList(
                    completedChores = applyFilters(completedChores, searchQuery, selectedCategoryFilter, sortOrder),
                    onChoreClick = onNavigateToChoreDetail
                )
            }
        }
    }
}

enum class SortOrder {
    NEWEST, REWARD_HIGH, REWARD_LOW, DIFFICULTY_HIGH
}

private fun applyFilters(chores: List<Chore>, query: String, category: String, sort: SortOrder): List<Chore> {
    return chores
        .filter { it.title.contains(query, ignoreCase = true) }
        .filter { if (category == "All") true else it.category == category }
        .let { list ->
            when (sort) {
                SortOrder.NEWEST -> list
                SortOrder.REWARD_HIGH -> list.sortedByDescending { it.totalReward }
                SortOrder.REWARD_LOW -> list.sortedBy { it.totalReward }
                SortOrder.DIFFICULTY_HIGH -> list.sortedByDescending { it.difficulty }
            }
        }
}

@Composable
fun FilterBottomSheetContent(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("All", "Kitchen", "Bathroom", "Living Room", "Outdoors", "General")
    
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Sort By", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOrder.entries.forEach { order ->
                FilterChip(
                    selected = selectedSort == order,
                    onClick = { onSortSelected(order) },
                    label = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Category", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Show Results")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = horizontalArrangement,
        content = { content() }
    )
}

@Composable
fun ActiveChoresList(
    availableChores: List<Chore>,
    assignedChores: List<Chore>,
    roommateChores: List<Chore>,
    choresToVerify: List<Chore>,
    isAdmin: Boolean,
    viewModel: ChoresViewModel,
    onChoreClick: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isAdmin && choresToVerify.isNotEmpty()) {
            item {
                Text("Pending Verification", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            items(choresToVerify) { chore ->
                var showRatingDialog by remember { mutableStateOf(false) }
                
                if (showRatingDialog) {
                    RatingDialog(
                        onDismiss = { showRatingDialog = false },
                        onConfirm = { rating, feedback ->
                            viewModel.approveChore(chore, rating, feedback)
                            showRatingDialog = false
                        }
                    )
                }

                VerificationCard(
                    chore = chore,
                    onApprove = { showRatingDialog = true },
                    onReject = { viewModel.rejectVerification(chore) },
                    onClick = { onChoreClick(chore.id) }
                )
            }
            item { HorizontalDivider() }
        }

        item {
            Text("Available for Bidding", style = MaterialTheme.typography.headlineSmall)
            Text("Set your credits for tasks you want to avoid.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (availableChores.isEmpty()) {
            item { Text("No chores matching criteria.", style = MaterialTheme.typography.bodySmall) }
        }

        items(availableChores) { chore ->
            BiddingChoreCard(
                chore = chore,
                isAdmin = isAdmin,
                onPlaceBid = { amount -> viewModel.placeBid(chore.id, amount) },
                onResolve = { viewModel.resolveBidding(chore) },
                onDelete = { viewModel.deleteChore(chore) },
                onClick = { onChoreClick(chore.id) },
                getBids = { viewModel.getBidsForChore(chore.id) }
            )
        }

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Assigned to You", style = MaterialTheme.typography.titleLarge)
        }

        if (assignedChores.isEmpty()) {
            item { Text("No chores assigned to you.", style = MaterialTheme.typography.bodySmall) }
        }

        items(assignedChores) { assignedChore ->
            val deadline = assignedChore.deadline
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onChoreClick(assignedChore.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(assignedChore.title, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reward: ${assignedChore.totalReward} credits", style = MaterialTheme.typography.bodySmall)
                            if (deadline != null) {
                                Spacer(Modifier.width(8.dp))
                                Text("•", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(8.dp))
                                Text("Due: ${dateFormatter.format(deadline)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (assignedChore.status == ChoreStatus.WAITING_FOR_VERIFICATION) {
                            Text("Waiting for verification...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Checkbox(
                        enabled = assignedChore.status == ChoreStatus.ASSIGNED,
                        checked = assignedChore.status != ChoreStatus.ASSIGNED,
                        onCheckedChange = { if (it) viewModel.requestVerification(assignedChore) }
                    )
                }
            }
        }

        if (roommateChores.isNotEmpty()) {
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Roommate Tasks", style = MaterialTheme.typography.titleLarge)
            }

            items(roommateChores) { chore ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onChoreClick(chore.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chore.title, style = MaterialTheme.typography.titleMedium)
                            Text("Assigned to: ${chore.assignedUserName}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (chore.status == ChoreStatus.ASSIGNED) {
                            IconButton(onClick = { viewModel.nudgeUser(chore) }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Nudge", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedChoresList(completedChores: List<Chore>, onChoreClick: (String) -> Unit) {
    if (completedChores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No completed chores yet.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(completedChores) { chore ->
                val rating = chore.rating
                ListItem(
                    modifier = Modifier.fillMaxWidth().clickable { onChoreClick(chore.id) },
                    headlineContent = { Text(chore.title) },
                    supportingContent = { Text("Completed by ${chore.assignedUserName ?: "Unknown"}") },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("+${chore.totalReward}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            if (rating != null) {
                                Row {
                                    repeat(rating) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD700))
                                    }
                                }
                            }
                        }
                    },
                    leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCard(chore: Chore, onApprove: () -> Unit, onReject: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(chore.title, style = MaterialTheme.typography.titleMedium)
                    Text("Done by: ${chore.assignedUserName}", style = MaterialTheme.typography.bodySmall)
                }
                if (chore.proofImageUrl != null) {
                    AsyncImage(
                        model = chore.proofImageUrl,
                        contentDescription = "Proof",
                        modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiddingChoreCard(
    chore: Chore,
    isAdmin: Boolean,
    onPlaceBid: (Int) -> Unit,
    onResolve: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    getBids: () -> kotlinx.coroutines.flow.Flow<List<Bid>>
) {
    var bidValue by remember { mutableFloatStateOf(0f) }
    var expanded by remember { mutableStateOf(false) }
    val bids by getBids().collectAsState(initial = emptyList())
    val currentPot = bids.sumOf { it.amount }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuggestionChip(onClick = {}, label = { Text(chore.category) }, modifier = Modifier.height(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(chore.title, style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Reward: ${chore.baseReward} + Pot: $currentPot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
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
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(bid.userName, style = MaterialTheme.typography.bodySmall)
                                Text("${bid.amount} credits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (isAdmin && bids.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onResolve, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resolve & Assign")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Bid to AVOID: ${bidValue.toInt()} Credits")
            Slider(value = bidValue, onValueChange = { bidValue = it }, valueRange = 0f..50f, steps = 49)
            Button(onClick = { onPlaceBid(bidValue.toInt()) }, modifier = Modifier.align(Alignment.End)) {
                Text("Place Bid")
            }
        }
    }
}

@Composable
fun RatingDialog(onDismiss: () -> Unit, onConfirm: (Int, String?) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var feedback by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate & Approve Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("How well was the task performed?")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(5) { index ->
                        val starIndex = index + 1
                        IconButton(onClick = { rating = starIndex }) {
                            Icon(
                                imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (starIndex <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Feedback (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rating, feedback.ifBlank { null }) }) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, ChoreFrequency, String, Date?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var difficulty by remember { mutableFloatStateOf(1f) }
    var frequency by remember { mutableStateOf(ChoreFrequency.ONCE) }
    var category by remember { mutableStateOf("Kitchen") }
    var expandedFreq by remember { mutableStateOf(false) }
    
    var deadline by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadline = datePickerState.selectedDateMillis?.let { Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val categories = listOf("Kitchen", "Bathroom", "Living Room", "Outdoors", "General")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExposedDropdownMenuBox(
                        expanded = expandedFreq, 
                        onExpandedChange = { expandedFreq = !expandedFreq },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFreq) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedFreq, onDismissRequest = { expandedFreq = false }) {
                            ChoreFrequency.entries.forEach { freqOption ->
                                DropdownMenuItem(
                                    text = { Text(freqOption.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { frequency = freqOption; expandedFreq = false }
                                )
                            }
                        }
                    }

                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).height(IntrinsicSize.Min)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (deadline == null) "No Deadline" else dateFormatter.format(deadline!!),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text("Difficulty: ${difficulty.toInt()}")
                Slider(value = difficulty, onValueChange = { difficulty = it }, valueRange = 1f..5f, steps = 3)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, description, difficulty.toInt(), frequency, category, deadline) }, enabled = title.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
