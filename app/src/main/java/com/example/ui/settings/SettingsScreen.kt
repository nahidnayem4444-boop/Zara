package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.ui.ZaraViewModel
import com.example.ui.profile.EditProfileDialog
import com.example.ui.theme.ZaraAccentRed
import com.example.ui.theme.ZaraSlate
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ZaraViewModel,
    onBackClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val blockedIds by viewModel.blockedUserIds.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var showEditProfile by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    var notifLikes by remember { mutableStateOf(true) }
    var notifComments by remember { mutableStateOf(true) }
    var notifFollows by remember { mutableStateOf(true) }
    var notifMessages by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Account & Profile Section
            Text("ACCOUNT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZaraSlate))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRow(
                        title = "Edit Profile",
                        subtitle = "Name, username, bio",
                        onClick = { showEditProfile = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    SettingsRow(
                        title = "Change Password",
                        subtitle = "Send account password reset email",
                        onClick = { showPasswordDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Section
            Text("PRIVACY & SAFETY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZaraSlate))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Private Account", fontWeight = FontWeight.SemiBold)
                            Text("Only approved users can follow you", style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate))
                        }
                        Switch(
                            checked = userProfile?.isPrivate == true,
                            onCheckedChange = { isPrivate ->
                                userProfile?.let {
                                    viewModel.updateProfile(it.displayName, it.username, it.bio, it.avatarUrl, isPrivate)
                                }
                            }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    SettingsRow(
                        title = "Blocked Users (${blockedIds.size})",
                        subtitle = "Manage blocked accounts",
                        onClick = { showBlockedUsersDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notifications Section
            Text("NOTIFICATIONS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZaraSlate))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    NotificationToggleRow("Likes", notifLikes) { notifLikes = it }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    NotificationToggleRow("Comments", notifComments) { notifComments = it }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    NotificationToggleRow("New Followers", notifFollows) { notifFollows = it }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    NotificationToggleRow("Chat Messages", notifMessages) { notifMessages = it }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About ZARA Section
            Text("ABOUT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = ZaraSlate))
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ZARA", fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Version 1.0.0 — Pure, lightweight, real-time social platform.", style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Log Out Button
            Button(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out of ZARA?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        Firebase.auth.signOut()
                        scope.launch {
                            try {
                                val credMan = CredentialManager.create(context)
                                credMan.clearCredentialState(ClearCredentialStateRequest())
                            } catch (ignored: Exception) {}
                            onSignOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZaraAccentRed)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasswordDialog) {
        val userEmail = Firebase.auth.currentUser?.email ?: ""
        var msg by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    Text("We will send a password reset link to $userEmail.")
                    if (msg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(msg ?: "", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (userEmail.isNotBlank()) {
                        scope.launch {
                            try {
                                Firebase.auth.sendPasswordResetEmail(userEmail).await()
                                msg = "Email sent! Check your inbox."
                            } catch (e: Exception) {
                                msg = e.localizedMessage ?: "Failed to send reset email"
                            }
                        }
                    }
                }) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Close") }
            }
        )
    }

    if (showBlockedUsersDialog) {
        val userMap = remember(allUsers) { allUsers.associateBy { it.userId } }
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            title = { Text("Blocked Users") },
            text = {
                if (blockedIds.isEmpty()) {
                    Text("You have not blocked any users.", color = ZaraSlate)
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(blockedIds.toList()) { blockedId ->
                            val user = userMap[blockedId]
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = user?.displayName ?: "@$blockedId",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(onClick = { viewModel.unblockUser(blockedId) }) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) { Text("Done") }
            }
        )
    }

    if (showEditProfile && userProfile != null) {
        EditProfileDialog(
            currentProfile = userProfile!!,
            onDismiss = { showEditProfile = false },
            onSave = { name, handle, bio, isPrivate ->
                viewModel.updateProfile(name, handle, bio, null, isPrivate) {
                    showEditProfile = false
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate))
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
