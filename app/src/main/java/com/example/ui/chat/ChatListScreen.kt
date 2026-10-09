package com.example.ui.chat

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.ui.ZaraViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ZaraMuted
import com.example.ui.theme.ZaraSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ZaraViewModel,
    onOpenConversation: (String, String) -> Unit, // conversationId, otherUserId
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }

    val userMap = remember(allUsers) { allUsers.associateBy { it.userId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "New Message")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (conversations.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a private real-time chat with anyone on ZARA.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showNewChatDialog = true }) {
                            Text("Start a Conversation")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(conversations, key = { it.conversationId }) { conv ->
                        val otherUserId = conv.participantIds.firstOrNull { it != viewModel.currentUserId } ?: ""
                        val otherUser = userMap[otherUserId]
                        ConversationRow(
                            conversation = conv,
                            otherUser = otherUser,
                            otherUserId = otherUserId,
                            onClick = { onOpenConversation(conv.conversationId, otherUserId) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.padding(start = 72.dp)
                        )
                    }
                }
            }
        }
    }

    // New Conversation Dialog
    if (showNewChatDialog) {
        NewChatDialog(
            allUsers = allUsers.filter { it.userId != viewModel.currentUserId },
            onDismiss = { showNewChatDialog = false },
            onUserSelected = { selectedUser ->
                showNewChatDialog = false
                viewModel.startConversationWithUser(selectedUser.userId) { convId ->
                    onOpenConversation(convId, selectedUser.userId)
                }
            }
        )
    }
}

@Composable
fun ConversationRow(
    conversation: Conversation,
    otherUser: UserProfile?,
    otherUserId: String,
    onClick: () -> Unit
) {
    val timeAgo = remember(conversation.lastMessageTimestamp) {
        val seconds = conversation.lastMessageTimestamp?.seconds
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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("conversation_row_${conversation.conversationId}")
    ) {
        UserAvatar(
            avatarUrl = otherUser?.avatarUrl,
            name = otherUser?.displayName ?: "User",
            size = 50.dp,
            showOnlineBadge = true,
            isOnline = otherUser?.isOnline == true
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = otherUser?.displayName ?: "User",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (timeAgo.isNotBlank()) {
                    Text(
                        text = timeAgo,
                        style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (conversation.lastMessageText.isNotBlank()) conversation.lastMessageText else "Started a conversation",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ZaraSlate,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NewChatDialog(
    allUsers: List<UserProfile>,
    onDismiss: () -> Unit,
    onUserSelected: (UserProfile) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allUsers, query) {
        if (query.isBlank()) allUsers
        else {
            val q = query.trim().lowercase()
            allUsers.filter { it.username.contains(q) || it.displayName.lowercase().contains(q) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Message", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ZaraSlate) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Text("No users found", color = ZaraSlate)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(filtered, key = { it.userId }) { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelected(user) }
                                    .padding(vertical = 8.dp)
                            ) {
                                UserAvatar(
                                    avatarUrl = user.avatarUrl,
                                    name = user.displayName,
                                    size = 40.dp,
                                    showOnlineBadge = true,
                                    isOnline = user.isOnline
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(user.displayName, fontWeight = FontWeight.SemiBold)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
