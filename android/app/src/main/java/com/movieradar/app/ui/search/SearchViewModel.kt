package com.movieradar.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.PersonSummary
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.WhereToWatch
import com.movieradar.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

enum class SearchFilter { ALL, MOVIES, SERIES, PEOPLE }

data class SearchState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val loading: Boolean = false,
    val error: String? = null,
    val results: List<TitleSummary> = emptyList(),
    val people: List<PersonSummary> = emptyList(),
    val providers: Map<String, WhereToWatch> = emptyMap(),
    val genres: Map<Int, String> = emptyMap(),
) {
    val visible: List<TitleSummary>
        get() = when (filter) {
            SearchFilter.ALL -> results
            SearchFilter.MOVIES -> results.filter { it.type == MediaType.MOVIE }
            SearchFilter.SERIES -> results.filter { it.type == MediaType.TV }
            SearchFilter.PEOPLE -> emptyList()
        }
}

fun TitleSummary.key() = "${type.path}-$id"

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: TitleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    private var providerJob: Job? = null

    init {
        viewModelScope.launch {
            _state.map { it.query.trim() }
                .distinctUntilChanged()
                .debounce(350)
                .collect { q -> runSearch(q) }
        }
        viewModelScope.launch {
            runCatching { repo.genreNames() }.onSuccess { g -> _state.update { it.copy(genres = g) } }
        }
    }

    fun onQueryChange(q: String) = _state.update { it.copy(query = q.take(100)) }
    fun onFilterChange(f: SearchFilter) = _state.update { it.copy(filter = f) }
    fun retry() = viewModelScope.launch { runSearch(_state.value.query.trim()) }

    private suspend fun runSearch(q: String) {
        providerJob?.cancel()
        if (q.length < 2) {
            _state.update { it.copy(loading = false, error = null, results = emptyList(), people = emptyList(), providers = emptyMap()) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val found = repo.search(q, includeAdult = settings.showAdult.first())
            _state.update { it.copy(loading = false, results = found.titles, people = found.people, providers = emptyMap()) }
            loadProviders(found.titles.take(15))
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = userMessage(e)) }
        }
    }

    /** Fetch "where to watch" logos for the top results, a few at a time (proxy caches them). */
    private fun loadProviders(items: List<TitleSummary>) {
        providerJob = viewModelScope.launch {
            val region = settings.region.first()
            val gate = Semaphore(4)
            items.forEach { item ->
                launch {
                    gate.withPermit {
                        runCatching { repo.whereToWatch(item.type, item.id, region) }.onSuccess { w ->
                            _state.update { it.copy(providers = it.providers + (item.key() to w)) }
                        }
                    }
                }
            }
        }
    }
}
