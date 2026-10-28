package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.coffeepeek.admin.di.coffee.showShelfInCoffeeTab
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** After a bag is added, the Coffee tab opens on the shelf, where the new bag stands first. */
internal class AndroidShelfScreenRenderer(private val entry: ShelfEntry, private val settings: SettingRepository) : ShelfScreenRenderer {
    @Composable
    override fun Archive() = entry.Archive(onBack = Navigator::popBack)

    @Composable
    override fun AddBag() {
        val scope = rememberCoroutineScope()
        entry.AddBag(onSaved = {
            // The destination leaves composition right away; the small write must still land.
            scope.launch(start = CoroutineStart.UNDISPATCHED) { withContext(NonCancellable) { settings.showShelfInCoffeeTab() } }
            Navigator.popThenSelectTab(Navigator.Screen.CoffeeTab)
        }, onClose = Navigator::popBack)
    }
}
