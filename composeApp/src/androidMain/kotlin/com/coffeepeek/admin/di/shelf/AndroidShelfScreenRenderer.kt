package com.coffeepeek.admin.di.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.di.coffee.showShelfInCoffeeTab
import com.coffeepeek.admin.di.homecheckin.AndroidCheckInCreateFlowRenderer
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.shelf.ShelfScreenRenderer
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.room.repository.SettingRepository
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * After a bag is added, the Coffee tab opens on the shelf, where the new bag stands first. On a bag's screen "Заварить чашку"
 * opens the Home check-in form with its coffee.
 */
internal class AndroidShelfScreenRenderer(
    private val entry: ShelfEntry,
    private val settings: SettingRepository,
    private val checkIns: AndroidCheckInCreateFlowRenderer,
) : ShelfScreenRenderer {
    @Composable
    override fun Archive() =
        entry.Archive(onBack = Navigator::popBack, onOpenBag = { Navigator.navigate(Navigator.Screen.ShelfBag(it)) }, modifier = Modifier)

    @Composable
    override fun Bag(bagId: String) = entry.Bag(bagId, onBack = Navigator::popBack,
        onOpenCoffee = { Navigator.navigate(Navigator.Screen.CoffeeDetail(it)) }, modifier = Modifier) { coffee, onClose ->
        checkIns.Home(HomeCheckInBean(coffee.slug, coffee.name, coffee.shelfBagId), onDismiss = { onClose(false) }, onSaved = { onClose(true) })
    }

    @Composable
    override fun AddBag() {
        val scope = rememberCoroutineScope()
        entry.AddBag(onSaved = {
            // The destination leaves composition right away; the small write must still land.
            scope.launch(start = CoroutineStart.UNDISPATCHED) { withContext(NonCancellable) { settings.showShelfInCoffeeTab() } }
            Navigator.popThenSelectTab(Navigator.Screen.CoffeeTab)
        }, onClose = Navigator::popBack, modifier = Modifier)
    }
}
