package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.NotificationItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NotificationRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("frndom_notifications_prefs", Context.MODE_PRIVATE)

    private val dbRef: DatabaseReference? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isNotEmpty()) {
                com.example.util.FirebaseDatabaseHelper.getInstance().getReference("notifications")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("NotificationRepo", "FirebaseDatabase not initialized: ${e.message}")
            null
        }
    }

    fun getLocalNotifications(recipientId: String): List<NotificationItem> {
        if (recipientId.isBlank()) return emptyList()
        val json = prefs.getString("notifications_$recipientId", null) ?: return emptyList()
        val list = mutableListOf<NotificationItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NotificationItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        recipientId = obj.optString("recipientId", recipientId),
                        senderId = obj.optString("senderId", ""),
                        senderName = obj.optString("senderName", "User"),
                        senderAvatarUrl = obj.optString("senderAvatarUrl", ""),
                        postId = obj.optString("postId", ""),
                        type = obj.optString("type", "like"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        imageUrl = obj.optString("imageUrl", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isRead = obj.optBoolean("isRead", false),
                        reactionKey = obj.optString("reactionKey", "")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error parsing cached notifications: ${e.message}")
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun saveLocalNotifications(recipientId: String, notifications: List<NotificationItem>) {
        if (recipientId.isBlank()) return
        try {
            val arr = JSONArray()
            for (n in notifications) {
                val obj = JSONObject().apply {
                    put("id", n.id)
                    put("recipientId", n.recipientId)
                    put("senderId", n.senderId)
                    put("senderName", n.senderName)
                    put("senderAvatarUrl", n.senderAvatarUrl)
                    put("postId", n.postId)
                    put("type", n.type)
                    put("title", n.title)
                    put("content", n.content)
                    put("imageUrl", n.imageUrl)
                    put("timestamp", n.timestamp)
                    put("isRead", n.isRead)
                    put("reactionKey", n.reactionKey)
                }
                arr.put(obj)
            }
            prefs.edit().putString("notifications_$recipientId", arr.toString()).apply()
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error saving cached notifications: ${e.message}")
        }
    }

    fun addNotification(notification: NotificationItem) {
        val targetRecipient = notification.recipientId.ifBlank { "global" }
        val newNotif = if (notification.id.isBlank()) notification.copy(id = UUID.randomUUID().toString(), recipientId = targetRecipient) else notification.copy(recipientId = targetRecipient)

        // 1. Immediately update local storage
        val currentList = getLocalNotifications(targetRecipient).toMutableList()
        currentList.removeAll { it.id == newNotif.id }
        currentList.add(0, newNotif)
        saveLocalNotifications(targetRecipient, currentList)

        // 2. Persist to Firebase Realtime Database
        try {
            dbRef?.child(targetRecipient)?.child(newNotif.id)?.setValue(newNotif.toMap())
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Firebase addNotification error: ${e.message}")
        }
    }

    fun getNotificationsFlow(recipientId: String): Flow<List<NotificationItem>> = callbackFlow {
        val target = recipientId.ifBlank { "global" }
        // 1. Send cached notifications immediately
        val initial = getLocalNotifications(target)
        trySend(initial)

        val recipientRef = try {
            dbRef?.child(target)
        } catch (e: Exception) {
            null
        }

        if (recipientRef == null) {
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fbList = mutableListOf<NotificationItem>()
                for (child in snapshot.children) {
                    val notif = child.getValue(NotificationItem::class.java)
                    if (notif != null) {
                        fbList.add(notif)
                    }
                }

                val mergedMap = LinkedHashMap<String, NotificationItem>()
                getLocalNotifications(target).forEach { mergedMap[it.id] = it }
                fbList.forEach { mergedMap[it.id] = it }
                val mergedList = mergedMap.values.toList().sortedByDescending { it.timestamp }

                saveLocalNotifications(target, mergedList)
                trySend(mergedList)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(getLocalNotifications(target))
            }
        }

        recipientRef.addValueEventListener(listener)
        awaitClose { recipientRef.removeEventListener(listener) }
    }

    fun markAllAsRead(recipientId: String) {
        val target = recipientId.ifBlank { "global" }
        val list = getLocalNotifications(target).map { it.copy(isRead = true) }
        saveLocalNotifications(target, list)
        try {
            list.forEach { n ->
                dbRef?.child(target)?.child(n.id)?.child("isRead")?.setValue(true)
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Firebase markAllAsRead error: ${e.message}")
        }
    }

    fun removeNotification(recipientId: String, postId: String, type: String, senderId: String = "") {
        val target = recipientId.ifBlank { "global" }
        val current = getLocalNotifications(target).toMutableList()
        val toRemove = current.filter {
            it.postId == postId && it.type == type && (senderId.isBlank() || it.senderId == senderId)
        }
        if (toRemove.isNotEmpty()) {
            current.removeAll(toRemove)
            saveLocalNotifications(target, current)
            try {
                toRemove.forEach { n ->
                    dbRef?.child(target)?.child(n.id)?.removeValue()
                }
            } catch (e: Exception) {
                Log.e("NotificationRepo", "Firebase removeNotification error: ${e.message}")
            }
        }
    }

    /**
     * Purges all like and comment notifications when disabled by Admin
     */
    fun purgeEngagementNotifications(recipientId: String = "") {
        try {
            val allKeys = prefs.all.keys.filter { it.startsWith("notifications_") }
            val targetsToPurge = if (recipientId.isNotBlank()) {
                listOf(recipientId)
            } else {
                allKeys.map { it.removePrefix("notifications_") } + listOf("global")
            }

            for (target in targetsToPurge.distinct()) {
                val current = getLocalNotifications(target)
                val filtered = current.filter { it.type != "like" && it.type != "comment" }
                val toRemove = current.filter { it.type == "like" || it.type == "comment" }

                if (toRemove.isNotEmpty()) {
                    saveLocalNotifications(target, filtered)
                    toRemove.forEach { n ->
                        try {
                            dbRef?.child(target)?.child(n.id)?.removeValue()
                        } catch (e: Exception) {
                            Log.e("NotificationRepo", "Firebase delete failed for ${n.id}: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "purgeEngagementNotifications error: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
