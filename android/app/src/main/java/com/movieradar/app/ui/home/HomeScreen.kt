package com.movieradar.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.movieradar.app.BuildConfig
import com.movieradar.app.domain.ArchiveItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.movieradar.app.ui.surprise.SurpriseSheet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.ShelfRow
import com.movieradar.app.ui.components.ShelfSkeleton
import com.movieradar.app.ui.components.TmdbImage
import com.movieradar.app.ui.theme.RadarColors

@Composable
fun HomeScreen(
    onOpen: (TitleSummary) -> Unit,
    onSettings: () -> Unit,
    onPlay: (identifier: String, title: String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSurprise by rememberSaveable { mutableStateOf(false) }
    if (showSurprise) SurpriseSheet(onDismiss = { showSurprise = false }, onOpen = { showSurprise = false; onOpen(it) })
    Box(Modifier.fillMaxSize().background(RadarColors.Black)) {
        when (val d = state.data) {
            Loadable.Loading -> Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 56.dp)) {
                repeat(3) { ShelfSkeleton() }
            }
            is Loadable.Failed -> ErrorState(d.message, vm::retry, Modifier.align(Alignment.Center))
            is Loadable.Ready -> LazyColumn(Modifier.fillMaxSize()) {
                d.value.hero?.let { hero -> item(key = "hero") { Hero(hero, onOpen) } }
                items(d.value.shelves, key = { it.title }) { ShelfRow(it, onOpen) }
                if (state.classics.isNotEmpty()) item(key = "classics") { ClassicsRow(state.classics, onPlay) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        TopBar(region = state.region, onSettings = onSettings, onSurprise = { showSurprise = true })
    }
}

@Composable
private fun TopBar(region: String, onSettings: () -> Unit, onSurprise: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Per app: MoviesRadar = "Movies" + red "Radar"; MovieRadar = "Movie" + orange "Radar".
        Text(BuildConfig.LOGO_TEXT, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text("Radar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(BuildConfig.LOGO_ACCENT))
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSurprise) { Icon(Icons.Outlined.Casino, contentDescription = "Surprise me") }
        Text(region, style = MaterialTheme.typography.labelLarge, color = RadarColors.TextSecondary)
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
    }
}

@Composable
private fun Hero(item: TitleSummary, onOpen: (TitleSummary) -> Unit) {
    Box(Modifier.fillMaxWidth().height(520.dp)) {
        AsyncImage(
            model = TmdbImage.backdrop(item.backdropPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.3f), 0.45f to Color.Transparent, 1f to Color.Black),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                if (item.type.path == "tv") "SERIES" else "FILM",
                style = MaterialTheme.typography.labelSmall,
                color = RadarColors.Accent,
            )
            Text(item.title, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                item.overview, style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Button(
                onClick = { onOpen(item) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            ) {
                Icon(Icons.Outlined.Info, null)
                Spacer(Modifier.padding(4.dp))
                Text("Where to watch")
            }
        }
    }
}

@Composable
private fun ClassicsRow(items: List<ArchiveItem>, onPlay: (identifier: String, title: String) -> Unit) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Free classics", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text("▶ PLAY IN APP", style = MaterialTheme.typography.labelSmall, color = RadarColors.Free)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.identifier }) { item ->
                Column(Modifier.width(180.dp).clickable { onPlay(item.identifier, item.title) }) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp))
                            .background(RadarColors.SurfaceHigh),
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        item.title, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp),
                    )
                    item.year?.let { Text(it.toString(), style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary) }
                }
            }
        }
        Text(
            "Public-domain films from the Internet Archive",
            style = MaterialTheme.typography.labelSmall,
            color = RadarColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
