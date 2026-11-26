package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.collections.impl.createCollectionsLibrarySession
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosCollectionsLibraryController =
    staticCompositionLocalOf<(IosCollectionsLibrary, String) -> UIViewController> {
        error("The iOS app must provide a collections library controller")
    }

@Composable
internal fun iosCollectionsLibraryContent(): () -> UIViewController {
    val factory = LocalIosCollectionsLibraryController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val saved = koinInject<SavedCollectionsRepository>()
    val snapshots = koinInject<CollectionsRepository>()
    val account = koinInject<CollectionsAccount>()
    return iosCollectionsLibraryContent(factory, { languageTag }, saved, snapshots, account)
}

internal fun iosCollectionsLibraryContent(factory: (IosCollectionsLibrary, String) -> UIViewController,
    language: () -> String, saved: SavedCollectionsRepository, snapshots: CollectionsRepository, account: CollectionsAccount,
): () -> UIViewController {
    return {
        val library = IosCollectionsLibrary(createCollectionsLibrarySession(saved, snapshots, account),
            Navigator::popBack, { Navigator.navigate(Navigator.Screen.Auth) },
            { Navigator.navigate(Navigator.Screen.CollectionEditor(it)) }, snapshots)
        factory(library, language())
    }
}
