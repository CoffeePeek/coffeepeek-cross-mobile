package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.create_account
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.component.PlatformGlassBackButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AuthFooterRow(
    onBack: (() -> Unit)?,
    onSecondary: () -> Unit,
    secondaryText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) PlatformGlassBackButton(onClick = onBack)
        else Spacer(Modifier.size(1.dp))
        Text(
            text = secondaryText,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = CpColor.Primary,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSecondary)
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun AuthFooterRowPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AuthFooterRow(null, {}, stringResource(Res.string.create_account))
}

@Preview @Composable private fun AuthFooterRowLightPreview() = AuthFooterRowPreview(false)
@Preview @Composable private fun AuthFooterRowDarkPreview() = AuthFooterRowPreview(true)
