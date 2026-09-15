package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.HistoryItem
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class WatchHistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("frndom_watch_history", Context.MODE_PRIVATE)

    private val _historyFlow = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<HistoryItem>> = _historyFlow.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val json = prefs.getString("watched_posts_v2", null)
            ?: prefs.getString("watched_posts", null)

        if (!json.isNullOrBlank()) {
            val list = mutableListOf<HistoryItem>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k == "mediaUrls") {
                            val urlsArr = obj.optJSONArray("mediaUrls")
                            val urls = mutableListOf<String>()
                            if (urlsArr != null) {
                                for (j in 0 until urlsArr.length()) {
                                    urls.add(urlsArr.optString(j))
                                }
                            }
                            map[k] = urls
                        } else {
                            map[k] = obj.opt(k)
                        }
                    }
                    val item = HistoryItem.fromMap(map)
                    if (item.id.isNotBlank()) {
                        list.add(item)
                    }
                }
            } catch (_: Exception) {}
            _historyFlow.value = list
        }
    }

    /**
     * Records a post or video view in history.
     * Excludes content authored by the current user.
     */
    fun recordHistory(post: PostItem, currentUserId: String?) {
        if (post.id.isBlank()) return
        // Do not record user's own posts/videos
        if (!currentUserId.isNullOrBlank() && post.authorId == currentUserId) {
            return
        }

        val historyId = "post_${post.id}"
        val current = _historyFlow.value.filter { it.id != historyId && it.itemId != post.id }

        val detectedType = when {
            post.mediaType.equals("reel", ignoreCase = true) || post.mediaType.equals("video", ignoreCase = true) -> "video"
            post.mediaType.equals("photo", ignoreCase = true) || post.mediaType.equals("image", ignoreCase = true) -> "image"
            post.mediaUrl.isNotBlank() && (post.mediaUrl.contains(".mp4") || post.mediaUrl.contains(".mkv")) -> "video"
            post.mediaUrl.isNotBlank() -> "image"
            post.getAllMediaUrls().isNotEmpty() -> "image"
            else -> "post"
        }

        val newItem = HistoryItem(
            id = historyId,
            itemId = post.id,
            authorId = post.authorId,
            authorName = post.authorName,
            authorAvatarUrl = post.authorAvatarUrl,
            content = post.content,
            mediaType = detectedType,
            mediaUrl = post.mediaUrl,
            mediaUrls = post.getAllMediaUrls(),
            isStory = false,
            likesCount = post.likesCount,
            commentsCount = post.commentsCount,
            createdAt = post.createdAt,
            viewedAt = System.currentTimeMillis()
        )

        // Keep up to 500 items for persistent lifetime history
        val updated = (listOf(newItem) + current).take(500)
        _historyFlow.value = updated
        persistHistory(updated)
    }

    /**
     * Records a story view in history.
     * Excludes user's own stories.
     */
    fun recordStoryView(story: StoryItem, currentUserId: String?) {
        if (story.id.isBlank()) return
        // Do not record user's own stories
        if (!currentUserId.isNullOrBlank() && story.userId == currentUserId) {
            return
        }

        val historyId = "story_${story.id}"
        val current = _historyFlow.value.filter { it.id != historyId && it.itemId != story.id }

        val newItem = HistoryItem(
            id = historyId,
            itemId = story.id,
            authorId = story.userId,
            authorName = story.userName,
            authorAvatarUrl = story.userAvatar,
            content = story.caption,
            mediaType = if (story.mediaType.equals("video", ignoreCase = true)) "video" else "story",
            mediaUrl = story.mediaUrl,
            mediaUrls = if (story.mediaUrl.isNotBlank()) listOf(story.mediaUrl) else emptyList(),
            isStory = true,
            createdAt = story.createdAt,
            viewedAt = System.currentTimeMillis()
        )

        val updated = (listOf(newItem) + current).take(500)
        _historyFlow.value = updated
        persistHistory(updated)
    }

    fun recordWatch(post: PostItem) {
        recordHistory(post, null)
    }

    fun removeItem(historyId: String) {
        val updated = _historyFlow.value.filter { it.id != historyId }
        _historyFlow.value = updated
        persistHistory(updated)
    }

    fun clearHistory() {
        _historyFlow.value = emptyList()
        prefs.edit().remove("watched_posts_v2").remove("watched_posts").apply()
    }

    private fun persistHistory(items: List<HistoryItem>) {
        try {
            val arr = JSONArray()
            for (item in items) {
                arr.put(JSONObject(item.toMap()))
            }
            prefs.edit().putString("watched_posts_v2", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: WatchHistoryRepository? = null

        fun getInstance(context: Context): WatchHistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: WatchHistoryRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
