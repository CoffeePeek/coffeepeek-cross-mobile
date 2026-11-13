package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_already_have_account
import com.coffeepeek.feature.auth.impl.resources.auth_login
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AuthFooterRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prompt: String? = null,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.glassControlSize)
        .clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(vertical = CoffeePeekDimens.spacing1), contentAlignment = Alignment.Center) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1, Alignment.CenterHorizontally)) {
            if (prompt != null) Text(prompt, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center)
            Text(text, color = colors.coffeePeekAccentForeground.copy(alpha = if (enabled) 1f else 0.38f),
                style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline)
        }
    }
}

@Composable
private fun AuthFooterPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AuthFooterRow(stringResource(Res.string.auth_login), {}, prompt = stringResource(Res.string.auth_already_have_account))
}

@Preview @Composable private fun AuthFooterLightPreview() = AuthFooterPreview(false)
@Preview @Composable private fun AuthFooterDarkPreview() = AuthFooterPreview(true)
