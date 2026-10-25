package com.coffeepeek.admin.di.contributions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.contributions.ContributionsScreenRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.ShopRepository
import com.coffeepeek.feature.contributions.api.AddRoasterEntry
import com.coffeepeek.feature.contributions.api.AddShopEntry
import com.coffeepeek.feature.contributions.api.ChangeEditorEntry
import com.coffeepeek.feature.contributions.api.ChangeRequestEntry
import com.coffeepeek.feature.contributions.api.ContributionsListEntry
import com.coffeepeek.feature.contributions.api.SuggestChangeEntry
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.impl.createAddRoasterEntry
import com.coffeepeek.feature.contributions.impl.createAddShopEntry
import com.coffeepeek.feature.contributions.impl.createChangeEditorEntry
import com.coffeepeek.feature.contributions.impl.createChangeRequestEntry
import com.coffeepeek.feature.contributions.impl.createContributionsListEntry
import com.coffeepeek.feature.contributions.impl.createSuggestChangeEntry
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind as LegacyKind
import com.coffeepeek.feature.contributions.api.ContributionKind

/** Routes the contribution screens to the migrated entries; navigation, the photo picker, the map and the sign-in stay app-owned. */
internal class FeatureContributionsScreenRenderer(
    private val list: ContributionsListEntry,
    private val addShop: AddShopEntry,
    private val addRoaster: AddRoasterEntry,
    private val suggest: SuggestChangeEntry,
    private val editor: ChangeEditorEntry,
    private val detail: ChangeRequestEntry,
) : ContributionsScreenRenderer {
    private val signIn = { Navigator.navigate(Navigator.Screen.Auth) }
    private val openShop = { slug: String -> Navigator.navigate(Navigator.Screen.ShopDetail(slug)) }

    @Composable
    override fun List(kind: LegacyKind) = list.Content(
        kind = ContributionKind.valueOf(kind.name), onBack = Navigator::popBack, onSignIn = signIn, onOpenShop = openShop,
        onOpenRequest = { Navigator.navigate(Navigator.Screen.ShopChangeRequestDetail(it)) }, modifier = Modifier,
    )

    @Composable
    override fun AddShop() = addShop.Content(onBack = Navigator::popBack, onSignIn = signIn, onAddRoaster = { Navigator.navigate(Navigator.Screen.AddRoaster) }, modifier = Modifier)

    @Composable
    override fun AddRoaster() = addRoaster.Content(onBack = Navigator::popBack, onSignIn = signIn, modifier = Modifier)

    @Composable
    override fun Suggest(shopId: String) = suggest.Content(
        shopSlug = shopId, onBack = Navigator::popBack, onSignIn = signIn,
        onPickSection = { Navigator.navigate(Navigator.Screen.ShopChangeEditor(shopId = shopId, section = it.name)) }, modifier = Modifier,
    )

    @Composable
    override fun Editor(shopId: String, section: String, requestId: String) = editor.Content(
        shopSlug = shopId, section = ChangeSection.entries.firstOrNull { it.name == section } ?: ChangeSection.Description,
        requestId = requestId.ifBlank { null }, onBack = Navigator::popBack, onSignIn = signIn,
        onOpenRequest = { Navigator.popThenNavigate(Navigator.Screen.ShopChangeRequestDetail(it)) }, modifier = Modifier,
    )

    @Composable
    override fun RequestDetail(requestId: String) = detail.Content(requestId = requestId, onBack = Navigator::popBack, onSignIn = signIn, onOpenShop = openShop, modifier = Modifier)
}

internal fun featureContributionsScreenRenderer(
    repository: ContributionsRepository, sessions: SessionReader, shops: ShopRepository,
): ContributionsScreenRenderer {
    val account = SessionContributionsAccount(sessions)
    val catalogs = ShopContributionCatalogs(shops)
    return FeatureContributionsScreenRenderer(
        list = createContributionsListEntry(repository, account),
        addShop = createAddShopEntry(repository, catalogs, AppContributionLocation(), account, addShopHost()),
        addRoaster = createAddRoasterEntry(repository, catalogs, account, addRoasterHost()),
        suggest = createSuggestChangeEntry(catalogs, account),
        editor = createChangeEditorEntry(repository, catalogs, account, changeEditorHost()),
        detail = createChangeRequestEntry(repository, account),
    )
}
