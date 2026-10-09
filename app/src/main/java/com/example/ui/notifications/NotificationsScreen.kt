package com.example.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.ui.ZaraViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ZaraAccentBlue
import com.example.ui.theme.ZaraAccentRed
import com.example.ui.theme.ZaraGreen
import com.example.ui.theme.ZaraSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: ZaraViewModel,
    onBackClick: () -> Unit,
    onNavigateToUser: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "All caught up",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You don't have any notifications right now.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(notifications, key = { it.notificationId }) { notif ->
                    NotificationRow(
                        item = notif,
                        onItemClick = {
                            viewModel.markNotificationAsRead(notif.notificationId)
                            if (notif.type == "message" && !notif.targetId.isNullOrBlank()) {
                                onNavigateToChat(notif.targetId)
                            } else {
                                onNavigateToUser(notif.senderId)
                            }
                        },
                        onAcceptFollowRequest = {
                            viewModel.respondToFollowRequest(notif.senderId, true)
                            viewModel.markNotificationAsRead(notif.notificationId)
                        },
                        onDeclineFollowRequest = {
                            viewModel.respondToFollowRequest(notif.senderId, false)
                            viewModel.markNotificationAsRead(notif.notificationId)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    item: NotificationItem,
    onItemClick: () -> Unit,
    onAcceptFollowRequest: () -> Unit,
    onDeclineFollowRequest: () -> Unit
) {
    val timeAgo = remember(item.createdAt) {
        val seconds = item.createdAt?.seconds
        if (seconds != null) {
            DateUtils.getRelativeTimeSpanString(
                seconds * 1000,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } else ""
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("notification_item_${item.notificationId}")
    ) {
        // Icon badge by type
        Box(modifier = Modifier.size(42.dp)) {
            UserAvatar(avatarUrl = null, name = item.senderDisplayName, size = 42.dp)
            val (badgeIcon, badgeColor) = when (item.type) {
                "like" -> Icons.Default.Favorite to ZaraAccentRed
                "comment" -> Icons.AutoMirrored.Filled.Chat to ZaraAccentBlue
                "follow", "follow_request" -> Icons.Default.PersonAdd to ZaraGreen
                else -> Icons.Default.Mail to ZaraSlate
            }
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.senderDisplayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (timeAgo.isNotBlank()) {
                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate, fontSize = 11.sp)
                )
            }

            // Follow Request Accept / Decline Action buttons
            if (item.type == "follow_request") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAcceptFollowRequest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Accept", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onDeclineFollowRequest,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Decline", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
