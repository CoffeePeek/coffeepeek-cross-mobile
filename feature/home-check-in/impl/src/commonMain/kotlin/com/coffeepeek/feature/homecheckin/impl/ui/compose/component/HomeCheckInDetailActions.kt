package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_delete
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_edit
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomeCheckInDetailActions(enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        CoffeePeekButton(stringResource(Res.string.home_check_in_edit), onEdit,
            Modifier.weight(1f), enabled = enabled)
        TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            CoffeePeekIcon(CoffeePeekIcons.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            Text(stringResource(Res.string.home_check_in_delete))
        }
    }
}

@Composable private fun HomeCheckInDetailActionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInDetailActions(true, {}, {}) }
}
@Preview @Composable private fun HomeCheckInDetailActionsLightPreview() = HomeCheckInDetailActionsPreview(false)
@Preview @Composable private fun HomeCheckInDetailActionsDarkPreview() = HomeCheckInDetailActionsPreview(true)
