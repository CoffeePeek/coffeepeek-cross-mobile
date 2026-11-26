package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.theme.uiKitStyle
import com.coffeepeek.admin.ui.component.PresentNativeController
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.collections.api.CollectionTarget
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import com.coffeepeek.feature.collections.domain.model.CollectionPlaceKind
import com.coffeepeek.feature.collections.domain.model.SavedCollectionTarget
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.*
import org.koin.compose.koinInject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun PlatformCollectionsPicker(target: CollectionTarget, onDismiss: () -> Unit,
    onSignIn: () -> Unit): Boolean {
    val saved = koinInject<SavedCollectionsRepository>()
    val snapshots = koinInject<CollectionsRepository>()
    val account = koinInject<CollectionsAccount>()
    val dismiss by rememberUpdatedState(onDismiss)
    val signIn by rememberUpdatedState(onSignIn)
    val workspace = remember(target, saved, snapshots, account) {
        val kind = if (target.kind == CollectionTargetKind.CoffeeShop) CollectionPlaceKind.CoffeeShop else CollectionPlaceKind.Roaster
        IosCollectionsWorkspace(CollectionsWorkspaceSession(saved, snapshots, account, CollectionsWorkspaceMode.Picker,
            null, null, SavedCollectionTarget(kind, target.slug), { dismiss() }, { signIn() }, {}, {}, {}, {}, {}, onSaved = {}), photos = snapshots)
    }
    val language = LocalCoffeePeekLanguageTag.current
    val factory = LocalIosCollectionsWorkspaceController.current
    val controller = remember(workspace, language) { factory(workspace, language) }
    val theme by ThemeManager.themeMode.collectAsState()
    SideEffect { controller.overrideUserInterfaceStyle = theme.uiKitStyle }
    PresentNativeController(controller, LocalUIViewController.current)
    DisposableEffect(workspace) { onDispose { workspace.close() } }
    return true
}
