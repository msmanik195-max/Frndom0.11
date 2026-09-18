package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID

object MediaUriHelper {
    private const val TAG = "MediaUriHelper"

    /**
     * Checks if a content URI is currently accessible for reading.
     * Prevents SecurityException from crashing ExoPlayer or MediaCodec.
     */
    fun isUriAccessible(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return false
        if (!uriString.startsWith("content://")) return true // file, http, https don't require ContentResolver

        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException: no permission for picker URI: $uriString")
            false
        } catch (e: Throwable) {
            Log.w(TAG, "Picker URI inaccessible: $uriString (${e.message})")
            false
        }
    }

    /**
     * Copies a transient content URI (e.g. from PhotoPicker) into the app's internal private storage
     * so that background threads, ExoPlayer, and Coil have permanent permission without SecurityException.
     */
    fun copyUriToAppStorage(
        context: Context,
        uri: Uri,
        folder: String = "media",
        defaultExtension: String = "mp4"
    ): String {
        val uriString = uri.toString()
        if (uriString.isBlank()) return ""
        if (uriString.startsWith("file://") || uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }

        return try {
            val extension = when {
                uriString.endsWith(".mp4", ignoreCase = true) || defaultExtension.equals("mp4", ignoreCase = true) -> "mp4"
                uriString.endsWith(".png", ignoreCase = true) || defaultExtension.equals("png", ignoreCase = true) -> "png"
                uriString.endsWith(".jpg", ignoreCase = true) || uriString.endsWith(".jpeg", ignoreCase = true) || defaultExtension.equals("jpg", ignoreCase = true) -> "jpg"
                else -> defaultExtension
            }
            val mediaDir = File(context.filesDir, folder).apply { mkdirs() }
            val targetFile = File(mediaDir, "${UUID.randomUUID()}.$extension")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Uri.fromFile(targetFile).toString()
            } else {
                ""
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException copying URI $uri: ${e.message}")
            ""
        } catch (e: Throwable) {
            Log.e(TAG, "Error copying URI $uri to local storage: ${e.message}")
            ""
        }
    }

    /**
     * Saves raw bytes to app-internal storage.
     */
    fun saveBytesToAppStorage(
        context: Context,
        bytes: ByteArray,
        folder: String = "media",
        filename: String
    ): File {
        val mediaDir = File(context.filesDir, folder).apply { mkdirs() }
        val file = File(mediaDir, filename)
        file.writeBytes(bytes)
        return file
    }

    /**
     * Sanitizes a media URL:
     * - If it's a content URI that is accessible, copies to local app storage.
     * - If it's an expired content URI (throws SecurityException), discards it to prevent crashes.
     * - Otherwise returns the URL as is.
     */
    fun sanitizeMediaUrl(
        context: Context,
        url: String,
        folder: String = "media",
        defaultExtension: String = "mp4"
    ): String {
        if (url.isBlank()) return ""
        if (!url.startsWith("content://")) return url

        return if (isUriAccessible(context, url)) {
            val localPath = copyUriToAppStorage(context, Uri.parse(url), folder, defaultExtension)
            if (localPath.isNotBlank()) localPath else ""
        } else {
            Log.w(TAG, "Discarded unpermitted content URI: $url")
            ""
        }
    }
}
