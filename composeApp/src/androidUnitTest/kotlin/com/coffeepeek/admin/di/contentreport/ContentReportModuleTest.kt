package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.network.SessionCredentials
import com.coffeepeek.core.network.SessionTokens
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contentreport.domain.repository.ContentReportRepository
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.*

class ContentReportModuleTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun constructionDoesNotReadCredentialsOrOpenAnAccountLease() {
        var credentialReads = 0
        val client = ApiClients("https://fixture.invalid", temporary.newFolder().path, false,
            credentials = object : SessionCredentials {
                override suspend fun load(): SessionTokens? { credentialReads++; error("Unexpected credentials") }
                override suspend fun save(tokens: SessionTokens?) { error("No writes") }
                override suspend fun replace(expected: SessionTokens, tokens: SessionTokens?): Boolean = false
            },
            refreshWith = { { Result.failure(IllegalStateException("unused")) } })
        val sessions = object : SessionReader by ReportTestSessions() {
            override fun peekSession() = error("No account lease at graph construction")
        }
        val app = koinApplication { modules(module { single { client }; single<SessionReader> { sessions } }, contentReportModule()) }
        try {
            assertSame(app.koin.get<FeatureContentReportRenderer>(), app.koin.get<FeatureContentReportRenderer>())
            assertEquals(1, app.koin.getAll<ContentReportRepository>().size)
            assertEquals(0, credentialReads)
        } finally {
            app.close()
            listOf(client.client, client.plainClient, client.uploadClient, client.imageClient).forEach { it.close() }
        }
    }
}
