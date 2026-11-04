package com.coffeepeek.admin.di.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.shelf.api.ShelfProfileEntry
import org.koin.compose.koinInject

@Composable
internal fun ProfileShelfPreview(slug: String) {
    koinInject<ShelfProfileEntry>().Preview(slug,
        onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
}
