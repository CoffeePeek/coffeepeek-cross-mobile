package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekScrollableColumn
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.icons.CoffeePeekMapIcon
import com.coffeepeek.core.designsystem.icons.coffeePeekMapIconPainter
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Each choice starts its flow immediately; the app owns authentication and navigation. */
@Composable
internal fun CheckInCreatePickerContent(
    onShop: () -> Unit, onHome: () -> Unit, onShelfBag: (() -> Unit)?, modifier: Modifier = Modifier,
) {
    CoffeePeekScrollableColumn(
        modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            if (onShelfBag != null) Text(stringResource(Res.string.home_check_in_create_what),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            else {
                Text(stringResource(Res.string.home_check_in_create_title),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.home_check_in_create_prompt),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            CheckInCreateOption(stringResource(Res.string.home_check_in_create_shop),
                stringResource(Res.string.home_check_in_create_shop_hint), rememberVectorPainter(CoffeePeekIcons.Location), onShop)
            CheckInCreateOption(stringResource(Res.string.home_check_in_create_home),
                stringResource(Res.string.home_check_in_create_home_hint), coffeePeekMapIconPainter(CoffeePeekMapIcon.CoffeeShop), onHome)
            if (onShelfBag != null) CheckInCreateOption(stringResource(Res.string.home_check_in_create_shelf_title),
                stringResource(Res.string.home_check_in_create_shelf_body), rememberVectorPainter(CoffeePeekIcons.Camera), onShelfBag)
        }
    }
}

@Composable
private fun CheckInCreateOption(title: String, description: String, icon: Painter, onClick: () -> Unit) {
    CoffeePeekCard(onClick = onClick, elevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(CoffeePeekDimens.spacing3)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            CoffeePeekIconBadge(icon, CoffeePeekIconBadgePalette.Gold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview @Composable private fun CheckInCreatePickerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreatePickerContent({}, {}, null)
}

@Preview @Composable private fun CheckInCreatePickerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreatePickerContent({}, {}, null)
}

@Preview @Composable private fun CheckInCreatePickerShelfLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreatePickerContent({}, {}, {})
}

@Preview @Composable private fun CheckInCreatePickerShelfDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreatePickerContent({}, {}, {})
}
