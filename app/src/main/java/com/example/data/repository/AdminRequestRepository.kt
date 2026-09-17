package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AppSystemSettings
import com.example.data.model.DepositRequestItem
import com.example.data.model.MaintenanceConfig
import com.example.data.model.MonetizationRequestItem
import com.example.data.model.PaymentMethodItem
import com.example.data.model.VerificationRequestItem
import com.example.data.model.WithdrawRequestItem
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AdminRequestRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_admin_requests_prefs", Context.MODE_PRIVATE)

    // Flow states initialized from local cache for instant UI rendering
    private val _paymentMethodsFlow = MutableStateFlow<List<PaymentMethodItem>>(loadPaymentMethods())
    val paymentMethodsFlow: StateFlow<List<PaymentMethodItem>> = _paymentMethodsFlow.asStateFlow()

    private val _depositRequestsFlow = MutableStateFlow<List<DepositRequestItem>>(loadDepositRequests())
    val depositRequestsFlow: StateFlow<List<DepositRequestItem>> = _depositRequestsFlow.asStateFlow()

    private val _withdrawRequestsFlow = MutableStateFlow<List<WithdrawRequestItem>>(loadWithdrawRequests())
    val withdrawRequestsFlow: StateFlow<List<WithdrawRequestItem>> = _withdrawRequestsFlow.asStateFlow()

    private val _monetizationRequestsFlow = MutableStateFlow<List<MonetizationRequestItem>>(loadMonetizationRequests())
    val monetizationRequestsFlow: StateFlow<List<MonetizationRequestItem>> = _monetizationRequestsFlow.asStateFlow()

    private val _verificationRequestsFlow = MutableStateFlow<List<VerificationRequestItem>>(loadVerificationRequests())
    val verificationRequestsFlow: StateFlow<List<VerificationRequestItem>> = _verificationRequestsFlow.asStateFlow()

    // Firebase Database Reference with admin_ prefix nodes
    private val rtdb: FirebaseDatabase? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseDatabase.getInstance("https://frndom-871ec-default-rtdb.firebaseio.com")
            } else {
                null
            }
        } catch (e: Exception) {
            try {
                FirebaseDatabase.getInstance()
            } catch (ex: Exception) {
                Log.w("AdminRequestRepository", "FirebaseDatabase not initialized: ${ex.message}")
                null
            }
        }
    }

    private val paymentMethodsRef: DatabaseReference? by lazy { rtdb?.getReference("admin_payment_methods") }
    private val depositReqRef: DatabaseReference? by lazy { rtdb?.getReference("admin_deposit_request") }
    private val withdrawReqRef: DatabaseReference? by lazy { rtdb?.getReference("admin_withdraw_request") }
    private val verificationReqRef: DatabaseReference? by lazy { rtdb?.getReference("admin_verification_request") }
    private val monetizationReqRef: DatabaseReference? by lazy { rtdb?.getReference("admin_monetization_request") }
    private val statsRef: DatabaseReference? by lazy { rtdb?.getReference("admin_stats") }
    private val pinRef: DatabaseReference? by lazy { rtdb?.getReference("admin_pin") }
    private val penRef: DatabaseReference? by lazy { rtdb?.getReference("admin_pen") }
    private val maintenanceRef: DatabaseReference? by lazy { rtdb?.getReference("admin_maintenance") }
    private val monetizationSettingsRef: DatabaseReference? by lazy { rtdb?.getReference("admin_monetization_settings") }
    private val appSettingsRef: DatabaseReference? by lazy { rtdb?.getReference("admin_app_settings") }
    private val homeFeedConfigRef: DatabaseReference? by lazy { rtdb?.getReference("admin_home_feed_config") }
    private val storyExpiryConfigRef: DatabaseReference? by lazy { rtdb?.getReference("admin_story_expiry_config") }
    private val suggestedItemsRef: DatabaseReference? by lazy { rtdb?.getReference("admin_suggested_items") }

    private val _adminPinFlow = MutableStateFlow<String>(loadAdminPin())
    val adminPinFlow: StateFlow<String> = _adminPinFlow.asStateFlow()

    private val _maintenanceConfigFlow = MutableStateFlow<MaintenanceConfig>(loadMaintenanceConfig())
    val maintenanceConfigFlow: StateFlow<MaintenanceConfig> = _maintenanceConfigFlow.asStateFlow()

    private val _monetizationSettingsFlow = MutableStateFlow<com.example.data.model.MonetizationSettings>(loadMonetizationSettings())
    val monetizationSettingsFlow: StateFlow<com.example.data.model.MonetizationSettings> = _monetizationSettingsFlow.asStateFlow()

    private val _appSettingsFlow = MutableStateFlow<AppSystemSettings>(loadAppSettings())
    val appSettingsFlow: StateFlow<AppSystemSettings> = _appSettingsFlow.asStateFlow()

    private val _homeFeedConfigFlow = MutableStateFlow<com.example.data.model.HomeFeedConfig>(loadHomeFeedConfig())
    val homeFeedConfigFlow: StateFlow<com.example.data.model.HomeFeedConfig> = _homeFeedConfigFlow.asStateFlow()

    private val _storyExpiryConfigFlow = MutableStateFlow<com.example.data.model.StoryExpiryConfig>(loadStoryExpiryConfig())
    val storyExpiryConfigFlow: StateFlow<com.example.data.model.StoryExpiryConfig> = _storyExpiryConfigFlow.asStateFlow()

    private val _suggestedItemsFlow = MutableStateFlow<List<com.example.data.model.AdminSuggestedItem>>(loadSuggestedItems())
    val suggestedItemsFlow: StateFlow<List<com.example.data.model.AdminSuggestedItem>> = _suggestedItemsFlow.asStateFlow()

    init {
        listenToFirebaseAppSettings()
        // 1. Payment methods
        if (_paymentMethodsFlow.value.isEmpty()) {
            val defaultMethods = getDefaultPaymentMethods()
            savePaymentMethodsLocally(defaultMethods)
            _paymentMethodsFlow.value = defaultMethods
        }
        try {
            _paymentMethodsFlow.value.forEach { pm ->
                paymentMethodsRef?.child(pm.id)?.setValue(pm.toMap())
            }
        } catch (_: Exception) {}

        // Clean any legacy demo requests from local storage
        val cleanDeposits = _depositRequestsFlow.value.filterNot { it.id.startsWith("dep_sample_") || it.userId.startsWith("user_demo_") }
        _depositRequestsFlow.value = cleanDeposits
        saveDepositRequestsLocally(cleanDeposits)

        val cleanWithdraws = _withdrawRequestsFlow.value.filterNot { it.id.startsWith("wdr_sample_") || it.userId.startsWith("user_demo_") }
        _withdrawRequestsFlow.value = cleanWithdraws
        saveWithdrawRequestsLocally(cleanWithdraws)

        val cleanVerifications = _verificationRequestsFlow.value.filterNot { it.id.startsWith("ver_sample_") || it.userId.startsWith("user_demo_") }
        _verificationRequestsFlow.value = cleanVerifications
        saveVerificationRequestsLocally(cleanVerifications)

        val cleanMonetizations = _monetizationRequestsFlow.value.filterNot { it.id.startsWith("mon_sample_") || it.userId.startsWith("user_demo_") }
        _monetizationRequestsFlow.value = cleanMonetizations
        saveMonetizationRequestsLocally(cleanMonetizations)

        // Push admin stats
        syncAdminStatsToFirebase()

        // Admin PIN (default 1234, synced to admin_pin and admin_pen)
        val initialPin = loadAdminPin()
        try {
            pinRef?.setValue(initialPin)
            penRef?.setValue(initialPin)
        } catch (_: Exception) {}

        // Start Firebase real-time listeners for all domains with admin_ node names
        listenToFirebaseAdminPin()
        listenToFirebaseMaintenance()
        listenToFirebaseMonetizationSettings()
        listenToFirebasePaymentMethods()
        listenToFirebaseDepositRequests()
        listenToFirebaseWithdrawRequests()
        listenToFirebaseVerificationRequests()
        listenToFirebaseMonetizationRequests()
        listenToFirebaseHomeFeedConfig()
        listenToFirebaseStoryExpiryConfig()
        listenToFirebaseSuggestedItems()
    }

    private fun loadMaintenanceConfig(): MaintenanceConfig {
        val enabled = prefs.getBoolean("admin_maintenance_enabled", false)
        val title = prefs.getString("admin_maintenance_title", "Maintenance") ?: "Maintenance"
        val desc = prefs.getString("admin_maintenance_desc", "We are currently performing system maintenance. Please check back later.")
            ?: "We are currently performing system maintenance. Please check back later."
        val time = prefs.getLong("admin_maintenance_time", System.currentTimeMillis())
        return MaintenanceConfig(
            isEnabled = enabled,
            title = title,
            description = desc,
            updatedAt = time
        )
    }

    private fun saveMaintenanceConfigLocally(config: MaintenanceConfig) {
        prefs.edit()
            .putBoolean("admin_maintenance_enabled", config.isEnabled)
            .putString("admin_maintenance_title", config.title)
            .putString("admin_maintenance_desc", config.description)
            .putLong("admin_maintenance_time", config.updatedAt)
            .apply()
    }

    private fun listenToFirebaseMaintenance() {
        try {
            maintenanceRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val isEnabled = snapshot.child("isEnabled").getValue(Boolean::class.java)
                            ?: (snapshot.child("isEnabled").getValue(String::class.java)?.toBoolean())
                            ?: false
                        val title = snapshot.child("title").getValue(String::class.java)?.ifBlank { "Maintenance" } ?: "Maintenance"
                        val desc = snapshot.child("description").getValue(String::class.java)
                            ?: "We are currently performing system maintenance. Please check back later."
                        val time = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val cfg = MaintenanceConfig(isEnabled, title, desc, time)
                        _maintenanceConfigFlow.value = cfg
                        saveMaintenanceConfigLocally(cfg)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "Maintenance listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up maintenance listener: ${e.message}")
        }
    }

    fun setMaintenanceMode(
        enabled: Boolean,
        title: String = "Maintenance",
        description: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val config = MaintenanceConfig(
            isEnabled = enabled,
            title = if (title.isBlank()) "Maintenance" else title.trim(),
            description = description.trim().ifBlank { "We are currently performing system maintenance. Please check back later." },
            updatedAt = System.currentTimeMillis()
        )
        _maintenanceConfigFlow.value = config
        saveMaintenanceConfigLocally(config)

        try {
            maintenanceRef?.setValue(config.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    private fun loadMonetizationSettings(): com.example.data.model.MonetizationSettings {
        val reelRate = prefs.getFloat("admin_monetization_reel_rate", 0.5f).toDouble()
        val imageRate = prefs.getFloat("admin_monetization_image_rate", 0.2f).toDouble()
        val textRate = prefs.getFloat("admin_monetization_text_rate", 0.1f).toDouble()
        val minTransfer = prefs.getFloat("admin_monetization_min_transfer", 5.0f).toDouble()
        val reqViews = prefs.getInt("admin_monetization_req_views", 500)
        val reqFollowers = prefs.getInt("admin_monetization_req_followers", 100)
        val reqPosts = prefs.getInt("admin_monetization_req_posts", 10)
        val reqReels = prefs.getInt("admin_monetization_req_reels", 5)
        val reqAge = prefs.getInt("admin_monetization_req_age", 7)
        val updatedAt = prefs.getLong("admin_monetization_updated_at", System.currentTimeMillis())
        return com.example.data.model.MonetizationSettings(reelRate, imageRate, textRate, minTransfer, reqViews, reqFollowers, reqPosts, reqReels, reqAge, updatedAt)
    }

    private fun saveMonetizationSettingsLocally(settings: com.example.data.model.MonetizationSettings) {
        prefs.edit()
            .putFloat("admin_monetization_reel_rate", settings.reelRatePer1000.toFloat())
            .putFloat("admin_monetization_image_rate", settings.imageRatePer1000.toFloat())
            .putFloat("admin_monetization_text_rate", settings.textRatePer1000.toFloat())
            .putFloat("admin_monetization_min_transfer", settings.minTransferAmount.toFloat())
            .putInt("admin_monetization_req_views", settings.reqTotalViews)
            .putInt("admin_monetization_req_followers", settings.reqTotalFollowers)
            .putInt("admin_monetization_req_posts", settings.reqTotalPosts)
            .putInt("admin_monetization_req_reels", settings.reqTotalReels)
            .putInt("admin_monetization_req_age", settings.reqAccountAgeDays)
            .putLong("admin_monetization_updated_at", settings.updatedAt)
            .apply()
    }

    private fun listenToFirebaseMonetizationSettings() {
        try {
            monetizationSettingsRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val reelRate = snapshot.child("reelRatePer1000").getValue(Double::class.java) ?: 0.5
                        val imageRate = snapshot.child("imageRatePer1000").getValue(Double::class.java) ?: 0.2
                        val textRate = snapshot.child("textRatePer1000").getValue(Double::class.java) ?: 0.1
                        val minTransfer = snapshot.child("minTransferAmount").getValue(Double::class.java) ?: 5.0
                        val reqViews = snapshot.child("reqTotalViews").getValue(Int::class.java) ?: 500
                        val reqFollowers = snapshot.child("reqTotalFollowers").getValue(Int::class.java) ?: 100
                        val reqPosts = snapshot.child("reqTotalPosts").getValue(Int::class.java) ?: 10
                        val reqReels = snapshot.child("reqTotalReels").getValue(Int::class.java) ?: 5
                        val reqAge = snapshot.child("reqAccountAgeDays").getValue(Int::class.java) ?: 7
                        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val cfg = com.example.data.model.MonetizationSettings(reelRate, imageRate, textRate, minTransfer, reqViews, reqFollowers, reqPosts, reqReels, reqAge, updatedAt)
                        _monetizationSettingsFlow.value = cfg
                        saveMonetizationSettingsLocally(cfg)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "MonetizationSettings listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up MonetizationSettings listener: ${e.message}")
        }
    }

    fun setMonetizationSettings(
        reelRate: Double,
        imageRate: Double,
        textRate: Double,
        minTransfer: Double,
        reqViews: Int = 500,
        reqFollowers: Int = 100,
        reqPosts: Int = 10,
        reqReels: Int = 5,
        reqAge: Int = 7,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val config = com.example.data.model.MonetizationSettings(
            reelRatePer1000 = reelRate,
            imageRatePer1000 = imageRate,
            textRatePer1000 = textRate,
            minTransferAmount = minTransfer,
            reqTotalViews = reqViews,
            reqTotalFollowers = reqFollowers,
            reqTotalPosts = reqPosts,
            reqTotalReels = reqReels,
            reqAccountAgeDays = reqAge,
            updatedAt = System.currentTimeMillis()
        )
        _monetizationSettingsFlow.value = config
        saveMonetizationSettingsLocally(config)

        try {
            monetizationSettingsRef?.setValue(config.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    private fun loadAppSettings(): AppSystemSettings {
        return AppSystemSettings(
            requireIdCardForVerification = prefs.getBoolean("app_setting_req_id_card", true),
            monetizationEnabled = prefs.getBoolean("app_setting_monetization_enabled", true),
            depositEnabled = prefs.getBoolean("app_setting_deposit_enabled", true),
            withdrawEnabled = prefs.getBoolean("app_setting_withdraw_enabled", true),
            textPostEnabled = prefs.getBoolean("app_setting_text_post_enabled", true),
            imagePostEnabled = prefs.getBoolean("app_setting_image_post_enabled", true),
            videoPostEnabled = prefs.getBoolean("app_setting_video_post_enabled", true),
            storyPostEnabled = prefs.getBoolean("app_setting_story_post_enabled", true),
            linkPostEnabled = prefs.getBoolean("app_setting_link_post_enabled", true),
            pageCreationEnabled = prefs.getBoolean("app_setting_page_creation_enabled", true),
            groupCreationEnabled = prefs.getBoolean("app_setting_group_creation_enabled", true),
            engagementNotificationsEnabled = prefs.getBoolean("app_setting_engagement_notif_enabled", true),
            maxVideoDurationMinutes = prefs.getInt("app_setting_max_video_min", 10),
            defaultUserDailyLimitText = prefs.getInt("app_setting_user_limit_text", 10),
            defaultUserDailyLimitImage = prefs.getInt("app_setting_user_limit_image", 10),
            defaultUserDailyLimitVideo = prefs.getInt("app_setting_user_limit_video", 5),
            defaultUserDailyLimitStory = prefs.getInt("app_setting_user_limit_story", 10),
            defaultUserDailyLimitLink = prefs.getInt("app_setting_user_limit_link", 10),
            defaultPageDailyLimitText = prefs.getInt("app_setting_page_limit_text", 20),
            defaultPageDailyLimitImage = prefs.getInt("app_setting_page_limit_image", 20),
            defaultPageDailyLimitVideo = prefs.getInt("app_setting_page_limit_video", 10),
            defaultPageDailyLimitStory = prefs.getInt("app_setting_page_limit_story", 20),
            defaultPageDailyLimitLink = prefs.getInt("app_setting_page_limit_link", 20),
            leaderboardLimit = prefs.getInt("app_setting_leaderboard_limit", 20),
            updatedAt = prefs.getLong("app_setting_updated_at", System.currentTimeMillis())
        )
    }

    private fun saveAppSettingsLocally(settings: AppSystemSettings) {
        prefs.edit()
            .putBoolean("app_setting_req_id_card", settings.requireIdCardForVerification)
            .putBoolean("app_setting_monetization_enabled", settings.monetizationEnabled)
            .putBoolean("app_setting_deposit_enabled", settings.depositEnabled)
            .putBoolean("app_setting_withdraw_enabled", settings.withdrawEnabled)
            .putBoolean("app_setting_text_post_enabled", settings.textPostEnabled)
            .putBoolean("app_setting_image_post_enabled", settings.imagePostEnabled)
            .putBoolean("app_setting_video_post_enabled", settings.videoPostEnabled)
            .putBoolean("app_setting_story_post_enabled", settings.storyPostEnabled)
            .putBoolean("app_setting_link_post_enabled", settings.linkPostEnabled)
            .putBoolean("app_setting_page_creation_enabled", settings.pageCreationEnabled)
            .putBoolean("app_setting_group_creation_enabled", settings.groupCreationEnabled)
            .putBoolean("app_setting_engagement_notif_enabled", settings.engagementNotificationsEnabled)
            .putInt("app_setting_max_video_min", settings.maxVideoDurationMinutes)
            .putInt("app_setting_user_limit_text", settings.defaultUserDailyLimitText)
            .putInt("app_setting_user_limit_image", settings.defaultUserDailyLimitImage)
            .putInt("app_setting_user_limit_video", settings.defaultUserDailyLimitVideo)
            .putInt("app_setting_user_limit_story", settings.defaultUserDailyLimitStory)
            .putInt("app_setting_user_limit_link", settings.defaultUserDailyLimitLink)
            .putInt("app_setting_page_limit_text", settings.defaultPageDailyLimitText)
            .putInt("app_setting_page_limit_image", settings.defaultPageDailyLimitImage)
            .putInt("app_setting_page_limit_video", settings.defaultPageDailyLimitVideo)
            .putInt("app_setting_page_limit_story", settings.defaultPageDailyLimitStory)
            .putInt("app_setting_page_limit_link", settings.defaultPageDailyLimitLink)
            .putInt("app_setting_leaderboard_limit", settings.leaderboardLimit)
            .putLong("app_setting_updated_at", settings.updatedAt)
            .apply()
    }

    private fun listenToFirebaseAppSettings() {
        try {
            appSettingsRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val reqIdCard = snapshot.child("requireIdCardForVerification").getValue(Boolean::class.java) ?: true
                        val monetization = snapshot.child("monetizationEnabled").getValue(Boolean::class.java) ?: true
                        val deposit = snapshot.child("depositEnabled").getValue(Boolean::class.java) ?: true
                        val withdraw = snapshot.child("withdrawEnabled").getValue(Boolean::class.java) ?: true
                        val textPost = snapshot.child("textPostEnabled").getValue(Boolean::class.java) ?: true
                        val imagePost = snapshot.child("imagePostEnabled").getValue(Boolean::class.java) ?: true
                        val videoPost = snapshot.child("videoPostEnabled").getValue(Boolean::class.java) ?: true
                        val storyPost = snapshot.child("storyPostEnabled").getValue(Boolean::class.java) ?: true
                        val linkPost = snapshot.child("linkPostEnabled").getValue(Boolean::class.java) ?: true
                        val pageCreate = snapshot.child("pageCreationEnabled").getValue(Boolean::class.java) ?: true
                        val groupCreate = snapshot.child("groupCreationEnabled").getValue(Boolean::class.java) ?: true
                        val engagementNotif = snapshot.child("engagementNotificationsEnabled").getValue(Boolean::class.java) ?: true
                        val maxVideoMin = snapshot.child("maxVideoDurationMinutes").getValue(Int::class.java) ?: 10
                        val userText = snapshot.child("defaultUserDailyLimitText").getValue(Int::class.java) ?: 10
                        val userImg = snapshot.child("defaultUserDailyLimitImage").getValue(Int::class.java) ?: 10
                        val userVid = snapshot.child("defaultUserDailyLimitVideo").getValue(Int::class.java) ?: 5
                        val userStory = snapshot.child("defaultUserDailyLimitStory").getValue(Int::class.java) ?: 10
                        val userLink = snapshot.child("defaultUserDailyLimitLink").getValue(Int::class.java) ?: 10
                        val pageText = snapshot.child("defaultPageDailyLimitText").getValue(Int::class.java) ?: 20
                        val pageImg = snapshot.child("defaultPageDailyLimitImage").getValue(Int::class.java) ?: 20
                        val pageVid = snapshot.child("defaultPageDailyLimitVideo").getValue(Int::class.java) ?: 10
                        val pageStory = snapshot.child("defaultPageDailyLimitStory").getValue(Int::class.java) ?: 20
                        val pageLink = snapshot.child("defaultPageDailyLimitLink").getValue(Int::class.java) ?: 20
                        val leaderboardLimit = snapshot.child("leaderboardLimit").getValue(Int::class.java) ?: 20
                        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                        val parsed = AppSystemSettings(
                            requireIdCardForVerification = reqIdCard,
                            monetizationEnabled = monetization,
                            depositEnabled = deposit,
                            withdrawEnabled = withdraw,
                            textPostEnabled = textPost,
                            imagePostEnabled = imagePost,
                            videoPostEnabled = videoPost,
                            storyPostEnabled = storyPost,
                            linkPostEnabled = linkPost,
                            pageCreationEnabled = pageCreate,
                            groupCreationEnabled = groupCreate,
                            engagementNotificationsEnabled = engagementNotif,
                            maxVideoDurationMinutes = maxVideoMin,
                            defaultUserDailyLimitText = userText,
                            defaultUserDailyLimitImage = userImg,
                            defaultUserDailyLimitVideo = userVid,
                            defaultUserDailyLimitStory = userStory,
                            defaultUserDailyLimitLink = userLink,
                            defaultPageDailyLimitText = pageText,
                            defaultPageDailyLimitImage = pageImg,
                            defaultPageDailyLimitVideo = pageVid,
                            defaultPageDailyLimitStory = pageStory,
                            defaultPageDailyLimitLink = pageLink,
                            leaderboardLimit = leaderboardLimit,
                            updatedAt = updatedAt
                        )
                        _appSettingsFlow.value = parsed
                        saveAppSettingsLocally(parsed)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "AppSettings listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up AppSettings listener: ${e.message}")
        }
    }

    fun getAppSettings(): AppSystemSettings {
        return _appSettingsFlow.value
    }

    fun updateAppSettings(settings: AppSystemSettings, onComplete: ((Boolean) -> Unit)? = null) {
        val updated = settings.copy(updatedAt = System.currentTimeMillis())
        _appSettingsFlow.value = updated
        saveAppSettingsLocally(updated)

        try {
            appSettingsRef?.setValue(updated.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    private fun loadHomeFeedConfig(): com.example.data.model.HomeFeedConfig {
        val json = prefs.getString("admin_home_feed_config_json", null)
        return com.example.data.model.HomeFeedConfig.fromJsonString(json)
    }

    private fun saveHomeFeedConfigLocally(config: com.example.data.model.HomeFeedConfig) {
        prefs.edit().putString("admin_home_feed_config_json", config.toJsonString()).apply()
    }

    private fun listenToFirebaseHomeFeedConfig() {
        try {
            homeFeedConfigRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val onlyFriends = snapshot.child("onlyFriendsPosts").getValue(Boolean::class.java) ?: false
                        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val sectionsSnap = snapshot.child("sections")
                        val list = mutableListOf<com.example.data.model.HomeFeedSection>()
                        for (child in sectionsSnap.children) {
                            val id = child.child("id").getValue(String::class.java) ?: ""
                            val typeStr = child.child("type").getValue(String::class.java) ?: "IMAGE_POSTS"
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val enabled = child.child("enabled").getValue(Boolean::class.java) ?: true
                            val minCount = child.child("minCount").getValue(Int::class.java) ?: 1
                            val maxCount = child.child("maxCount").getValue(Int::class.java) ?: 5
                            val secType = try {
                                com.example.data.model.FeedSectionType.valueOf(typeStr)
                            } catch (_: Exception) {
                                com.example.data.model.FeedSectionType.IMAGE_POSTS
                            }
                            list.add(
                                com.example.data.model.HomeFeedSection(
                                    id = id.ifBlank { "sec_${list.size}" },
                                    type = secType,
                                    title = title.ifBlank { secType.defaultTitle },
                                    enabled = enabled,
                                    minCount = minCount,
                                    maxCount = maxCount
                                )
                            )
                        }
                        val parsed = com.example.data.model.HomeFeedConfig(
                            onlyFriendsPosts = onlyFriends,
                            sections = if (list.isEmpty()) com.example.data.model.HomeFeedConfig.defaultSections() else list,
                            updatedAt = updatedAt
                        )
                        _homeFeedConfigFlow.value = parsed
                        saveHomeFeedConfigLocally(parsed)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "HomeFeedConfig listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up HomeFeedConfig listener: ${e.message}")
        }
    }

    fun getHomeFeedConfig(): com.example.data.model.HomeFeedConfig {
        return _homeFeedConfigFlow.value
    }

    fun updateHomeFeedConfig(config: com.example.data.model.HomeFeedConfig, onComplete: ((Boolean) -> Unit)? = null) {
        val updated = config.copy(updatedAt = System.currentTimeMillis())
        _homeFeedConfigFlow.value = updated
        saveHomeFeedConfigLocally(updated)

        try {
            homeFeedConfigRef?.setValue(updated.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    private fun loadStoryExpiryConfig(): com.example.data.model.StoryExpiryConfig {
        val json = prefs.getString("admin_story_expiry_config_json", null)
        return com.example.data.model.StoryExpiryConfig.fromJsonString(json)
    }

    private fun saveStoryExpiryConfigLocally(config: com.example.data.model.StoryExpiryConfig) {
        prefs.edit().putString("admin_story_expiry_config_json", config.toJsonString()).apply()
    }

    private fun listenToFirebaseStoryExpiryConfig() {
        try {
            storyExpiryConfigRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val hours = snapshot.child("hours").getValue(Int::class.java) ?: 24
                        val minutes = snapshot.child("minutes").getValue(Int::class.java) ?: 0
                        val seconds = snapshot.child("seconds").getValue(Int::class.java) ?: 0
                        val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val parsed = com.example.data.model.StoryExpiryConfig(
                            hours = hours,
                            minutes = minutes,
                            seconds = seconds,
                            updatedAt = updatedAt
                        )
                        _storyExpiryConfigFlow.value = parsed
                        saveStoryExpiryConfigLocally(parsed)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "StoryExpiryConfig listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up StoryExpiryConfig listener: ${e.message}")
        }
    }

    fun getStoryExpiryConfig(): com.example.data.model.StoryExpiryConfig {
        return _storyExpiryConfigFlow.value
    }

    fun getStoryExpiryDurationMs(): Long {
        return _storyExpiryConfigFlow.value.totalDurationMs
    }

    fun updateStoryExpiryConfig(config: com.example.data.model.StoryExpiryConfig, onComplete: ((Boolean) -> Unit)? = null) {
        val updated = config.copy(updatedAt = System.currentTimeMillis())
        _storyExpiryConfigFlow.value = updated
        saveStoryExpiryConfigLocally(updated)

        try {
            storyExpiryConfigRef?.setValue(updated.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    private fun loadSuggestedItems(): List<com.example.data.model.AdminSuggestedItem> {
        val json = prefs.getString("admin_suggested_items_json", null) ?: return emptyList()
        val list = mutableListOf<com.example.data.model.AdminSuggestedItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(com.example.data.model.AdminSuggestedItem.fromJson(obj))
            }
        } catch (_: Exception) {}
        return list.sortedBy { it.order }
    }

    private fun saveSuggestedItemsLocally(list: List<com.example.data.model.AdminSuggestedItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("admin_suggested_items_json", arr.toString()).apply()
    }

    private fun listenToFirebaseSuggestedItems() {
        try {
            suggestedItemsRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<com.example.data.model.AdminSuggestedItem>()
                    for (child in snapshot.children) {
                        try {
                            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
                            val typeStr = child.child("type").getValue(String::class.java) ?: com.example.data.model.SuggestedItemType.PROFILE.name
                            val type = try {
                                com.example.data.model.SuggestedItemType.valueOf(typeStr)
                            } catch (_: Exception) {
                                com.example.data.model.SuggestedItemType.PROFILE
                            }
                            val targetId = child.child("targetId").getValue(String::class.java) ?: ""
                            val title = child.child("title").getValue(String::class.java) ?: ""
                            val subtitle = child.child("subtitle").getValue(String::class.java) ?: ""
                            val imageUrl = child.child("imageUrl").getValue(String::class.java) ?: ""
                            val isVerified = child.child("isVerified").getValue(Boolean::class.java) ?: false
                            val badgeText = child.child("badgeText").getValue(String::class.java) ?: ""
                            val order = child.child("order").getValue(Int::class.java) ?: 0
                            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                            list.add(
                                com.example.data.model.AdminSuggestedItem(
                                    id = id,
                                    type = type,
                                    targetId = targetId,
                                    title = title,
                                    subtitle = subtitle,
                                    imageUrl = imageUrl,
                                    isVerified = isVerified,
                                    badgeText = badgeText,
                                    order = order,
                                    createdAt = createdAt
                                )
                            )
                        } catch (_: Exception) {}
                    }
                    val sorted = list.sortedBy { it.order }
                    _suggestedItemsFlow.value = sorted
                    saveSuggestedItemsLocally(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "SuggestedItems listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up SuggestedItems listener: ${e.message}")
        }
    }

    fun addSuggestedItem(item: com.example.data.model.AdminSuggestedItem, onComplete: ((Boolean) -> Unit)? = null) {
        val finalItem = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
        val current = _suggestedItemsFlow.value.toMutableList()
        current.add(finalItem)
        val sorted = current.sortedBy { it.order }
        _suggestedItemsFlow.value = sorted
        saveSuggestedItemsLocally(sorted)

        try {
            suggestedItemsRef?.child(finalItem.id)?.setValue(finalItem.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    fun updateSuggestedItem(item: com.example.data.model.AdminSuggestedItem, onComplete: ((Boolean) -> Unit)? = null) {
        val current = _suggestedItemsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        val sorted = current.sortedBy { it.order }
        _suggestedItemsFlow.value = sorted
        saveSuggestedItemsLocally(sorted)

        try {
            suggestedItemsRef?.child(item.id)?.setValue(item.toMap())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    fun deleteSuggestedItem(id: String, onComplete: ((Boolean) -> Unit)? = null) {
        val current = _suggestedItemsFlow.value.filterNot { it.id == id }
        _suggestedItemsFlow.value = current
        saveSuggestedItemsLocally(current)

        try {
            suggestedItemsRef?.child(id)?.removeValue()
            onComplete?.invoke(true)
        } catch (e: Exception) {
            onComplete?.invoke(true)
        }
    }

    fun reorderSuggestedItems(items: List<com.example.data.model.AdminSuggestedItem>) {
        val reordered = items.mapIndexed { idx, item -> item.copy(order = idx) }
        _suggestedItemsFlow.value = reordered
        saveSuggestedItemsLocally(reordered)

        try {
            reordered.forEach { item ->
                suggestedItemsRef?.child(item.id)?.setValue(item.toMap())
            }
        } catch (_: Exception) {}
    }

    private fun loadAdminPin(): String {
        return prefs.getString("admin_security_pin", "1234") ?: "1234"
    }

    private fun saveAdminPinLocally(pin: String) {
        prefs.edit().putString("admin_security_pin", pin).apply()
    }

    private fun listenToFirebaseAdminPin() {
        try {
            pinRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pin = snapshot.getValue(String::class.java)
                        ?: snapshot.getValue(Long::class.java)?.toString()
                        ?: (snapshot.value as? Map<*, *>)?.get("pin")?.toString()
                    if (!pin.isNullOrBlank()) {
                        _adminPinFlow.value = pin
                        saveAdminPinLocally(pin)
                    } else {
                        val defaultPin = "1234"
                        _adminPinFlow.value = defaultPin
                        saveAdminPinLocally(defaultPin)
                        pinRef?.setValue(defaultPin)
                        penRef?.setValue(defaultPin)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "AdminPin listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdminRequestRepository", "Error setting up admin_pin listener: ${e.message}")
        }
    }

    fun verifyAdminPin(enteredPin: String): Boolean {
        val currentPin = _adminPinFlow.value.ifBlank { "1234" }
        return enteredPin.trim() == currentPin.trim()
    }

    fun updateAdminPin(newPin: String, onComplete: (Boolean) -> Unit = {}) {
        val cleanPin = newPin.trim()
        if (cleanPin.length < 4) {
            onComplete(false)
            return
        }
        _adminPinFlow.value = cleanPin
        saveAdminPinLocally(cleanPin)
        try {
            pinRef?.setValue(cleanPin)
            penRef?.setValue(cleanPin)
            onComplete(true)
        } catch (e: Exception) {
            onComplete(true)
        }
    }

    private fun syncAdminStatsToFirebase() {
        try {
            val statsMap = mapOf(
                "totalPaymentMethods" to _paymentMethodsFlow.value.size,
                "totalDepositRequests" to _depositRequestsFlow.value.size,
                "totalWithdrawRequests" to _withdrawRequestsFlow.value.size,
                "totalVerificationRequests" to _verificationRequestsFlow.value.size,
                "totalMonetizationRequests" to _monetizationRequestsFlow.value.size,
                "updatedAt" to System.currentTimeMillis()
            )
            statsRef?.setValue(statsMap)
        } catch (_: Exception) {}
    }

    private fun getDefaultPaymentMethods(): List<PaymentMethodItem> {
        return listOf(
            PaymentMethodItem(
                id = "pm_bkash",
                name = "bKash",
                accountNumber = "01712345678",
                accountType = "Personal (Send Money)",
                instructions = "1. Go to your bKash app or dial *247#.\n2. Choose 'Send Money' option.\n3. Enter the bKash number: 01712345678.\n4. Complete payment and copy the Transaction ID (TrxID).\n5. Enter your phone number and TrxID below to verify.",
                colorHex = "#E2136E",
                isActive = true
            ),
            PaymentMethodItem(
                id = "pm_nagad",
                name = "Nagad",
                accountNumber = "01812345678",
                accountType = "Personal (Send Money)",
                instructions = "1. Open your Nagad app or dial *167#.\n2. Select 'Send Money' option.\n3. Enter the Nagad number: 01812345678.\n4. Complete the transfer and copy your TrxID.\n5. Enter your sender number and TrxID below.",
                colorHex = "#F7941D",
                isActive = true
            ),
            PaymentMethodItem(
                id = "pm_rocket",
                name = "Rocket",
                accountNumber = "01912345678-9",
                accountType = "Personal",
                instructions = "1. Open Rocket App or dial *322#.\n2. Choose 'Send Money' to 01912345678-9.\n3. Enter amount and confirm.\n4. Copy the Transaction ID and submit it below.",
                colorHex = "#8C3494",
                isActive = true
            ),
            PaymentMethodItem(
                id = "pm_bank",
                name = "Bank Transfer",
                accountNumber = "City Bank: 1102345678901",
                accountType = "Corporate / Savings",
                instructions = "Bank: City Bank PLC\nAccount Name: Frndom Services Ltd\nAccount Number: 1102345678901\nBranch: Gulshan 2, Dhaka\nRouting: 225271890\nSubmit your bank reference/transaction number below.",
                colorHex = "#008937",
                isActive = true
            )
        )
    }

    // =========================================================================
    // 1. PAYMENT METHODS (admin_payment_methods)
    // =========================================================================

    private fun listenToFirebasePaymentMethods() {
        try {
            paymentMethodsRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PaymentMethodItem>()
                    for (child in snapshot.children) {
                        val item = parsePaymentMethodSnapshot(child)
                        if (item != null) {
                            list.add(item)
                        }
                    }

                    if (list.isNotEmpty()) {
                        _paymentMethodsFlow.value = list
                        savePaymentMethodsLocally(list)
                    } else {
                        // If empty on Firebase, initialize default methods to Firebase
                        val defaults = getDefaultPaymentMethods()
                        _paymentMethodsFlow.value = defaults
                        savePaymentMethodsLocally(defaults)
                        defaults.forEach { pm ->
                            paymentMethodsRef?.child(pm.id)?.setValue(pm.toMap())
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "PaymentMethods listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun parsePaymentMethodSnapshot(child: DataSnapshot): PaymentMethodItem? {
        return try {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
            if (id.isBlank()) return null
            val name = child.child("name").getValue(String::class.java) ?: ""
            val accountNumber = child.child("accountNumber").getValue(String::class.java)
                ?: child.child("accountNumber").getValue(Long::class.java)?.toString()
                ?: ""
            val accountType = child.child("accountType").getValue(String::class.java) ?: "Personal"
            val instructions = child.child("instructions").getValue(String::class.java) ?: ""
            val colorHex = child.child("colorHex").getValue(String::class.java) ?: "#E2136E"
            val isActive = child.child("isActive").getValue(Boolean::class.java) ?: true
            PaymentMethodItem(
                id = id,
                name = name,
                accountNumber = accountNumber,
                accountType = accountType,
                instructions = instructions,
                colorHex = colorHex,
                isActive = isActive
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPaymentMethods(): List<PaymentMethodItem> {
        val json = prefs.getString("payment_methods_json", null) ?: return emptyList()
        val list = mutableListOf<PaymentMethodItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    PaymentMethodItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        accountNumber = obj.optString("accountNumber", ""),
                        accountType = obj.optString("accountType", "Personal"),
                        instructions = obj.optString("instructions", ""),
                        colorHex = obj.optString("colorHex", "#E2136E"),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun savePaymentMethodsLocally(list: List<PaymentMethodItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("accountNumber", item.accountNumber)
                    put("accountType", item.accountType)
                    put("instructions", item.instructions)
                    put("colorHex", item.colorHex)
                    put("isActive", item.isActive)
                }
                arr.put(obj)
            }
            prefs.edit().putString("payment_methods_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addOrUpdatePaymentMethod(item: PaymentMethodItem) {
        val current = _paymentMethodsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        _paymentMethodsFlow.value = current
        savePaymentMethodsLocally(current)

        // Sync to Firebase Realtime Database
        try {
            paymentMethodsRef?.child(item.id)?.setValue(item.toMap())
        } catch (_: Exception) {}
    }

    fun togglePaymentMethod(id: String) {
        val current = _paymentMethodsFlow.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
        _paymentMethodsFlow.value = current
        savePaymentMethodsLocally(current)

        val updatedItem = current.firstOrNull { it.id == id }
        if (updatedItem != null) {
            try {
                paymentMethodsRef?.child(id)?.child("isActive")?.setValue(updatedItem.isActive)
            } catch (_: Exception) {}
        }
    }

    fun deletePaymentMethod(id: String) {
        val current = _paymentMethodsFlow.value.filter { it.id != id }
        _paymentMethodsFlow.value = current
        savePaymentMethodsLocally(current)

        try {
            paymentMethodsRef?.child(id)?.removeValue()
        } catch (_: Exception) {}
    }

    // =========================================================================
    // 2. DEPOSIT REQUESTS (admin_deposit_request)
    // =========================================================================

    private fun listenToFirebaseDepositRequests() {
        try {
            depositReqRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<DepositRequestItem>()
                    for (child in snapshot.children) {
                        val item = parseDepositRequestSnapshot(child)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.filterNot { it.id.startsWith("dep_sample_") || it.userId.startsWith("user_demo_") }
                        .sortedByDescending { it.createdAt }
                    _depositRequestsFlow.value = sorted
                    saveDepositRequestsLocally(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "DepositRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun parseDepositRequestSnapshot(child: DataSnapshot): DepositRequestItem? {
        return try {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
            if (id.isBlank()) return null
            val userId = child.child("userId").getValue(String::class.java) ?: ""
            val userName = child.child("userName").getValue(String::class.java) ?: ""
            val userEmail = child.child("userEmail").getValue(String::class.java) ?: ""
            val amount = child.child("amount").getValue(Double::class.java)
                ?: child.child("amount").getValue(Long::class.java)?.toDouble()
                ?: child.child("amount").getValue(String::class.java)?.toDoubleOrNull()
                ?: 0.0
            val methodName = child.child("methodName").getValue(String::class.java) ?: ""
            val senderNumber = child.child("senderNumber").getValue(String::class.java)
                ?: child.child("senderNumber").getValue(Long::class.java)?.toString()
                ?: ""
            val transactionId = child.child("transactionId").getValue(String::class.java) ?: ""
            val status = child.child("status").getValue(String::class.java) ?: "PENDING"
            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val adminNote = child.child("adminNote").getValue(String::class.java) ?: ""
            DepositRequestItem(
                id = id,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                amount = amount,
                methodName = methodName,
                senderNumber = senderNumber,
                transactionId = transactionId,
                status = status,
                createdAt = createdAt,
                adminNote = adminNote
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadDepositRequests(): List<DepositRequestItem> {
        val json = prefs.getString("deposit_requests_json", null) ?: return emptyList()
        val list = mutableListOf<DepositRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DepositRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        amount = obj.optDouble("amount", 0.0),
                        methodName = obj.optString("methodName", ""),
                        senderNumber = obj.optString("senderNumber", ""),
                        transactionId = obj.optString("transactionId", ""),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveDepositRequestsLocally(list: List<DepositRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("amount", item.amount)
                    put("methodName", item.methodName)
                    put("senderNumber", item.senderNumber)
                    put("transactionId", item.transactionId)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("deposit_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitDepositRequest(
        userId: String,
        userName: String,
        userEmail: String,
        amount: Double,
        methodName: String,
        senderNumber: String,
        transactionId: String
    ): DepositRequestItem {
        val item = DepositRequestItem(
            id = "dep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            amount = amount,
            methodName = methodName,
            senderNumber = senderNumber,
            transactionId = transactionId,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _depositRequestsFlow.value.filter { it.id != item.id }
        _depositRequestsFlow.value = updated
        saveDepositRequestsLocally(updated)

        // Save directly to Firebase Realtime Database
        try {
            depositReqRef?.child(item.id)?.setValue(item.toMap())
        } catch (_: Exception) {}

        // Record a single PENDING transaction in wallet repository so user sees it as Pending
        try {
            val walletRepo = WalletRepository.getInstance(context)
            walletRepo.recordPendingDeposit(
                requestId = item.id,
                amount = amount,
                method = methodName,
                trxId = transactionId,
                senderNumber = senderNumber
            )
        } catch (_: Exception) {}

        return item
    }

    fun approveDepositRequest(requestId: String, walletRepo: WalletRepository): Boolean {
        val current = _depositRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _depositRequestsFlow.value = current
        saveDepositRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            depositReqRef?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
        } catch (_: Exception) {}

        // Update the existing pending transaction to COMPLETED (single card updated, no duplicate!)
        walletRepo.approvePendingDeposit(
            depositId = req.id,
            amount = req.amount,
            method = req.methodName,
            trxId = req.transactionId
        )
        return true
    }

    fun rejectDepositRequest(requestId: String, adminNote: String = ""): Boolean {
        val current = _depositRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _depositRequestsFlow.value = current
        saveDepositRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            depositReqRef?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Update the existing pending transaction to REJECTED
        try {
            val walletRepo = WalletRepository.getInstance(context)
            walletRepo.rejectPendingDeposit(
                depositId = req.id,
                reason = adminNote,
                trxId = req.transactionId
            )
        } catch (_: Exception) {}
        return true
    }

    // =========================================================================
    // 3. WITHDRAW REQUESTS (admin_withdraw_request)
    // =========================================================================

    private fun listenToFirebaseWithdrawRequests() {
        try {
            withdrawReqRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<WithdrawRequestItem>()
                    for (child in snapshot.children) {
                        val item = parseWithdrawRequestSnapshot(child)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.filterNot { it.id.startsWith("wdr_sample_") || it.userId.startsWith("user_demo_") }
                        .sortedByDescending { it.createdAt }
                    _withdrawRequestsFlow.value = sorted
                    saveWithdrawRequestsLocally(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "WithdrawRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun parseWithdrawRequestSnapshot(child: DataSnapshot): WithdrawRequestItem? {
        return try {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
            if (id.isBlank()) return null
            val userId = child.child("userId").getValue(String::class.java) ?: ""
            val userName = child.child("userName").getValue(String::class.java) ?: ""
            val userEmail = child.child("userEmail").getValue(String::class.java) ?: ""
            val amount = child.child("amount").getValue(Double::class.java)
                ?: child.child("amount").getValue(Long::class.java)?.toDouble()
                ?: child.child("amount").getValue(String::class.java)?.toDoubleOrNull()
                ?: 0.0
            val methodName = child.child("methodName").getValue(String::class.java) ?: ""
            val accountNumber = child.child("accountNumber").getValue(String::class.java)
                ?: child.child("accountNumber").getValue(Long::class.java)?.toString()
                ?: ""
            val status = child.child("status").getValue(String::class.java) ?: "PENDING"
            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val adminNote = child.child("adminNote").getValue(String::class.java) ?: ""
            WithdrawRequestItem(
                id = id,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                amount = amount,
                methodName = methodName,
                accountNumber = accountNumber,
                status = status,
                createdAt = createdAt,
                adminNote = adminNote
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadWithdrawRequests(): List<WithdrawRequestItem> {
        val json = prefs.getString("withdraw_requests_json", null) ?: return emptyList()
        val list = mutableListOf<WithdrawRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WithdrawRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        amount = obj.optDouble("amount", 0.0),
                        methodName = obj.optString("methodName", ""),
                        accountNumber = obj.optString("accountNumber", ""),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveWithdrawRequestsLocally(list: List<WithdrawRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("amount", item.amount)
                    put("methodName", item.methodName)
                    put("accountNumber", item.accountNumber)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("withdraw_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitWithdrawRequest(
        userId: String,
        userName: String,
        userEmail: String,
        amount: Double,
        methodName: String,
        accountNumber: String
    ): WithdrawRequestItem {
        val item = WithdrawRequestItem(
            id = "wth_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            amount = amount,
            methodName = methodName,
            accountNumber = accountNumber,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _withdrawRequestsFlow.value.filter { it.id != item.id }
        _withdrawRequestsFlow.value = updated
        saveWithdrawRequestsLocally(updated)

        // Save directly to Firebase Realtime Database
        try {
            withdrawReqRef?.child(item.id)?.setValue(item.toMap())
        } catch (_: Exception) {}

        return item
    }

    fun approveWithdrawRequest(requestId: String): Boolean {
        val current = _withdrawRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _withdrawRequestsFlow.value = current
        saveWithdrawRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            withdrawReqRef?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
        } catch (_: Exception) {}

        return true
    }

    fun rejectWithdrawRequest(requestId: String, walletRepo: WalletRepository, adminNote: String = ""): Boolean {
        val current = _withdrawRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _withdrawRequestsFlow.value = current
        saveWithdrawRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            withdrawReqRef?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Refund money back to user's wallet!
        walletRepo.recharge(req.amount, "Withdrawal Refund (${req.methodName} rejected)")
        return true
    }

    // =========================================================================
    // 4. VERIFICATION REQUESTS (admin_verification_request)
    // =========================================================================

    private fun listenToFirebaseVerificationRequests() {
        try {
            verificationReqRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<VerificationRequestItem>()
                    for (child in snapshot.children) {
                        val item = parseVerificationRequestSnapshot(child)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.filterNot { it.id.startsWith("ver_sample_") || it.userId.startsWith("user_demo_") }
                        .sortedByDescending { it.createdAt }
                    _verificationRequestsFlow.value = sorted
                    saveVerificationRequestsLocally(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "VerificationRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun parseVerificationRequestSnapshot(child: DataSnapshot): VerificationRequestItem? {
        return try {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
            if (id.isBlank()) return null
            val userId = child.child("userId").getValue(String::class.java) ?: ""
            val userName = child.child("userName").getValue(String::class.java) ?: ""
            val userEmail = child.child("userEmail").getValue(String::class.java) ?: ""
            val userPhone = child.child("userPhone").getValue(String::class.java)
                ?: child.child("userPhone").getValue(Long::class.java)?.toString()
                ?: ""
            val planTitle = child.child("planTitle").getValue(String::class.java) ?: ""
            val durationDays = child.child("durationDays").getValue(Int::class.java)
                ?: child.child("durationDays").getValue(Long::class.java)?.toInt()
                ?: 30
            val price = child.child("price").getValue(Double::class.java)
                ?: child.child("price").getValue(Long::class.java)?.toDouble()
                ?: child.child("price").getValue(String::class.java)?.toDoubleOrNull()
                ?: 0.0
            val status = child.child("status").getValue(String::class.java) ?: "PENDING"
            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val adminNote = child.child("adminNote").getValue(String::class.java) ?: ""
            val idCardFrontUrl = child.child("idCardFrontUrl").getValue(String::class.java) ?: ""
            val idCardBackUrl = child.child("idCardBackUrl").getValue(String::class.java) ?: ""
            VerificationRequestItem(
                id = id,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                userPhone = userPhone,
                planTitle = planTitle,
                durationDays = durationDays,
                price = price,
                status = status,
                createdAt = createdAt,
                adminNote = adminNote,
                idCardFrontUrl = idCardFrontUrl,
                idCardBackUrl = idCardBackUrl
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadVerificationRequests(): List<VerificationRequestItem> {
        val json = prefs.getString("verification_requests_json", null) ?: return emptyList()
        val list = mutableListOf<VerificationRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    VerificationRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        userPhone = obj.optString("userPhone", ""),
                        planTitle = obj.optString("planTitle", ""),
                        durationDays = obj.optInt("durationDays", 30),
                        price = obj.optDouble("price", 0.0),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", ""),
                        idCardFrontUrl = obj.optString("idCardFrontUrl", ""),
                        idCardBackUrl = obj.optString("idCardBackUrl", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveVerificationRequestsLocally(list: List<VerificationRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("userPhone", item.userPhone)
                    put("planTitle", item.planTitle)
                    put("durationDays", item.durationDays)
                    put("price", item.price)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                    put("idCardFrontUrl", item.idCardFrontUrl)
                    put("idCardBackUrl", item.idCardBackUrl)
                }
                arr.put(obj)
            }
            prefs.edit().putString("verification_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitVerificationRequest(
        userId: String,
        userName: String,
        userEmail: String,
        userPhone: String,
        planTitle: String,
        durationDays: Int,
        price: Double,
        idCardFrontUrl: String = "",
        idCardBackUrl: String = ""
    ): VerificationRequestItem {
        val item = VerificationRequestItem(
            id = "ver_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            userPhone = userPhone,
            planTitle = planTitle,
            durationDays = durationDays,
            price = price,
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            idCardFrontUrl = idCardFrontUrl,
            idCardBackUrl = idCardBackUrl
        )
        val updated = listOf(item) + _verificationRequestsFlow.value.filter { it.id != item.id }
        _verificationRequestsFlow.value = updated
        saveVerificationRequestsLocally(updated)

        // Save directly to Firebase Realtime Database
        try {
            verificationReqRef?.child(item.id)?.setValue(item.toMap())
        } catch (_: Exception) {}

        return item
    }

    fun approveVerificationRequest(
        requestId: String,
        userRepo: UserRepository
    ): Boolean {
        val current = _verificationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _verificationRequestsFlow.value = current
        saveVerificationRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            verificationReqRef?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
        } catch (_: Exception) {}

        // Calculate verified until
        val durationMs = req.durationDays * 24L * 60L * 60L * 1000L
        val verifiedUntil = System.currentTimeMillis() + durationMs

        // Grant persistent verification across DB and Cache
        userRepo.savePersistentVerification(
            uid = req.userId,
            email = req.userEmail,
            phone = req.userPhone,
            verifiedUntil = verifiedUntil,
            verificationType = "GREEN_BADGE",
            planTitle = req.planTitle
        )

        return true
    }

    fun rejectVerificationRequest(
        requestId: String,
        walletRepo: WalletRepository,
        userRepo: UserRepository,
        adminNote: String = ""
    ): Boolean {
        val current = _verificationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _verificationRequestsFlow.value = current
        saveVerificationRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            verificationReqRef?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Revoke any active verification for this user
        userRepo.revokePersistentVerification(
            uid = req.userId,
            email = req.userEmail,
            phone = req.userPhone
        )

        // Refund money back to user's wallet!
        if (req.price > 0) {
            walletRepo.recharge(req.price, "Verification Refund (${req.planTitle} rejected)")
        }

        return true
    }

    // =========================================================================
    // 5. MONETIZATION REQUESTS (admin_monetization_request)
    // =========================================================================

    private fun listenToFirebaseMonetizationRequests() {
        try {
            monetizationReqRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<MonetizationRequestItem>()
                    for (child in snapshot.children) {
                        val item = parseMonetizationRequestSnapshot(child)
                        if (item != null) {
                            list.add(item)
                        }
                    }
                    val sorted = list.filterNot { it.id.startsWith("mon_sample_") || it.userId.startsWith("user_demo_") }
                        .sortedByDescending { it.createdAt }
                    _monetizationRequestsFlow.value = sorted
                    saveMonetizationRequestsLocally(sorted)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdminRequestRepository", "MonetizationRequests listener cancelled: ${error.message}")
                }
            })
        } catch (_: Exception) {}
    }

    private fun parseMonetizationRequestSnapshot(child: DataSnapshot): MonetizationRequestItem? {
        return try {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: ""
            if (id.isBlank()) return null
            val userId = child.child("userId").getValue(String::class.java) ?: ""
            val userName = child.child("userName").getValue(String::class.java) ?: ""
            val userEmail = child.child("userEmail").getValue(String::class.java) ?: ""
            val viewsCount = child.child("viewsCount").getValue(Int::class.java)
                ?: child.child("viewsCount").getValue(Long::class.java)?.toInt()
                ?: 0
            val followersCount = child.child("followersCount").getValue(Int::class.java)
                ?: child.child("followersCount").getValue(Long::class.java)?.toInt()
                ?: 0
            val postsCount = child.child("postsCount").getValue(Int::class.java)
                ?: child.child("postsCount").getValue(Long::class.java)?.toInt()
                ?: 0
            val reelsCount = child.child("reelsCount").getValue(Int::class.java)
                ?: child.child("reelsCount").getValue(Long::class.java)?.toInt()
                ?: 0
            val accountAgeDays = child.child("accountAgeDays").getValue(Int::class.java)
                ?: child.child("accountAgeDays").getValue(Long::class.java)?.toInt()
                ?: 0
            val status = child.child("status").getValue(String::class.java) ?: "PENDING"
            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val adminNote = child.child("adminNote").getValue(String::class.java) ?: ""
            MonetizationRequestItem(
                id = id,
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                viewsCount = viewsCount,
                followersCount = followersCount,
                postsCount = postsCount,
                reelsCount = reelsCount,
                accountAgeDays = accountAgeDays,
                status = status,
                createdAt = createdAt,
                adminNote = adminNote
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadMonetizationRequests(): List<MonetizationRequestItem> {
        val json = prefs.getString("monetization_requests_json", null) ?: return emptyList()
        val list = mutableListOf<MonetizationRequestItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    MonetizationRequestItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", ""),
                        userEmail = obj.optString("userEmail", ""),
                        viewsCount = obj.optInt("viewsCount", 0),
                        followersCount = obj.optInt("followersCount", 0),
                        postsCount = obj.optInt("postsCount", 0),
                        reelsCount = obj.optInt("reelsCount", 0),
                        accountAgeDays = obj.optInt("accountAgeDays", 0),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        adminNote = obj.optString("adminNote", "")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveMonetizationRequestsLocally(list: List<MonetizationRequestItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("viewsCount", item.viewsCount)
                    put("followersCount", item.followersCount)
                    put("postsCount", item.postsCount)
                    put("reelsCount", item.reelsCount)
                    put("accountAgeDays", item.accountAgeDays)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("adminNote", item.adminNote)
                }
                arr.put(obj)
            }
            prefs.edit().putString("monetization_requests_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitMonetizationRequest(
        userId: String,
        userName: String,
        userEmail: String,
        viewsCount: Int,
        followersCount: Int,
        postsCount: Int,
        reelsCount: Int,
        accountAgeDays: Int
    ): MonetizationRequestItem {
        // Mark applied in shared prefs
        context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("monetization_status_$userId", "PENDING")
            .apply()

        val item = MonetizationRequestItem(
            id = "mon_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            viewsCount = viewsCount,
            followersCount = followersCount,
            postsCount = postsCount,
            reelsCount = reelsCount,
            accountAgeDays = accountAgeDays,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(item) + _monetizationRequestsFlow.value.filter { it.userId != userId || it.status != "PENDING" }
        _monetizationRequestsFlow.value = updated
        saveMonetizationRequestsLocally(updated)

        // Save directly to Firebase Realtime Database
        try {
            monetizationReqRef?.child(item.id)?.setValue(item.toMap())
        } catch (_: Exception) {}

        return item
    }

    fun getMonetizationStatusForUser(userId: String): String {
        if (userId.isBlank()) return "NONE"
        val fundPrefs = context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
        val explicitStatus = fundPrefs.getString("monetization_status_$userId", null)
        if (!explicitStatus.isNullOrBlank()) return explicitStatus

        val req = _monetizationRequestsFlow.value.firstOrNull { it.userId == userId }
        return req?.status ?: "NONE"
    }

    fun approveMonetizationRequest(requestId: String): Boolean {
        val current = _monetizationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "APPROVED")
        current[index] = updatedReq
        _monetizationRequestsFlow.value = current
        saveMonetizationRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            monetizationReqRef?.child(requestId)?.updateChildren(mapOf("status" to "APPROVED"))
        } catch (_: Exception) {}

        // Set user status to APPROVED
        context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("monetization_status_${req.userId}", "APPROVED")
            .apply()

        return true
    }

    fun rejectMonetizationRequest(requestId: String, adminNote: String = ""): Boolean {
        val current = _monetizationRequestsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == requestId }
        if (index < 0) return false
        val req = current[index]
        if (req.status != "PENDING") return false

        val updatedReq = req.copy(status = "REJECTED", adminNote = adminNote)
        current[index] = updatedReq
        _monetizationRequestsFlow.value = current
        saveMonetizationRequestsLocally(current)

        // Sync to Firebase RTDB
        try {
            monetizationReqRef?.child(requestId)?.updateChildren(mapOf("status" to "REJECTED", "adminNote" to adminNote))
        } catch (_: Exception) {}

        // Set user status to REJECTED
        context.getSharedPreferences("frndom_creator_fund_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("monetization_status_${req.userId}", "REJECTED")
            .apply()

        return true
    }

    companion object {
        @Volatile
        private var instance: AdminRequestRepository? = null

        fun getInstance(context: Context): AdminRequestRepository {
            return instance ?: synchronized(this) {
                instance ?: AdminRequestRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
