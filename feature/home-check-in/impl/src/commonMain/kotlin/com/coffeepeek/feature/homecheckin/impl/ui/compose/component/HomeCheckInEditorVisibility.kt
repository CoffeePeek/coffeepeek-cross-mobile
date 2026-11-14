package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_public_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_public_title
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeCheckInEditorVisibility(isPublic: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight).toggleable(
            value = isPublic, enabled = enabled, role = Role.Switch, onValueChange = onChange,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(stringResource(Res.string.home_check_in_public_title), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.home_check_in_public_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(CoffeePeekDimens.spacing3))
        CoffeePeekSwitch(checked = isPublic, onCheckedChange = null, enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable private fun HomeCheckInEditorVisibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorVisibility(dark, true, {}) }
}
@Preview @Composable private fun HomeCheckInEditorVisibilityLightPreview() = HomeCheckInEditorVisibilityPreview(false)
@Preview @Composable private fun HomeCheckInEditorVisibilityDarkPreview() = HomeCheckInEditorVisibilityPreview(true)
