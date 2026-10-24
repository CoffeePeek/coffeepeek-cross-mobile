package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_choose_bean
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HomeCheckInEditorSelect(
    value: String, icon: ImageVector, description: String, enabled: Boolean,
    error: String? = null, artwork: (@Composable (Modifier) -> Unit)? = null, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().clip(shape)
        .background(if (error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
        .border(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, shape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics(mergeDescendants = true) { contentDescription = description; error?.let { error(it) } }
        .heightIn(min = 44.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (artwork != null) artwork(Modifier.size(24.dp))
        else Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(CoffeePeekIcons.ChevronUpDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview @Composable private fun HomeCheckInEditorSelectPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorSelect(stringResource(Res.string.home_check_in_choose_bean), CoffeePeekIcons.CoffeeBean,
        stringResource(Res.string.home_check_in_choose_bean), true, onClick = {}) }
}
