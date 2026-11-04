package com.coffeepeek.admin.di.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionsEntry
import com.coffeepeek.feature.collections.domain.repository.SavedCollectionsRepository
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@Composable
internal actual fun CollectionsPickerHost() {
    val collections = koinInject<SavedCollectionsRepository>()
    val favorites = koinInject<AccountFavorites>()
    LaunchedEffect(collections, favorites) { synchronizeCollectionFavorites(collections.observeChanges(), favorites) }
    val requests = getKoin().getOrNull<CollectionPickerRequests>() ?: return
    val request by requests.state.collectAsStateWithLifecycle()
    request?.let { current -> key(current) {
        LaunchedEffect(current) { requests.presented(current) }
        koinInject<CollectionsEntry>().Picker(current.target, onDismiss = { requests.dismiss(current) },
            onSignIn = {
                requests.dismiss(current)
                Navigator.navigate(Navigator.Screen.Auth)
            }, modifier = Modifier)
    } }
}
