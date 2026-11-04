package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionsEntry
import org.koin.compose.koinInject

@Composable
internal fun ProfileOwnerCollection() {
    koinInject<CollectionsEntry>().OwnerProfile(
        onOpenCollection = { Navigator.navigate(Navigator.Screen.CollectionEditor(it)) },
        onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
        modifier = Modifier)
}

internal fun openProfileCollections() {
    AppProfileAnalytics.favoritesOpened()
    Navigator.navigate(Navigator.Screen.CollectionsLibrary)
}
