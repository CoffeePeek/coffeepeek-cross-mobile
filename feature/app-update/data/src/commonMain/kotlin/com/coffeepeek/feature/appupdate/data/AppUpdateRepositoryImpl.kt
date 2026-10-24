package com.coffeepeek.feature.appupdate.data

import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.api.DismissedUpdateStore
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.withTimeout

/** [client] is the unauthenticated API client: the version endpoint needs no account. */
class AppUpdateRepositoryImpl(
    private val client: HttpClient,
    private val environment: AppUpdateEnvironment,
    private val dismissed: DismissedUpdateStore,
) : AppUpdateRepository {
    override suspend fun fetch(): AppUpdate? = withTimeout(10_000) {
        val response = client.get("/api/v1/app-version") {
            parameter("platform", environment.platform)
            environment.channel?.let { parameter("channel", it) }
            header("Cache-Control", "no-store")
        }
        if (response.status.value != 200) return@withTimeout null
        response.body<AppVersionResponse>().toUpdate()
    }

    override suspend fun dismissedVersion() = dismissed.read()
    override suspend fun dismiss(version: Long) = dismissed.write(version)
}
