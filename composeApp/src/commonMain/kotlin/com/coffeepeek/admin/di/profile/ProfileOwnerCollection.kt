package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionsEntry
import org.koin.compose.koinInject

@Composable
internal fun ProfileOwnerCollection(fallback: @Composable () -> Unit) {
    koinInject<CollectionsEntry>().OwnerProfile(
        onOpenCollection = { Navigator.navigate(Navigator.Screen.SavedPublicCollection(it)) },
        onSignIn = { Navigator.navigate(Navigator.Screen.Auth) },
        fallback = fallback, modifier = Modifier)
}
