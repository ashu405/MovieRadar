package com.movieradar.app.ui.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movieradar.app.data.BrowseSort
import com.movieradar.app.data.Genre
import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.Category
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.components.ProviderLogo
import com.movieradar.app.ui.theme.RadarColors

@Composable
fun BrowseScreen(onOpen: (TitleSummary) -> Unit, onOpenFranchises: () -> Unit = {}, vm: BrowseViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    BackHandler(enabled = s.genre != null) { vm.closeGenre() }
    Column(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (s.genre != null) {
                IconButton(onClick = vm::closeGenre) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Genres") }
            } else {
                Box(Modifier.padding(start = 12.dp))
            }
            Text(s.genre?.name ?: "Browse", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Movies", s.type == MediaType.MOVIE) { vm.setType(MediaType.MOVIE) }
            Chip("Series", s.type == MediaType.TV) { vm.setType(MediaType.TV) }
        }
        if (s.genre == null) GenreGrid(s.genres, s.loading, s.error, vm::openAll, vm::openGenre, vm::openCategory, onOpenFranchises, vm::retry)
        else Results(s, onOpen, vm)
    }
}

@Composable
private fun GenreGrid(
    genres: List<Genre>,
    loading: Boolean,
    error: String?,
    onAll: () -> Unit,
    onGenre: (Genre) -> Unit,
    onCategory: (Category) -> Unit,
    onFranchises: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        error != null && genres.isEmpty() -> ErrorState(error, onRetry)
        loading && genres.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RadarColors.Accent)
        }
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { GenreTile("All genres", 0, onAll) }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("More categories") }
            item(key = "franchises") { GenreTile("🎬 Franchises", 97, onFranchises) }
            items(Category.entries, key = { "c-${it.name}" }) { c -> GenreTile(c.title, 100 + c.ordinal * 3) { onCategory(c) } }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionLabel("Genres") }
            items(genres, key = { it.id }) { g -> GenreTile(g.name, g.id) { onGenre(g) } }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

private val TileColors = listOf(
    Color(0xFF7B1E3A), Color(0xFF1E4D7B), Color(0xFF2E6B3A), Color(0xFF6B4B1E),
    Color(0xFF4B1E6B), Color(0xFF1E6B66), Color(0xFF6B1E1E), Color(0xFF3A3A6B),
)

@Composable
private fun GenreTile(name: String, id: Int, onClick: () -> Unit) {
    val base = TileColors[Math.floorMod(id, TileColors.size)]
    Box(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.35f))))
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Results(s: BrowseState, onOpen: (TitleSummary) -> Unit, vm: BrowseViewModel) {
    // Filters
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Chip("Any age", s.filter.age == null) { vm.setAge(null) } }
        items(AgeBucket.entries) { a ->
            Chip(if (a == AgeBucket.ALL) "All ages" else a.label, s.filter.age == a) { vm.setAge(a) }
        }
    }
    if (s.services.isNotEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item { Chip("Any service", s.filter.providerId == null) { vm.setService(null) } }
            items(s.services, key = { it.id }) { p ->
                FilterChip(
                    selected = s.filter.providerId == p.id,
                    onClick = { vm.setService(if (s.filter.providerId == p.id) null else p.id) },
                    label = { Text(p.name, maxLines = 1) },
                    leadingIcon = { ProviderLogo(p, size = 20.dp) },
                    colors = chipColors(),
                )
            }
        }
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BrowseSort.entries) { o -> Chip(o.label, s.filter.sort == o) { vm.setSort(o) } }
    }
    if (s.filter.age != null) {
        Text(
            "Age ratings: ${s.ratingSystem}",
            style = MaterialTheme.typography.labelSmall,
            color = RadarColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
    }
    if (s.loading && s.items.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth(), color = RadarColors.Accent)

    val grid = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= grid.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, s.items.size) { if (nearEnd && s.items.isNotEmpty()) vm.loadMore() }

    when {
        s.error != null && s.items.isEmpty() -> ErrorState(s.error, vm::retry)
        s.loading && s.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RadarColors.Accent)
        }
        s.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No titles match these filters.", color = RadarColors.TextSecondary)
        }
        else -> LazyVerticalGrid(
            state = grid,
            columns = GridCells.Adaptive(108.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(s.items, key = { it.key }) { t -> PosterCard(t, onClick = { onOpen(t) }, width = 108.dp) }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = RadarColors.TextPrimary,
    selectedLabelColor = Color.Black,
)

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) }, colors = chipColors())
}
