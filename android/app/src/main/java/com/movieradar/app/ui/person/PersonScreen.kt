package com.movieradar.app.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.domain.PersonDetails
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.PersonRoute
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.MetaLine
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.components.ProfilePhoto
import com.movieradar.app.ui.components.userMessage
import com.movieradar.app.ui.theme.RadarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: TitleRepository,
) : ViewModel() {
    private val id = savedState.toRoute<PersonRoute>().id
    private val _state = MutableStateFlow<Loadable<PersonDetails>>(Loadable.Loading)
    val state: StateFlow<Loadable<PersonDetails>> = _state.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _state.value = Loadable.Loading
        viewModelScope.launch {
            _state.value = try {
                Loadable.Ready(repo.person(id))
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                Loadable.Failed(userMessage(e))
            }
        }
    }
}

private enum class Tab { MOVIES, SERIES }

@Composable
fun PersonScreen(onBack: () -> Unit, onOpen: (TitleSummary) -> Unit, vm: PersonViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        when (val d = s) {
            Loadable.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = RadarColors.Accent)
            is Loadable.Failed -> ErrorState(d.message, vm::retry, Modifier.align(Alignment.Center))
            is Loadable.Ready -> PersonContent(d.value, onOpen)
        }
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            modifier = Modifier.padding(8.dp),
        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
    }
}

@Composable
private fun PersonContent(p: PersonDetails, onOpen: (TitleSummary) -> Unit) {
    // Open on whichever list has titles (some actors only do TV).
    var tab by rememberSaveable { mutableStateOf(if (p.movies.isEmpty() && p.series.isNotEmpty()) Tab.SERIES else Tab.MOVIES) }
    var bioExpanded by rememberSaveable { mutableStateOf(false) }
    val items = if (tab == Tab.MOVIES) p.movies else p.series

    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(p.profilePath, size = 112.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    p.department?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = RadarColors.Accent) }
                    MetaLine(listOfNotNull(bornLine(p.birthday), p.placeOfBirth))
                }
            }
        }
        p.biography?.let { bio ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RadarColors.TextSecondary,
                    maxLines = if (bioExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { bioExpanded = !bioExpanded },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip("Movies (${p.movies.size})", tab == Tab.MOVIES) { tab = Tab.MOVIES }
                TabChip("Series (${p.series.size})", tab == Tab.SERIES) { tab = Tab.SERIES }
            }
        }
        if (items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("No acting credits here.", color = RadarColors.TextSecondary)
            }
        }
        items(items, key = { it.key }) { t ->
            Column {
                PosterCard(t, onClick = { onOpen(t) }, width = 108.dp)
                Text(
                    t.year ?: "Upcoming",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (t.year == null) RadarColors.Accent else RadarColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarColors.TextPrimary, selectedLabelColor = Color.Black),
    )
}

private fun bornLine(birthday: String?): String? = try {
    birthday?.let { "Born " + LocalDate.parse(it).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) }
} catch (_: Exception) { null }
