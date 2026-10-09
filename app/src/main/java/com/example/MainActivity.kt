package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.ZaraRepository
import com.example.ui.ZaraViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.ChatDetailScreen
import com.example.ui.chat.ChatListScreen
import com.example.ui.create.CreatePostScreen
import com.example.ui.home.HomeScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class BottomNavTab {
    HOME, SEARCH, CREATE, CHAT, PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        ZaraMainApp(currentUserId = user.uid)
    }
}

@Composable
fun ZaraMainApp(currentUserId: String) {
    val viewModel: ZaraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ZaraViewModel(ZaraRepository(db), currentUserId)
            }
        }
    )

    var currentTab by remember { mutableStateOf(BottomNavTab.HOME) }
    var activeChatPair by remember { mutableStateOf<Pair<String, String>?>(null) } // (convId, otherUserId)
    var viewingProfileUserId by remember { mutableStateOf<String?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val isTopLevel = activeChatPair == null && viewingProfileUserId == null && !showNotifications && !showSettings

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.HOME,
                        onClick = { currentTab = BottomNavTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.SEARCH,
                        onClick = { currentTab = BottomNavTab.SEARCH },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        modifier = Modifier.testTag("nav_item_search")
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.CREATE,
                        onClick = { currentTab = BottomNavTab.CREATE },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create", modifier = Modifier.size(28.dp)) },
                        label = { Text("Post") },
                        modifier = Modifier.testTag("nav_item_create")
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.CHAT,
                        onClick = { currentTab = BottomNavTab.CHAT },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") },
                        modifier = Modifier.testTag("nav_item_chat")
                    )
                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.PROFILE,
                        onClick = { currentTab = BottomNavTab.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        modifier = Modifier.testTag("nav_item_profile")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Secondary screens take precedence with BackHandler
                activeChatPair != null -> {
                    BackHandler { activeChatPair = null }
                    val (convId, otherId) = activeChatPair!!
                    ChatDetailScreen(
                        conversationId = convId,
                        otherUserId = otherId,
                        viewModel = viewModel,
                        onBackClick = { activeChatPair = null }
                    )
                }
                viewingProfileUserId != null -> {
                    BackHandler { viewingProfileUserId = null }
                    ProfileScreen(
                        userId = viewingProfileUserId!!,
                        viewModel = viewModel,
                        onNavigateToSettings = { showSettings = true },
                        onNavigateToChat = { convId ->
                            activeChatPair = Pair(convId, viewingProfileUserId!!)
                        },
                        onBackClick = { viewingProfileUserId = null }
                    )
                }
                showNotifications -> {
                    BackHandler { showNotifications = false }
                    NotificationsScreen(
                        viewModel = viewModel,
                        onBackClick = { showNotifications = false },
                        onNavigateToUser = { uid ->
                            showNotifications = false
                            viewingProfileUserId = uid
                        },
                        onNavigateToChat = { convId ->
                            showNotifications = false
                            val conv = viewModel.conversations.value.firstOrNull { it.conversationId == convId }
                            val otherId = conv?.participantIds?.firstOrNull { it != currentUserId } ?: ""
                            activeChatPair = Pair(convId, otherId)
                        }
                    )
                }
                showSettings -> {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { showSettings = false },
                        onSignOut = {
                            showSettings = false
                            // Will transition to AuthScreen via Firebase.auth listener
                        }
                    )
                }
                // Primary bottom tabs
                else -> {
                    when (currentTab) {
                        BottomNavTab.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToNotifications = { showNotifications = true },
                                onNavigateToUserProfile = { uid ->
                                    if (uid == currentUserId) currentTab = BottomNavTab.PROFILE
                                    else viewingProfileUserId = uid
                                },
                                onNavigateToCreatePost = { currentTab = BottomNavTab.CREATE }
                            )
                        }
                        BottomNavTab.SEARCH -> {
                            SearchScreen(
                                viewModel = viewModel,
                                onNavigateToUserProfile = { uid ->
                                    if (uid == currentUserId) currentTab = BottomNavTab.PROFILE
                                    else viewingProfileUserId = uid
                                }
                            )
                        }
                        BottomNavTab.CREATE -> {
                            CreatePostScreen(
                                viewModel = viewModel,
                                onPostCreated = { currentTab = BottomNavTab.HOME }
                            )
                        }
                        BottomNavTab.CHAT -> {
                            ChatListScreen(
                                viewModel = viewModel,
                                onOpenConversation = { convId, otherId ->
                                    activeChatPair = Pair(convId, otherId)
                                }
                            )
                        }
                        BottomNavTab.PROFILE -> {
                            ProfileScreen(
                                userId = currentUserId,
                                viewModel = viewModel,
                                onNavigateToSettings = { showSettings = true },
                                onNavigateToChat = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
