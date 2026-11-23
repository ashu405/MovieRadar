package com.movieradar.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieradar.app.data.BrowseFilter
import com.movieradar.app.data.BrowseSort
import com.movieradar.app.data.Genre
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.Category
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseState(
    val region: String = SettingsRepository.DEFAULT_REGION,
    val type: MediaType = MediaType.MOVIE,
    val genres: List<Genre> = emptyList(),
    val services: List<Provider> = emptyList(),
    val genre: Genre? = null, // null → show the genre grid
    val filter: BrowseFilter = BrowseFilter(),
    val items: List<TitleSummary> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    /** Ratings shown are India's in IN; every other region uses US labels (see AgeRating.discoverFilter). */
    val ratingSystem: String get() = if (region == "IN") "India (CBFC)" else "US"
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: TitleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BrowseState())
    val state: StateFlow<BrowseState> = _state.asStateFlow()
    private var loadJob: Job? = null

    init { loadMeta() }

    fun retry() = if (_state.value.genre == null) loadMeta() else reload()

    fun setType(type: MediaType) {
        if (type == _state.value.type) return
        _state.update { it.copy(type = type, genres = emptyList(), genre = null, items = emptyList()) }
        loadMeta()
    }

    fun openGenre(g: Genre) {
        _state.update { it.copy(genre = g, filter = it.filter.copy(genreId = g.id, category = null)) }
        reload()
    }

    fun openCategory(c: Category) {
        _state.update { it.copy(genre = Genre(-1 - c.ordinal, c.title), filter = it.filter.copy(genreId = null, category = c)) }
        reload()
    }

    /** "All genres" entry at the top of the grid. */
    fun openAll() {
        _state.update { it.copy(genre = Genre(0, "All genres"), filter = it.filter.copy(genreId = null, category = null)) }
        reload()
    }

    fun closeGenre() {
        loadJob?.cancel()
        _state.update { it.copy(genre = null, items = emptyList(), page = 0, error = null, loading = false) }
    }

    fun setAge(a: AgeBucket?) = updateFilter { it.copy(age = a) }
    fun setService(id: Int?) = updateFilter { it.copy(providerId = id) }
    fun setSort(s: BrowseSort) = updateFilter { it.copy(sort = s) }

    fun loadMore() {
        val s = _state.value
        if (s.loading || !s.hasMore) return
        load(s.page + 1)
    }

    private fun updateFilter(f: (BrowseFilter) -> BrowseFilter) {
        _state.update { it.copy(filter = f(it.filter)) }
        reload()
    }

    private fun reload() {
        _state.update { it.copy(items = emptyList(), page = 0, hasMore = false) }
        load(1)
    }

    private fun load(page: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val region = settings.region.first()
                val s = _state.value
                val adult = settings.showAdult.first()
                val res = repo.browse(s.type, region, s.filter.copy(includeAdult = adult), page)
                _state.update { cur ->
                    cur.copy(
                        region = region,
                        items = (if (page == 1) res.items else cur.items + res.items).distinctBy { it.key },
                        page = page, hasMore = res.hasMore, loading = false,
                    )
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = userMessage(e)) }
            }
        }
    }

    private fun loadMeta() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val region = settings.region.first()
                val genres = repo.genres(_state.value.type)
                val services = runCatching { repo.topServices(region, limit = 6) }.getOrDefault(emptyList())
                _state.update { it.copy(region = region, genres = genres, services = services, loading = false) }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = userMessage(e)) }
            }
        }
    }
}
