package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility to format post/comment timestamps in Facebook-style relative time in English.
 *
 * Rules:
 * - < 1 minute: "Just now"
 * - 1 minute: "1 min ago"
 * - < 60 minutes: "X mins ago"
 * - 1 hour: "1 hour ago"
 * - < 24 hours: "X hours ago"
 * - 1 day: "1 day ago"
 * - 2..6 days: "X days ago"
 * - 7 days: "7 days ago"
 * - Older than 7 days (same year): "d MMM 'at' h:mm a" (e.g., "15 Sep at 4:30 PM")
 * - Different year: "d MMM yyyy 'at' h:mm a" (e.g., "15 Sep 2024 at 4:30 PM")
 */
fun formatPostTimestamp(createdAt: Long): String {
    if (createdAt <= 0L) return "Just now"

    val now = System.currentTimeMillis()
    val diff = (now - createdAt).coerceAtLeast(0L)

    val seconds = diff / 1000L
    if (seconds < 60L) {
        return "Just now"
    }

    val minutes = seconds / 60L
    if (minutes < 60L) {
        return if (minutes == 1L) "1 min ago" else "$minutes mins ago"
    }

    val hours = minutes / 60L
    if (hours < 24L) {
        return if (hours == 1L) "1 hour ago" else "$hours hours ago"
    }

    val days = hours / 24L
    if (days == 1L) {
        return "1 day ago"
    }
    if (days in 2L..7L) {
        return "$days days ago"
    }

    val postCal = Calendar.getInstance().apply { timeInMillis = createdAt }
    val currentCal = Calendar.getInstance().apply { timeInMillis = now }

    return if (postCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)) {
        SimpleDateFormat("d MMM 'at' h:mm a", Locale.ENGLISH).format(Date(createdAt))
    } else {
        SimpleDateFormat("d MMM yyyy 'at' h:mm a", Locale.ENGLISH).format(Date(createdAt))
    }
}
