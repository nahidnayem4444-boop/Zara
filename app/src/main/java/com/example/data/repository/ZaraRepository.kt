package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.BlockItem
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.FollowRelation
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.ReportItem
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ZaraRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- Profile & User Management ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                emit(null)
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val path = "users/$uid"
        try {
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val finalProfile = profile.copy(userId = uid)
            if (snapshot.exists()) {
                val existing = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                userRef.update(
                    mapOf(
                        "username" to finalProfile.username,
                        "displayName" to finalProfile.displayName,
                        "bio" to finalProfile.bio,
                        "avatarUrl" to finalProfile.avatarUrl,
                        "isPrivate" to finalProfile.isPrivate,
                        "isOnline" to finalProfile.isOnline,
                        "followersCount" to (existing?.followersCount ?: 0L),
                        "followingCount" to (existing?.followingCount ?: 0L),
                        "postsCount" to (existing?.postsCount ?: 0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).await()
            } else {
                userRef.set(finalProfile.toMap()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection(path)
            .limit(50)
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun setOnlineStatus(isOnline: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.success(Unit)
        val path = "users/$uid"
        try {
            db.collection("users").document(uid)
                .update("isOnline", isOnline, "updatedAt", FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Posts ---

    fun observeFeedPosts(): Flow<List<Post>> {
        val path = "posts"
        return db.collection(path)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    fun observeUserPosts(userId: String): Flow<List<Post>> {
        val path = "posts"
        return db.collection(path)
            .whereEqualTo("authorId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Post::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun createPost(content: String, imageUrl: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val postId = "post_${UUID.randomUUID()}"
        val path = "posts/$postId"
        try {
            val userSnap = db.collection("users").document(uid).get().await()
            val user = userSnap.toObject(UserProfile::class.java)
            val username = user?.username ?: "user_${uid.take(5)}"
            val displayName = user?.displayName ?: "User"
            val avatarUrl = user?.avatarUrl

            val post = Post(
                postId = postId,
                authorId = uid,
                authorUsername = username,
                authorDisplayName = displayName,
                authorAvatarUrl = avatarUrl,
                content = content,
                imageUrl = imageUrl,
                likesCount = 0L,
                commentsCount = 0L,
                repostsCount = 0L
            )
            db.collection("posts").document(postId).set(post.toMap()).await()

            // Increment user's posts count
            try {
                db.collection("users").document(uid).update(
                    "postsCount", FieldValue.increment(1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (ignored: Exception) {}

            Result.success(postId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val path = "posts/$postId"
        try {
            db.collection("posts").document(postId).delete().await()
            try {
                db.collection("users").document(uid).update(
                    "postsCount", FieldValue.increment(-1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (ignored: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Likes ---

    fun observeIsLiked(postId: String, userId: String): Flow<Boolean> {
        val path = "posts/$postId/likes/$userId"
        return db.collection("posts").document(postId).collection("likes").document(userId)
            .snapshots()
            .map { it.exists() }
            .catch { emit(false) }
            .flowOn(Dispatchers.IO)
    }

    suspend fun toggleLike(postId: String, currentIsLiked: Boolean, postAuthorId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val path = "posts/$postId/likes/$uid"
        try {
            val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
            val postRef = db.collection("posts").document(postId)
            if (currentIsLiked) {
                likeRef.delete().await()
                postRef.update(
                    "likesCount", FieldValue.increment(-1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } else {
                val likeData = mapOf(
                    "userId" to uid,
                    "postId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                likeRef.set(likeData).await()
                postRef.update(
                    "likesCount", FieldValue.increment(1),
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()

                if (postAuthorId != uid) {
                    sendNotification(
                        targetUserId = postAuthorId,
                        type = "like",
                        targetId = postId,
                        content = "liked your post"
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Comments ---

    fun observeComments(postId: String): Flow<List<PostComment>> {
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId).collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)
    }

    suspend fun addComment(postId: String, text: String, postAuthorId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val commentId = "cmt_${UUID.randomUUID()}"
        val path = "posts/$postId/comments/$commentId"
        try {
            val userSnap = db.collection("users").document(uid).get().await()
            val user = userSnap.toObject(UserProfile::class.java)
            val comment = PostComment(
                commentId = commentId,
                postId = postId,
                authorId = uid,
                authorUsername = user?.username ?: "user",
                authorDisplayName = user?.displayName ?: "User",
                authorAvatarUrl = user?.avatarUrl,
                content = text
            )
            db.collection("posts").document(postId).collection("comments").document(commentId)
                .set(comment.toMap()).await()

            db.collection("posts").document(postId).update(
                "commentsCount", FieldValue.increment(1),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            if (postAuthorId != uid) {
                sendNotification(
                    targetUserId = postAuthorId,
                    type = "comment",
                    targetId = postId,
                    content = "commented: \"${text.take(30)}\""
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "posts/$postId/comments/$commentId"
        try {
            db.collection("posts").document(postId).collection("comments").document(commentId).delete().await()
            db.collection("posts").document(postId).update(
                "commentsCount", FieldValue.increment(-1),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Follow System ---

    fun observeFollowStatus(targetUserId: String): Flow<String?> {
        val uid = auth.currentUser?.uid ?: return flow { emit(null) }
        val followId = "${uid}_${targetUserId}"
        val path = "follows/$followId"
        return db.collection("follows").document(followId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.getString("status") else null
            }
            .catch { emit(null) }
            .flowOn(Dispatchers.IO)
    }

    suspend fun followUser(targetUserId: String, isTargetPrivate: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val followId = "${uid}_${targetUserId}"
        val path = "follows/$followId"
        val status = if (isTargetPrivate) "pending" else "accepted"
        try {
            val relation = FollowRelation(
                followerId = uid,
                followingId = targetUserId,
                status = status
            )
            db.collection("follows").document(followId).set(relation.toMap()).await()

            if (status == "accepted") {
                try {
                    db.collection("users").document(uid).update(
                        "followingCount", FieldValue.increment(1),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                    db.collection("users").document(targetUserId).update(
                        "followersCount", FieldValue.increment(1),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                } catch (ignored: Exception) {}

                sendNotification(
                    targetUserId = targetUserId,
                    type = "follow",
                    targetId = uid,
                    content = "started following you"
                )
            } else {
                sendNotification(
                    targetUserId = targetUserId,
                    type = "follow_request",
                    targetId = uid,
                    content = "requested to follow you"
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val followId = "${uid}_${targetUserId}"
        val path = "follows/$followId"
        try {
            val snap = db.collection("follows").document(followId).get().await()
            val wasAccepted = snap.getString("status") == "accepted"
            db.collection("follows").document(followId).delete().await()

            if (wasAccepted) {
                try {
                    db.collection("users").document(uid).update(
                        "followingCount", FieldValue.increment(-1),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                    db.collection("users").document(targetUserId).update(
                        "followersCount", FieldValue.increment(-1),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                } catch (ignored: Exception) {}
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToFollowRequest(followerId: String, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val followId = "${followerId}_${uid}"
        val path = "follows/$followId"
        try {
            if (accept) {
                db.collection("follows").document(followId).update("status", "accepted").await()
                try {
                    db.collection("users").document(followerId).update(
                        "followingCount", FieldValue.increment(1),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                    db.collection("users").document(uid).update(
                        "followersCount", FieldValue.increment(1),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                } catch (ignored: Exception) {}
            } else {
                db.collection("follows").document(followId).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeFollowingIds(userId: String): Flow<Set<String>> {
        val path = "follows"
        return db.collection(path)
            .whereEqualTo("followerId", userId)
            .whereEqualTo("status", "accepted")
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.getString("followingId") }.toSet()
            }
            .catch { emit(emptySet()) }
            .flowOn(Dispatchers.IO)
    }

    // --- Real-Time Chat ---

    fun observeConversations(): Flow<List<Conversation>> {
        val uid = auth.currentUser?.uid ?: return flow { emit(emptyList()) }
        val path = "conversations"
        return db.collection(path)
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastMessageTimestamp?.seconds ?: it.updatedAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getOrCreateConversation(otherUserId: String): Result<String> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val path = "conversations"
        try {
            // Find existing conversation with both participants
            val query = db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .get().await()

            val existing = query.documents.firstOrNull { doc ->
                val list = doc.get("participantIds") as? List<*>
                list?.contains(otherUserId) == true
            }

            if (existing != null) {
                return@withContext Result.success(existing.id)
            }

            val convId = "conv_${UUID.randomUUID()}"
            val conversation = Conversation(
                conversationId = convId,
                participantIds = listOf(uid, otherUserId).sorted(),
                lastMessageText = "",
                lastMessageSenderId = "",
                lastMessageTimestamp = null,
                typingUserId = null
            )
            db.collection("conversations").document(convId).set(conversation.toMap()).await()
            Result.success(convId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .snapshots()
            .map { snap ->
                snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                emit(emptyList())
            }
            .flowOn(Dispatchers.IO)
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> {
        val path = "conversations/$conversationId"
        return db.collection("conversations").document(conversationId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { emit(null) }
            .flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(
        conversationId: String,
        participantIds: List<String>,
        text: String,
        imageUrl: String? = null,
        replyToId: String? = null,
        replyToText: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val messageId = "msg_${UUID.randomUUID()}"
        val path = "conversations/$conversationId/messages/$messageId"
        try {
            val userSnap = db.collection("users").document(uid).get().await()
            val senderName = userSnap.getString("displayName") ?: "User"

            val message = ChatMessage(
                messageId = messageId,
                conversationId = conversationId,
                participantIds = participantIds,
                senderId = uid,
                senderName = senderName,
                text = text,
                imageUrl = imageUrl,
                replyToId = replyToId,
                replyToText = replyToText,
                status = "sent",
                isDeleted = false
            )
            db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .set(message.toMap()).await()

            // Update parent conversation last message
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "lastMessageText" to if (text.isNotBlank()) text else "📷 Photo",
                    "lastMessageSenderId" to uid,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "typingUserId" to null,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Notify other participant
            val receiverId = participantIds.firstOrNull { it != uid }
            if (receiverId != null) {
                sendNotification(
                    targetUserId = receiverId,
                    type = "message",
                    targetId = conversationId,
                    content = text.take(50).ifBlank { "Sent a photo" }
                )
            }

            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val path = "conversations/$conversationId/messages/$messageId"
        try {
            db.collection("conversations").document(conversationId)
                .collection("messages").document(messageId)
                .update("isDeleted", true, "text", "This message was deleted")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsSeen(conversationId: String, messages: List<ChatMessage>): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.success(Unit)
        val unreadMessages = messages.filter { it.senderId != uid && it.status != "seen" }
        for (msg in unreadMessages) {
            try {
                db.collection("conversations").document(conversationId)
                    .collection("messages").document(msg.messageId)
                    .update("status", "seen").await()
            } catch (ignored: Exception) {}
        }
        Result.success(Unit)
    }

    suspend fun setTypingIndicator(conversationId: String, isTyping: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext Result.success(Unit)
        try {
            db.collection("conversations").document(conversationId).update(
                "typingUserId", if (isTyping) uid else null,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (ignored: Exception) {
            Result.success(Unit)
        }
    }

    // --- Notifications ---

    fun observeNotifications(): Flow<List<NotificationItem>> {
        val uid = auth.currentUser?.uid ?: return flow { emit(emptyList()) }
        val path = "users/$uid/notifications"
        return db.collection("users").document(uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snap ->
                snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        try {
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .update("isRead", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendNotification(
        targetUserId: String,
        type: String,
        targetId: String?,
        content: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        try {
            val userSnap = db.collection("users").document(uid).get().await()
            val username = userSnap.getString("username") ?: "user"
            val displayName = userSnap.getString("displayName") ?: "User"
            val notificationId = "notif_${UUID.randomUUID()}"
            val notification = NotificationItem(
                notificationId = notificationId,
                userId = targetUserId,
                senderId = uid,
                senderUsername = username,
                senderDisplayName = displayName,
                type = type,
                targetId = targetId,
                content = content,
                isRead = false
            )
            db.collection("users").document(targetUserId)
                .collection("notifications").document(notificationId)
                .set(notification.toMap()).await()
        } catch (ignored: Exception) {}
    }

    // --- Blocks & Reports ---

    fun observeBlockedUserIds(): Flow<Set<String>> {
        val uid = auth.currentUser?.uid ?: return flow { emit(emptySet()) }
        val path = "blocks"
        return db.collection(path)
            .whereEqualTo("blockerId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.getString("blockedId") }.toSet()
            }
            .catch { emit(emptySet()) }
            .flowOn(Dispatchers.IO)
    }

    suspend fun blockUser(targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUserId}"
        val path = "blocks/$blockId"
        try {
            val block = BlockItem(blockerId = uid, blockedId = targetUserId)
            db.collection("blocks").document(blockId).set(block.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val blockId = "${uid}_${targetUserId}"
        val path = "blocks/$blockId"
        try {
            db.collection("blocks").document(blockId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun reportContent(targetType: String, targetId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = requireUserId()
        val reportId = "report_${UUID.randomUUID()}"
        val path = "reports/$reportId"
        try {
            val report = ReportItem(
                reportId = reportId,
                reporterId = uid,
                targetType = targetType,
                targetId = targetId,
                reason = reason
            )
            db.collection("reports").document(reportId).set(report.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
