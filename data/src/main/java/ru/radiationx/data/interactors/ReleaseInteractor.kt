package ru.radiationx.data.interactors

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import ru.radiationx.data.datasource.holders.EpisodesCheckerHolder
import ru.radiationx.data.datasource.holders.PreferencesHolder
import ru.radiationx.data.entity.domain.release.EpisodeAccess
import ru.radiationx.data.entity.domain.release.RandomRelease
import ru.radiationx.data.entity.domain.release.Release
import ru.radiationx.data.entity.domain.types.EpisodeId
import ru.radiationx.data.entity.domain.types.ReleaseCode
import ru.radiationx.data.entity.domain.types.ReleaseId
import ru.radiationx.data.repository.ReleaseRepository
import javax.inject.Inject

/**
 * Created by radiationx on 17.02.18.
 */
class ReleaseInteractor @Inject constructor(
    private val releaseRepository: ReleaseRepository,
    private val episodesCheckerStorage: EpisodesCheckerHolder,
    private val preferencesHolder: PreferencesHolder,
) {

    private val releaseItems = MutableStateFlow<List<Release>>(emptyList())
    private val releases = MutableStateFlow<List<Release>>(emptyList())

    private val sharedRequests = SharedRequests<RequestKey, Release>()

    suspend fun getRandomRelease(): RandomRelease = releaseRepository.getRandomRelease()

    private suspend fun loadRelease(releaseId: ReleaseId): Release {
        return releaseRepository.getRelease(releaseId).also(::updateFullCache)
    }

    private suspend fun loadRelease(releaseCode: ReleaseCode): Release {
        return releaseRepository.getRelease(releaseCode).also(::updateFullCache)
    }

    suspend fun loadRelease(
        releaseId: ReleaseId? = null,
        releaseCode: ReleaseCode? = null,
    ): Release {
        val key = RequestKey(releaseId, releaseCode)
        return sharedRequests.request(key) {
            when {
                releaseId != null -> loadRelease(releaseId)
                releaseCode != null -> loadRelease(releaseCode)
                else -> throw Exception("Unknown id=null or code=null")
            }
        }
    }

    fun getItem(releaseId: ReleaseId? = null, releaseCode: ReleaseCode? = null): Release? {
        return releaseItems.value.findRelease(releaseId, releaseCode)
    }

    suspend fun getFull(releaseId: ReleaseId? = null, releaseCode: ReleaseCode? = null): Release? {
        return observeFull(releaseId, releaseCode).firstOrNull()
    }

    fun observeItem(releaseId: ReleaseId? = null, releaseCode: ReleaseCode? = null): Flow<Release> {
        return releaseItems.mapNotNull { it.findRelease(releaseId, releaseCode) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFull(releaseId: ReleaseId? = null, releaseCode: ReleaseCode? = null): Flow<Release> {
        return flow {
            emit(updateIfNotExists(releaseId, releaseCode))
        }.flatMapLatest {
            releases.mapNotNull { it.findRelease(releaseId, releaseCode) }
        }
    }

    fun updateItemsCache(items: List<Release>) {
        releaseItems.update { releaseItems ->
            releaseItems.filterNot { release ->
                items.any {
                    check(release, it.id, it.code)
                }
            } + items
        }
    }

    fun updateFullCache(release: Release) {
        releases.update { releases ->
            releases.filterNot {
                check(it, release.id, release.code)
            } + release
        }
    }

    /* Common */
    fun observeAccesses(releaseId: ReleaseId): Flow<List<EpisodeAccess>> {
        return episodesCheckerStorage.observeEpisodes().map { accesses ->
            accesses.filter { it.id.releaseId == releaseId }
        }
    }

    suspend fun getAccesses(releaseId: ReleaseId): List<EpisodeAccess> {
        return episodesCheckerStorage.getEpisodes(releaseId)
    }

    suspend fun getAccess(id: EpisodeId): EpisodeAccess? {
        return episodesCheckerStorage.getEpisode(id)
    }

    suspend fun resetAccessHistory(releaseId: ReleaseId) {
        episodesCheckerStorage.remove(releaseId)
    }

    suspend fun markAllViewed(id: ReleaseId) {
        updateEpisodes(id) { accesses ->
            accesses.map { it.copy(isViewed = true) }
        }
    }

    suspend fun markUnviewed(id: EpisodeId) {
        updateEpisode(id) {
            it.copy(
                isViewed = false,
                lastAccess = 0
            )
        }
    }

    suspend fun setAccessSeek(id: EpisodeId, seek: Long) {
        updateEpisode(id) {
            it.copy(
                seek = seek,
                lastAccess = System.currentTimeMillis(),
                isViewed = true
            )
        }
    }

    private suspend fun updateEpisode(id: EpisodeId, block: (EpisodeAccess) -> EpisodeAccess) {
        val access = episodesCheckerStorage.getEpisode(id) ?: return
        val newAccess = block.invoke(access)
        episodesCheckerStorage.putEpisode(newAccess)
    }

    private suspend fun updateEpisodes(
        id: ReleaseId,
        block: (List<EpisodeAccess>) -> List<EpisodeAccess>,
    ) {
        val access = episodesCheckerStorage.getEpisodes(id)
        val newAccess = block.invoke(access)
        episodesCheckerStorage.putAllEpisode(newAccess)
    }

    private suspend fun updateIfNotExists(
        releaseId: ReleaseId? = null,
        releaseCode: ReleaseCode? = null,
    ) {
        val release = releases.value.findRelease(releaseId, releaseCode)
        if (release != null) {
            return
        }
        runCatching {
            loadRelease(releaseId, releaseCode)
        }
    }

    private fun List<Release>.findRelease(id: ReleaseId?, code: ReleaseCode?): Release? = find {
        check(it, id, code)
    }

    private fun check(release: Release, id: ReleaseId?, code: ReleaseCode?): Boolean {
        val foundById = id != null && release.id == id
        val foundByCode = code != null && release.code == code
        return foundById || foundByCode
    }

    data class RequestKey(
        val id: ReleaseId?,
        val code: ReleaseCode?,
    )

}