package ru.radiationx.anilibria.model.data.remote.api

import io.reactivex.Single
import org.json.JSONObject
import ru.radiationx.anilibria.di.qualifier.ApiClient
import ru.radiationx.data.entity.app.Paginated
import ru.radiationx.data.entity.app.release.RandomRelease
import ru.radiationx.data.entity.app.release.ReleaseFull
import ru.radiationx.data.entity.app.release.ReleaseItem
import ru.radiationx.anilibria.model.data.remote.Api
import ru.radiationx.anilibria.model.data.remote.ApiResponse
import ru.radiationx.anilibria.model.data.remote.IClient
import ru.radiationx.anilibria.model.data.remote.address.ApiConfig
import ru.radiationx.anilibria.model.data.remote.parsers.ReleaseParser
import javax.inject.Inject

/* Created by radiationx on 31.10.17. */

class ReleaseApi @Inject constructor(
        @ApiClient private val client: IClient,
        private val releaseParser: ReleaseParser,
        private val apiConfig: ApiConfig
) {

    fun getRandomRelease(): Single<RandomRelease> {
        val args: MutableMap<String, String> = mutableMapOf(
                "query" to "random_release"
        )
        return client.post(apiConfig.apiUrl, args)
                .compose(ApiResponse.fetchResult<JSONObject>())
                .map { releaseParser.parseRandomRelease(it) }
    }

    fun getRelease(releaseId: Int): Single<ReleaseFull> {
        val args: MutableMap<String, String> = mutableMapOf(
                "query" to "release",
                "id" to releaseId.toString()
        )
        return client.post(apiConfig.apiUrl, args)
                .compose(ApiResponse.fetchResult<JSONObject>())
                .map { releaseParser.release(it) }
    }

    fun getRelease(releaseCode: String): Single<ReleaseFull> {
        val args: MutableMap<String, String> = mutableMapOf(
                "query" to "release",
                "code" to releaseCode
        )
        return client.post(apiConfig.apiUrl, args)
                .compose(ApiResponse.fetchResult<JSONObject>())
                .map { releaseParser.release(it) }
    }

    fun getReleases(page: Int): Single<Paginated<List<ReleaseItem>>> {
        val args: MutableMap<String, String> = mutableMapOf(
                "query" to "list",
                "page" to page.toString(),
                "filter" to "id,torrents,playlist,favorite,moon,blockedInfo",
                "rm" to "true"
        )
        return client.post(apiConfig.apiUrl, args)
                .compose(ApiResponse.fetchResult<JSONObject>())
                .map { releaseParser.releases(it) }
    }


}

        