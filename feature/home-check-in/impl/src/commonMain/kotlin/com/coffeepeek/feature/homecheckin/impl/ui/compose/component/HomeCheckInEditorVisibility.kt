package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_public_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_public_title
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun HomeCheckInEditorVisibility(isPublic: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(isPublic, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.home_check_in_public_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.home_check_in_public_hint), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        CoffeePeekSwitch(isPublic, null, Modifier.clearAndSetSemantics {}, enabled)
    }
}

@Composable private fun HomeCheckInEditorVisibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorVisibility(false, true, {}) }
}
@Preview @Composable private fun HomeCheckInEditorVisibilityLightPreview() = HomeCheckInEditorVisibilityPreview(false)
@Preview @Composable private fun HomeCheckInEditorVisibilityDarkPreview() = HomeCheckInEditorVisibilityPreview(true)
