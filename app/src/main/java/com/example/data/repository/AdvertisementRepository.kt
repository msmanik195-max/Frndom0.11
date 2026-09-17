package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AdPlacementSettings
import com.example.data.model.AdvertisementItem
import com.example.data.model.PostItem
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

class AdvertisementRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_advertisements_prefs", Context.MODE_PRIVATE)

    private val _advertisementsFlow = MutableStateFlow<List<AdvertisementItem>>(loadAdvertisementsLocally())
    val advertisementsFlow: StateFlow<List<AdvertisementItem>> = _advertisementsFlow.asStateFlow()

    private val _placementSettingsFlow = MutableStateFlow(loadPlacementSettingsLocally())
    val placementSettingsFlow: StateFlow<AdPlacementSettings> = _placementSettingsFlow.asStateFlow()
    val adPlacementSettingsFlow: StateFlow<AdPlacementSettings> = _placementSettingsFlow.asStateFlow()

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
                Log.w("AdvertisementRepo", "FirebaseDatabase not initialized: ${ex.message}")
                null
            }
        }
    }

    private val adsRef: DatabaseReference? by lazy { rtdb?.getReference("admin_advertisements") }
    private val placementRef: DatabaseReference? by lazy { rtdb?.getReference("admin_ad_placement_settings") }

    init {
        // Clean any cached demo ads
        val filtered = _advertisementsFlow.value.filterNot { it.id.startsWith("ad_demo_") || it.userId.startsWith("user_demo_") }
        _advertisementsFlow.value = filtered
        saveAdvertisementsLocally(filtered)
        setupFirebaseListener()
        setupPlacementListener()
    }

    private fun setupPlacementListener() {
        try {
            placementRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val homeEnabled = snapshot.child("homeAdsEnabled").getValue(Boolean::class.java) ?: true
                        val homeInterval = snapshot.child("homePostInterval").getValue(Int::class.java) ?: 5
                        val reelsEnabled = snapshot.child("reelsAdsEnabled").getValue(Boolean::class.java) ?: true
                        val reelsInterval = snapshot.child("reelsVideoInterval").getValue(Int::class.java) ?: 5
                        val updated = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                        val settings = AdPlacementSettings(
                            homeAdsEnabled = homeEnabled,
                            homePostInterval = homeInterval.coerceAtLeast(1),
                            reelsAdsEnabled = reelsEnabled,
                            reelsVideoInterval = reelsInterval.coerceAtLeast(1),
                            updatedAt = updated
                        )
                        _placementSettingsFlow.value = settings
                        prefs.edit().putString("placement_settings_json", settings.toJsonString()).apply()
                    } catch (e: Exception) {
                        Log.w("AdvertisementRepo", "Error parsing placement settings: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        } catch (e: Exception) {
            Log.w("AdvertisementRepo", "Placement listener setup error: ${e.message}")
        }
    }

    private fun loadPlacementSettingsLocally(): AdPlacementSettings {
        val json = prefs.getString("placement_settings_json", null)
        return AdPlacementSettings.fromJsonString(json)
    }

    fun savePlacementSettings(settings: AdPlacementSettings) {
        _placementSettingsFlow.value = settings
        prefs.edit().putString("placement_settings_json", settings.toJsonString()).apply()
        try {
            placementRef?.setValue(settings.toMap())
        } catch (e: Exception) {
            Log.w("AdvertisementRepo", "Failed to sync placement settings to Firebase: ${e.message}")
        }
    }

    private fun setupFirebaseListener() {
        try {
            adsRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val firebaseList = mutableListOf<AdvertisementItem>()
                    for (child in snapshot.children) {
                        try {
                            val ad = child.getValue(AdvertisementItem::class.java)
                            if (ad != null && ad.id.isNotBlank() && !ad.id.startsWith("ad_demo_") && !ad.userId.startsWith("user_demo_")) {
                                firebaseList.add(ad)
                            }
                        } catch (e: Exception) {
                            Log.w("AdvertisementRepo", "Error parsing Firebase ad: ${e.message}")
                        }
                    }
                    val merged = (firebaseList + _advertisementsFlow.value.filterNot { it.id.startsWith("ad_demo_") })
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt }
                    _advertisementsFlow.value = merged
                    saveAdvertisementsLocally(merged)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("AdvertisementRepo", "Firebase listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("AdvertisementRepo", "Failed to setup ads listener: ${e.message}")
        }
    }

    private fun loadAdvertisementsLocally(): List<AdvertisementItem> {
        val json = prefs.getString("ads_list_json", null) ?: return emptyList()
        val list = mutableListOf<AdvertisementItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val userId = obj.optString("userId", "")
                if (id.startsWith("ad_demo_") || userId.startsWith("user_demo_")) {
                    continue
                }
                list.add(
                    AdvertisementItem(
                        id = id,
                        userId = userId,
                        userName = obj.optString("userName", "Advertiser"),
                        userEmail = obj.optString("userEmail", ""),
                        userPhone = obj.optString("userPhone", ""),
                        userAvatar = obj.optString("userAvatar", ""),
                        campaignName = obj.optString("campaignName", ""),
                        campaignGoal = obj.optString("campaignGoal", "Website Visits"),
                        headline = obj.optString("headline", ""),
                        description = obj.optString("description", ""),
                        mediaUrl = obj.optString("mediaUrl", ""),
                        mediaType = obj.optString("mediaType", "photo"),
                        postType = obj.optString("postType", "PROFILE"),
                        pageId = obj.optString("pageId", ""),
                        pageName = obj.optString("pageName", ""),
                        pageAvatarUrl = obj.optString("pageAvatarUrl", ""),
                        linkedPostId = obj.optString("linkedPostId", ""),
                        destinationUrl = obj.optString("destinationUrl", ""),
                        callToAction = obj.optString("callToAction", "Learn More"),
                        targetLocation = obj.optString("targetLocation", "All Bangladesh"),
                        targetAgeRange = obj.optString("targetAgeRange", "18 - 65+"),
                        targetGender = obj.optString("targetGender", "All"),
                        dailyBudget = obj.optDouble("dailyBudget", 100.0),
                        durationDays = obj.optInt("durationDays", 5),
                        totalBudget = obj.optDouble("totalBudget", 500.0),
                        status = obj.optString("status", "PENDING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        approvedAt = obj.optLong("approvedAt", 0L),
                        adminNote = obj.optString("adminNote", ""),
                        estimatedReach = obj.optInt("estimatedReach", 5000),
                        estimatedClicks = obj.optInt("estimatedClicks", 350),
                        impressions = obj.optInt("impressions", 0),
                        clicks = obj.optInt("clicks", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveAdvertisementsLocally(list: List<AdvertisementItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("userId", item.userId)
                    put("userName", item.userName)
                    put("userEmail", item.userEmail)
                    put("userPhone", item.userPhone)
                    put("userAvatar", item.userAvatar)
                    put("campaignName", item.campaignName)
                    put("campaignGoal", item.campaignGoal)
                    put("headline", item.headline)
                    put("description", item.description)
                    put("mediaUrl", item.mediaUrl)
                    put("mediaType", item.mediaType)
                    put("postType", item.postType)
                    put("pageId", item.pageId)
                    put("pageName", item.pageName)
                    put("pageAvatarUrl", item.pageAvatarUrl)
                    put("linkedPostId", item.linkedPostId)
                    put("destinationUrl", item.destinationUrl)
                    put("callToAction", item.callToAction)
                    put("targetLocation", item.targetLocation)
                    put("targetAgeRange", item.targetAgeRange)
                    put("targetGender", item.targetGender)
                    put("dailyBudget", item.dailyBudget)
                    put("durationDays", item.durationDays)
                    put("totalBudget", item.totalBudget)
                    put("status", item.status)
                    put("createdAt", item.createdAt)
                    put("approvedAt", item.approvedAt)
                    put("adminNote", item.adminNote)
                    put("estimatedReach", item.estimatedReach)
                    put("estimatedClicks", item.estimatedClicks)
                    put("impressions", item.impressions)
                    put("clicks", item.clicks)
                }
                arr.put(obj)
            }
            prefs.edit().putString("ads_list_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun submitAdvertisement(
        ad: AdvertisementItem,
        walletRepo: WalletRepository,
        postRepo: PostRepository? = null
    ): Result<AdvertisementItem> {
        val currentBalance = walletRepo.balanceFlow.value
        if (currentBalance < ad.totalBudget) {
            return Result.failure(
                Exception("Insufficient wallet balance! Needed BDT ${String.format("%.2f", ad.totalBudget)}, but available is BDT ${String.format("%.2f", currentBalance)}. Please recharge first.")
            )
        }

        // Deduct from wallet
        val deducted = walletRepo.deduct(
            amount = ad.totalBudget,
            title = "Ad Campaign: ${ad.campaignName}",
            subtitle = "Deduction for ${ad.durationDays} days @ BDT ${ad.dailyBudget}/day"
        )
        if (!deducted) {
            return Result.failure(Exception("Failed to process payment from wallet."))
        }

        var linkedPostId = ad.linkedPostId
        // Also post to user profile or page timeline as requested
        if (postRepo != null) {
            try {
                val newPostId = if (linkedPostId.isNotBlank()) linkedPostId else UUID.randomUUID().toString()
                val isPage = ad.postType.equals("PAGE", ignoreCase = true) && ad.pageId.isNotBlank()
                val authorId = if (isPage) ad.pageId else ad.userId
                val authorName = if (isPage) ad.pageName else ad.userName
                val authorAvatar = if (isPage) ad.pageAvatarUrl else ad.userAvatar
                val postContent = buildString {
                    if (ad.headline.isNotBlank()) append(ad.headline)
                    if (ad.headline.isNotBlank() && ad.description.isNotBlank()) append("\n\n")
                    if (ad.description.isNotBlank()) append(ad.description)
                }
                val mType = if (ad.mediaType.equals("video", ignoreCase = true)) "video" else if (ad.mediaUrl.isNotBlank()) "photo" else "text"

                val createdPost = PostItem(
                    id = newPostId,
                    authorId = authorId,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatar,
                    content = postContent,
                    mediaType = mType,
                    mediaUrl = ad.mediaUrl,
                    audience = "Public",
                    pageId = if (isPage) ad.pageId else "",
                    pageName = if (isPage) ad.pageName else "",
                    createdAt = System.currentTimeMillis()
                )
                postRepo.createPost(createdPost)
                linkedPostId = newPostId
            } catch (e: Exception) {
                Log.w("AdvertisementRepo", "Failed to create linked post: ${e.message}")
            }
        }

        val finalAd = ad.copy(
            id = if (ad.id.isBlank()) UUID.randomUUID().toString() else ad.id,
            linkedPostId = linkedPostId,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        val updated = listOf(finalAd) + _advertisementsFlow.value.filter { it.id != finalAd.id }
        _advertisementsFlow.value = updated
        saveAdvertisementsLocally(updated)

        // Sync to Firebase RTDB
        try {
            adsRef?.child(finalAd.id)?.setValue(finalAd.toMap())
        } catch (e: Exception) {
            Log.w("AdvertisementRepo", "Failed to sync to Firebase: ${e.message}")
        }

        return Result.success(finalAd)
    }

    fun updateAdvertisementStatus(
        adId: String,
        newStatus: String,
        adminNote: String = "",
        walletRepo: WalletRepository? = null,
        refundWallet: Boolean = false
    ): Boolean {
        val list = _advertisementsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == adId }
        if (index < 0) return false

        val currentAd = list[index]

        // If admin chose to refund wallet on rejection
        if (refundWallet && walletRepo != null && currentAd.totalBudget > 0) {
            walletRepo.refund(
                amount = currentAd.totalBudget,
                title = "Refund: Ad Campaign",
                subtitle = "${currentAd.campaignName} (Status: $newStatus)"
            )
        }

        val updatedAd = currentAd.copy(
            status = newStatus,
            adminNote = adminNote.ifBlank { currentAd.adminNote },
            approvedAt = if (newStatus == "RUNNING" && currentAd.approvedAt == 0L) System.currentTimeMillis() else currentAd.approvedAt
        )

        list[index] = updatedAd
        _advertisementsFlow.value = list
        saveAdvertisementsLocally(list)

        // Sync to Firebase
        try {
            adsRef?.child(adId)?.updateChildren(
                mapOf(
                    "status" to newStatus,
                    "adminNote" to updatedAd.adminNote,
                    "approvedAt" to updatedAd.approvedAt
                )
            )
        } catch (_: Exception) {}

        return true
    }

    fun togglePauseResume(adId: String): Boolean {
        val list = _advertisementsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == adId }
        if (index < 0) return false

        val currentAd = list[index]
        val newStatus = when (currentAd.status) {
            "RUNNING" -> "PAUSED"
            "PAUSED" -> "RUNNING"
            else -> return false
        }

        val updatedAd = currentAd.copy(status = newStatus)
        list[index] = updatedAd
        _advertisementsFlow.value = list
        saveAdvertisementsLocally(list)

        try {
            adsRef?.child(adId)?.child("status")?.setValue(newStatus)
        } catch (_: Exception) {}

        return true
    }

    fun deleteAdvertisement(adId: String): Boolean {
        val updated = _advertisementsFlow.value.filter { it.id != adId }
        _advertisementsFlow.value = updated
        saveAdvertisementsLocally(updated)

        try {
            adsRef?.child(adId)?.removeValue()
        } catch (_: Exception) {}

        return true
    }

    fun recordImpression(adId: String) {
        val list = _advertisementsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == adId }
        if (index < 0) return

        val ad = list[index]
        val updatedAd = ad.copy(impressions = ad.impressions + 1)
        list[index] = updatedAd
        _advertisementsFlow.value = list
        saveAdvertisementsLocally(list)

        try {
            adsRef?.child(adId)?.child("impressions")?.setValue(updatedAd.impressions)
        } catch (_: Exception) {}
    }

    fun recordClick(adId: String) {
        val list = _advertisementsFlow.value.toMutableList()
        val index = list.indexOfFirst { it.id == adId }
        if (index < 0) return

        val ad = list[index]
        val updatedAd = ad.copy(clicks = ad.clicks + 1)
        list[index] = updatedAd
        _advertisementsFlow.value = list
        saveAdvertisementsLocally(list)

        try {
            adsRef?.child(adId)?.child("clicks")?.setValue(updatedAd.clicks)
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: AdvertisementRepository? = null

        fun getInstance(context: Context): AdvertisementRepository {
            return instance ?: synchronized(this) {
                instance ?: AdvertisementRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
