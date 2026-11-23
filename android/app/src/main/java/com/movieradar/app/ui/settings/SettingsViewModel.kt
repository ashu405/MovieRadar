package com.movieradar.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.Region
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: TitleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val region: StateFlow<String> = settings.region
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_REGION)

    val showAdult: StateFlow<Boolean> = settings.showAdult
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setShowAdult(on: Boolean) = viewModelScope.launch { settings.setShowAdult(on) }

    val alertsEnabled: StateFlow<Boolean> = settings.alertsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setAlertsEnabled(on: Boolean) = viewModelScope.launch { settings.setAlertsEnabled(on) }

    val myServices: StateFlow<Set<OttService>> = settings.myServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun toggleService(s: OttService) = viewModelScope.launch {
        val now = myServices.value
        settings.setMyServices(if (s in now) now - s else now + s)
    }

    private val _regions = MutableStateFlow<Loadable<List<Region>>>(Loadable.Loading)
    val regions: StateFlow<Loadable<List<Region>>> = _regions.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _regions.value = Loadable.Loading
        _regions.value = try { Loadable.Ready(repo.regions()) }
        catch (e: CancellationException) { throw e } catch (e: Exception) { Loadable.Failed(userMessage(e)) }
    }

    fun select(code: String) = viewModelScope.launch { settings.setRegion(code) }
}
