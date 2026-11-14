package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Choose a photo first. Scanner preparation is offered only after a photo was selected. */
@Composable
internal fun ShelfScanIntro(onCamera: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.shelf_scan_intro_title), Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        ShelfScanExample()
        Text(stringResource(Res.string.shelf_scan_intro_body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekButton(stringResource(Res.string.shelf_scan_camera), onCamera,
            leadingIcon = { Icon(CoffeePeekIcons.Camera, null) })
        ShelfSecondaryButton(stringResource(Res.string.shelf_scan_gallery), onGallery)
    }
}

@Composable
private fun ShelfScanIntroPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfScanIntro({}, {}, Modifier.background(MaterialTheme.colorScheme.background))
}

@Preview @Composable private fun ShelfScanIntroLightPreview() = ShelfScanIntroPreview(false)
@Preview @Composable private fun ShelfScanIntroDarkPreview() = ShelfScanIntroPreview(true)
