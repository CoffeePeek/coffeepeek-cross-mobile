package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CpTopBar
import com.coffeepeek.admin.ui.component.SettingsIconBadge
import com.coffeepeek.admin.ui.component.SettingsIconPalette
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.model.City
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun CityScreen(vm: ProfileViewModel = koinInject()) {
    val cities by vm.cities.collectAsState()
    val selectedCityId by vm.selectedCityId.collectAsState()
    CityScreenContent(cities, selectedCityId, vm::setCity, Navigator::popBack)
}

@Composable
internal fun CityScreenContent(
    cities: List<City>, selectedCityId: String?, onSelect: (String) -> Unit, onBack: () -> Unit,
    copy: LegacyCityCopy = legacyCityCopy(),
) {
    CoffeePeekScaffold(topBar = { CpTopBar(copy.title, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = CpDimens.spacing4).verticalScroll(rememberScrollState())) {
            CityHeading(copy)
            CoffeePeekCard(shape = RoundedCornerShape(CpDimens.cardRadius),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = 0.dp,
                contentPadding = PaddingValues(0.dp)) {
                cities.forEachIndexed { index, city ->
                    CityOption(city, city.id == selectedCityId, copy.selected) { onSelect(city.id) }
                    if (index != cities.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CityHeading(copy: LegacyCityCopy) {
    Spacer(Modifier.height(CpDimens.spacing4))
    Text(copy.heading, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(CpDimens.spacing2))
    Text(copy.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(CpDimens.spacing6))
}

@Composable
private fun CityOption(city: City, selected: Boolean, selectedDescription: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = CpDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically) {
        SettingsIconBadge(CpIcons.Location, SettingsIconPalette.Aqua)
        Spacer(Modifier.size(CpDimens.spacing3))
        Text(city.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (selected) Icon(CpIcons.Check, selectedDescription, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(CpDimens.settingsIconSize))
    }
}

internal data class LegacyCityCopy(val title: String, val heading: String, val description: String, val selected: String)

@Composable
private fun legacyCityCopy() = LegacyCityCopy(
    stringResource(Res.string.legacy_settings_city), stringResource(Res.string.legacy_settings_city_heading),
    stringResource(Res.string.legacy_settings_city_page_description), stringResource(Res.string.legacy_settings_selected),
)

@Composable
private fun LegacyCityPreview(dark: Boolean, belarusian: Boolean = false) = CoffeePeekTheme(darkTheme = dark) {
    val copy = if (belarusian) LegacyCityCopy("Горад", "Ваш горад", "Вызначае, якія кавярні паказваць у першую чаргу.", "Абрана")
    else LegacyCityCopy("Город", "Ваш город", "Определяет, какие кофейни показывать в первую очередь.", "Выбрано")
    CityScreenContent(listOf(City("minsk", if (belarusian) "Мінск" else "Минск"), City("brest", "Брэст")), "minsk", {}, {}, copy)
}

@Preview @Composable private fun LegacyCityRussianLightPreview() = LegacyCityPreview(false)
@Preview @Composable private fun LegacyCityRussianDarkPreview() = LegacyCityPreview(true)
@Preview @Composable private fun LegacyCityBelarusianLightPreview() = LegacyCityPreview(false, true)
@Preview @Composable private fun LegacyCityBelarusianDarkPreview() = LegacyCityPreview(true, true)
