package com.coffeepeek.admin.di.contributions

import com.coffeepeek.core.session.Session
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.domain.model.DrinkOption
import com.coffeepeek.feature.contributions.domain.model.ShopSnapshot
import com.coffeepeek.feature.contributions.domain.repository.ContributionCatalogSource
import com.coffeepeek.feature.roaster.api.PublicRoasterCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The account key is the user id (else the token) of an active session, null for a guest, as in the other migrated owners. */
internal class SessionContributionsAccount(private val sessions: SessionReader) : ContributionsAccount {
    private fun key(session: Session?): String? = if (session != null && sessions.isActiveSession(session)) session.userId ?: session.accessToken else null
    override val accountKey: Flow<String?> = sessions.observeSession().map(::key)
    override fun currentAccountKey(): String? = key(sessions.peekSession())
}

/** The forms' reference reads come from Contributions' own source; nothing here knows the legacy shop repository any more. */
internal class SourceContributionCatalogs(private val source: ContributionCatalogSource) : ContributionCatalogs {
    override suspend fun catalogs(): Result<Catalogs> = source.catalogs()
    override suspend fun shop(slug: String): Result<ShopSnapshot> = source.shop(slug)
    override suspend fun drinks(): Result<List<DrinkOption>> = source.drinks()
}

/** The roasters a form offers are the public Roaster catalog's entries, the same list and cache the roaster screens use. */
internal fun roasterOptions(catalog: PublicRoasterCatalog): suspend () -> Result<List<CatalogOption>> = {
    catalog.load().map { rows -> rows.map { CatalogOption(it.slug, it.name, it.photoUrl) } }
}
