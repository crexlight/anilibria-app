package ru.radiationx.anilibria.ui.activities.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.anilibria.model.loading.DataLoadingState
import ru.radiationx.data.datasource.holders.EpisodesCheckerHolder
import ru.radiationx.data.entity.domain.release.Episode
import ru.radiationx.data.entity.domain.release.PlayerSkips
import ru.radiationx.data.entity.domain.release.Release
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.data.entity.domain.types.ReleaseId
import ru.radiationx.data.interactors.ReleaseInteractor
import ru.radiationx.shared.ktx.coRunCatching
import toothpick.InjectConstructor

data class PlayerExtra(
    val episodeId: EpisodeId,
)

@InjectConstructor
class PlayerViewModel(
    private val argExtra: PlayerExtra,
    private val releaseInteractor: ReleaseInteractor,
    private val episodesCheckerHolder: EpisodesCheckerHolder,
) : ViewModel() {

    private val _episodeId = MutableStateFlow(argExtra.episodeId)

    private val _dataState = MutableStateFlow(LoadingState<Release>())

    private val _loadingState = MutableStateFlow(LoadingState<PlayerDataState>())
    val loadingState = _loadingState.asStateFlow()

    init {

        combine(
            _episodeId,
            _dataState
        ) { episodeId, dataState ->
            _loadingState.value = LoadingState(
                loading = dataState.loading,
                data = dataState.data?.toDataState(episodeId),
                error = dataState.error
            )
        }.launchIn(viewModelScope)

        combine(
            _episodeId,
            _dataState.mapNotNull { it.data }
        ) { episodeId, release ->
            val access = episodesCheckerHolder.getEpisodes(episodeId.releaseId).find {
                it.id == episodeId
            }
            val episodeStates = release.episodes.map {
                it.toState()
            }
            val event = PlayerPlayEvent(episodeStates, episodeId, access?.seek ?: 0)

        }

    }

    fun playEpisode(episodeId: EpisodeId) {
        _episodeId.value = episodeId
        loadData(episodeId)
    }

    fun refresh() {
        loadData(_episodeId.value)
    }

    fun onSettingsClick() {

    }

    fun onPlaylistClick() {

    }

    fun onEpisodeChanged(episodeId: EpisodeId) {

    }

    private fun loadData(episodeId: EpisodeId) {
        viewModelScope.launch {
            _dataState.update { LoadingState(loading = true) }
            coRunCatching {
                requireNotNull(releaseInteractor.getFull(episodeId.releaseId)) {
                    "Loaded release is null"
                }
            }.onSuccess { release ->
                _dataState.update { it.copy(data = release) }
            }.onFailure { error ->
                _dataState.update { it.copy(error = error) }
            }
            _dataState.update { it.copy(loading = false) }
        }
    }

    fun Release.toDataState(episodeId: EpisodeId) = PlayerDataState(
        id = id,
        title = (title ?: titleEng).orEmpty(),
        episodeTitle = episodes.find { it.id == episodeId }?.title.orEmpty()
    )

    fun Episode.toState() = EpisodeState(
        id = id,
        title = title.orEmpty(),
        url = requireNotNull(urlSd ?: urlHd ?: urlFullHd),
        skips = skips
    )
}

data class LoadingState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: Throwable? = null,
)

data class PlayerScreenState(
    val loading: DataLoadingState<PlayerDataState>,
)

data class PlayerDataState(
    val id: ReleaseId,
    val title: String,
    val episodeTitle: String,
)

data class PlayerPlayEvent(
    val episodes: List<EpisodeState>,
    val episodeId: EpisodeId,
    val seek: Long,
)

data class EpisodeState(
    val id: EpisodeId,
    val title: String,
    val url: String,
    val skips: PlayerSkips?,
)