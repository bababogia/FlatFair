package com.example.flatfair.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flatfair.domain.model.Notification
import com.example.flatfair.domain.model.NotificationType
import com.example.flatfair.ui.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        dateString = dateFormatter.format(Date(notification.timestamp)),
                        onMarkAsRead = { viewModel.markAsRead(notification.id) },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    dateString: String,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onMarkAsRead() },
        headlineContent = { 
            Text(
                text = notification.title,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        supportingContent = {
            Column {
                Text(text = notification.body)
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        leadingContent = {
            val icon = when (notification.type) {
                NotificationType.NUDGE -> Icons.Default.Notifications
                NotificationType.CHORE_ASSIGNED -> Icons.Default.AssignmentInd
                NotificationType.CHORE_APPROVED -> Icons.Default.CheckCircle
                NotificationType.EXPENSE_ADDED -> Icons.Default.ReceiptLong
                NotificationType.POLL_STARTED -> Icons.Default.Poll
                NotificationType.ANNOUNCEMENT_POSTED -> Icons.Default.Notifications
                NotificationType.EVENT_ADDED -> Icons.Default.Event
                NotificationType.ANONYMOUS_POST_ADDED -> Icons.Default.Forum
            }
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (!notification.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = 12.dp, y = (-4).dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
            }
        },
        colors = if (!notification.isRead) 
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
        else ListItemDefaults.colors()
    )
}
