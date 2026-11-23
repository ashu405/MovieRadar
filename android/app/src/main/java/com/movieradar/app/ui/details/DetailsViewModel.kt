package com.movieradar.app.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.movieradar.app.alerts.AlertRepository
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.archive.ArchiveRepository
import com.movieradar.app.data.local.LibraryRepository
import com.movieradar.app.domain.ArchiveItem
import com.movieradar.app.domain.Franchise
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.key
import kotlinx.coroutines.sync.withPermit
import com.movieradar.app.domain.TitleSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleDetails
import com.movieradar.app.ui.DetailsRoute
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DetailsState(
    val region: String,
    val details: Loadable<TitleDetails>,
    val saved: Boolean = false,
    val watched: Boolean = false,
    /** Legal Internet Archive copy (movies only), when one confidently matches. */
    val freeCopy: ArchiveItem? = null,
    /** Exact per-service title URLs (provider id → url); empty until loaded or if none found. */
    val links: Map<Int, String> = emptyMap(),
    val alertOn: Boolean = false,
    /** Franchise strip: the parts (release order) and each part's streaming services. */
    val franchise: Franchise? = null,
    val franchiseStreaming: Map<String, List<Provider>> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: TitleRepository,
    private val archive: ArchiveRepository,
    private val library: LibraryRepository,
    private val alerts: AlertRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val route = savedState.toRoute<DetailsRoute>()
    private val type = MediaType.fromPath(route.type) ?: MediaType.MOVIE
    private val refresh = MutableStateFlow(0)

    private val freeCopy = MutableStateFlow<ArchiveItem?>(null)
    private val links = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val franchise = MutableStateFlow<Pair<Franchise?, Map<String, List<Provider>>>>(null to emptyMap())
    private val saved = library.isSaved(TitleSummary(route.id, type, "", null, null, null, "", 0.0, emptyList()))

    private val base: Flow<DetailsState> = combine(settings.region, refresh) { r, _ -> r }
        .flatMapLatest { region ->
            flow {
                emit(DetailsState(region, Loadable.Loading))
                val r = try {
                    Loadable.Ready(repo.details(type, route.id, region).also { findFreeCopy(it); loadLinks(region); loadFranchise(it, region) })
                } catch (e: CancellationException) { throw e } catch (e: Exception) {
                    Loadable.Failed(userMessage(e))
                }
                emit(DetailsState(region, r))
            }
        }

    private val alertOn = alerts.hasAlert(TitleSummary(route.id, type, "", null, null, null, "", 0.0, emptyList()))
    private val watched = library.isWatched(TitleSummary(route.id, type, "", null, null, null, "", 0.0, emptyList()))

    private val extras = combine(links, alertOn, franchise) { l, a, fr -> Triple(l, a, fr) }
    private val marks = combine(saved, watched) { s, w -> s to w }

    val state: StateFlow<DetailsState> = combine(base, marks, freeCopy, extras) { b, (s, w), f, (l, a, fr) ->
        b.copy(saved = s, watched = w, freeCopy = f, links = l, alertOn = a, franchise = fr.first, franchiseStreaming = fr.second)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsState(SettingsRepository.DEFAULT_REGION, Loadable.Loading))

    fun retry() { refresh.value++ }

    fun toggleSaved() {
        val d = (state.value.details as? Loadable.Ready)?.value ?: return
        val now = state.value.saved
        viewModelScope.launch { library.setSaved(d.summary, !now) }
    }

    /** ✅ Marking watched also takes it off the Watchlist. */
    fun toggleWatched() {
        val d = (state.value.details as? Loadable.Ready)?.value ?: return
        val now = state.value.watched
        viewModelScope.launch { library.setWatched(d.summary, !now) }
    }

    fun setAlert(on: Boolean) {
        val d = (state.value.details as? Loadable.Ready)?.value ?: return
        viewModelScope.launch {
            if (on) alerts.enable(d.summary, settings.region.first()) else alerts.disable(d.summary)
        }
    }

    private var franchiseJob: Job? = null

    /** Loads the franchise strip in the background; each part's streaming logos fill in as they arrive. */
    private fun loadFranchise(d: TitleDetails, region: String) {
        val ref = d.franchise ?: return
        if (franchiseJob?.isActive == true) return
        franchiseJob = viewModelScope.launch {
            val f = runCatching { repo.franchise(ref.id) }.getOrNull() ?: return@launch
            if (f.parts.size < 2) return@launch
            franchise.value = f to emptyMap()
            val gate = kotlinx.coroutines.sync.Semaphore(4)
            f.ordered.forEach { part ->
                launch {
                    gate.withPermit {
                        runCatching { repo.whereToWatch(part.type, part.id, region).headline }.onSuccess { p ->
                            franchise.value = franchise.value.let { (ff, m) -> ff to (m + (part.key to p)) }
                        }
                    }
                }
            }
        }
    }

    private var linksJob: Job? = null

    /** Exact "open in app" links come from the proxy separately; failure just means search fallback. */
    private fun loadLinks(region: String) {
        linksJob?.cancel()
        linksJob = viewModelScope.launch {
            links.value = emptyMap()
            links.value = runCatching { repo.titleLinks(type, route.id, region) }.getOrDefault(emptyMap())
        }
    }

    private var freeLookup: Job? = null

    /** Internet Archive lookup runs separately, so Details shows immediately and the button appears when found. */
    private fun findFreeCopy(d: TitleDetails) {
        if (d.summary.type != MediaType.MOVIE || freeLookup?.isActive == true || freeCopy.value != null) return
        freeLookup = viewModelScope.launch {
            freeCopy.value = runCatching { archive.findFor(d.summary.title, d.summary.year?.toIntOrNull()) }.getOrNull()
        }
    }
}
