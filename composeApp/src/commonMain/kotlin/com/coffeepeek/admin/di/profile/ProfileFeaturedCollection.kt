package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.collections.api.CollectionsEntry
import org.koin.compose.koinInject

/** Profile supplies a verified visible author's public slug; collections owns its rendering and reads. */
@Composable
internal fun ProfileFeaturedCollection(slug: String) {
    koinInject<CollectionsEntry>().Featured(slug,
        onOpenCollection = { Navigator.navigate(Navigator.Screen.SavedPublicCollection(it)) }, modifier = Modifier)
}
