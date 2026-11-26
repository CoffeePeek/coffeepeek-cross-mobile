package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.LocalRootNavigationOrigin
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.feature.collections.api.CollectionsEntry
import org.koin.compose.koinInject

@Composable
internal fun CollectionEditorDestination(id: String?) {
    val origin = LocalRootNavigationOrigin.current
    koinInject<CollectionsEntry>().Editor(id, onBack = Navigator::popBack,
        onSaved = { Navigator.returnToProfile(origin) },
        onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
        onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
        onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
        onShareText = ShareHelper::shareText, onShareImages = ShareHelper::shareImages, modifier = Modifier)
}
