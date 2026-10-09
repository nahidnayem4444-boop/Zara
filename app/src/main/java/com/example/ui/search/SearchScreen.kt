package com.example.ui.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.ZaraViewModel
import com.example.ui.components.UserAvatar
import com.example.ui.home.PostCard
import com.example.ui.theme.ZaraMuted
import com.example.ui.theme.ZaraSlate

val TrendingHashtags = listOf("#minimal", "#thoughts", "#photography", "#daily", "#design", "#books", "#coffee", "#zara")

@Composable
fun SearchScreen(
    viewModel: ZaraViewModel,
    onNavigateToUserProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val allUsers by viewModel.allUsers.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()
    val followingIds by viewModel.followingIds.collectAsState()
    val blockedIds by viewModel.blockedUserIds.collectAsState()

    val isHashtagSearch = searchQuery.startsWith("#") || searchQuery.contains("#")

    val matchingUsers = remember(allUsers, searchQuery, blockedIds) {
        if (searchQuery.isBlank() || isHashtagSearch) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allUsers.filter { user ->
                user.userId != viewModel.currentUserId &&
                user.userId !in blockedIds &&
                (user.username.contains(q) || user.displayName.lowercase().contains(q))
            }
        }
    }

    val matchingPosts = remember(feedPosts, searchQuery, isHashtagSearch, blockedIds) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            feedPosts.filter { post ->
                post.authorId !in blockedIds && post.content.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users or #hashtags...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ZaraSlate) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ZaraSlate)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_text_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Trending Hashtags Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(TrendingHashtags) { tag ->
                AssistChip(
                    onClick = { searchQuery = tag },
                    label = { Text(tag, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Search Results List
        if (searchQuery.isBlank()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Discover on ZARA",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Search for creators by @username, name, or explore trending #hashtags.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else if (isHashtagSearch) {
            // Hashtag Posts Results
            if (matchingPosts.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No posts found for $searchQuery", style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(matchingPosts, key = { it.postId }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = viewModel.currentUserId,
                            viewModel = viewModel,
                            onUserClick = { onNavigateToUserProfile(post.authorId) },
                            onCommentClick = {}
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }
        } else {
            // User Search Results
            if (matchingUsers.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("No users found matching \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium.copy(color = ZaraSlate))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(matchingUsers, key = { it.userId }) { user ->
                        val isFollowing = user.userId in followingIds
                        UserSearchRow(
                            user = user,
                            isFollowing = isFollowing,
                            onUserClick = { onNavigateToUserProfile(user.userId) },
                            onFollowClick = {
                                if (isFollowing) {
                                    viewModel.unfollowUser(user.userId)
                                } else {
                                    viewModel.followUser(user.userId, user.isPrivate)
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchRow(
    user: UserProfile,
    isFollowing: Boolean,
    onUserClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        UserAvatar(avatarUrl = user.avatarUrl, name = user.displayName, size = 46.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall.copy(color = ZaraSlate)
            )
            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }

        if (isFollowing) {
            OutlinedButton(
                onClick = onFollowClick,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Following", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
        } else {
            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text("Follow", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}
