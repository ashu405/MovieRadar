package com.movieradar.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieradar.app.data.HomeData
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.archive.ArchiveRepository
import com.movieradar.app.domain.ArchiveItem
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(val region: String, val data: Loadable<HomeData>, val classics: List<ArchiveItem> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: TitleRepository,
    settings: SettingsRepository,
    private val archive: ArchiveRepository,
) : ViewModel() {
    private val refresh = MutableStateFlow(0)

    /** Loaded independently: an Internet Archive outage must not break the Home screen. */
    private val classics = flow {
        emit(emptyList())
        emit(runCatching { archive.classics() }.getOrDefault(emptyList()))
    }

    private val base = combine(settings.region, settings.showAdult, refresh) { r, a, _ -> r to a }
        .flatMapLatest { (region, adult) ->
            flow {
                emit(HomeState(region, Loadable.Loading))
                val result = try {
                    Loadable.Ready(repo.homeShelves(region, includeAdult = adult))
                } catch (e: CancellationException) { throw e } catch (e: Exception) {
                    Loadable.Failed(userMessage(e))
                }
                emit(HomeState(region, result))
            }
        }

    val state: StateFlow<HomeState> = combine(base, classics) { b, c -> b.copy(classics = c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState(SettingsRepository.DEFAULT_REGION, Loadable.Loading))

    fun retry() { refresh.value++ }
}
