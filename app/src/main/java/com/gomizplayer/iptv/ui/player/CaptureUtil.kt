package com.gomizplayer.iptv.ui.player

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Finds the first SurfaceView inside a view hierarchy (ExoPlayer's video surface). */
private fun findSurfaceView(root: View): SurfaceView? {
    if (root is SurfaceView) return root
    if (root is ViewGroup) {
        for (i in 0 until root.childCount) {
            findSurfaceView(root.getChildAt(i))?.let { return it }
        }
    }
    return null
}

/**
 * Captures the currently displayed video frame as a Bitmap using PixelCopy.
 * Returns null if the SurfaceView can't be copied (e.g. video is hardware-decoded and protected).
 */
suspend fun capturePlayerFrame(playerView: PlayerView): Bitmap? = suspendCancellableCoroutine { cont ->
    val surfaceView = findSurfaceView(playerView) ?: run {
        cont.resume(null)
        return@suspendCancellableCoroutine
    }
    val w = surfaceView.width.coerceAtLeast(1)
    val h = surfaceView.height.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    try {
        android.view.PixelCopy.request(
            surfaceView,
            bitmap,
            { result ->
                if (cont.isActive) {
                    cont.resume(if (result == android.view.PixelCopy.SUCCESS) bitmap else null)
                }
            },
            Handler(Looper.getMainLooper())
        )
    } catch (_: Exception) {
        if (cont.isActive) cont.resume(null)
    }
}
fun saveBitmapToPictures(context: android.content.Context, bitmap: android.graphics.Bitmap?, displayName: String): Boolean {
    if (bitmap == null) return false
    return runCatching {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "GomizPlayer_${System.currentTimeMillis()}.jpg")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        true
    }.getOrDefault(false)
}
