package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.*
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.*
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosCollectionsWorkspaceController =
    staticCompositionLocalOf<(IosCollectionsWorkspace, String) -> UIViewController> {
        error("The iOS app must provide a collections workspace controller")
    }

@Composable
internal fun iosCollectionsWorkspaceContent(): (String, String?, String?) -> UIViewController {
    val factory = LocalIosCollectionsWorkspaceController.current
    val language by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val saved = koinInject<SavedCollectionsRepository>()
    val snapshots = koinInject<CollectionsRepository>()
    val account = koinInject<CollectionsAccount>()
    return { mode, id, slug ->
        val session = if (mode == "Snapshot") IosCollectionsWorkspace(null, PublicCollectionSession(
            checkNotNull(id), snapshots, account, Navigator::popBack, ::openSavedList,
            ::openCollectionShop, ::openCollectionRoaster, ShareHelper::shareText))
        else IosCollectionsWorkspace(CollectionsWorkspaceSession(saved, snapshots, account,
            CollectionsWorkspaceMode.valueOf(mode), id, slug, null, Navigator::popBack,
            { Navigator.navigate(Navigator.Screen.Auth) },
            { Navigator.navigate(Navigator.Screen.CollectionEditor(it)) },
            { Navigator.navigate(Navigator.Screen.SavedPublicCollection(it)) },
            ::openCollectionShop, ::openCollectionRoaster, ShareHelper::shareText))
        factory(session, language)
    }
}

private fun openSavedList(id: String) {
    Navigator.popBack()
    Navigator.navigate(Navigator.Screen.SavedPublicCollection(id))
}
private fun openCollectionShop(slug: String) = Navigator.navigate(Navigator.Screen.ShopDetail(slug))
private fun openCollectionRoaster(slug: String) = Navigator.navigate(Navigator.Screen.RoasterDetail(slug))

