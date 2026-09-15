package com.example.ui.components

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun FrndomVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    isLooping: Boolean = true,
    onDoubleTap: (() -> Unit)? = null,
    onSingleTap: (() -> Unit)? = null
) {
    if (videoUrl.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {}
        return
    }

    val context = LocalContext.current.applicationContext
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isMuted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var hasPlaybackError by remember { mutableStateOf(false) }

    // Safe ExoPlayer instantiation with decoder fallback to prevent component resource errors (code 6)
    val exoPlayer = remember(videoUrl) {
        try {
            val renderersFactory = DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                setEnableDecoderFallback(true)
            }
            ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .build()
                .apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    setMediaItem(mediaItem)
                    repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    playWhenReady = autoPlay
                    if (autoPlay) {
                        prepare()
                    }
                }
        } catch (e: Throwable) {
            Log.e("FrndomVideoPlayer", "Error creating ExoPlayer: ${e.message}")
            null
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasPlaybackError = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("FrndomVideoPlayer", "Playback error handled: ${error.errorCodeName} (code=${error.errorCode})")
                hasPlaybackError = true
                isBuffering = false
                isPlaying = false
            }
        }
        player.addListener(listener)

        onDispose {
            try {
                player.removeListener(listener)
                player.stop()
                player.release()
            } catch (e: Throwable) {
                Log.e("FrndomVideoPlayer", "Error releasing player: ${e.message}")
            }
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(autoPlay) {
        exoPlayer?.let { player ->
            if (autoPlay) {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.playWhenReady = true
            } else {
                player.playWhenReady = false
                player.pause()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (exoPlayer != null && !hasPlaybackError) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Transparent tap gesture detector over video player
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(videoUrl) {
                    detectTapGestures(
                        onDoubleTap = {
                            onDoubleTap?.invoke()
                        },
                        onTap = {
                            if (onSingleTap != null) {
                                onSingleTap()
                            } else {
                                exoPlayer?.let { player ->
                                    if (player.isPlaying) {
                                        player.pause()
                                    } else {
                                        if (player.playbackState == Player.STATE_IDLE) {
                                            player.prepare()
                                        }
                                        player.play()
                                    }
                                }
                            }
                        }
                    )
                }
        )

        // Loading spinner when video is buffering while playing
        if (isBuffering && isPlaying) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
        }

        // Center Play icon overlay if paused / not playing
        if (!isPlaying || hasPlaybackError) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .size(64.dp)
                    .clickable {
                        exoPlayer?.let { player ->
                            if (player.playbackState == Player.STATE_IDLE) {
                                player.prepare()
                            }
                            player.play()
                            isPlaying = true
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        // Mute / Unmute Button in bottom corner
        if (exoPlayer != null && !hasPlaybackError) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(34.dp)
                    .clickable { isMuted = !isMuted }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
