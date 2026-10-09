package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val isPrivate: Boolean = false,
    val isOnline: Boolean = true,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username,
        "displayName" to displayName,
        "bio" to bio,
        "avatarUrl" to avatarUrl,
        "isPrivate" to isPrivate,
        "isOnline" to isOnline,
        "followersCount" to followersCount,
        "followingCount" to followingCount,
        "postsCount" to postsCount,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val repostsCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "postId" to postId,
        "authorId" to authorId,
        "authorUsername" to authorUsername,
        "authorDisplayName" to authorDisplayName,
        "authorAvatarUrl" to authorAvatarUrl,
        "content" to content,
        "imageUrl" to imageUrl,
        "likesCount" to likesCount,
        "commentsCount" to commentsCount,
        "repostsCount" to repostsCount,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

data class FollowRelation(
    val followerId: String = "",
    val followingId: String = "",
    val status: String = "accepted", // "accepted" or "pending"
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "followerId" to followerId,
        "followingId" to followingId,
        "status" to status,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}

data class PostComment(
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String? = null,
    val content: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "commentId" to commentId,
        "postId" to postId,
        "authorId" to authorId,
        "authorUsername" to authorUsername,
        "authorDisplayName" to authorDisplayName,
        "authorAvatarUrl" to authorAvatarUrl,
        "content" to content,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}

data class Conversation(
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val typingUserId: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "conversationId" to conversationId,
        "participantIds" to participantIds,
        "lastMessageText" to lastMessageText,
        "lastMessageSenderId" to lastMessageSenderId,
        "lastMessageTimestamp" to (lastMessageTimestamp ?: FieldValue.serverTimestamp()),
        "typingUserId" to typingUserId,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

data class ChatMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val status: String = "sent", // "sent", "delivered", "seen"
    val isDeleted: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "messageId" to messageId,
        "conversationId" to conversationId,
        "participantIds" to participantIds,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "imageUrl" to imageUrl,
        "replyToId" to replyToId,
        "replyToText" to replyToText,
        "status" to status,
        "isDeleted" to isDeleted,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}

data class NotificationItem(
    val notificationId: String = "",
    val userId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderDisplayName: String = "",
    val type: String = "like", // "like", "comment", "follow", "follow_request", "message"
    val targetId: String? = null,
    val content: String = "",
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "notificationId" to notificationId,
        "userId" to userId,
        "senderId" to senderId,
        "senderUsername" to senderUsername,
        "senderDisplayName" to senderDisplayName,
        "type" to type,
        "targetId" to targetId,
        "content" to content,
        "isRead" to isRead,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}

data class BlockItem(
    val blockerId: String = "",
    val blockedId: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "blockerId" to blockerId,
        "blockedId" to blockedId,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}

data class ReportItem(
    val reportId: String = "",
    val reporterId: String = "",
    val targetType: String = "post", // "user" or "post"
    val targetId: String = "",
    val reason: String = "",
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any> = mapOf(
        "reportId" to reportId,
        "reporterId" to reporterId,
        "targetType" to targetType,
        "targetId" to targetId,
        "reason" to reason,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp())
    ).filterValues { it != null } as Map<String, Any>
}
