package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekScrollableColumn
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectionCard
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal enum class CheckInCreateChoice { Shop, Home, ShelfBag }

/** Type choice only; the app owns authentication, the modal and navigation. */
@Composable
internal fun CheckInCreatePickerContent(
    choice: CheckInCreateChoice, onSelect: (CheckInCreateChoice) -> Unit, onContinue: () -> Unit,
    modifier: Modifier = Modifier, shelfAvailable: Boolean = false,
) {
    CoffeePeekScrollableColumn(
        modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            if (shelfAvailable) Text(stringResource(Res.string.home_check_in_create_what),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            else {
                Text(stringResource(Res.string.home_check_in_create_title),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.home_check_in_create_prompt),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            CoffeePeekSelectionCard(stringResource(Res.string.home_check_in_create_shop),
                stringResource(Res.string.home_check_in_create_shop_hint), CoffeePeekIcons.Location,
                choice == CheckInCreateChoice.Shop, { onSelect(CheckInCreateChoice.Shop) })
            CoffeePeekSelectionCard(stringResource(Res.string.home_check_in_create_home),
                stringResource(Res.string.home_check_in_create_home_hint), CoffeePeekIcons.Coffee,
                choice == CheckInCreateChoice.Home, { onSelect(CheckInCreateChoice.Home) })
            if (shelfAvailable) CoffeePeekSelectionCard(stringResource(Res.string.home_check_in_create_shelf_title),
                stringResource(Res.string.home_check_in_create_shelf_body), CoffeePeekIcons.Camera,
                choice == CheckInCreateChoice.ShelfBag, { onSelect(CheckInCreateChoice.ShelfBag) })
        }
        CoffeePeekButton(stringResource(Res.string.home_check_in_create_continue), onContinue)
    }
}

@Preview @Composable private fun CheckInCreatePickerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreatePickerContent(CheckInCreateChoice.Shop, {}, {})
}

@Preview @Composable private fun CheckInCreatePickerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreatePickerContent(CheckInCreateChoice.Home, {}, {})
}

@Preview @Composable private fun CheckInCreatePickerShelfLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreatePickerContent(CheckInCreateChoice.ShelfBag, {}, {}, shelfAvailable = true)
}

@Preview @Composable private fun CheckInCreatePickerShelfDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreatePickerContent(CheckInCreateChoice.ShelfBag, {}, {}, shelfAvailable = true)
}
