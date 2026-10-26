package com.coffeepeek.admin.di.authorblocking

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingEntry
import org.koin.compose.koinInject

/** Root callbacks only: feature implementation owns all rendering and confirmation state. */
@Composable
internal fun AuthorBlockingScreenRoute() {
    koinInject<AuthorBlockingEntry>().Screen(onBack = { Navigator.popBack() },
        onLogin = { Navigator.navigate(Navigator.Screen.Auth) }, onRegister = { Navigator.navigate(Navigator.Screen.Register) })
}

@Composable
internal fun AuthorBlockingRootHost() {
    koinInject<AuthorBlockingEntry>().Host(onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
        bottomClearanceDp = CpDimens.floatingNavContentClearance.value)
}
