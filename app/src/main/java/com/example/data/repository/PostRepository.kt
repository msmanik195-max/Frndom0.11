package com.example.data.repository

import kotlinx.coroutines.channels.awaitClose

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import android.content.Context
import android.util.Log
import com.example.data.model.PostItem
import com.example.data.model.PostReport
import com.example.data.model.ReactionType
import com.example.data.model.UserProfile
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import android.net.Uri
import com.example.data.service.MediaUploadService
import com.example.util.MediaUriHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class PostUploadState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val progressPercent: Int = 0,
    val statusText: String = "",
    val mediaType: String = "text",
    val previewUri: Uri? = null,
    val contentPreview: String = "",
    val isCompleted: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

class PostRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_posts_prefs", Context.MODE_PRIVATE)
    private val notificationRepository = NotificationRepository(context)
    private val uploadScope = CoroutineScope(Dispatchers.IO)

    private val _postUploadState = MutableStateFlow(PostUploadState())
    val postUploadState = _postUploadState.asStateFlow()

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                com.example.util.FirebaseDatabaseHelper.getInstance().getReference("posts")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("PostRepository", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    private val _postsFlow = MutableStateFlow<List<PostItem>>(getLocalPosts())
    val postsFlow = _postsFlow.asStateFlow()

    private val _reportsFlow = MutableStateFlow<List<PostReport>>(emptyList())
    val reportsFlow = _reportsFlow.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        listenToFirebasePosts()
        listenToPostReports()
    }

    private fun listenToFirebasePosts() {
        try {
            dbRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PostItem>()
                    for (child in snapshot.children) {
                        try {
                            val post = child.getValue(PostItem::class.java)
                            if (post != null) {
                                val isVerified = post.isAuthorVerified || UserRepository.isUserVerifiedStatic(post.authorId)
                                val safeMediaUrl = MediaUriHelper.sanitizeMediaUrl(context, post.mediaUrl, if (post.mediaType == "reel") "reels" else "posts", if (post.mediaType == "reel" || post.mediaType == "video") "mp4" else "jpg")
                                val sanitizedPost = if (safeMediaUrl != post.mediaUrl) post.copy(mediaUrl = safeMediaUrl) else post
                                list.add(if (isVerified && !sanitizedPost.isAuthorVerified) sanitizedPost.copy(isAuthorVerified = true) else sanitizedPost)
                            }
                        } catch (_: Exception) {}
                    }
                    val sortedList = list.sortedByDescending { it.createdAt }
                    saveLocalPosts(sortedList)
                    _postsFlow.value = sortedList
                    _isRefreshing.value = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w("PostRepository", "Firebase posts cancelled: ${error.message}")
                    _isRefreshing.value = false
                }
            })
        } catch (e: Exception) {
            Log.e("PostRepository", "Error setting up posts listener: ${e.message}")
        }
    }

    fun refreshPosts() {
        _isRefreshing.value = true
        try {
            dbRef?.get()?.addOnSuccessListener { snapshot ->
                val list = mutableListOf<PostItem>()
                for (child in snapshot.children) {
                    val post = child.getValue(PostItem::class.java)
                    if (post != null) {
                        val isVerified = post.isAuthorVerified || UserRepository.isUserVerifiedStatic(post.authorId)
                        list.add(if (isVerified && !post.isAuthorVerified) post.copy(isAuthorVerified = true) else post)
                    }
                }
                val sortedList = list.sortedByDescending { it.createdAt }
                
                // Add a small artificial delay to make the shimmer effect visible and feel like a real refresh
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    kotlinx.coroutines.delay(1200)
                    saveLocalPosts(sortedList)
                    _postsFlow.value = sortedList
                    _isRefreshing.value = false
                }
            }?.addOnFailureListener {
                _isRefreshing.value = false
            } ?: run {
                _isRefreshing.value = false
            }
        } catch (e: Exception) {
            _isRefreshing.value = false
        }
    }

    fun getLocalPosts(): List<PostItem> {
        val json = prefs.getString("cached_posts", null) ?: return emptyList()
        val list = mutableListOf<PostItem>()
        var hadMigration = false
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val likedByMap = mutableMapOf<String, Boolean>()
                if (obj.has("likedByMap")) {
                    val likedObj = obj.getJSONObject("likedByMap")
                    val keys = likedObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        likedByMap[k] = likedObj.getBoolean(k)
                    }
                }

                val reactionsMap = mutableMapOf<String, String>()
                if (obj.has("reactionsMap")) {
                    val reactObj = obj.getJSONObject("reactionsMap")
                    val keys = reactObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        reactionsMap[k] = reactObj.getString(k)
                    }
                }

                val viewedByMap = mutableMapOf<String, Boolean>()
                if (obj.has("viewedByMap")) {
                    val viewedObj = obj.getJSONObject("viewedByMap")
                    val keys = viewedObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        viewedByMap[k] = viewedObj.getBoolean(k)
                    }
                }

                val mediaUrlsList = mutableListOf<String>()
                if (obj.has("mediaUrls")) {
                    val mArr = obj.getJSONArray("mediaUrls")
                    for (m in 0 until mArr.length()) {
                        val rawM = mArr.getString(m)
                        val safeM = MediaUriHelper.sanitizeMediaUrl(context, rawM, "posts", "jpg")
                        if (safeM.isNotBlank()) {
                            mediaUrlsList.add(safeM)
                        }
                        if (rawM.isNotBlank() && safeM != rawM) {
                            hadMigration = true
                        }
                    }
                }

                val authorId = obj.optString("authorId", "")
                val isVerifiedFromCache = obj.optBoolean("isAuthorVerified", false)
                val isVerified = isVerifiedFromCache || UserRepository.isUserVerifiedStatic(authorId)

                val storedViewsCount = obj.optInt("viewsCount", viewedByMap.size)
                val effectiveViewsCount = if (viewedByMap.isNotEmpty()) viewedByMap.size else storedViewsCount

                val mediaType = obj.optString("mediaType", "text")
                val rawMediaUrl = obj.optString("mediaUrl", "")
                val safeMediaUrl = MediaUriHelper.sanitizeMediaUrl(
                    context,
                    rawMediaUrl,
                    if (mediaType == "reel") "reels" else "posts",
                    if (mediaType == "reel" || mediaType == "video") "mp4" else "jpg"
                )
                if (rawMediaUrl.isNotBlank() && safeMediaUrl != rawMediaUrl) {
                    hadMigration = true
                }

                list.add(
                    PostItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        authorId = authorId,
                        authorName = obj.optString("authorName", "User"),
                        authorAvatarUrl = obj.optString("authorAvatarUrl", ""),
                        content = obj.optString("content", ""),
                        backgroundStyle = obj.optString("backgroundStyle", "none"),
                        fontSize = obj.optInt("fontSize", 24),
                        textAlign = obj.optString("textAlign", "center"),
                        mediaType = mediaType,
                        mediaUrl = safeMediaUrl,
                        mediaUrls = mediaUrlsList,
                        audience = obj.optString("audience", "Public"),
                        groupId = obj.optString("groupId", ""),
                        groupName = obj.optString("groupName", ""),
                        pageId = obj.optString("pageId", ""),
                        pageName = obj.optString("pageName", ""),
                        likesCount = obj.optInt("likesCount", 0),
                        commentsCount = obj.optInt("commentsCount", 0),
                        sharesCount = obj.optInt("sharesCount", 0),
                        viewsCount = effectiveViewsCount,
                        likedByMap = likedByMap,
                        reactionsMap = reactionsMap,
                        viewedByMap = viewedByMap,
                        isAuthorVerified = isVerified,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error parsing cached posts: ${e.message}")
        }
        val sorted = list.sortedByDescending { it.createdAt }
        if (hadMigration) {
            saveLocalPosts(sorted)
        }
        return sorted
    }

    fun saveLocalPosts(posts: List<PostItem>) {
        try {
            val arr = JSONArray()
            for (p in posts) {
                val isVerified = p.isAuthorVerified || UserRepository.isUserVerifiedStatic(p.authorId)
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("authorId", p.authorId)
                    put("authorName", p.authorName)
                    put("authorAvatarUrl", p.authorAvatarUrl)
                    put("content", p.content)
                    put("backgroundStyle", p.backgroundStyle)
                    put("fontSize", p.fontSize)
                    put("textAlign", p.textAlign)
                    put("mediaType", p.mediaType)
                    put("mediaUrl", p.mediaUrl)
                    val mArr = JSONArray()
                    p.mediaUrls.forEach { mArr.put(it) }
                    put("mediaUrls", mArr)
                    put("audience", p.audience)
                    put("groupId", p.groupId)
                    put("groupName", p.groupName)
                    put("pageId", p.pageId)
                    put("pageName", p.pageName)
                    put("likesCount", p.likesCount)
                    put("commentsCount", p.commentsCount)
                    put("sharesCount", p.sharesCount)
                    put("viewsCount", p.viewsCount)
                    put("createdAt", p.createdAt)
                    val likedObj = JSONObject()
                    p.likedByMap.forEach { (k, v) -> likedObj.put(k, v) }
                    put("likedByMap", likedObj)
                    val reactObj = JSONObject()
                    p.reactionsMap.forEach { (k, v) -> reactObj.put(k, v) }
                    put("reactionsMap", reactObj)
                    val viewedObj = JSONObject()
                    p.viewedByMap.forEach { (k, v) -> viewedObj.put(k, v) }
                    put("viewedByMap", viewedObj)
                    put("isAuthorVerified", isVerified)
                }
                arr.put(obj)
            }
            prefs.edit().putString("cached_posts", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error saving cached posts: ${e.message}")
        }
    }

    fun createPost(post: PostItem) {
        val authorVerified = post.isAuthorVerified || UserRepository.isUserVerifiedStatic(post.authorId)
        val newPost = (if (post.id.isBlank()) post.copy(id = UUID.randomUUID().toString()) else post).copy(
            isAuthorVerified = authorVerified
        )
        val current = _postsFlow.value.toMutableList()
        current.add(0, newPost)
        _postsFlow.value = current
        saveLocalPosts(current)

        // Record content created for daily limits enforcement
        try {
            val contentType = when {
                newPost.mediaType == "reel" || newPost.mediaType == "video" -> "video"
                newPost.mediaType == "photo" || newPost.mediaUrls.isNotEmpty() || newPost.mediaUrl.isNotBlank() -> "image"
                newPost.content.contains("http://") || newPost.content.contains("https://") || newPost.content.contains("www.") -> "link"
                else -> "text"
            }
            val isPage = newPost.pageId.isNotBlank()
            val entityId = if (isPage) newPost.pageId else newPost.authorId
            ContentLimitManager.getInstance(context).recordContentCreated(entityId, isPage, contentType)
        } catch (e: Exception) {
            Log.w("PostRepository", "Error recording daily limit count: ${e.message}")
        }

        try {
            dbRef?.child(newPost.id)?.setValue(newPost.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase create post error: ${e.message}")
        }
    }

    /**
     * Non-blocking background post uploader with real-time progress state updates
     */
    fun uploadAndCreatePost(
        postTemplate: PostItem,
        mediaUris: List<Uri>,
        videoUri: Uri?,
        mediaUploadService: MediaUploadService
    ) {
        val mediaType = postTemplate.mediaType
        val previewUri = mediaUris.firstOrNull() ?: videoUri

        uploadScope.launch {
            try {
                _postUploadState.value = PostUploadState(
                    isUploading = true,
                    progress = 0.05f,
                    progressPercent = 5,
                    statusText = "Preparing upload...",
                    mediaType = mediaType,
                    previewUri = previewUri,
                    contentPreview = postTemplate.content,
                    isCompleted = false,
                    isError = false
                )

                val uploadedUrls = mutableListOf<String>()

                if (videoUri != null || mediaType == "reel" || mediaType == "video") {
                    val uri = videoUri ?: previewUri
                    if (uri != null) {
                        _postUploadState.value = _postUploadState.value.copy(
                            progress = 0.15f,
                            progressPercent = 15,
                            statusText = "Uploading video..."
                        )

                        // Simulate smooth progress ticks while uploading video in background
                        val progressJob = launch {
                            val milestones = listOf(
                                0.25f to 25,
                                0.40f to 40,
                                0.58f to 58,
                                0.72f to 72,
                                0.85f to 85,
                                0.90f to 90
                            )
                            for ((prog, pct) in milestones) {
                                delay(600)
                                if (_postUploadState.value.isUploading && !_postUploadState.value.isCompleted) {
                                    _postUploadState.value = _postUploadState.value.copy(
                                        progress = prog,
                                        progressPercent = pct,
                                        statusText = "Uploading video ($pct%)..."
                                    )
                                }
                            }
                        }

                        val folder = if (mediaType == "reel") "reels" else "videos"
                        val uploadRes = mediaUploadService.uploadVideoUri(uri, folder = folder)
                        progressJob.cancel()

                        val uploadedUrl = uploadRes.getOrDefault(uri.toString())
                        uploadedUrls.add(uploadedUrl)
                    }
                } else if (mediaUris.isNotEmpty()) {
                    val total = mediaUris.size
                    for (index in mediaUris.indices) {
                        val currentUri = mediaUris[index]
                        val startProg = 0.10f + (index.toFloat() / total) * 0.80f
                        val startPct = (startProg * 100).toInt()

                        _postUploadState.value = _postUploadState.value.copy(
                            progress = startProg,
                            progressPercent = startPct,
                            statusText = "Uploading photo ${index + 1} of $total ($startPct%)..."
                        )

                        val res = mediaUploadService.uploadImageUri(currentUri, folder = "posts")
                        uploadedUrls.add(res.getOrDefault(currentUri.toString()))

                        val endProg = 0.10f + ((index + 1).toFloat() / total) * 0.80f
                        val endPct = (endProg * 100).toInt()
                        _postUploadState.value = _postUploadState.value.copy(
                            progress = endProg,
                            progressPercent = endPct,
                            statusText = "Photo ${index + 1} uploaded"
                        )
                        delay(200)
                    }
                } else {
                    // Text only post
                    _postUploadState.value = _postUploadState.value.copy(
                        progress = 0.50f,
                        progressPercent = 50,
                        statusText = "Publishing post..."
                    )
                    delay(300)
                }

                _postUploadState.value = _postUploadState.value.copy(
                    progress = 0.95f,
                    progressPercent = 95,
                    statusText = "Finalizing post..."
                )
                delay(300)

                val finalMediaType = when {
                    mediaType == "reel" -> "reel"
                    mediaType == "video" -> "video"
                    uploadedUrls.isNotEmpty() -> "photo"
                    else -> "text"
                }

                val finalPost = postTemplate.copy(
                    mediaUrl = uploadedUrls.firstOrNull() ?: "",
                    mediaUrls = uploadedUrls,
                    mediaType = finalMediaType
                )

                createPost(finalPost)

                _postUploadState.value = _postUploadState.value.copy(
                    progress = 1.0f,
                    progressPercent = 100,
                    isCompleted = true,
                    statusText = "Post published successfully!"
                )

                // Keep completed card for 2.5 seconds so user sees 100% completion
                delay(2500)
                _postUploadState.value = PostUploadState(isUploading = false)
            } catch (e: Exception) {
                Log.e("PostRepository", "Background post upload error: ${e.message}", e)
                _postUploadState.value = _postUploadState.value.copy(
                    isError = true,
                    statusText = "Upload failed. Please try again."
                )
                delay(3000)
                _postUploadState.value = PostUploadState(isUploading = false)
            }
        }
    }

    fun setReaction(postId: String, userId: String, reaction: ReactionType?) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val prevReaction = post.getUserReaction(userId)
            val updatedReactionsMap = post.reactionsMap.toMutableMap()
            val updatedLikedByMap = post.likedByMap.toMutableMap()

            var newLikesCount = post.likesCount

            if (reaction == null) {
                // Remove reaction
                if (prevReaction != null) {
                    newLikesCount = (newLikesCount - 1).coerceAtLeast(0)
                }
                updatedReactionsMap.remove(userId)
                updatedLikedByMap.remove(userId)
            } else {
                // Add or update reaction
                if (prevReaction == null) {
                    newLikesCount += 1
                }
                updatedReactionsMap[userId] = reaction.key
                updatedLikedByMap[userId] = true
            }

            val updatedPost = post.copy(
                likesCount = newLikesCount,
                likedByMap = updatedLikedByMap,
                reactionsMap = updatedReactionsMap
            )
            current[index] = updatedPost
            _postsFlow.value = current
            saveLocalPosts(current)

            // Trigger or remove notification for reaction
            if (post.authorId.isNotBlank()) {
                if (reaction != null) {
                    val adminRepo = AdminRequestRepository.getInstance(context)
                    if (adminRepo.getAppSettings().engagementNotificationsEnabled) {
                        val userRepo = UserRepository(context)
                        val senderProfile: UserProfile? = userRepo.getLocalUserProfile(userId)
                        val senderName = if (senderProfile != null && senderProfile.fullName.isNotBlank()) {
                            senderProfile.fullName
                        } else if (senderProfile != null && (senderProfile.firstName.isNotBlank() || senderProfile.lastName.isNotBlank())) {
                            "${senderProfile.firstName} ${senderProfile.lastName}".trim()
                        } else {
                            "Someone"
                        }
                        val senderAvatar = senderProfile?.profilePictureUrl ?: ""
                        val postType = if (post.mediaType == "reel" || post.mediaType == "video") "reel" else "post"
                        val reactionText = when (reaction) {
                            ReactionType.LIKE -> "liked your $postType."
                            ReactionType.LOVE -> "loved your $postType."
                            ReactionType.CARE -> "reacted 🥰 to your $postType."
                            ReactionType.HAHA -> "reacted 😆 to your $postType."
                            ReactionType.WOW -> "reacted 😮 to your $postType."
                            ReactionType.SAD -> "reacted 😢 to your $postType."
                            ReactionType.ANGRY -> "reacted 😡 to your $postType."
                        }

                        notificationRepository.addNotification(
                            com.example.data.model.NotificationItem(
                                recipientId = post.authorId,
                                senderId = userId,
                                senderName = senderName,
                                senderAvatarUrl = senderAvatar,
                                postId = post.id,
                                type = "like",
                                reactionKey = reaction.key,
                                content = reactionText,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                } else {
                    // Removed like/reaction: remove notification
                    notificationRepository.removeNotification(
                        recipientId = post.authorId,
                        postId = post.id,
                        type = "like",
                        senderId = userId
                    )
                }
            }

            try {
                dbRef?.child(postId)?.child("likesCount")?.setValue(newLikesCount)
                dbRef?.child(postId)?.child("reactionsMap")?.child(userId)?.setValue(reaction?.key)
                dbRef?.child(postId)?.child("likedByMap")?.child(userId)?.setValue(if (reaction != null) true else null)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase setReaction error: ${e.message}")
            }
        }
    }

    fun toggleLike(postId: String, userId: String) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val hasReacted = post.getUserReaction(userId) != null
            if (hasReacted) {
                setReaction(postId, userId, null)
            } else {
                setReaction(postId, userId, ReactionType.LIKE)
            }
        }
    }

    fun addComment(postId: String) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val updated = post.copy(commentsCount = post.commentsCount + 1)
            current[index] = updated
            _postsFlow.value = current
            saveLocalPosts(current)

            try {
                dbRef?.child(postId)?.child("commentsCount")?.setValue(updated.commentsCount)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase comment error: ${e.message}")
            }
        }
    }

    fun incrementShare(postId: String) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            val updated = post.copy(sharesCount = post.sharesCount + 1)
            current[index] = updated
            _postsFlow.value = current
            saveLocalPosts(current)

            try {
                dbRef?.child(postId)?.child("sharesCount")?.setValue(updated.sharesCount)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase share error: ${e.message}")
            }
        }
    }

    /**
     * Records a unique view for a post, reel, video or photo.
     * Guaranteed: 1 view per unique user/profile/page account.
     * Even if the user watches 100 times, viewsCount remains 1 for this viewerId.
     */
    fun recordPostView(postId: String, viewerId: String) {
        if (postId.isBlank() || viewerId.isBlank()) return
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == postId }
        if (index >= 0) {
            val post = current[index]
            if (post.authorId == viewerId) {
                // Author viewing their own post - DO NOT increment
                return
            }
            if (post.viewedByMap.containsKey(viewerId)) {
                // Already viewed by this user or page account - DO NOT increment
                return
            }
            val newViewedByMap = post.viewedByMap + (viewerId to true)
            val newViewsCount = newViewedByMap.size
            val updated = post.copy(
                viewedByMap = newViewedByMap,
                viewsCount = newViewsCount
            )
            current[index] = updated
            _postsFlow.value = current
            saveLocalPosts(current)

            try {
                dbRef?.child(postId)?.child("viewedByMap")?.child(viewerId)?.setValue(true)
                dbRef?.child(postId)?.child("viewsCount")?.setValue(newViewsCount)
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase recordPostView error: ${e.message}")
            }
        }
    }

    fun addDetailedComment(comment: com.example.data.model.CommentItem) {
        // 1. Immediately save to local persistent cache
        saveLocalComment(comment)

        // 2. Increment post comments count
        addComment(comment.postId)

        // 3. Trigger notification for post author
        val post = _postsFlow.value.firstOrNull { it.id == comment.postId }
        if (post != null && post.authorId.isNotBlank()) {
            val adminRepo = AdminRequestRepository.getInstance(context)
            if (adminRepo.getAppSettings().engagementNotificationsEnabled) {
                val postType = if (post.mediaType == "reel" || post.mediaType == "video") "reel" else "post"
                val commentSnippet = if (comment.text.isNotBlank()) ": \"${comment.text.take(30)}\"" else ""
                notificationRepository.addNotification(
                    com.example.data.model.NotificationItem(
                        recipientId = post.authorId,
                        senderId = comment.authorId,
                        senderName = comment.authorName,
                        senderAvatarUrl = comment.authorAvatarUrl,
                        postId = post.id,
                        type = "comment",
                        content = "commented on your $postType$commentSnippet",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        // 4. Save to Firebase Database
        try {
            val commentsRef = FirebaseDatabase.getInstance().getReference("post_comments").child(comment.postId)
            commentsRef.child(comment.id).setValue(comment.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase addDetailedComment error: ${e.message}")
        }
    }

    fun getLocalComments(postId: String): List<com.example.data.model.CommentItem> {
        val json = prefs.getString("comments_$postId", null) ?: return emptyList()
        val list = mutableListOf<com.example.data.model.CommentItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val likedByMap = mutableMapOf<String, Boolean>()
                if (obj.has("likedByMap")) {
                    val likedObj = obj.getJSONObject("likedByMap")
                    val keys = likedObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        likedByMap[k] = likedObj.getBoolean(k)
                    }
                }
                list.add(
                    com.example.data.model.CommentItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        postId = obj.optString("postId", postId),
                        authorId = obj.optString("authorId", ""),
                        authorName = obj.optString("authorName", "User"),
                        authorAvatarUrl = obj.optString("authorAvatarUrl", ""),
                        text = obj.optString("text", ""),
                        emojiSticker = obj.optString("emojiSticker", ""),
                        parentCommentId = obj.optString("parentCommentId", ""),
                        replyToAuthorName = obj.optString("replyToAuthorName", ""),
                        likesCount = obj.optInt("likesCount", 0),
                        likedByMap = likedByMap,
                        isAuthorVerified = obj.optBoolean("isAuthorVerified", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error parsing cached comments for $postId: ${e.message}")
        }
        return list.sortedBy { it.createdAt }
    }

    fun saveLocalComment(comment: com.example.data.model.CommentItem) {
        val current = getLocalComments(comment.postId).toMutableList()
        val index = current.indexOfFirst { it.id == comment.id }
        if (index >= 0) {
            current[index] = comment
        } else {
            current.add(comment)
        }
        saveAllLocalComments(comment.postId, current)
    }

    fun saveAllLocalComments(postId: String, comments: List<com.example.data.model.CommentItem>) {
        try {
            val arr = JSONArray()
            for (c in comments) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("postId", c.postId)
                    put("authorId", c.authorId)
                    put("authorName", c.authorName)
                    put("authorAvatarUrl", c.authorAvatarUrl)
                    put("text", c.text)
                    put("emojiSticker", c.emojiSticker)
                    put("parentCommentId", c.parentCommentId)
                    put("replyToAuthorName", c.replyToAuthorName)
                    put("likesCount", c.likesCount)
                    put("createdAt", c.createdAt)
                    put("isAuthorVerified", c.isAuthorVerified)
                    val likedObj = JSONObject()
                    c.likedByMap.forEach { (k, v) -> likedObj.put(k, v) }
                    put("likedByMap", likedObj)
                }
                arr.put(obj)
            }
            prefs.edit().putString("comments_$postId", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("PostRepository", "Error saving cached comments for $postId: ${e.message}")
        }
    }

    fun getCommentsFlow(postId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.model.CommentItem>> = kotlinx.coroutines.flow.callbackFlow {
        // Send initial local cached comments immediately
        val initialList = getLocalComments(postId)
        trySend(initialList)

        val commentsRef = try {
            FirebaseDatabase.getInstance().getReference("post_comments").child(postId)
        } catch (e: Exception) {
            null
        }

        if (commentsRef == null) {
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fbList = mutableListOf<com.example.data.model.CommentItem>()
                for (child in snapshot.children) {
                    child.getValue(com.example.data.model.CommentItem::class.java)?.let { fbList.add(it) }
                }
                // Merge Firebase comments with local cached comments
                val mergedMap = LinkedHashMap<String, com.example.data.model.CommentItem>()
                getLocalComments(postId).forEach { mergedMap[it.id] = it }
                fbList.forEach { mergedMap[it.id] = it }
                val mergedList = mergedMap.values.toList().sortedBy { it.createdAt }

                saveAllLocalComments(postId, mergedList)
                trySend(mergedList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Keep local cached comments on error
                trySend(getLocalComments(postId))
            }
        }

        commentsRef.addValueEventListener(listener)
        awaitClose { commentsRef.removeEventListener(listener) }
    }

    fun deleteComment(postId: String, commentId: String) {
        val post = _postsFlow.value.firstOrNull { it.id == postId }
        val comments = getLocalComments(postId).toMutableList()
        val targetComment = comments.firstOrNull { it.id == commentId }
        if (targetComment != null) {
            comments.remove(targetComment)
            saveAllLocalComments(postId, comments)

            // Remove notification for post author
            if (post != null && post.authorId.isNotBlank()) {
                notificationRepository.removeNotification(
                    recipientId = post.authorId,
                    postId = postId,
                    type = "comment",
                    senderId = targetComment.authorId
                )
            }

            // Decrement comments count on the post
            val postList = _postsFlow.value.toMutableList()
            val postIndex = postList.indexOfFirst { it.id == postId }
            if (postIndex >= 0) {
                val p = postList[postIndex]
                val newCount = (p.commentsCount - 1).coerceAtLeast(0)
                postList[postIndex] = p.copy(commentsCount = newCount)
                _postsFlow.value = postList
                saveLocalPosts(postList)
                try {
                    dbRef?.child(postId)?.child("commentsCount")?.setValue(newCount)
                } catch (e: Exception) {
                    Log.e("PostRepository", "Firebase update commentsCount error: ${e.message}")
                }
            }

            try {
                FirebaseDatabase.getInstance().getReference("post_comments")
                    .child(postId).child(commentId).removeValue()
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase deleteComment error: ${e.message}")
            }
        }
    }

    // ==========================================
    // Post Editing, Deleting, Saving & Reporting
    // ==========================================

    fun updatePost(updatedPost: PostItem) {
        val current = _postsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == updatedPost.id }
        if (index >= 0) {
            current[index] = updatedPost
        } else {
            current.add(0, updatedPost)
        }
        _postsFlow.value = current
        saveLocalPosts(current)

        try {
            dbRef?.child(updatedPost.id)?.setValue(updatedPost.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase updatePost error: ${e.message}")
        }
    }

    fun deletePost(postId: String) {
        val current = _postsFlow.value.toMutableList()
        current.removeAll { it.id == postId }
        _postsFlow.value = current
        saveLocalPosts(current)

        try {
            dbRef?.child(postId)?.removeValue()
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase deletePost error: ${e.message}")
        }
    }

    fun deletePostsByAdvertisementId(adId: String) {
        if (adId.isBlank()) return
        val current = _postsFlow.value.toMutableList()
        val toRemove = current.filter { it.advertisementId == adId }
        if (toRemove.isEmpty()) return
        current.removeAll { it.advertisementId == adId }
        _postsFlow.value = current
        saveLocalPosts(current)

        for (p in toRemove) {
            try {
                dbRef?.child(p.id)?.removeValue()
            } catch (_: Exception) {}
        }
    }

    fun isPostSaved(userId: String, postId: String): Boolean {
        if (userId.isBlank() || postId.isBlank()) return false
        val savedSet = prefs.getStringSet("saved_posts_$userId", emptySet()) ?: emptySet()
        return savedSet.contains(postId)
    }

    fun toggleSavePost(userId: String, postId: String): Boolean {
        if (userId.isBlank() || postId.isBlank()) return false
        val savedSet = prefs.getStringSet("saved_posts_$userId", emptySet())?.toMutableSet() ?: mutableSetOf()
        val isNowSaved = if (savedSet.contains(postId)) {
            savedSet.remove(postId)
            false
        } else {
            savedSet.add(postId)
            true
        }
        prefs.edit().putStringSet("saved_posts_$userId", savedSet).apply()

        try {
            val savedRef = FirebaseDatabase.getInstance().getReference("user_saved_posts").child(userId).child(postId)
            if (isNowSaved) {
                savedRef.setValue(System.currentTimeMillis())
            } else {
                savedRef.removeValue()
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase toggleSavePost error: ${e.message}")
        }
        return isNowSaved
    }

    fun getSavedPosts(userId: String): List<PostItem> {
        if (userId.isBlank()) return emptyList()
        val savedSet = prefs.getStringSet("saved_posts_$userId", emptySet()) ?: emptySet()
        return _postsFlow.value.filter { savedSet.contains(it.id) }
    }

    fun updateUserAvatarAndName(userId: String, newAvatar: String, newName: String = "") {
        if (userId.isBlank()) return
        val current = _postsFlow.value
        var hasChanges = false
        val updated = current.map { post ->
            if (post.authorId == userId) {
                hasChanges = true
                post.copy(
                    authorAvatarUrl = newAvatar.ifBlank { post.authorAvatarUrl },
                    authorName = newName.ifBlank { post.authorName }
                )
            } else {
                post
            }
        }
        if (hasChanges) {
            _postsFlow.value = updated
            saveLocalPosts(updated)
            try {
                dbRef?.get()?.addOnSuccessListener { snapshot ->
                    for (child in snapshot.children) {
                        val authorId = child.child("authorId").getValue(String::class.java)
                        if (authorId == userId) {
                            val key = child.key ?: continue
                            val updates = mutableMapOf<String, Any>()
                            if (newAvatar.isNotBlank()) updates["authorAvatarUrl"] = newAvatar
                            if (newName.isNotBlank()) updates["authorName"] = newName
                            dbRef?.child(key)?.updateChildren(updates)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun listenToPostReports() {
        try {
            val reportsRef = FirebaseDatabase.getInstance().getReference("admin_post_reports")
            reportsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<PostReport>()
                    for (child in snapshot.children) {
                        try {
                            val key = child.key ?: ""
                            val id = child.child("id").getValue(String::class.java) ?: key
                            val postId = child.child("postId").getValue(String::class.java) ?: ""
                            val reporterId = child.child("reporterId").getValue(String::class.java) ?: ""
                            val reporterName = child.child("reporterName").getValue(String::class.java) ?: ""
                            val reason = child.child("reason").getValue(String::class.java) ?: ""
                            val details = child.child("details").getValue(String::class.java) ?: ""
                            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                            val status = child.child("status").getValue(String::class.java) ?: "pending"
                            val postAuthorId = child.child("postAuthorId").getValue(String::class.java) ?: ""
                            val postAuthorName = child.child("postAuthorName").getValue(String::class.java) ?: ""
                            val postAuthorAvatarUrl = child.child("postAuthorAvatarUrl").getValue(String::class.java) ?: ""
                            val postContent = child.child("postContent").getValue(String::class.java) ?: ""
                            val postMediaType = child.child("postMediaType").getValue(String::class.java) ?: "text"
                            val postMediaUrl = child.child("postMediaUrl").getValue(String::class.java) ?: ""
                            val rawMediaUrls = child.child("postMediaUrls").value
                            val postMediaUrls = (rawMediaUrls as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                            list.add(
                                PostReport(
                                    id = if (id.isNotBlank()) id else key,
                                    postId = postId,
                                    reporterId = reporterId,
                                    reporterName = reporterName,
                                    reason = reason,
                                    details = details,
                                    timestamp = timestamp,
                                    status = status,
                                    postAuthorId = postAuthorId,
                                    postAuthorName = postAuthorName,
                                    postAuthorAvatarUrl = postAuthorAvatarUrl,
                                    postContent = postContent,
                                    postMediaType = postMediaType,
                                    postMediaUrl = postMediaUrl,
                                    postMediaUrls = postMediaUrls
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("PostRepository", "Error parsing report: ${e.message}")
                        }
                    }
                    _reportsFlow.value = list.sortedByDescending { it.timestamp }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PostRepository", "Reports listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("PostRepository", "Error setting up reports listener: ${e.message}")
        }
    }

    fun reportPost(post: PostItem, reporterId: String, reporterName: String = "", reason: String, details: String = "") {
        try {
            val reportId = UUID.randomUUID().toString()
            val report = PostReport(
                id = reportId,
                postId = post.id,
                reporterId = reporterId,
                reporterName = reporterName,
                reason = reason,
                details = details,
                timestamp = System.currentTimeMillis(),
                status = "pending",
                postAuthorId = post.authorId,
                postAuthorName = post.authorName,
                postAuthorAvatarUrl = post.authorAvatarUrl,
                postContent = post.content,
                postMediaType = post.mediaType,
                postMediaUrl = post.mediaUrl,
                postMediaUrls = post.getAllMediaUrls()
            )
            val ref = FirebaseDatabase.getInstance().getReference("admin_post_reports")
            ref.child(reportId).setValue(report.toMap())
        } catch (e: Exception) {
            Log.e("PostRepository", "Firebase reportPost error: ${e.message}")
        }
    }

    fun reportPost(postId: String, reporterId: String, reason: String, details: String = "") {
        val foundPost = _postsFlow.value.firstOrNull { it.id == postId }
        if (foundPost != null) {
            reportPost(foundPost, reporterId, "", reason, details)
        } else {
            try {
                val reportId = UUID.randomUUID().toString()
                val report = PostReport(
                    id = reportId,
                    postId = postId,
                    reporterId = reporterId,
                    reason = reason,
                    details = details,
                    timestamp = System.currentTimeMillis(),
                    status = "pending"
                )
                FirebaseDatabase.getInstance().getReference("admin_post_reports").child(reportId).setValue(report.toMap())
            } catch (e: Exception) {
                Log.e("PostRepository", "Firebase reportPost error: ${e.message}")
            }
        }
    }

    fun dismissReport(reportId: String) {
        try {
            val ref = FirebaseDatabase.getInstance().getReference("admin_post_reports")
            ref.child(reportId).removeValue()
            _reportsFlow.value = _reportsFlow.value.filterNot { it.id == reportId }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error dismissing report: ${e.message}")
        }
    }

    fun deleteReportedPost(report: PostReport) {
        try {
            if (report.postId.isNotBlank()) {
                deletePost(report.postId)
            }
            val ref = FirebaseDatabase.getInstance().getReference("admin_post_reports")
            ref.child(report.id).removeValue()
            _reportsFlow.value = _reportsFlow.value.filterNot { it.id == report.id }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting reported post: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: PostRepository? = null

        fun getInstance(context: Context): PostRepository {
            return instance ?: synchronized(this) {
                instance ?: PostRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
