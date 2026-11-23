package com.movieradar.app.ui.surprise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.movieradar.app.data.TitleRepository
import com.movieradar.app.data.settings.SettingsRepository
import com.movieradar.app.domain.Length
import com.movieradar.app.domain.Mood
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.SurpriseRequest
import com.movieradar.app.domain.SurpriseRules
import com.movieradar.app.domain.TitleKind
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.components.userMessage
import com.movieradar.app.ui.theme.RadarColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SurpriseState(
    val mood: Mood = Mood.FEEL_GOOD,
    val kind: TitleKind = TitleKind.EITHER,
    val length: Length = Length.ANY,
    /** null = "Any of mine". */
    val service: OttService? = null,
    val loading: Boolean = false,
    val picks: List<TitleSummary> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class SurpriseViewModel @Inject constructor(
    private val repo: TitleRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SurpriseState())
    val state: StateFlow<SurpriseState> = _state.asStateFlow()
    val adult: StateFlow<Boolean> = settings.showAdult.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var poolKey: SurpriseRequest? = null
    private var pool: List<TitleSummary> = emptyList()
    private val shown = mutableSetOf<String>()
    private var job: Job? = null

    fun setMood(m: Mood) = change { it.copy(mood = m) }
    fun setKind(k: TitleKind) = change { it.copy(kind = k) }
    fun setLength(l: Length) = change { it.copy(length = l) }
    fun setService(s: OttService?) = change { it.copy(service = s) }

    private fun change(f: (SurpriseState) -> SurpriseState) {
        _state.update(f)
        roll()
    }

    /** 3 picks for the current choices; Shuffle calls this again (no repeats this session). */
    fun roll() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                val s = _state.value
                val req = SurpriseRequest(
                    mood = s.mood, kind = s.kind, length = s.length,
                    services = SurpriseRules.resolveServices(s.service, settings.myServices.first()),
                    region = settings.region.first(),
                )
                if (req != poolKey) {
                    pool = repo.surprisePool(req)
                    poolKey = req
                    shown.clear()
                }
                var picks = SurpriseRules.pick(pool, shown)
                if (picks.isEmpty() && pool.isNotEmpty()) { shown.clear(); picks = SurpriseRules.pick(pool, shown) }
                shown += picks.map { it.key }
                _state.update {
                    it.copy(
                        loading = false, picks = picks,
                        message = if (picks.isEmpty()) "Nothing found for this mix. Try \"Any length\" or another service." else null,
                    )
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                _state.update { it.copy(loading = false, message = userMessage(e)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurpriseSheet(onDismiss: () -> Unit, onOpen: (TitleSummary) -> Unit, vm: SurpriseViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val adult by vm.adult.collectAsStateWithLifecycle()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.runtime.LaunchedEffect(Unit) { if (s.picks.isEmpty()) vm.roll() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = RadarColors.Surface) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("🎲 Surprise me", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
            Label("Mood")
            ChipRow(Mood.available(adult), s.mood, { it.title }, vm::setMood)
            Label("Type")
            ChipRow(TitleKind.entries, s.kind, { it.label }, vm::setKind)
            if (s.kind != TitleKind.SERIES) {
                Label("Length")
                ChipRow(Length.entries, s.length, { it.label }, vm::setLength)
            }
            Label("Service")
            ChipRow(listOf<OttService?>(null) + OttService.entries, s.service, { it?.label ?: "Any of mine" }, vm::setService)

            Box(Modifier.fillMaxWidth().height(250.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
                when {
                    s.loading -> CircularProgressIndicator(color = RadarColors.Accent)
                    s.message != null -> Text(s.message!!, color = RadarColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                    else -> Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.picks.forEach { t ->
                            Column(Modifier.weight(1f)) {
                                PosterCard(t, onClick = { onOpen(t) }, width = 110.dp)
                                Text(t.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp))
                                if (t.rating > 0) Text("★ %.1f".format(t.rating), style = MaterialTheme.typography.labelMedium, color = RadarColors.Gold)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = vm::roll,
                enabled = !s.loading,
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.Accent, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.Shuffle, null)
                Spacer(Modifier.padding(4.dp))
                Text("Shuffle")
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = RadarColors.TextSecondary, modifier = Modifier.padding(start = 16.dp, top = 6.dp))
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { o ->
            FilterChip(
                selected = o == selected, onClick = { onPick(o) }, label = { Text(label(o)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarColors.TextPrimary, selectedLabelColor = Color.Black),
            )
        }
    }
}
