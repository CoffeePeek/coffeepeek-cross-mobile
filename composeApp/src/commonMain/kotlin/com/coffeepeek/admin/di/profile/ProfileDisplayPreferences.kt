package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionsEntry
import org.koin.compose.koinInject

/** Profile edit and Settings share the same feature-owned, server-backed controls. */
@Composable
internal fun ProfileDisplayPreferences() {
    val signIn = { Navigator.navigate(Navigator.Screen.Auth) }
    koinInject<CollectionsEntry>().ProfilePreference(signIn, Modifier)
}
