package com.coffeepeek.feature.shelf.impl

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shelf.api.ShelfBrewCoffee
import com.coffeepeek.feature.shelf.api.ShelfBrewLog
import com.coffeepeek.feature.shelf.api.ShelfCoffeeSearch
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.api.ShelfCamera
import com.coffeepeek.feature.shelf.api.ShelfGalleryPicker
import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import com.coffeepeek.feature.shelf.domain.repository.BagScanner
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.ShelfAddViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfArrivals
import com.coffeepeek.feature.shelf.impl.ui.ShelfBagHandoff
import com.coffeepeek.feature.shelf.impl.ui.ShelfBagViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfArchiveViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfAddScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfArchiveScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfBagScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfAddEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfEvent
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

/** What the shelf needs from the platform and the application; none of it is touched until a screen is shown. */
class ShelfPorts(
    val scanner: BagScanner,
    val coffees: ShelfCoffeeSearch,
    val image: ShelfImage,
    val gallery: ShelfGalleryPicker,
    val camera: ShelfCamera,
    val timeZone: ShelfTimeZone,
    val brews: ShelfBrewLog,
)

/**
 * Construct once in composition. The shelf's ViewModel lives in the caller's ViewModel-store owner (the Coffee tab);
 * the archive, a bag's screen and the scan live in their own destinations.
 */
@OptIn(ExperimentalTime::class)
fun createShelfEntry(repository: ShelfRepository, ports: ShelfPorts): ShelfEntry =
    ShelfApiImpl(repository, ports, ShelfCalendar(ports.timeZone) { Clock.System.now() })

private class ShelfApiImpl(
    private val repository: ShelfRepository,
    private val ports: ShelfPorts,
    private val calendar: ShelfCalendar,
) : ShelfEntry {
    private val instanceKey = Uuid.random().toString()
    private val arrivals = ShelfArrivals()
    private val bags = ShelfBagHandoff()

    @Composable
    override fun Shelf(
        signedIn: Boolean, onOpenArchive: () -> Unit, onBrowseCatalog: () -> Unit, onSignIn: () -> Unit, onAddBag: () -> Unit,
        onOpenBag: (String) -> Unit, modifier: Modifier, bottomClearance: Dp, header: @Composable ColumnScope.() -> Unit,
    ) {
        key(this) {
            val model = viewModel(key = "shelf:$instanceKey") { ShelfViewModel(repository, calendar, arrivals, bags) }
            ShelfScreen(model, signedIn, ports.image, bottomClearance, modifier, header) { event ->
                when (event) {
                    ShelfEvent.OpenArchive -> onOpenArchive()
                    ShelfEvent.BrowseCatalog -> onBrowseCatalog()
                    ShelfEvent.SignIn -> onSignIn()
                    ShelfEvent.AddBag -> onAddBag()
                    is ShelfEvent.OpenBag -> onOpenBag(event.id)
                }
            }
        }
    }

    @Composable
    override fun Archive(onBack: () -> Unit, onOpenBag: (String) -> Unit, modifier: Modifier) {
        key(this) {
            val model = viewModel(key = "shelf-archive:$instanceKey") { ShelfArchiveViewModel(repository, calendar, bags) }
            ShelfArchiveScreen(model, ports.image, modifier) { event ->
                when (event) {
                    ShelfArchiveEvent.Back -> onBack()
                    is ShelfArchiveEvent.OpenBag -> onOpenBag(event.id)
                }
            }
        }
    }

    @Composable
    override fun Bag(
        bagId: String, onBack: () -> Unit, onOpenCoffee: (String) -> Unit, modifier: Modifier,
        brew: (@Composable (ShelfBrewCoffee, (Boolean) -> Unit) -> Unit)?,
    ) {
        key(this, bagId) {
            val model = viewModel(key = "shelf-bag:$instanceKey:$bagId") { ShelfBagViewModel(bagId, repository, calendar, bags, ports.brews) }
            ShelfBagScreen(model, ports.image, brew, modifier) { event ->
                when (event) {
                    ShelfBagEvent.Back -> onBack()
                    is ShelfBagEvent.OpenCoffee -> onOpenCoffee(event.slug)
                }
            }
        }
    }

    @Composable
    override fun AddBag(onSaved: () -> Unit, onClose: () -> Unit, modifier: Modifier) {
        key(this) {
            val model = viewModel(key = "shelf-add:$instanceKey") { ShelfAddViewModel(repository, ports.scanner, ports.coffees, arrivals, calendar) }
            ShelfAddScreen(model, ports.gallery, ports.camera, modifier) { event ->
                when (event) {
                    ShelfAddEvent.Saved -> onSaved()
                    ShelfAddEvent.Close -> onClose()
                }
            }
        }
    }
}
