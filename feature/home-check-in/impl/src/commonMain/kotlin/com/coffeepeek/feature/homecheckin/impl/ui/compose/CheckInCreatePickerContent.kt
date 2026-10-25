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

/** Type choice only; the app owns authentication, the modal and navigation. */
@Composable
internal fun CheckInCreatePickerContent(
    homeSelected: Boolean, onSelectHome: (Boolean) -> Unit, onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CoffeePeekScrollableColumn(
        modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing6),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(stringResource(Res.string.home_check_in_create_title),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.home_check_in_create_prompt),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
            CoffeePeekSelectionCard(stringResource(Res.string.home_check_in_create_shop),
                stringResource(Res.string.home_check_in_create_shop_hint), CoffeePeekIcons.Location,
                !homeSelected, { onSelectHome(false) })
            CoffeePeekSelectionCard(stringResource(Res.string.home_check_in_create_home),
                stringResource(Res.string.home_check_in_create_home_hint), CoffeePeekIcons.Coffee,
                homeSelected, { onSelectHome(true) })
        }
        CoffeePeekButton(stringResource(Res.string.home_check_in_create_continue), onContinue)
    }
}

@Preview @Composable private fun CheckInCreatePickerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreatePickerContent(false, {}, {})
}

@Preview @Composable private fun CheckInCreatePickerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreatePickerContent(true, {}, {})
}
