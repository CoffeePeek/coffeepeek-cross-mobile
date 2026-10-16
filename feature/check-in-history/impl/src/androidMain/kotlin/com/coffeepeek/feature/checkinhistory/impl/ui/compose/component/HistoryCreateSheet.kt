package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgePalette
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.HistorySource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryCreateSheet(onDismiss: () -> Unit, onCreate: (HistorySource) -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismiss, stringResource(Res.string.history_close)) {
        HistoryCreateContent(onCreate, modifier)
    }
}

@Composable private fun HistoryCreateContent(onCreate: (HistorySource) -> Unit, modifier: Modifier = Modifier) {
    var source by rememberSaveable { mutableStateOf(HistorySource.Shop) }
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.history_new), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.history_where), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HistorySource.entries.forEach { option ->
                val selected = option == source
                val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
                Row(Modifier.fillMaxWidth().clip(shape)
                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.outline, shape)
                    .selectable(selected, role = Role.RadioButton, onClick = { source = option })
                    .heightIn(min = 72.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CoffeePeekIconBadge(if (option == HistorySource.Shop) CoffeePeekIcons.Location else CoffeePeekIcons.Coffee,
                        CoffeePeekIconBadgePalette.Gold)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(if (option == HistorySource.Shop) Res.string.history_at_shop else Res.string.history_at_home),
                            style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(if (option == HistorySource.Shop) Res.string.history_create_shop_hint else Res.string.history_create_home_hint),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Icon(CoffeePeekIcons.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        CoffeePeekButton(stringResource(Res.string.history_continue), { onCreate(source) })
    }
}

@PreviewLightDark @Composable private fun HistoryCreatePreview() = CoffeePeekTheme { HistoryCreateContent({}) }
