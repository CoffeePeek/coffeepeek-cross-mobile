package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.feature.shelf.api.ShelfImage
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem

/**
 * One movable tile per bag. A lifted bag moves from the shelf to the lifted layer and back as the same tile, with its card
 * already loaded, instead of being drawn anew and blinking while the copy loads. Call [Tile] for a bag in one place at a time.
 */
@Stable
internal class ShelfTiles(private val image: ShelfImage) {
    private val tiles = mutableMapOf<String, @Composable (ShelfBagItem, Modifier, Dp, Boolean) -> Unit>()

    @Composable
    fun Tile(item: ShelfBagItem, modifier: Modifier, elevation: Dp = 4.dp, highlighted: Boolean = false) {
        val tile = tiles.getOrPut(item.id) {
            movableContentOf { bag: ShelfBagItem, place: Modifier, lift: Dp, marked: Boolean ->
                ShelfBagTile(bag, image, place, elevation = lift, highlighted = marked)
            }
        }
        tile(item, modifier, elevation, highlighted)
    }

    /** Forgets the tiles of bags no longer on the shelf. */
    fun retain(ids: Set<String>) {
        tiles.keys.retainAll(ids)
    }
}

@Composable
internal fun rememberShelfTiles(image: ShelfImage): ShelfTiles = remember(image) { ShelfTiles(image) }
