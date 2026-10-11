package com.example.flatfair.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            Surface(shadowElevation = 2.dp) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = { Text("Bidding & Chores", fontWeight = FontWeight.Bold) },
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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        )
                    )

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Active", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("History", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddChoreDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("New Chore") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
        Text("Sort By", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOrder.entries.forEach { order ->
                FilterChip(
                    selected = selectedSort == order,
                    onClick = { onSortSelected(order) },
                    label = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDismiss, 
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Show Results", fontWeight = FontWeight.Bold)
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isAdmin && choresToVerify.isNotEmpty()) {
            item {
                Text("Pending Verification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(choresToVerify, key = { "verify_${it.id}" }) { chore ->
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
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }

        item {
            Column {
                Text("Available for Bidding", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Set credits for tasks you'd rather NOT do. Highest bidder pays, pot goes to the one who takes it!", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (availableChores.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "No chores available for bidding right now.", 
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        items(availableChores, key = { "bid_${it.id}" }) { chore ->
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
            Spacer(modifier = Modifier.height(8.dp))
            Text("Assigned to You", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (assignedChores.isEmpty()) {
            item {
                Text("Relax! You have no chores assigned.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }

        items(assignedChores, key = { "assigned_${it.id}" }) { assignedChore ->
            val deadline = assignedChore.deadline
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(20.dp),
                onClick = { onChoreClick(assignedChore.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(Modifier.width(8.dp))
                            Text(assignedChore.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(14.dp), tint = Color(0xFFB8860B))
                            Spacer(Modifier.width(4.dp))
                            Text("${assignedChore.totalReward} credits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            if (deadline != null) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(2.dp))
                                Text(dateFormatter.format(deadline), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (assignedChore.status == ChoreStatus.WAITING_FOR_VERIFICATION) {
                            Text(
                                "Waiting for approval...", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    
                    if (assignedChore.status == ChoreStatus.ASSIGNED) {
                        Button(
                            onClick = { viewModel.requestVerification(assignedChore) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Done", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Icon(
                            Icons.Default.AccessTime, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        if (roommateChores.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Roommate Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(roommateChores, key = { "roommate_${it.id}" }) { chore ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { onChoreClick(chore.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(chore.title, style = MaterialTheme.typography.titleMedium)
                            Text("Being handled by ${chore.assignedUserName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        if (chore.status == ChoreStatus.ASSIGNED) {
                            FilledTonalIconButton(
                                onClick = { viewModel.nudgeUser(chore) },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Nudge", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun CompletedChoresList(completedChores: List<Chore>, onChoreClick: (String) -> Unit) {
    if (completedChores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Text("History is empty.", color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(completedChores, key = { "completed_${it.id}" }) { chore ->
                val rating = chore.rating
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = { onChoreClick(chore.id) }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(chore.title, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Done by ${chore.assignedUserName}") },
                        trailingContent = {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("+${chore.totalReward}", color = Color(0xFF388E3C), fontWeight = FontWeight.Black)
                                if (rating != null) {
                                    Row {
                                        repeat(5) { index ->
                                            Icon(
                                                Icons.Default.Star, 
                                                null, 
                                                modifier = Modifier.size(10.dp), 
                                                tint = if (index < rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        leadingContent = { 
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.padding(8.dp))
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationCard(chore: Chore, onApprove: () -> Unit, onReject: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Approval Needed", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    Text(chore.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${chore.assignedUserName} finished this task.", style = MaterialTheme.typography.bodySmall)
                }
                if (chore.proofImageUrl != null) {
                    AsyncImage(
                        model = chore.proofImageUrl,
                        contentDescription = "Proof",
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onReject, 
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ThumbUp, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
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

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        ),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = chore.category.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(chore.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Stars, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Reward: ${chore.baseReward} + Pot: $currentPot", 
                            style = MaterialTheme.typography.bodySmall, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Row {
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                            contentDescription = null
                        )
                    }
                }
            }

            if (chore.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(chore.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text("Avoid-Bids Leaderboard", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    if (bids.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("No bids placed yet.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
                        }
                    } else {
                        bids.sortedByDescending { it.amount }.forEach { bid ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bid.userName, style = MaterialTheme.typography.bodyMedium)
                                Text("${bid.amount} pts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    if (isAdmin && bids.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onResolve, 
                            modifier = Modifier.fillMaxWidth().height(48.dp), 
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finalize & Assign", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your bid to AVOID:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("${bidValue.toInt()} Credits", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = { onPlaceBid(bidValue.toInt()) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bid")
                }
            }
            Slider(
                value = bidValue, 
                onValueChange = { bidValue = it }, 
                valueRange = 0f..50f, 
                steps = 49,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun RatingDialog(onDismiss: () -> Unit, onConfirm: (Int, String?) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var feedback by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Quality Inspection", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("How would you rate the performance of this chore?")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(5) { index ->
                        val starIndex = index + 1
                        IconButton(onClick = { rating = starIndex }) {
                            Icon(
                                imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (starIndex <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    label = { Text("Feedback for the roommate") },
                    placeholder = { Text("Great job! / Missed a spot...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rating, feedback.ifBlank { null }) }, shape = RoundedCornerShape(12.dp)) {
                Text("Confirm Approval")
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
        shape = RoundedCornerShape(28.dp),
        title = { Text("Create Household Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    label = { Text("Task Title") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Column {
                    Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Task Details") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
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
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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
                        modifier = Modifier.weight(1f).height(IntrinsicSize.Min),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (deadline == null) "Deadline" else dateFormatter.format(deadline!!),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column {
                    Text("Difficulty Level: ${difficulty.toInt()}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Slider(
                        value = difficulty, 
                        onValueChange = { difficulty = it }, 
                        valueRange = 1f..5f, 
                        steps = 3,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, difficulty.toInt(), frequency, category, deadline) }, 
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
