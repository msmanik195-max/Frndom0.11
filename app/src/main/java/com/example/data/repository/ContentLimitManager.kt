package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSystemSettings
import com.example.data.model.PageItem
import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ContentValidationResult {
    object Allowed : ContentValidationResult()
    data class Blocked(val reason: String) : ContentValidationResult()
}

data class ContentLimitStatus(
    val isEnabled: Boolean,
    val countToday: Int,
    val limit: Int,
    val isBlocked: Boolean,
    val reason: String
)

class ContentLimitManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("frndom_content_limits_prefs", Context.MODE_PRIVATE)
    private val adminRepo = AdminRequestRepository.getInstance(context)

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Returns the number of posts/stories of [contentType] created today by [entityId].
     * contentType: "image", "video", "text", "link", "story"
     */
    fun getDailyCount(entityId: String, isPage: Boolean, contentType: String): Int {
        if (entityId.isBlank()) return 0
        val dateStr = getTodayDateString()
        val prefKey = "count_${if (isPage) "page" else "user"}_${entityId}_${dateStr}_${contentType}"
        return prefs.getInt(prefKey, 0)
    }

    /**
     * Increment the daily counter when content is published
     */
    fun recordContentCreated(entityId: String, isPage: Boolean, contentType: String) {
        if (entityId.isBlank()) return
        val dateStr = getTodayDateString()
        val prefKey = "count_${if (isPage) "page" else "user"}_${entityId}_${dateStr}_${contentType}"
        val current = prefs.getInt(prefKey, 0)
        prefs.edit().putInt(prefKey, current + 1).apply()
    }

    /**
     * Check if a post can be created (convenience overload)
     */
    fun validatePostCreation(
        entityId: String,
        isPage: Boolean,
        mediaType: String,
        content: String,
        mediaCount: Int,
        userProfile: UserProfile? = null,
        pageItem: PageItem? = null
    ): ContentValidationResult {
        val hasPhotos = mediaType == "photo" || (mediaCount > 0 && mediaType != "video" && mediaType != "reel")
        val hasVideo = mediaType == "video"
        val isReel = mediaType == "reel"
        return validatePostCreation(
            authorId = entityId,
            isPage = isPage,
            postText = content,
            hasPhotos = hasPhotos,
            hasVideo = hasVideo,
            isReel = isReel,
            userProfile = userProfile,
            pageItem = pageItem
        )
    }

    /**
     * Check if a post can be created
     */
    fun validatePostCreation(
        authorId: String,
        isPage: Boolean,
        postText: String,
        hasPhotos: Boolean,
        hasVideo: Boolean,
        isReel: Boolean,
        userProfile: UserProfile? = null,
        pageItem: PageItem? = null
    ): ContentValidationResult {
        val settings = adminRepo.getAppSettings()

        // 1. Photos/Images Check
        if (hasPhotos) {
            if (!settings.imagePostEnabled) {
                return ContentValidationResult.Blocked("Photo posting is currently disabled by Admin.")
            }
            val limit = if (isPage) {
                if (pageItem != null && pageItem.dailyPostLimitImage >= 0) pageItem.dailyPostLimitImage else settings.defaultPageDailyLimitImage
            } else {
                if (userProfile != null && userProfile.dailyPostLimitImage >= 0) userProfile.dailyPostLimitImage else settings.defaultUserDailyLimitImage
            }

            if (limit == 0) {
                return ContentValidationResult.Blocked("Photo posting limit is 0. Photos are not allowed.")
            }
            if (limit > 0) {
                val current = getDailyCount(authorId, isPage, "image")
                if (current >= limit) {
                    return ContentValidationResult.Blocked("Daily photo limit reached ($current/$limit). You cannot post more photos today.")
                }
            }
        }

        // 2. Videos/Reels Check
        if (hasVideo || isReel) {
            if (!settings.videoPostEnabled) {
                return ContentValidationResult.Blocked("Video posting is currently disabled by Admin.")
            }
            val limit = if (isPage) {
                if (pageItem != null && pageItem.dailyPostLimitVideo >= 0) pageItem.dailyPostLimitVideo else settings.defaultPageDailyLimitVideo
            } else {
                if (userProfile != null && userProfile.dailyPostLimitVideo >= 0) userProfile.dailyPostLimitVideo else settings.defaultUserDailyLimitVideo
            }

            if (limit == 0) {
                return ContentValidationResult.Blocked("Video posting limit is 0. Videos are not allowed.")
            }
            if (limit > 0) {
                val current = getDailyCount(authorId, isPage, "video")
                if (current >= limit) {
                    return ContentValidationResult.Blocked("Daily video limit reached ($current/$limit). You cannot post more videos today.")
                }
            }
        }

        // 3. Link Check (URLs in text)
        val hasLink = postText.contains("http://", ignoreCase = true) ||
                postText.contains("https://", ignoreCase = true) ||
                postText.contains("www.", ignoreCase = true)

        if (hasLink) {
            if (!settings.linkPostEnabled) {
                return ContentValidationResult.Blocked("Link posting is currently disabled by Admin.")
            }
            val limit = if (isPage) settings.defaultPageDailyLimitLink else settings.defaultUserDailyLimitLink
            if (limit == 0) {
                return ContentValidationResult.Blocked("Link posting limit is 0. Links are not allowed.")
            }
            if (limit > 0) {
                val current = getDailyCount(authorId, isPage, "link")
                if (current >= limit) {
                    return ContentValidationResult.Blocked("Daily link limit reached ($current/$limit). You cannot post more links today.")
                }
            }
        }

        // 4. Text-only Status Check
        if (!hasPhotos && !hasVideo && !isReel) {
            if (!settings.textPostEnabled) {
                return ContentValidationResult.Blocked("Text posts are currently disabled by Admin.")
            }
            val limit = if (isPage) {
                if (pageItem != null && pageItem.dailyPostLimitText >= 0) pageItem.dailyPostLimitText else settings.defaultPageDailyLimitText
            } else {
                if (userProfile != null && userProfile.dailyPostLimitText >= 0) userProfile.dailyPostLimitText else settings.defaultUserDailyLimitText
            }

            if (limit == 0) {
                return ContentValidationResult.Blocked("Text post limit is 0. Text posts are not allowed.")
            }
            if (limit > 0) {
                val current = getDailyCount(authorId, isPage, "text")
                if (current >= limit) {
                    return ContentValidationResult.Blocked("Daily text post limit reached ($current/$limit). You cannot post more text updates today.")
                }
            }
        }

        return ContentValidationResult.Allowed
    }

    /**
     * Check if a story can be created
     */
    fun validateStoryCreation(
        userId: String,
        userProfile: UserProfile? = null
    ): ContentValidationResult {
        val settings = adminRepo.getAppSettings()

        if (!settings.storyPostEnabled) {
            return ContentValidationResult.Blocked("Story creation is currently disabled by Admin.")
        }

        val limit = if (userProfile != null && userProfile.dailyPostLimitStory >= 0) {
            userProfile.dailyPostLimitStory
        } else {
            settings.defaultUserDailyLimitStory
        }

        if (limit == 0) {
            return ContentValidationResult.Blocked("Story posting limit is 0. Stories are not allowed.")
        }
        if (limit > 0) {
            val current = getDailyCount(userId, false, "story")
            if (current >= limit) {
                return ContentValidationResult.Blocked("Daily story limit reached ($current/$limit). You cannot create more stories today.")
            }
        }

        return ContentValidationResult.Allowed
    }

    /**
     * Get real-time status of photo posting for UI display
     */
    fun getImageStatus(authorId: String, isPage: Boolean, userProfile: UserProfile? = null, pageItem: PageItem? = null): ContentLimitStatus {
        val settings = adminRepo.getAppSettings()
        val limit = if (isPage) {
            if (pageItem != null && pageItem.dailyPostLimitImage >= 0) pageItem.dailyPostLimitImage else settings.defaultPageDailyLimitImage
        } else {
            if (userProfile != null && userProfile.dailyPostLimitImage >= 0) userProfile.dailyPostLimitImage else settings.defaultUserDailyLimitImage
        }
        val count = getDailyCount(authorId, isPage, "image")
        val isBlocked = !settings.imagePostEnabled || limit == 0 || (limit > 0 && count >= limit)
        val reason = when {
            !settings.imagePostEnabled -> "Photo posting disabled by admin"
            limit == 0 -> "Photo limit is 0 (blocked)"
            limit > 0 && count >= limit -> "Daily photo limit reached ($count/$limit)"
            limit > 0 -> "$count/$limit photos used today"
            else -> "Unlimited photos"
        }
        return ContentLimitStatus(settings.imagePostEnabled, count, limit, isBlocked, reason)
    }

    /**
     * Get real-time status of video posting for UI display
     */
    fun getVideoStatus(authorId: String, isPage: Boolean, userProfile: UserProfile? = null, pageItem: PageItem? = null): ContentLimitStatus {
        val settings = adminRepo.getAppSettings()
        val limit = if (isPage) {
            if (pageItem != null && pageItem.dailyPostLimitVideo >= 0) pageItem.dailyPostLimitVideo else settings.defaultPageDailyLimitVideo
        } else {
            if (userProfile != null && userProfile.dailyPostLimitVideo >= 0) userProfile.dailyPostLimitVideo else settings.defaultUserDailyLimitVideo
        }
        val count = getDailyCount(authorId, isPage, "video")
        val isBlocked = !settings.videoPostEnabled || limit == 0 || (limit > 0 && count >= limit)
        val reason = when {
            !settings.videoPostEnabled -> "Video posting disabled by admin"
            limit == 0 -> "Video limit is 0 (blocked)"
            limit > 0 && count >= limit -> "Daily video limit reached ($count/$limit)"
            limit > 0 -> "$count/$limit videos used today"
            else -> "Unlimited videos"
        }
        return ContentLimitStatus(settings.videoPostEnabled, count, limit, isBlocked, reason)
    }

    /**
     * Get real-time status of text posting for UI display
     */
    fun getTextStatus(authorId: String, isPage: Boolean, userProfile: UserProfile? = null, pageItem: PageItem? = null): ContentLimitStatus {
        val settings = adminRepo.getAppSettings()
        val limit = if (isPage) {
            if (pageItem != null && pageItem.dailyPostLimitText >= 0) pageItem.dailyPostLimitText else settings.defaultPageDailyLimitText
        } else {
            if (userProfile != null && userProfile.dailyPostLimitText >= 0) userProfile.dailyPostLimitText else settings.defaultUserDailyLimitText
        }
        val count = getDailyCount(authorId, isPage, "text")
        val isBlocked = !settings.textPostEnabled || limit == 0 || (limit > 0 && count >= limit)
        val reason = when {
            !settings.textPostEnabled -> "Text posting disabled by admin"
            limit == 0 -> "Text limit is 0 (blocked)"
            limit > 0 && count >= limit -> "Daily text limit reached ($count/$limit)"
            limit > 0 -> "$count/$limit text posts used today"
            else -> "Unlimited text posts"
        }
        return ContentLimitStatus(settings.textPostEnabled, count, limit, isBlocked, reason)
    }

    /**
     * Get real-time status of link posting for UI display
     */
    fun getLinkStatus(authorId: String, isPage: Boolean): ContentLimitStatus {
        val settings = adminRepo.getAppSettings()
        val limit = if (isPage) settings.defaultPageDailyLimitLink else settings.defaultUserDailyLimitLink
        val count = getDailyCount(authorId, isPage, "link")
        val isBlocked = !settings.linkPostEnabled || limit == 0 || (limit > 0 && count >= limit)
        val reason = when {
            !settings.linkPostEnabled -> "Link posting disabled by admin"
            limit == 0 -> "Link limit is 0 (blocked)"
            limit > 0 && count >= limit -> "Daily link limit reached ($count/$limit)"
            limit > 0 -> "$count/$limit links used today"
            else -> "Unlimited links"
        }
        return ContentLimitStatus(settings.linkPostEnabled, count, limit, isBlocked, reason)
    }

    /**
     * Validate whether a user can create a page
     */
    fun validatePageCreation(): ContentValidationResult {
        val settings = adminRepo.getAppSettings()
        if (!settings.pageCreationEnabled) {
            return ContentValidationResult.Blocked("Page creation is currently disabled by Admin.")
        }
        return ContentValidationResult.Allowed
    }

    /**
     * Validate whether a user can create a group
     */
    fun validateGroupCreation(): ContentValidationResult {
        val settings = adminRepo.getAppSettings()
        if (!settings.groupCreationEnabled) {
            return ContentValidationResult.Blocked("Group creation is currently disabled by Admin.")
        }
        return ContentValidationResult.Allowed
    }

    companion object {
        @Volatile
        private var instance: ContentLimitManager? = null

        fun getInstance(context: Context): ContentLimitManager {
            return instance ?: synchronized(this) {
                instance ?: ContentLimitManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
