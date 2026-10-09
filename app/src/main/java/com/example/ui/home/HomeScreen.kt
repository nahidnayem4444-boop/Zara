package com.example.ui.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.ui.ZaraViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ZaraAccentRed
import com.example.ui.theme.ZaraBlack
import com.example.ui.theme.ZaraMuted
import com.example.ui.theme.ZaraSlate
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ZaraViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.feedPosts.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val blockedIds by viewModel.blockedUserIds.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifications = remember(notifications) { notifications.count { !it.isRead } }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activeCommentPost by remember { mutableStateOf<Post?>(null) }

    // Filter posts
    val filteredPosts = remember(posts, followingIds, blockedIds, selectedTabIndex) {
        val nonBlocked = posts.filter { it.authorId !in blockedIds }
        if (selectedTabIndex == 0) {
            // Following feed: show posts from followed users + own posts
            nonBlocked.filter { it.authorId in followingIds || it.authorId == viewModel.currentUserId }
        } else {
            // Discover feed: all public posts
            nonBlocked
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ZARA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge {
                                        Text("$unreadNotifications")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Feed Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Following",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_following")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Discover",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_discover")
                )
            }

            if (filteredPosts.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTabIndex == 0) "Your Following feed is quiet" else "No posts yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTabIndex == 0)
                                "Follow users in Discover or tap + to share your first post."
                            else
                                "Be the first to share your thoughts on ZARA!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                if (selectedTabIndex == 0) selectedTabIndex = 1 else onNavigateToCreatePost()
                            }
                        ) {
                            Text(if (selectedTabIndex == 0) "Explore Discover" else "Create a Post")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPosts, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = viewModel.currentUserId,
                            viewModel = viewModel,
                            onUserClick = { onNavigateToUserProfile(post.authorId) },
                            onCommentClick = { activeCommentPost = post },
                            modifier = Modifier.testTag("post_card_${post.postId}")
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // Comments Sheet
    if (activeCommentPost != null) {
        val post = activeCommentPost!!
        CommentsBottomSheet(
            post = post,
            viewModel = viewModel,
            onDismiss = { activeCommentPost = null }
        )
    }
}

@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    viewModel: ZaraViewModel,
    onUserClick: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isLikedFlow = remember(post.postId, currentUserId) {
        viewModel.repository.observeIsLiked(post.postId, currentUserId)
    }
    val isLiked by isLikedFlow.collectAsState(initial = false)

    val timeAgo = remember(post.createdAt) {
        val seconds = post.createdAt?.seconds ?: (System.currentTimeMillis() / 1000)
        DateUtils.getRelativeTimeSpanString(
            seconds * 1000,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Author header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            UserAvatar(
                avatarUrl = post.authorAvatarUrl,
                name = post.authorDisplayName,
                size = 40.dp,
                onClick = onUserClick
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onUserClick)
            ) {
                Text(
                    text = post.authorDisplayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${post.authorUsername} · $timeAgo",
                    style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate),
                    maxLines = 1
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Post options",
                        tint = ZaraSlate
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (post.authorId == currentUserId) {
                        DropdownMenuItem(
                            text = { Text("Delete post") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ZaraAccentRed) },
                            onClick = {
                                showMenu = false
                                viewModel.deletePost(post.postId)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Report post") },
                            leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.reportContent("post", post.postId, "Inappropriate content")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block @${post.authorUsername}") },
                            onClick = {
                                showMenu = false
                                viewModel.blockUser(post.authorId)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Post text content
        Text(
            text = post.content,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        // Optional image attachment
        if (!post.imageUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(post.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Post image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions row (Like, Comment, Repost)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Like
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { viewModel.toggleLike(post.postId, isLiked, post.authorId) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) ZaraAccentRed else ZaraSlate,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.likesCount}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isLiked) ZaraAccentRed else ZaraSlate,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Comment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onCommentClick)
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Comment",
                    tint = ZaraSlate,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.commentsCount}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ZaraSlate,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Repost / Share
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repost",
                    tint = ZaraSlate,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.repostsCount}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ZaraSlate,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    viewModel: ZaraViewModel,
    onDismiss: () -> Unit
) {
    val commentsFlow = remember(post.postId) { viewModel.repository.observeComments(post.postId) }
    val comments by commentsFlow.collectAsState(initial = emptyList())
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            if (comments.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Text(
                        text = "No comments yet. Start the conversation!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(comments, key = { it.commentId }) { comment ->
                        CommentItem(
                            comment = comment,
                            currentUserId = viewModel.currentUserId,
                            onDelete = { viewModel.deleteComment(post.postId, comment.commentId) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add comment input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Write a comment...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(post.postId, commentText, post.authorId) {
                                commentText = ""
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: PostComment,
    currentUserId: String,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        UserAvatar(avatarUrl = comment.authorAvatarUrl, name = comment.authorDisplayName, size = 32.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${comment.authorDisplayName}  @${comment.authorUsername}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (comment.authorId == currentUserId) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete comment",
                    tint = ZaraMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
