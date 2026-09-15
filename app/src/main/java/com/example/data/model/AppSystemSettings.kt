package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class AppSystemSettings(
    // 1. Top toggle: Required ID card for verification
    val requireIdCardForVerification: Boolean = true,

    // 2. Platform Feature Toggles
    val monetizationEnabled: Boolean = true,
    val depositEnabled: Boolean = true,
    val withdrawEnabled: Boolean = true,
    val textPostEnabled: Boolean = true,
    val imagePostEnabled: Boolean = true,
    val videoPostEnabled: Boolean = true,
    val storyPostEnabled: Boolean = true,
    val linkPostEnabled: Boolean = true,
    val pageCreationEnabled: Boolean = true,
    val groupCreationEnabled: Boolean = true,
    val engagementNotificationsEnabled: Boolean = true,

    // 3. Video Duration Limit (minutes)
    val maxVideoDurationMinutes: Int = 10,

    // 4. Global Default Daily Post Limits for Users (-1 for unlimited, 0 for blocked/disallowed, >0 for exact count)
    val defaultUserDailyLimitText: Int = 10,
    val defaultUserDailyLimitImage: Int = 10,
    val defaultUserDailyLimitVideo: Int = 5,
    val defaultUserDailyLimitStory: Int = 10,
    val defaultUserDailyLimitLink: Int = 10,

    // 5. Global Default Daily Post Limits for Pages
    val defaultPageDailyLimitText: Int = 20,
    val defaultPageDailyLimitImage: Int = 20,
    val defaultPageDailyLimitVideo: Int = 10,
    val defaultPageDailyLimitStory: Int = 20,
    val defaultPageDailyLimitLink: Int = 20,

    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "requireIdCardForVerification" to requireIdCardForVerification,
        "monetizationEnabled" to monetizationEnabled,
        "depositEnabled" to depositEnabled,
        "withdrawEnabled" to withdrawEnabled,
        "textPostEnabled" to textPostEnabled,
        "imagePostEnabled" to imagePostEnabled,
        "videoPostEnabled" to videoPostEnabled,
        "storyPostEnabled" to storyPostEnabled,
        "linkPostEnabled" to linkPostEnabled,
        "pageCreationEnabled" to pageCreationEnabled,
        "groupCreationEnabled" to groupCreationEnabled,
        "engagementNotificationsEnabled" to engagementNotificationsEnabled,
        "maxVideoDurationMinutes" to maxVideoDurationMinutes,
        "defaultUserDailyLimitText" to defaultUserDailyLimitText,
        "defaultUserDailyLimitImage" to defaultUserDailyLimitImage,
        "defaultUserDailyLimitVideo" to defaultUserDailyLimitVideo,
        "defaultUserDailyLimitStory" to defaultUserDailyLimitStory,
        "defaultUserDailyLimitLink" to defaultUserDailyLimitLink,
        "defaultPageDailyLimitText" to defaultPageDailyLimitText,
        "defaultPageDailyLimitImage" to defaultPageDailyLimitImage,
        "defaultPageDailyLimitVideo" to defaultPageDailyLimitVideo,
        "defaultPageDailyLimitStory" to defaultPageDailyLimitStory,
        "defaultPageDailyLimitLink" to defaultPageDailyLimitLink,
        "updatedAt" to updatedAt
    )
}
