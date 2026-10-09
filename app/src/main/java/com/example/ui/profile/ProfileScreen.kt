package com.example.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.ZaraViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.home.PostCard
import com.example.ui.theme.ZaraAccentRed
import com.example.ui.theme.ZaraSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    viewModel: ZaraViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSelf = userId == viewModel.currentUserId

    val profileFlow = remember(userId) { viewModel.repository.observeUserProfile(userId) }
    val profile by profileFlow.collectAsState(initial = null)

    val postsFlow = remember(userId) { viewModel.repository.observeUserPosts(userId) }
    val posts by postsFlow.collectAsState(initial = emptyList())

    val followStatusFlow = remember(userId) { viewModel.repository.observeFollowStatus(userId) }
    val followStatus by followStatusFlow.collectAsState(initial = null)

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("@${profile?.username ?: "profile"}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSelf) {
                        IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_button")) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    } else {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Report user") },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.reportContent("user", userId, "Inappropriate profile")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block user") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = ZaraAccentRed) },
                                onClick = {
                                    showMenu = false
                                    viewModel.blockUser(userId)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Profile Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserAvatar(
                            avatarUrl = profile?.avatarUrl,
                            name = profile?.displayName ?: "User",
                            size = 72.dp,
                            showOnlineBadge = true,
                            isOnline = profile?.isOnline == true
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        // Stats
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.weight(1f)
                        ) {
                            StatColumn("Posts", "${posts.size}")
                            StatColumn("Followers", "${profile?.followersCount ?: 0}")
                            StatColumn("Following", "${profile?.followingCount ?: 0}")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Name and Bio
                    Text(
                        text = profile?.displayName ?: "User",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (!profile?.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = profile?.bio ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons (Follow, Edit Profile, Chat)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSelf) {
                            OutlinedButton(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile")
                            }
                        } else {
                            if (followStatus == "accepted") {
                                OutlinedButton(
                                    onClick = { viewModel.unfollowUser(userId) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Following")
                                }
                            } else if (followStatus == "pending") {
                                OutlinedButton(
                                    onClick = { viewModel.unfollowUser(userId) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Requested")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.followUser(userId, profile?.isPrivate == true) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Follow")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.startConversationWithUser(userId) { convId ->
                                        onNavigateToChat(convId)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }

            // User's Posts Section
            if (posts.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Text("No posts yet", style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate))
                    }
                }
            } else {
                items(posts, key = { it.postId }) { post ->
                    PostCard(
                        post = post,
                        currentUserId = viewModel.currentUserId,
                        viewModel = viewModel,
                        onUserClick = {},
                        onCommentClick = {}
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            }
        }
    }

    if (showEditProfileDialog && profile != null) {
        EditProfileDialog(
            currentProfile = profile!!,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, handle, bio, isPrivate ->
                viewModel.updateProfile(name, handle, bio, null, isPrivate) {
                    showEditProfileDialog = false
                }
            }
        )
    }
}

@Composable
fun StatColumn(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate)
        )
    }
}

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.displayName) }
    var username by remember { mutableStateOf(currentProfile.username) }
    var bio by remember { mutableStateOf(currentProfile.bio) }
    var isPrivate by remember { mutableStateOf(currentProfile.isPrivate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Account", fontWeight = FontWeight.SemiBold)
                        Text("Require approval for new followers", style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate))
                    }
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, username, bio, isPrivate) },
                enabled = name.isNotBlank() && username.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
