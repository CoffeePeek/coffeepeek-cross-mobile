package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.domain.PublicCheckInPreparation
import com.coffeepeek.feature.profile.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileCheckInPreparation(preparation: PublicCheckInPreparation?) {
    if (preparation == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        preparation.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        val details = listOfNotNull(preparation.brewMethodCode?.let { profileBrewMethodLabel(it) },
            preparation.doseGrams?.let { stringResource(Res.string.profile_check_in_dose, it.toString()) },
            preparation.brewDurationSeconds?.let { stringResource(Res.string.profile_check_in_seconds, it) },
            preparation.tasteScore?.let { stringResource(Res.string.profile_check_in_taste, it) })
            .joinToString(stringResource(Res.string.profile_check_in_separator))
        if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun profileBrewMethodLabel(code: String): String = when (code) {
    "espresso" -> stringResource(Res.string.profile_brew_espresso)
    "pour_over" -> stringResource(Res.string.profile_brew_pour_over)
    "batch_drip" -> stringResource(Res.string.profile_brew_batch)
    "moka_pot" -> stringResource(Res.string.profile_brew_moka)
    "cezve" -> stringResource(Res.string.profile_brew_cezve)
    "aeropress" -> stringResource(Res.string.profile_brew_aeropress)
    "french_press" -> stringResource(Res.string.profile_brew_french_press)
    "other" -> stringResource(Res.string.profile_brew_other)
    "hario-v60" -> stringResource(Res.string.profile_brew_hario)
    "chemex" -> stringResource(Res.string.profile_brew_chemex)
    "cold-brew" -> stringResource(Res.string.profile_brew_cold)
    "batch-brew" -> stringResource(Res.string.profile_brew_batch_brew)
    "moka-pot" -> stringResource(Res.string.profile_brew_moka_pot)
    "cezve-turkish-coffee" -> stringResource(Res.string.profile_brew_turkish)
    else -> code
}

@Composable private fun PreparationPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    ProfileCheckInPreparation(PublicCheckInPreparation("Эфиопия", "hario-v60", 18.0, 180, 4))
}
@Preview @Composable private fun PreparationLightPreview() = PreparationPreview(false)
@Preview @Composable private fun PreparationDarkPreview() = PreparationPreview(true)
