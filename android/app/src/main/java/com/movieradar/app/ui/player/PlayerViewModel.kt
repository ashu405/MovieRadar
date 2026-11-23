package com.movieradar.app.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.movieradar.app.data.archive.ArchiveRepository
import com.movieradar.app.data.local.LibraryRepository
import com.movieradar.app.domain.PlayableItem
import com.movieradar.app.ui.PlayerRoute
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerData(val playable: PlayableItem, val resumeAtMs: Long)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val archive: ArchiveRepository,
    private val library: LibraryRepository,
) : ViewModel() {
    val route = savedState.toRoute<PlayerRoute>()
    private val _state = MutableStateFlow<Loadable<PlayerData>>(Loadable.Loading)
    val state: StateFlow<Loadable<PlayerData>> = _state.asStateFlow()

    init { load() }

    fun retry() = load()

    private fun load() {
        _state.value = Loadable.Loading
        viewModelScope.launch {
            _state.value = try {
                val p = archive.playable(route.identifier)
                if (p.sources.isEmpty()) Loadable.Failed("No playable MP4 file in this item.")
                else Loadable.Ready(PlayerData(p, library.resumePositionMs(route.identifier)))
            } catch (e: CancellationException) { throw e } catch (e: IllegalArgumentException) {
                Loadable.Failed(e.message ?: "Can't play this item.")
            } catch (e: Exception) {
                Loadable.Failed(userMessage(e))
            }
        }
    }

    /** Called on a timer and when leaving the player. */
    fun saveProgress(positionMs: Long, durationMs: Long) {
        val title = (state.value as? Loadable.Ready)?.value?.playable?.item?.title ?: route.title
        library.recordProgress(route.identifier, title, positionMs, durationMs)
    }
}
