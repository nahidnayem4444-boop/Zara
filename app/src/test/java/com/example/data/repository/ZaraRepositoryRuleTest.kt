package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZaraRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveUserProfile_authenticatedUser_savesSuccessfully() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = ZaraRepository(firestore)

      val profile = UserProfile(
        userId = uid,
        username = "alice_zara",
        displayName = "Alice",
        bio = "Hello from ZARA test",
        isPrivate = false,
        isOnline = true
      )
      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveUserProfile(profile)
      }
      assertTrue(result.isSuccess)

      val loaded = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeUserProfile(uid).first()
      }
      assertNotNull(loaded)
      assertEquals("alice_zara", loaded?.username)
    }
  }

  @Test
  fun createAndDeletePost_authenticatedAuthor_succeeds() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = ZaraRepository(firestore)

      repo.saveUserProfile(UserProfile(userId = uid, username = "alice", displayName = "Alice"))

      val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createPost("Excited to be on ZARA!")
      }
      assertTrue(createResult.isSuccess)
      val postId = createResult.getOrThrow()
      assertTrue(postId.isNotEmpty())

      val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.deletePost(postId)
      }
      assertTrue(deleteResult.isSuccess)
    }
  }

  @Test
  fun observeFeedPosts_unauthenticatedUser_returnsEmptyList() {
    runBlocking {
      auth.signOut()
      val repo = ZaraRepository(firestore)

      val posts = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeFeedPosts().first()
      }
      assertTrue(posts.isEmpty())
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
