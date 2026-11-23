package com.movieradar.app.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.movieradar.app.domain.PlayableItem
import com.movieradar.app.domain.VideoSource
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.theme.RadarColors
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(onBack: () -> Unit, vm: PlayerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    ImmersiveLandscape()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            Loadable.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = RadarColors.Accent)
            is Loadable.Failed -> ErrorState(s.message, vm::retry, Modifier.align(Alignment.Center))
            is Loadable.Ready -> VideoPlayer(s.value.playable, s.value.resumeAtMs, vm::saveProgress, onBack)
        }
        if (state !is Loadable.Ready) BackButton(onBack, Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
        modifier = modifier.padding(12.dp),
    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    playable: PlayableItem,
    resumeAtMs: Long,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var sourceUrl by rememberSaveable { mutableStateOf(playable.defaultSource?.url) }
    var controlsVisible by remember { mutableStateOf(true) }
    var qualityMenu by remember { mutableStateOf(false) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    // (Re)load when the chosen quality changes, keeping the current position.
    LaunchedEffect(sourceUrl) {
        val url = sourceUrl ?: return@LaunchedEffect
        val startAt = if (player.currentMediaItem == null) resumeAtMs else player.currentPosition
        player.setMediaItem(buildMediaItem(url, playable), startAt)
        player.prepare()
    }
    // Save progress every 5 seconds while the screen is open.
    LaunchedEffect(player) {
        while (true) {
            delay(5_000)
            if (player.isPlaying) onProgress(player.currentPosition, player.duration.coerceAtLeast(0))
        }
    }
    DisposableEffect(player) {
        onDispose {
            if (player.currentMediaItem != null) onProgress(player.currentPosition, player.duration.coerceAtLeast(0))
            player.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                    keepScreenOn = true
                    setShowSubtitleButton(playable.subtitles.isNotEmpty())
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> controlsVisible = v == android.view.View.VISIBLE })
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (controlsVisible) {
            Row(Modifier.align(Alignment.TopStart).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Text(playable.item.title, color = Color.White, maxLines = 1)
            }
            if (playable.sources.size > 1) {
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                    IconButton(
                        onClick = { qualityMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    ) { Icon(Icons.Outlined.HighQuality, contentDescription = "Quality") }
                    DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                        playable.sources.forEach { src ->
                            DropdownMenuItem(
                                text = { Text(qualityLabel(src, src.url == sourceUrl)) },
                                onClick = { qualityMenu = false; sourceUrl = src.url },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun qualityLabel(s: VideoSource, selected: Boolean): String {
    val size = when (val b = s.sizeBytes) {
        null -> ""
        in 1_000_000_000L..Long.MAX_VALUE -> " · %.1f GB".format(b / 1e9)
        else -> " · ${b / 1_000_000} MB"
    }
    return (if (selected) "✓ " else "    ") + s.label + size
}

private fun buildMediaItem(url: String, playable: PlayableItem): MediaItem {
    val subs = playable.subtitles.mapIndexed { i, s ->
        MediaItem.SubtitleConfiguration.Builder(s.url.toUri())
            .setMimeType(if (s.isVtt) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP)
            .setLabel(s.label)
            .setSelectionFlags(if (i == 0) C.SELECTION_FLAG_DEFAULT else 0)
            .build()
    }
    return MediaItem.Builder().setUri(url).setSubtitleConfigurations(subs).build()
}

/** Landscape + hidden system bars while the player is open; restored on exit. */
@Composable
private fun ImmersiveLandscape() {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = previous
        }
    }
}
