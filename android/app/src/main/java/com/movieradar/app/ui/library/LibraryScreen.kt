package com.movieradar.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.movieradar.app.alerts.AlertRepository
import com.movieradar.app.data.local.LibraryRepository
import com.movieradar.app.data.local.ProgressEntity
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.theme.RadarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val loaded: Boolean = false,
    val continueWatching: List<ProgressEntity> = emptyList(),
    val watchlist: List<TitleSummary> = emptyList(),
    val alerts: List<TitleSummary> = emptyList(),
    val watched: List<TitleSummary> = emptyList(),
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val alertRepo: AlertRepository,
) : ViewModel() {
    val state: StateFlow<LibraryState> =
        combine(library.continueWatching, library.watchlist, alertRepo.alerts, library.watched) { c, w, a, d ->
            LibraryState(true, c, w, a, d)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    fun stopAlert(t: TitleSummary) = viewModelScope.launch { alertRepo.disable(t) }

    /** Long-press on a Watchlist poster: move it to ✅ Watched. */
    fun markWatched(t: TitleSummary) = viewModelScope.launch { library.setWatched(t, true) }

    /** Undo of [markWatched]: back onto the Watchlist. */
    fun undoMarkWatched(t: TitleSummary) = viewModelScope.launch {
        library.setWatched(t, false)
        library.setSaved(t, true)
    }

    fun unmarkWatched(t: TitleSummary) = viewModelScope.launch { library.setWatched(t, false) }
}

@Composable
fun LibraryScreen(
    onOpen: (TitleSummary) -> Unit,
    onPlay: (identifier: String, title: String) -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun offerUndo(message: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) undo()
        }
    }
    Box(Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Library", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))
        }
        if (s.continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Section("Continue watching") }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.continueWatching, key = { it.identifier }) { p -> ContinueCard(p) { onPlay(p.identifier, p.title) } }
                }
            }
        }
        if (s.alerts.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Section("🔔 Alerts") }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "We'll notify you when these start streaming. Long-press to stop.",
                    style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(s.alerts, key = { "a-${it.key}" }) { t ->
                        PosterCard(t, onClick = { onOpen(t) }, width = 96.dp, onLongClick = { vm.stopAlert(t) })
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Section("Watchlist") }
        if (s.loaded && s.watchlist.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Nothing saved yet. Tap \"Watchlist\" on any title to keep it here.",
                    color = RadarColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (s.watchlist.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Hint("Watched it? Long-press to move it to ✅ Watched.") }
        }
        items(s.watchlist, key = { it.key }) { t ->
            PosterCard(t, onClick = { onOpen(t) }, width = 108.dp, onLongClick = {
                vm.markWatched(t)
                offerUndo("Moved to Watched") { vm.undoMarkWatched(t) }
            })
        }
        if (s.watched.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { Section("✅ Watched (${s.watched.size})") }
            item(span = { GridItemSpan(maxLineSpan) }) { Hint("Long-press to unmark.") }
            items(s.watched, key = { "w-${it.key}" }) { t ->
                PosterCard(t, onClick = { onOpen(t) }, width = 108.dp, onLongClick = {
                    vm.unmarkWatched(t)
                    offerUndo("Removed from Watched") { vm.markWatched(t) }
                })
            }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary)
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
private fun ContinueCard(p: ProgressEntity, onClick: () -> Unit) {
    Column(Modifier.width(200.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(RadarColors.SurfaceHigh)) {
            AsyncImage(
                model = "https://archive.org/services/img/${p.identifier}",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (p.durationMs > 0) {
            LinearProgressIndicator(
                progress = { (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) },
                color = RadarColors.Accent,
                trackColor = RadarColors.SurfaceHigh,
                modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 1.dp),
            )
        }
        Text(
            p.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp).align(Alignment.Start),
        )
    }
}
