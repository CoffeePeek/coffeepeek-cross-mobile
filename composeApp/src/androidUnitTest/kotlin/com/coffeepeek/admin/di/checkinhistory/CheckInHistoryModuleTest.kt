package com.coffeepeek.admin.di.checkinhistory

import com.coffeepeek.admin.di.shop.CheckInTestSessions
import com.coffeepeek.api.CoffeePeekClient
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.checkinhistory.domain.repository.CheckInHistoryRepository
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CheckInHistoryModuleTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun graphResolvesLazilyWithoutReadingCredentialsOrOpeningAccountLeases() {
        var credentials = 0
        var sessionReads = 0
        val client = CoffeePeekClient("https://fixture.invalid", temporary.newFolder("cache").path, false,
            getToken = { credentials++; error("Unexpected credentials read") },
            saveToken = { error("Unexpected credentials write") })
        val sessions = object : SessionRepository by CheckInTestSessions() {
            override fun peekSession(): com.coffeepeek.domain.model.Session? {
                sessionReads++
                error("Resolving a graph must not open an account lease")
            }
        }
        val app = koinApplication {
            modules(module { single { client }; single<SessionRepository> { sessions } },
                checkInHistoryModule("https://fixture.invalid"))
        }
        try {
            assertSame(app.koin.get<AndroidCheckInHistoryScreenRenderer>(), app.koin.get<AndroidCheckInHistoryScreenRenderer>())
            assertSame(app.koin.get<AndroidCheckInHistoryCalendar>(), app.koin.get<AndroidCheckInHistoryCalendar>())
            assertEquals(1, app.koin.getAll<CheckInHistoryRepository>().size)
            assertEquals(0, credentials)
            assertEquals(0, sessionReads)
        } finally {
            app.close()
            client.client.close(); client.plainClient.close(); client.uploadClient.close()
        }
    }
}
