package com.movieradar.app.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieradar.app.alerts.AlertRepository
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.UpcomingTab
import com.movieradar.app.data.local.LibraryRepository
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.OttReleases
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.ReleaseDay
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.UpcomingRules
import com.movieradar.app.domain.key
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class UpcomingState(
    val month: YearMonth,
    val tab: UpcomingTab,
    val days: Loadable<List<ReleaseDay>>,
    val savedKeys: Set<String> = emptySet(),
    /** On OTT only: selected service (null = All), and service tags for the row badges. */
    val service: OttService? = null,
    val releases: OttReleases = OttReleases.EMPTY,
    val logos: Map<OttService, String?> = emptyMap(),
    val alertKeys: Set<String> = emptySet(),
)

/** Raw load for a month + tab; the service filter is applied on top without reloading. */
private data class Loaded(val month: YearMonth, val tab: UpcomingTab, val data: Loadable<OttReleases>)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repo: TitleRepository,
    private val settings: SettingsRepository,
    private val library: LibraryRepository,
    private val alerts: AlertRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val tab = MutableStateFlow(UpcomingTab.MOVIES)
    private val refresh = MutableStateFlow(0)
    private val savedKeys = library.watchlist.map { list -> list.map { it.key }.toSet() }

    private val service = MutableStateFlow<OttService?>(null)
    private val alertKeys = alerts.alerts.map { list -> list.map { it.key }.toSet() }
    private val logos = settings.region.map { repo.ottServiceLogos(it) }

    private val loaded = combine(month, tab, settings.region, refresh) { m, t, r, _ -> Triple(m, t, r) }
        .flatMapLatest { (m, t, region) ->
            flow {
                emit(Loaded(m, t, Loadable.Loading))
                val data = try {
                    Loadable.Ready(
                        if (t == UpcomingTab.OTT) repo.ottReleases(m, region)
                        else OttReleases(repo.releases(m, region, t), emptyMap()),
                    )
                } catch (e: CancellationException) { throw e } catch (e: Exception) {
                    Loadable.Failed(userMessage(e))
                }
                emit(Loaded(m, t, data))
            }
        }

    private val savedAndAlerts = combine(savedKeys, alertKeys) { s, a -> s to a }

    val state: StateFlow<UpcomingState> = combine(loaded, service, savedAndAlerts, logos.onStart { emit(emptyMap()) }) { l, svc, sa, lg ->
        val (k, ak) = sa
        val active = if (l.tab == UpcomingTab.OTT) svc else null
        val days = when (val d = l.data) {
            Loadable.Loading -> Loadable.Loading
            is Loadable.Failed -> d
            is Loadable.Ready -> Loadable.Ready(UpcomingRules.groupByDay(l.month, d.value.filter(active)))
        }
        UpcomingState(l.month, l.tab, days, k, active, (l.data as? Loadable.Ready)?.value ?: OttReleases.EMPTY, lg, ak)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingState(YearMonth.now(), UpcomingTab.MOVIES, Loadable.Loading))

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun thisMonth() { month.value = YearMonth.now() }
    fun setTab(t: UpcomingTab) { tab.value = t }
    fun setService(s: OttService?) { service.value = s }
    fun retry() { refresh.value++ }

    fun setAlert(t: TitleSummary, on: Boolean) {
        viewModelScope.launch { if (on) alerts.enable(t, settings.region.first()) else alerts.disable(t) }
    }

    fun toggleSaved(t: TitleSummary) {
        val saved = t.key in state.value.savedKeys
        viewModelScope.launch { library.setSaved(t, !saved) }
    }
}
