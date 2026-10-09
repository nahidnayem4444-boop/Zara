package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.UserProfile
import com.example.data.repository.ZaraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ZaraViewModel(
    val repository: ZaraRepository,
    val currentUserId: String
) : ViewModel() {

    // --- State Streams ---

    val currentUserProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val feedPosts: StateFlow<List<Post>> = repository.observeFeedPosts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followingIds: StateFlow<Set<String>> = repository.observeFollowingIds(currentUserId)
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val conversations: StateFlow<List<Conversation>> = repository.observeConversations()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationItem>> = repository.observeNotifications()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedUserIds: StateFlow<Set<String>> = repository.observeBlockedUserIds()
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    init {
        // Set online status on ViewModel start
        viewModelScope.launch {
            repository.setOnlineStatus(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Best effort offline status
        viewModelScope.launch {
            repository.setOnlineStatus(false)
        }
    }

    // --- Profile Actions ---

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        avatarUrl: String?,
        isPrivate: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val existing = currentUserProfile.value
            val updated = UserProfile(
                userId = currentUserId,
                username = username.trim().lowercase(),
                displayName = displayName.trim(),
                bio = bio.trim(),
                avatarUrl = avatarUrl ?: existing?.avatarUrl,
                isPrivate = isPrivate,
                isOnline = true,
                followersCount = existing?.followersCount ?: 0L,
                followingCount = existing?.followingCount ?: 0L,
                postsCount = existing?.postsCount ?: 0L
            )
            val res = repository.saveUserProfile(updated)
            if (res.isSuccess) {
                _statusMessage.value = "Profile updated"
                onComplete(true)
            } else {
                _statusMessage.value = "Failed to update profile: ${res.exceptionOrNull()?.localizedMessage}"
                onComplete(false)
            }
        }
    }

    // --- Post Actions ---

    fun createPost(content: String, imageUrl: String? = null, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.createPost(content.trim(), imageUrl)
            if (result.isSuccess) {
                _statusMessage.value = "Post shared successfully"
                onComplete(true)
            } else {
                _statusMessage.value = "Failed to create post: ${result.exceptionOrNull()?.localizedMessage}"
                onComplete(false)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val res = repository.deletePost(postId)
            if (res.isSuccess) {
                _statusMessage.value = "Post deleted"
            } else {
                _statusMessage.value = "Failed to delete post"
            }
        }
    }

    fun toggleLike(postId: String, isLiked: Boolean, authorId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId, isLiked, authorId)
        }
    }

    fun addComment(postId: String, text: String, authorId: String, onComplete: () -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = repository.addComment(postId, text.trim(), authorId)
            if (res.isSuccess) {
                onComplete()
            } else {
                _statusMessage.value = "Failed to add comment"
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(postId, commentId)
        }
    }

    // --- Follow Actions ---

    fun followUser(targetUserId: String, isTargetPrivate: Boolean) {
        viewModelScope.launch {
            val res = repository.followUser(targetUserId, isTargetPrivate)
            if (res.isSuccess) {
                _statusMessage.value = if (isTargetPrivate) "Follow request sent" else "Following user"
            }
        }
    }

    fun unfollowUser(targetUserId: String) {
        viewModelScope.launch {
            val res = repository.unfollowUser(targetUserId)
            if (res.isSuccess) {
                _statusMessage.value = "Unfollowed"
            }
        }
    }

    fun respondToFollowRequest(followerId: String, accept: Boolean) {
        viewModelScope.launch {
            val res = repository.respondToFollowRequest(followerId, accept)
            if (res.isSuccess) {
                _statusMessage.value = if (accept) "Request accepted" else "Request declined"
            }
        }
    }

    // --- Chat Actions ---

    fun sendMessage(
        conversationId: String,
        participantIds: List<String>,
        text: String,
        imageUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (text.isBlank() && imageUrl.isNullOrBlank()) return
        viewModelScope.launch {
            val res = repository.sendMessage(conversationId, participantIds, text.trim(), imageUrl, replyToId, replyToText)
            onComplete(res.isSuccess)
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(conversationId, messageId)
        }
    }

    fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.setTypingIndicator(conversationId, isTyping)
        }
    }

    fun markMessagesAsSeen(conversationId: String, messages: List<ChatMessage>) {
        viewModelScope.launch {
            repository.markMessagesAsSeen(conversationId, messages)
        }
    }

    fun startConversationWithUser(otherUserId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val res = repository.getOrCreateConversation(otherUserId)
            if (res.isSuccess) {
                onReady(res.getOrThrow())
            } else {
                _statusMessage.value = "Failed to start chat"
            }
        }
    }

    // --- Notification Actions ---

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    // --- Moderation & Privacy ---

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            val res = repository.blockUser(targetUserId)
            if (res.isSuccess) {
                _statusMessage.value = "User blocked"
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            val res = repository.unblockUser(targetUserId)
            if (res.isSuccess) {
                _statusMessage.value = "User unblocked"
            }
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.reportContent(targetType, targetId, reason)
            if (res.isSuccess) {
                _statusMessage.value = "Report submitted. Thank you for keeping ZARA safe."
            }
        }
    }
}
