package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.impl.resources.*
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomePreparation

@Composable
internal fun HomeCheckInRecipeCard(entry: HomePreparation, methodLabel: String?) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(CoffeePeekIcons.Coffee, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(Res.string.home_check_in_method), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(homeRecipeMethodLabel(entry, methodLabel),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            entry.drinkName?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (entry.doseGrams != null || entry.brewDurationSeconds != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entry.doseGrams?.let { dose ->
                        val number = (if ('.' in dose) dose.trimEnd('0').trimEnd('.') else dose).replace('.', ',')
                        RecipeMetric(stringResource(Res.string.home_check_in_dose), stringResource(Res.string.home_check_in_grams, number))
                    }
                    entry.brewDurationSeconds?.let {
                        RecipeMetric(stringResource(Res.string.home_check_in_duration), durationLabel(it))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun durationLabel(seconds: Int): String = listOfNotNull(
    if (seconds >= 3600) stringResource(Res.string.home_check_in_hours, seconds / 3600) else null,
    if (seconds / 60 % 60 > 0) stringResource(Res.string.home_check_in_minutes, seconds / 60 % 60) else null,
    if (seconds % 60 > 0 || seconds == 0) stringResource(Res.string.home_check_in_seconds, seconds % 60) else null,
).joinToString(" ")

@Composable
private fun homeRecipeMethodLabel(entry: HomePreparation, label: String?): String {
    val generic = entry.brewMethodCode == "other" && (label == null || label == entry.brewMethodCode)
    return if (generic) stringResource(Res.string.home_check_in_method_other)
    else label ?: stringResource(Res.string.home_check_in_unspecified)
}

@Composable private fun HomeCheckInRecipeCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInRecipeCard(previewHomePreparation(), "Hario V60")
}
@Preview @Composable private fun HomeCheckInRecipeCardLightPreview() = HomeCheckInRecipeCardPreview(false)
@Preview @Composable private fun HomeCheckInRecipeCardDarkPreview() = HomeCheckInRecipeCardPreview(true)
