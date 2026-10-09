package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.example.data.repository.ZaraRepository
import com.example.ui.theme.ZaraAccentRed
import com.example.ui.theme.ZaraBlack
import com.example.ui.theme.ZaraGreen
import com.example.ui.theme.ZaraSlate
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Log In, 1 = Sign Up
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Sign Up custom initial profile inputs
    var customUsername by remember { mutableStateOf("") }
    var customDisplayName by remember { mutableStateOf("") }
    var customBio by remember { mutableStateOf("") }

    // Password reset dialog state
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    // Attempt silent auto-sign-in on startup with single-option request
    LaunchedEffect(Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            onAuthSuccess()
            return@LaunchedEffect
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return@LaunchedEffect
        }

        val autoSignInOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(autoSignInOption).build()

        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            }
        } catch (e: Exception) {
            // Normal silent fallback to interactive prompt
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp)
        ) {
            // Minimalist Brand Identity
            Text(
                text = "ZARA",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.testTag("auth_title")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pure, distraction-free social & real-time chat",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ZaraSlate,
                    fontSize = 14.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Navigation Tabs: Log In | Sign Up
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_mode_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "Log In",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("login_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    text = {
                        Text(
                            text = "Sign Up",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("signup_tab")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (selectedTab == 0) {
                // --- LOG IN VIEW ---
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sign in with your verified Google account credentials to access your ZARA feed and real-time messages.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Login Action Button
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        val clientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (e: Exception) {
                            errorMessage = "Google Sign-In configuration missing"
                            isLoading = false
                            return@Button
                        }

                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                        scope.launch {
                            try {
                                val result = credentialManager.getCredential(context as Activity, request)
                                val cred = result.credential
                                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                                    val authCredential = GoogleAuthProvider.getCredential(token, null)
                                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                                    val user = authResult.user
                                    if (user != null) {
                                        val repo = ZaraRepository(context)
                                        val defaultHandle = (user.email?.substringBefore("@") ?: "user_${user.uid.take(5)}")
                                            .filter { it.isLetterOrDigit() || it == '_' }
                                        repo.saveUserProfile(
                                            UserProfile(
                                                userId = user.uid,
                                                username = defaultHandle,
                                                displayName = user.displayName ?: "User",
                                                avatarUrl = user.photoUrl?.toString(),
                                                isOnline = true
                                            )
                                        )
                                    }
                                    isLoading = false
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Unexpected credential format"
                                    isLoading = false
                                }
                            } catch (e: GetCredentialCancellationException) {
                                Log.w("Auth", "Log In cancelled: ${e.message}")
                                isLoading = false
                            } catch (e: Exception) {
                                Log.e("Auth", "Log In failed", e)
                                errorMessage = e.localizedMessage ?: "Log In failed"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Log In with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Reset & Recovery
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp), tint = ZaraSlate)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Password Reset / Account Recovery",
                        style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate)
                    )
                }

            } else {
                // --- SIGN UP VIEW ---
                Text(
                    text = "Create Your Account",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set your desired profile handle and connect securely via Google Credentials.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Custom handle and display name fields
                OutlinedTextField(
                    value = customUsername,
                    onValueChange = { customUsername = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                    label = { Text("Choose Username (@handle)") },
                    placeholder = { Text("e.g. alex_design") },
                    leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = ZaraSlate, modifier = Modifier.padding(start = 12.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_username_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customDisplayName,
                    onValueChange = { customDisplayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Alex Rivera") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ZaraSlate) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_displayname_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customBio,
                    onValueChange = { customBio = it },
                    label = { Text("Bio (optional)") },
                    placeholder = { Text("Tell the ZARA community about you...") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_bio_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Sign Up Action Button
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        val clientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (e: Exception) {
                            errorMessage = "Google configuration missing"
                            isLoading = false
                            return@Button
                        }

                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                        scope.launch {
                            try {
                                val result = credentialManager.getCredential(context as Activity, request)
                                val cred = result.credential
                                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                                    val authCredential = GoogleAuthProvider.getCredential(token, null)
                                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                                    val user = authResult.user
                                    if (user != null) {
                                        val repo = ZaraRepository(context)
                                        val finalHandle = if (customUsername.isNotBlank())
                                            customUsername.trim().lowercase()
                                        else
                                            (user.email?.substringBefore("@") ?: "user_${user.uid.take(5)}").filter { it.isLetterOrDigit() || it == '_' }

                                        val finalName = if (customDisplayName.isNotBlank())
                                            customDisplayName.trim()
                                        else
                                            user.displayName ?: "User"

                                        repo.saveUserProfile(
                                            UserProfile(
                                                userId = user.uid,
                                                username = finalHandle,
                                                displayName = finalName,
                                                bio = customBio.trim(),
                                                avatarUrl = user.photoUrl?.toString(),
                                                isOnline = true
                                            )
                                        )
                                    }
                                    isLoading = false
                                    onAuthSuccess()
                                } else {
                                    errorMessage = "Unexpected credential format"
                                    isLoading = false
                                }
                            } catch (e: GetCredentialCancellationException) {
                                Log.w("Auth", "Sign Up cancelled: ${e.message}")
                                isLoading = false
                            } catch (e: Exception) {
                                Log.e("Auth", "Sign Up failed", e)
                                errorMessage = e.localizedMessage ?: "Sign Up failed"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign Up with Google",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Error Message Display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(color = ZaraAccentRed),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Password Reset / Account Recovery Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                resetMessage = null
            },
            title = { Text("Account Recovery & Password Reset", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Enter your Google email address to receive recovery and account verification instructions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (resetMessage?.contains("sent", ignoreCase = true) == true) ZaraGreen else ZaraAccentRed,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            scope.launch {
                                try {
                                    Firebase.auth.sendPasswordResetEmail(resetEmail.trim()).await()
                                    resetMessage = "Recovery instructions sent to $resetEmail"
                                } catch (e: Exception) {
                                    resetMessage = e.localizedMessage ?: "Failed to send instructions"
                                }
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
