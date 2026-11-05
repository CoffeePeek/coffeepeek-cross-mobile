package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.Res
import com.coffeepeek.feature.shelf.impl.resources.shelf_archive
import com.coffeepeek.feature.shelf.impl.resources.shelf_profile_title
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShelfHeader(onArchive: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.shelf_profile_title), Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (onArchive != null) CoffeePeekIconButton(onArchive) {
            Icon(CoffeePeekIcons.Archive, stringResource(Res.string.shelf_archive), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ShelfHeaderPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ShelfHeader({}, Modifier.background(MaterialTheme.colorScheme.background).padding(CoffeePeekDimens.spacing4))
}

@Preview @Composable private fun ShelfHeaderLightPreview() = ShelfHeaderPreview(false)
@Preview @Composable private fun ShelfHeaderDarkPreview() = ShelfHeaderPreview(true)
