package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.api.ShelfProfileEntry
import org.koin.compose.koinInject

@Composable
internal fun ProfileOwnShelf(modifier: Modifier, header: @Composable () -> Unit) {
    val preferences = koinInject<ShelfProfileEntry>()
    val signIn = { Navigator.navigate(Navigator.Screen.Auth) }
    koinInject<ShelfEntry>().Shelf(
        signedIn = true,
        onOpenArchive = { Navigator.navigate(Navigator.Screen.ShelfArchive) },
        onSignIn = signIn,
        onAddBag = { Navigator.navigate(Navigator.Screen.ShelfAddBag) },
        onOpenBag = { Navigator.navigate(Navigator.Screen.ShelfBag(it)) },
        modifier = modifier,
        bottomClearance = LocalFloatingNavClearance.current,
        header = { header() },
        visibilitySettings = { preferences.Visibility(signIn, Modifier) },
    )
}
