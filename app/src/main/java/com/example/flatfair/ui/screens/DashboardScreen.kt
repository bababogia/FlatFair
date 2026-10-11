package com.example.flatfair.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.User
import com.example.flatfair.ui.viewmodel.ActivityViewModel
import com.example.flatfair.ui.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToChores: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToShopping: () -> Unit = {},
    onNavigateToAnnouncements: () -> Unit = {},
    onNavigateToAnonymousBoard: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToRewards: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPolls: () -> Unit = {},
    onNavigateToRoommateProfile: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    activityViewModel: ActivityViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val household by viewModel.currentHousehold.collectAsState()
    val activeChores by viewModel.activeChores.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val members by viewModel.householdMembers.collectAsState()
    val pendingExpenses by viewModel.pendingExpensesCount.collectAsState()
    val pendingVerifications by viewModel.pendingVerificationsCount.collectAsState()
    val availableBids by viewModel.availableBidsCount.collectAsState()
    val pendingShopping by viewModel.pendingShoppingItemsCount.collectAsState()
    val announcementsCount by viewModel.announcementsCount.collectAsState()
    val upcomingEvents by viewModel.upcomingEventsCount.collectAsState()
    val activePolls by viewModel.activePollsCount.collectAsState()
    val recentActivities by activityViewModel.recentActivities.collectAsState()

    var showRules by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("FlatFair")
                        if (household != null) {
                            Text(
                                text = household!!.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                Badge()
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Presence Widget
            if (members.isNotEmpty()) {
                item {
                    Text("Who's Home?", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(members) { member ->
                            PresenceAvatar(member = member) {
                                onNavigateToRoommateProfile(member.id)
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToHistory,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your Dorm Credits", style = MaterialTheme.typography.titleMedium)
                            Text("${user?.creditBalance ?: 0}", style = MaterialTheme.typography.headlineLarge)
                            Text(
                                "Tap for history",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Button(onClick = onNavigateToRewards) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Rewards")
                        }
                    }
                }
            }

            // Quick Access Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedCard(
                        onClick = onNavigateToAnonymousBoard,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Board", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    OutlinedCard(
                        onClick = onNavigateToAnnouncements,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Announce", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    OutlinedCard(
                        onClick = onNavigateToPolls,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Poll, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Text("Polls", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Household Rules Preview
            if (household != null && household!!.houseRules.isNotBlank()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showRules = !showRules }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("House Rules", style = MaterialTheme.typography.titleSmall)
                                }
                                Icon(
                                    imageVector = if (showRules) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(visible = showRules) {
                                Text(
                                    text = household!!.houseRules,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Alerts Section
            if (pendingVerifications > 0 || availableBids > 0 || pendingExpenses > 0 || pendingShopping > 0 || announcementsCount > 0 || upcomingEvents > 0 || activePolls > 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (announcementsCount > 0) {
                            AlertCard(
                                title = "New Announcements",
                                text = "There are $announcementsCount messages for the household",
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = onNavigateToAnnouncements
                            )
                        }
                        if (activePolls > 0) {
                            AlertCard(
                                title = "Active Polls",
                                text = "You have $activePolls polls to vote in",
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = onNavigateToPolls
                            )
                        }
                        if (upcomingEvents > 0) {
                            AlertCard(
                                title = "Upcoming Events",
                                text = "There are $upcomingEvents events in the next 3 days",
                                color = Color(0xFFE8EAF6),
                                onClick = onNavigateToCalendar
                            )
                        }
                        if (pendingVerifications > 0) {
                            AlertCard(
                                title = "Verification Required",
                                text = "There are $pendingVerifications chores waiting for approval",
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                onClick = onNavigateToChores
                            )
                        }
                        if (availableBids > 0) {
                            AlertCard(
                                title = "New Bids Open",
                                text = "$availableBids chores are in the bidding phase",
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = onNavigateToChores
                            )
                        }
                        if (pendingExpenses > 0) {
                            AlertCard(
                                title = "Unsettled Expenses",
                                text = "You have $pendingExpenses shared expenses to settle",
                                color = MaterialTheme.colorScheme.errorContainer,
                                onClick = onNavigateToExpenses
                            )
                        }
                        if (pendingShopping > 0) {
                            AlertCard(
                                title = "Shopping List",
                                text = "$pendingShopping items need to be bought",
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = onNavigateToShopping
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Active Tasks", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onNavigateToChores) {
                        Text("Manage")
                    }
                }
            }

            if (activeChores.isEmpty()) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No chores assigned to you. Relax!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(activeChores) { chore ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(chore.title) },
                            supportingContent = { Text("Reward: ${chore.totalReward} pts") },
                            trailingContent = { 
                                if (chore.status == com.example.flatfair.domain.model.ChoreStatus.WAITING_FOR_VERIFICATION) {
                                    SuggestionChip(onClick = {}, label = { Text("Pending") })
                                } else {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            }

            if (recentActivities.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Activity", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onNavigateToActivity) {
                            Text("See All")
                        }
                    }
                }
                
                items(recentActivities.take(3)) { activity ->
                    ListItem(
                        headlineContent = { Text("${activity.userName} ${activity.action}") },
                        supportingContent = { Text(activity.targetName) },
                        trailingContent = { Text(dateFormatter.format(Date(activity.timestamp)), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Roommate Leaderboard", style = MaterialTheme.typography.titleLarge)
            }

            itemsIndexed(leaderboard) { index, member ->
                val isMe = member.id == user?.id
                val isMemberAdmin = household?.adminIds?.contains(member.id) == true
                ListItem(
                    modifier = Modifier.clickable { onNavigateToRoommateProfile(member.id) },
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMe) "${member.name} (You)" else member.name,
                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isMemberAdmin) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = "Admin",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    supportingContent = { Text("${member.creditBalance} credits") },
                    leadingContent = {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (index == 0) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (index == 0) Color(0xFFB8860B) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = if (isMe) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else ListItemDefaults.colors()
                )
                if (index < leaderboard.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun PresenceAvatar(member: User, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = if (member.isHome) BorderStroke(2.dp, Color(0xFF4CAF50)) else null
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = if (member.isHome) MaterialTheme.colorScheme.onSurfaceVariant 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // Small status dot
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (member.isHome) Color(0xFF4CAF50) else Color.Gray)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = member.name.split(" ").firstOrNull() ?: "",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlertCard(
    title: String,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(title) },
            supportingContent = { Text(text) },
            leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
            trailingContent = { Text("View", fontWeight = FontWeight.Bold) }
        )
    }
}
