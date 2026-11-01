package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

internal val ShelfLedgeHeight = 20.dp

/** The board the bags stand on. */
@Composable
internal fun ShelfLedge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier.fillMaxWidth().height(ShelfLedgeHeight).shadow(3.dp, shape)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape),
    )
}

/** Two bare shelves for the empty and guest states. */
@Composable
internal fun ShelfEmptyShelves(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 96.dp), verticalArrangement = Arrangement.spacedBy(112.dp)) {
        ShelfLedge()
        ShelfLedge()
    }
}

@Composable
private fun ShelfLedgePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4)) { ShelfEmptyShelves() }
}

@Preview @Composable private fun ShelfLedgeLightPreview() = ShelfLedgePreview(false)
@Preview @Composable private fun ShelfLedgeDarkPreview() = ShelfLedgePreview(true)
