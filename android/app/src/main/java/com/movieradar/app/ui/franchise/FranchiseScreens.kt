package com.movieradar.app.ui.franchise

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.Franchise
import com.movieradar.app.domain.FranchiseRef
import com.movieradar.app.domain.Franchises
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.FranchiseRoute
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.MetaLine
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.components.ProviderLogo
import com.movieradar.app.ui.components.userMessage
import com.movieradar.app.ui.theme.RadarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

// ---------- One franchise: parts in release order with where they stream ----------

data class FranchiseState(
    val data: Loadable<Franchise> = Loadable.Loading,
    /** Subscription/free services per part (title key → providers), filled in as they load. */
    val streaming: Map<String, List<Provider>> = emptyMap(),
)

@HiltViewModel
class FranchiseViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: TitleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val id = savedState.toRoute<FranchiseRoute>().id
    private val _state = MutableStateFlow(FranchiseState())
    val state: StateFlow<FranchiseState> = _state.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = FranchiseState()
            val f = try { repo.franchise(id) } catch (e: CancellationException) { throw e } catch (e: Exception) {
                _state.value = FranchiseState(Loadable.Failed(userMessage(e))); return@launch
            }
            _state.update { it.copy(data = Loadable.Ready(f)) }
            val region = settings.region.first()
            val gate = Semaphore(4)
            f.ordered.forEach { part ->
                launch {
                    gate.withPermit {
                        runCatching { repo.whereToWatch(part.type, part.id, region).headline }.onSuccess { p ->
                            _state.update { it.copy(streaming = it.streaming + (part.key to p)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FranchiseScreen(onBack: () -> Unit, onOpen: (TitleSummary) -> Unit, vm: FranchiseViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        when (val d = s.data) {
            Loadable.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = RadarColors.Accent)
            is Loadable.Failed -> ErrorState(d.message, vm::retry, Modifier.align(Alignment.Center))
            is Loadable.Ready -> {
                val f = d.value
                val parts = f.ordered
                LazyColumn(contentPadding = PaddingValues(top = 56.dp, bottom = 24.dp)) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text(f.name, style = MaterialTheme.typography.headlineSmall)
                            Text("${parts.size} films · in release order", style = MaterialTheme.typography.labelLarge, color = RadarColors.Accent)
                            if (f.overview.isNotBlank()) {
                                Text(f.overview, style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
                                    maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                    itemsIndexed(parts, key = { _, t -> t.key }) { i, t ->
                        PartRow(i + 1, t, s.streaming[t.key], onClick = { onOpen(t) })
                    }
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
    }
}

@Composable
private fun PartRow(n: Int, t: TitleSummary, streaming: List<Provider>?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$n", style = MaterialTheme.typography.headlineSmall, color = RadarColors.TextSecondary)
        PosterCard(t, onClick = onClick, width = 64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            MetaLine(listOfNotNull(t.year ?: "Upcoming", if (t.rating > 0) "★ %.1f".format(t.rating) else null))
            when {
                streaming == null -> {}
                streaming.isEmpty() -> Text("Not streaming in your region", style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary)
                else -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { streaming.take(4).forEach { ProviderLogo(it, size = 24.dp) } }
            }
        }
    }
}

// ---------- Browse → 🎬 Franchises: popular grid + search ----------

data class FranchiseListState(val query: String = "", val results: List<FranchiseRef>? = null, val loading: Boolean = false)

@OptIn(FlowPreview::class)
@HiltViewModel
class FranchiseListViewModel @Inject constructor(private val repo: TitleRepository) : ViewModel() {
    private val _state = MutableStateFlow(FranchiseListState())
    val state: StateFlow<FranchiseListState> = _state.asStateFlow()
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            query.debounce(350).distinctUntilChanged().collect { q ->
                if (q.trim().length < 2) { _state.update { it.copy(results = null, loading = false) }; return@collect }
                _state.update { it.copy(loading = true) }
                val r = runCatching { repo.searchFranchises(q.trim()) }.getOrDefault(emptyList())
                _state.update { it.copy(results = r, loading = false) }
            }
        }
    }

    fun onQuery(q: String) { _state.update { it.copy(query = q.take(60)) }; query.value = q.take(60) }
}

@Composable
fun FranchiseListScreen(onBack: () -> Unit, onOpenFranchise: (FranchiseRef) -> Unit, vm: FranchiseListViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("🎬 Franchises", style = MaterialTheme.typography.headlineSmall)
        }
        TextField(
            value = s.query, onValueChange = vm::onQuery, singleLine = true,
            placeholder = { Text("Search a franchise, e.g. Housefull, Spider-Man") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = RadarColors.SurfaceHigh, unfocusedContainerColor = RadarColors.SurfaceHigh,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val list = s.results ?: Franchises.POPULAR
        if (s.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(16.dp), color = RadarColors.Accent)
        if (s.results != null && list.isEmpty() && !s.loading) {
            Text("No franchise found.", color = RadarColors.TextSecondary, modifier = Modifier.padding(16.dp))
        }
        if (s.results == null) Text("Popular", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(list, key = { it.id }) { f -> FranchiseTile(f) { onOpenFranchise(f) } }
        }
    }
}

@Composable
private fun FranchiseTile(f: FranchiseRef, onClick: () -> Unit) {
    val base = listOf(Color(0xFF7B1E3A), Color(0xFF1E4D7B), Color(0xFF2E6B3A), Color(0xFF6B4B1E), Color(0xFF4B1E6B))[Math.floorMod(f.id, 5)]
    Box(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.35f))))
            .clickable(onClick = onClick).padding(14.dp),
        contentAlignment = Alignment.BottomStart,
    ) { Text(f.name.removeSuffix(" Collection"), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
}
