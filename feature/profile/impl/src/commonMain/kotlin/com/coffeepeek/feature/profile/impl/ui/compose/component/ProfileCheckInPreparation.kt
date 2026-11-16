package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekPostShelfBagRow
import com.coffeepeek.core.designsystem.component.coffeePeekPostShelfBagCardImage
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileSlots
import com.coffeepeek.feature.profile.domain.PublicCheckInPreparation
import com.coffeepeek.feature.profile.impl.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileCheckInPreparation(preparation: PublicCheckInPreparation?, slots: ProfileSlots) {
    if (preparation == null) return
    val details = preparationDetails(preparation)
    val bag = preparation.shelfBagCardUrl?.takeIf(String::isNotBlank)
    if (bag != null) {
        ProfilePreparationBag(preparation, bag, details, slots)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        preparation.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun preparationDetails(preparation: PublicCheckInPreparation): String = listOfNotNull(
    preparation.brewMethodCode?.let { profileBrewMethodLabel(it) },
    preparation.doseGrams?.let { stringResource(Res.string.profile_check_in_dose, it.toString()) },
    preparation.brewDurationSeconds?.let { stringResource(Res.string.profile_check_in_seconds, it) },
    preparation.tasteScore?.let { stringResource(Res.string.profile_check_in_taste, it) },
).joinToString(stringResource(Res.string.profile_check_in_separator))

@Composable
private fun ProfilePreparationBag(preparation: PublicCheckInPreparation, bag: String, details: String, slots: ProfileSlots) {
    val onCoffee = preparation.coffeeProductSlug?.takeIf(String::isNotBlank)?.let { slug -> { slots.onOpenCoffee(slug) } }
    CoffeePeekPostShelfBagRow(
        title = { ProfilePreparationTitle(preparation.beanName, preparation.roasterName) },
        details = details.takeIf(String::isNotBlank),
        onClick = onCoffee,
        onClickLabel = stringResource(Res.string.profile_open_coffee).takeIf { onCoffee != null },
        showChevron = onCoffee != null,
        card = { slots.logoImage(bag, null, Modifier.coffeePeekPostShelfBagCardImage()) },
    )
}

@Composable
private fun ProfilePreparationTitle(name: String?, roaster: String?) {
    val bean = name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.profile_shelf_bag)
    val separator = stringResource(Res.string.profile_check_in_separator)
    val quiet = SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val title = buildAnnotatedString {
        append(bean)
        roaster?.takeIf(String::isNotBlank)?.let {
            withStyle(quiet) {
                append(separator)
                append(it)
            }
        }
    }
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
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

@Composable private fun PreparationPreview(dark: Boolean, bag: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val slots = ProfileSlots(image = { _, _, modifier -> Box(modifier.background(MaterialTheme.colorScheme.tertiary)) },
        photoViewer = { _, _, _, _ -> }, photoPrefetch = { _, _ -> })
    ProfileCheckInPreparation(PublicCheckInPreparation("Эфиопия", "hario-v60", 18.0, 180, 4,
        roasterName = "Sibaristica", coffeeProductSlug = if (bag) "ethiopia" else null,
        shelfBagCardUrl = if (bag) "preview-bag" else null), slots)
}
@Preview @Composable private fun PreparationLightPreview() = PreparationPreview(false)
@Preview @Composable private fun PreparationDarkPreview() = PreparationPreview(true)
@Preview @Composable private fun PreparationBagLightPreview() = PreparationPreview(false, bag = true)
@Preview @Composable private fun PreparationBagDarkPreview() = PreparationPreview(true, bag = true)
