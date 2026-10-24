package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto
import com.coffeepeek.feature.contributions.domain.model.PriceRange

/**
 * What the shop form borrows from the application: the location permission, the map picker, the photo picker, the price control, the brew method
 * icons, the roaster logos, the happy mascot and the success/failure feedback. Defaults keep previews and tests free of app services.
 */
class AddShopHost(
    /** Asks for the location permission once when the form opens; exactly one of the callbacks is called. */
    val locationPermission: @Composable (onGranted: () -> Unit, onDenied: () -> Unit) -> Unit = { _, _ -> },
    /** The map dialog: confirms a point with its address or is dismissed. */
    val mapPicker: @Composable (initial: GeoPoint?, onConfirm: (point: GeoPoint, address: String) -> Unit, onDismiss: () -> Unit) -> Unit = { _, _, _ -> },
    /** Picks photos (several at once), shows the chosen ones with a remove action and the title and hint. */
    val photos: @Composable (photos: List<PendingPhoto>, max: Int, title: String, hint: String, onAdd: (List<PendingPhoto>) -> Unit, onRemove: (Int) -> Unit) -> Unit =
        { _, _, _, _, _, _ -> },
    val priceSelector: @Composable (selected: PriceRange?, onSelect: (PriceRange?) -> Unit) -> Unit = { _, _ -> },
    val brewMethodIcon: @Composable (name: String) -> Unit = {},
    val roasterLogo: @Composable (roaster: CatalogOption) -> Unit = {},
    /** A cover photo of the sent shop, or the placeholder when there is none. */
    val shopImage: @Composable (bytes: ByteArray?, modifier: Modifier) -> Unit = { _, _ -> },
    /** Called once when the form ends: true for a sent shop, false for a failed one. */
    val feedback: (success: Boolean) -> Unit = {},
)

/** «Добавить кофейню». The caller owns navigation and the ViewModel store. */
interface AddShopEntry {
    @Composable
    fun Content(
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onAddRoaster: () -> Unit,
        modifier: Modifier = Modifier,
    )
}
