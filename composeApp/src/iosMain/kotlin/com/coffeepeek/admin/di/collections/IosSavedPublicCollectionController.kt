package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.SavedPublicCollectionSession
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosSavedPublicCollectionController =
    staticCompositionLocalOf<(IosSavedPublicCollection, String) -> UIViewController> {
        error("The iOS app must provide a public collection controller")
    }

@Composable
internal fun iosSavedPublicCollectionContent(): (String) -> UIViewController {
    val factory = LocalIosSavedPublicCollectionController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val saved = koinInject<SavedCollectionsRepository>()
    val snapshots = koinInject<CollectionsRepository>()
    val account = koinInject<CollectionsAccount>()
    return { id ->
        val session = SavedPublicCollectionSession(id, saved, snapshots, account,
            Navigator::popBack, { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) }, ShareHelper::shareText, ShareHelper::shareImages)
        factory(IosSavedPublicCollection(session), languageTag)
    }
}
