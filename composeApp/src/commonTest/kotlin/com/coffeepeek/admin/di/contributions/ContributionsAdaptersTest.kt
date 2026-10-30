package com.coffeepeek.admin.di.contributions

import com.coffeepeek.admin.feature.community.ui.FeedSessions
import com.coffeepeek.core.session.Session
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import com.coffeepeek.feature.contributions.domain.repository.ContributionCatalogSource
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalogEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ContributionsAdaptersTest {
    @Test fun theAccountKeyIsTheUserIdElseTheTokenAndNullForAGuestOrAnInactiveSession() = runTest {
        val sessions = FeedSessions(null)
        val account = SessionContributionsAccount(sessions)
        assertNull(account.accountKey.first()); assertNull(account.currentAccountKey())
        sessions.applySession(Session("token", userId = "user-1"))
        assertEquals("user-1", account.accountKey.first()); assertEquals("user-1", account.currentAccountKey())
        sessions.applySession(Session("token-only"))
        assertEquals("token-only", account.currentAccountKey())
        sessions.applySession(Session(" ", userId = "user-1"))
        assertNull(account.currentAccountKey()); assertNull(account.accountKey.first())
    }

    @Test fun theRoasterOptionsAreThePublicRoasterCatalogEntriesWithTheirLogo() = runTest {
        val catalog = PublicRoasterCatalog { Result.success(listOf(PublicRoasterCatalogEntry("r1", "Roaster", "https://logo"), PublicRoasterCatalogEntry("r2", "Plain", null))) }
        assertEquals(listOf(CatalogOption("r1", "Roaster", "https://logo"), CatalogOption("r2", "Plain", null)), roasterOptions(catalog)().getOrThrow())
        val failing = PublicRoasterCatalog { Result.failure(IllegalStateException("offline")) }
        assertTrue(roasterOptions(failing)().isFailure)
    }

    @Test fun theFormsCatalogsAreTheContributionSourceUnchanged() = runTest {
        val source = object : ContributionCatalogSource {
            override suspend fun catalogs() = Result.success(Catalogs(beans = listOf(CatalogOption("b", "Arabica"))))
            override suspend fun shop(slug: String) = Result.success(ShopSnapshot(slug, "Cafe"))
            override suspend fun drinks() = Result.success(listOf(DrinkOption("espresso", "Эспрессо")))
        }
        val catalogs = SourceContributionCatalogs(source)
        assertEquals(listOf("b"), catalogs.catalogs().getOrThrow().beans.map { it.id })
        assertEquals("Cafe", catalogs.shop("cafe-slug").getOrThrow().title)
        assertEquals(listOf("espresso"), catalogs.drinks().getOrThrow().map { it.slug })
    }
}
