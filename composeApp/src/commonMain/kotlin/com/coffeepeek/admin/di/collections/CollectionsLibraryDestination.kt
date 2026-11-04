package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionsEntry
import org.koin.compose.koinInject

@Composable
internal fun CollectionsLibraryDestination() {
    koinInject<CollectionsEntry>().Library(onBack = Navigator::popBack,
        onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
        onOpenEditor = { Navigator.navigate(Navigator.Screen.CollectionEditor(it)) }, modifier = Modifier)
}
