package com.coffeepeek.feature.shelf.impl

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shelf.api.ShelfEntry
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.api.ShelfTimeZone
import com.coffeepeek.feature.shelf.domain.repository.ShelfRepository
import com.coffeepeek.feature.shelf.impl.ui.ShelfArchiveViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfArchiveScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfArchiveEvent
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfEvent
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfCalendar
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

/**
 * Construct once in composition. The shelf's ViewModel lives in the caller's ViewModel-store owner (the Coffee tab),
 * the archive's in its own destination; the application supplies the protected image loader and the time zone.
 */
@OptIn(ExperimentalTime::class)
fun createShelfEntry(repository: ShelfRepository, image: ShelfImage, timeZone: ShelfTimeZone): ShelfEntry =
    ShelfApiImpl(repository, image, ShelfCalendar(timeZone) { Clock.System.now() })

private class ShelfApiImpl(
    private val repository: ShelfRepository,
    private val image: ShelfImage,
    private val calendar: ShelfCalendar,
) : ShelfEntry {
    private val instanceKey = Uuid.random().toString()

    @Composable
    override fun Shelf(
        signedIn: Boolean, onOpenArchive: () -> Unit, onBrowseCatalog: () -> Unit, onSignIn: () -> Unit,
        modifier: Modifier, bottomClearance: Dp, header: @Composable ColumnScope.() -> Unit,
    ) {
        key(this) {
            val model = viewModel(key = "shelf:$instanceKey") { ShelfViewModel(repository, calendar) }
            ShelfScreen(model, signedIn, image, bottomClearance, modifier, header) { event ->
                when (event) {
                    ShelfEvent.OpenArchive -> onOpenArchive()
                    ShelfEvent.BrowseCatalog -> onBrowseCatalog()
                    ShelfEvent.SignIn -> onSignIn()
                }
            }
        }
    }

    @Composable
    override fun Archive(onBack: () -> Unit, modifier: Modifier) {
        key(this) {
            val model = viewModel(key = "shelf-archive:$instanceKey") { ShelfArchiveViewModel(repository, calendar) }
            ShelfArchiveScreen(model, image, modifier) { event -> when (event) { ShelfArchiveEvent.Back -> onBack() } }
        }
    }
}
