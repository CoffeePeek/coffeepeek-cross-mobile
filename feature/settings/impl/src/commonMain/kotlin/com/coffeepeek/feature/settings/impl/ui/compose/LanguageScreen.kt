package com.coffeepeek.feature.settings.impl.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRefreshError
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.settings.domain.AppLanguage
import com.coffeepeek.feature.settings.impl.resources.*
import com.coffeepeek.feature.settings.impl.ui.LanguageViewModel
import com.coffeepeek.feature.settings.impl.ui.model.LanguageAction
import com.coffeepeek.feature.settings.impl.ui.model.LanguageState
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun LanguageScreen(vm: LanguageViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    LanguageContent(state, { vm.onAction(LanguageAction.Select(it)) }, { vm.onAction(LanguageAction.RetrySave) }, onBack, modifier)
}

@Composable
internal fun LanguageContent(
    state: LanguageState, onSelect: (AppLanguage) -> Unit, onRetry: () -> Unit,
    onBack: () -> Unit, modifier: Modifier = Modifier, copy: LanguageCopy = languageCopy(),
) {
    CoffeePeekScaffold(modifier, topBar = { CoffeePeekTopBar(copy.title, copy.back, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(coffeePeekScrollContentPadding(padding)).verticalScroll(rememberScrollState())) {
            LanguageHeading(copy)
            LanguageOptions(state, copy, onSelect)
            if (state.isSaving) {
                Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), contentAlignment = Alignment.Center) {
                    CoffeePeekLoader(copy.saving)
                }
            } else if (state.hasSaveError) {
                CoffeePeekRefreshError(onRetry, Modifier.padding(CoffeePeekDimens.spacing4), message = copy.saveError, retryLabel = copy.retry)
            }
        }
    }
}

@Composable
private fun LanguageHeading(copy: LanguageCopy) {
    Column(Modifier.padding(horizontal = CoffeePeekDimens.spacing4)) {
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        Text(copy.heading, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(CoffeePeekDimens.spacing2))
        Text(copy.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(CoffeePeekDimens.spacing6))
    }
}

@Composable
private fun LanguageOptions(state: LanguageState, copy: LanguageCopy, onSelect: (AppLanguage) -> Unit) {
    CoffeePeekCard(
        modifier = Modifier.padding(horizontal = CoffeePeekDimens.spacing4).selectableGroup(),
        shape = RoundedCornerShape(CoffeePeekDimens.cardRadius),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = 0.dp, contentPadding = PaddingValues(0.dp),
    ) {
        AppLanguage.entries.forEachIndexed { index, language ->
            val name = if (language == AppLanguage.RUSSIAN) copy.russian else copy.belarusian
            LanguageOption(name, language == state.selected, !state.isSaving) { onSelect(language) }
            if (index != AppLanguage.entries.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun LanguageOption(name: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 64.dp).padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (selected) {
            Icon(CoffeePeekIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(CoffeePeekDimens.settingsIconSize))
        }
    }
}

internal data class LanguageCopy(
    val title: String, val heading: String, val description: String, val back: String, val retry: String,
    val saving: String, val saveError: String, val russian: String, val belarusian: String,
)

@Composable
private fun languageCopy() = LanguageCopy(
    title = stringResource(Res.string.settings_language_title),
    heading = stringResource(Res.string.settings_language_heading),
    description = stringResource(Res.string.settings_language_description),
    back = stringResource(Res.string.settings_back),
    retry = stringResource(Res.string.settings_retry),
    saving = stringResource(Res.string.settings_language_saving),
    saveError = stringResource(Res.string.settings_language_save_error),
    russian = stringResource(Res.string.settings_language_russian),
    belarusian = stringResource(Res.string.settings_language_belarusian),
)

private val russianPreviewCopy = LanguageCopy(
    title = "Язык", heading = "Язык приложения", description = "Выберите язык интерфейса. Выбор сохранится после перезапуска.",
    back = "Назад", retry = "Повторить", saving = "Сохраняем язык…",
    saveError = "Не удалось сохранить язык. Попробуйте ещё раз.", russian = "Русский", belarusian = "Беларуская",
)
private val belarusianPreviewCopy = LanguageCopy(
    title = "Мова", heading = "Мова праграмы", description = "Абярыце мову інтэрфейсу. Выбар захаваецца пасля перазапуску.",
    back = "Назад", retry = "Паўтарыць", saving = "Захоўваем мову…",
    saveError = "Не ўдалося захаваць мову. Паспрабуйце яшчэ раз.", russian = "Русский", belarusian = "Беларуская",
)

@Composable
private fun LanguagePreview(dark: Boolean, belarusian: Boolean = false, saving: Boolean = false, error: Boolean = false) =
    CoffeePeekTheme(darkTheme = dark) {
        val selected = if (belarusian) AppLanguage.BELARUSIAN else AppLanguage.RUSSIAN
        val failed = AppLanguage.entries.firstOrNull { error && it != selected }
        Surface {
            LanguageContent(LanguageState(selected, saving, failed), {}, {}, {},
                copy = if (belarusian) belarusianPreviewCopy else russianPreviewCopy)
        }
    }

@Preview @Composable private fun LanguageRussianLightPreview() = LanguagePreview(false)
@Preview @Composable private fun LanguageRussianDarkPreview() = LanguagePreview(true)
@Preview @Composable private fun LanguageBelarusianLightPreview() = LanguagePreview(false, belarusian = true)
@Preview @Composable private fun LanguageBelarusianDarkPreview() = LanguagePreview(true, belarusian = true)
@Preview @Composable private fun LanguageSavingLightPreview() = LanguagePreview(false, saving = true)
@Preview @Composable private fun LanguageSavingDarkPreview() = LanguagePreview(true, saving = true)
@Preview @Composable private fun LanguageErrorLightPreview() = LanguagePreview(false, belarusian = true, error = true)
@Preview @Composable private fun LanguageErrorDarkPreview() = LanguagePreview(true, belarusian = true, error = true)
