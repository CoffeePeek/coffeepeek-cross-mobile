package com.coffeepeek.admin.di.contentreport

import com.coffeepeek.api.CoffeePeekClient
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
        val client = CoffeePeekClient("https://fixture.invalid", temporary.newFolder().path, false,
            getToken = { credentialReads++; error("Unexpected credentials") }, saveToken = { error("No writes") })
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
