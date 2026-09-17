package com.example.util

import com.google.firebase.database.FirebaseDatabase

/**
 * Centralized Firebase helper ensuring both com.frndom (User App) and com.Flikata.admin (Admin App)
 * point to the exact same Realtime Database URL and Firebase Storage Bucket.
 */
object FirebaseDatabaseHelper {
    const val DATABASE_URL = "https://frndom-871ec-default-rtdb.firebaseio.com"
    const val STORAGE_BUCKET = "frndom-871ec.firebasestorage.app"
    const val PROJECT_ID = "frndom-871ec"

    fun getInstance(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(DATABASE_URL)
        } catch (_: Throwable) {
            FirebaseDatabase.getInstance()
        }
    }
}
