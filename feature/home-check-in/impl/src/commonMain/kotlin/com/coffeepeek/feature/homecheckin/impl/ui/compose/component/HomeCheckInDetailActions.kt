package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomeCheckInDetailActions(enabled: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CoffeePeekButton(stringResource(Res.string.home_check_in_edit), onEdit, enabled = enabled)
        TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(CoffeePeekIcons.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.home_check_in_delete))
        }
    }
}

@Composable private fun HomeCheckInDetailActionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInDetailActions(true, {}, {}) }
}
@Preview @Composable private fun HomeCheckInDetailActionsLightPreview() = HomeCheckInDetailActionsPreview(false)
@Preview @Composable private fun HomeCheckInDetailActionsDarkPreview() = HomeCheckInDetailActionsPreview(true)
