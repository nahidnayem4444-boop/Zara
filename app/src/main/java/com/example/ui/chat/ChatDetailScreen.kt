package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ChatMessage
import com.example.ui.ZaraViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.create.SampleImages
import com.example.ui.theme.ZaraAccentBlue
import com.example.ui.theme.ZaraAccentRed
import com.example.ui.theme.ZaraBorder
import com.example.ui.theme.ZaraGreen
import com.example.ui.theme.ZaraMuted
import com.example.ui.theme.ZaraSlate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    otherUserId: String,
    viewModel: ZaraViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messagesFlow = remember(conversationId) { viewModel.repository.observeMessages(conversationId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    val convFlow = remember(conversationId) { viewModel.repository.observeConversation(conversationId) }
    val conversation by convFlow.collectAsState(initial = null)

    val otherUserFlow = remember(otherUserId) { viewModel.repository.observeUserProfile(otherUserId) }
    val otherUser by otherUserFlow.collectAsState(initial = null)

    var messageInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Mark messages as seen when reading
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            viewModel.markMessagesAsSeen(conversationId, messages)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Clear typing status on leave
    DisposableEffect(conversationId) {
        onDispose {
            viewModel.setTypingStatus(conversationId, false)
        }
    }

    val isOtherUserTyping = remember(conversation, otherUserId) {
        conversation?.typingUserId == otherUserId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            avatarUrl = otherUser?.avatarUrl,
                            name = otherUser?.displayName ?: "User",
                            size = 38.dp,
                            showOnlineBadge = true,
                            isOnline = otherUser?.isOnline == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = otherUser?.displayName ?: "User",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isOtherUserTyping) "typing..." else if (otherUser?.isOnline == true) "Online" else "Offline",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isOtherUserTyping) ZaraAccentBlue else if (otherUser?.isOnline == true) ZaraGreen else ZaraSlate,
                                    fontWeight = if (isOtherUserTyping) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                },
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
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    val isMine = msg.senderId == viewModel.currentUserId
                    MessageBubble(
                        message = msg,
                        isMine = isMine,
                        onReply = { replyingToMessage = msg },
                        onDelete = { viewModel.deleteMessage(conversationId, msg.messageId) }
                    )
                }

                if (isOtherUserTyping) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        ) {
                            Text(
                                text = "${otherUser?.displayName ?: "User"} is typing...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZaraSlate,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            )
                        }
                    }
                }
            }

            // Replying banner
            AnimatedVisibility(visible = replyingToMessage != null) {
                if (replyingToMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, tint = ZaraSlate, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${replyingToMessage!!.senderName}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ZaraSlate)
                            )
                            Text(
                                text = replyingToMessage!!.text.take(40),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { replyingToMessage = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = ZaraSlate, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Attached image banner
            AnimatedVisibility(visible = selectedImageUrl != null) {
                if (selectedImageUrl != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUrl,
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Photo attached",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedImageUrl = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = ZaraSlate)
                        }
                    }
                }
            }

            // Bottom Input Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = { showImagePickerDialog = true }) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Attach photo",
                        tint = if (selectedImageUrl != null) ZaraAccentBlue else ZaraSlate
                    )
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = {
                        messageInput = it
                        viewModel.setTypingStatus(conversationId, it.isNotBlank())
                    },
                    placeholder = { Text("Message...") },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank() || selectedImageUrl != null) {
                            val textToSend = messageInput
                            val imgToSend = selectedImageUrl
                            val reply = replyingToMessage
                            messageInput = ""
                            selectedImageUrl = null
                            replyingToMessage = null
                            viewModel.setTypingStatus(conversationId, false)

                            val participants = listOf(viewModel.currentUserId, otherUserId).sorted()
                            viewModel.sendMessage(
                                conversationId = conversationId,
                                participantIds = participants,
                                text = textToSend,
                                imageUrl = imgToSend,
                                replyToId = reply?.messageId,
                                replyToText = reply?.text
                            )
                        }
                    },
                    enabled = messageInput.isNotBlank() || selectedImageUrl != null,
                    modifier = Modifier.testTag("send_chat_message_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageInput.isNotBlank() || selectedImageUrl != null)
                            MaterialTheme.colorScheme.primary
                        else ZaraMuted
                    )
                }
            }
        }
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Attach Photo") },
            text = {
                Column {
                    Text("Select a photo to send in chat:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SampleImages.take(4).forEach { url ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedImageUrl = url
                                        showImagePickerDialog = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImagePickerDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(message.createdAt) {
        val seconds = message.createdAt?.seconds
        if (seconds != null) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(seconds * 1000))
        } else ""
    }

    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMine) 16.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .clickable { showMenu = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Quoted reply snippet
                if (!message.replyToText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMine) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = message.replyToText,
                            style = MaterialTheme.typography.bodySmall.copy(color = textColor.copy(alpha = 0.8f)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Attached image
                if (!message.imageUrl.isNullOrBlank() && !message.isDeleted) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Message image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontStyle = if (message.isDeleted) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time + Delivery status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                    if (isMine && !message.isDeleted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            "seen" -> Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Seen",
                                tint = ZaraAccentBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            "delivered" -> Icon(
                                Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Icon(
                                Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onReply()
                    }
                )
                if (isMine && !message.isDeleted) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ZaraAccentRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
