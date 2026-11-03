package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.step_done
import coffeepeek.composeapp.generated.resources.step_email
import coffeepeek.composeapp.generated.resources.step_register
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AuthStepper(activeIndex: Int, steps: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {
                    selected = index == activeIndex
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = if (index == 0) Color.Transparent else MaterialTheme.colorScheme.outline)
                    AuthStepMarker(index, activeIndex)
                    HorizontalDivider(Modifier.weight(1f), color = if (index == steps.lastIndex) Color.Transparent else MaterialTheme.colorScheme.outline)
                }
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = if (index <= activeIndex) CpColor.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AuthStepMarker(index: Int, activeIndex: Int) {
    val active = index <= activeIndex
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape)
            .background(if (active) CpColor.Primary else Color.Transparent)
            .border(1.dp, if (active) CpColor.Primary else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (index < activeIndex) {
            Icon(CpIcons.Check, contentDescription = null, tint = CpColor.DarkTextOnPrimary, modifier = Modifier.size(14.dp))
        } else {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (active) CpColor.DarkTextOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthStepperPreview(dark: Boolean, fontScale: Float = 1f) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val steps = listOf(stringResource(Res.string.step_email), stringResource(Res.string.step_register), stringResource(Res.string.step_done))
            AuthStepper(0, steps)
            AuthStepper(1, steps)
            AuthStepper(2, steps)
        }
    }
}

@Preview(widthDp = 208) @Composable private fun AuthStepperLightPreview() = AuthStepperPreview(false)
@Preview(widthDp = 208) @Composable private fun AuthStepperDarkPreview() = AuthStepperPreview(true)
@Preview(widthDp = 208) @Composable private fun AuthStepperLargeLightPreview() = AuthStepperPreview(false, 1.3f)
@Preview(widthDp = 208) @Composable private fun AuthStepperLargeDarkPreview() = AuthStepperPreview(true, 1.3f)
