package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekActionRow
import com.coffeepeek.core.designsystem.component.CoffeePeekCheckmarkRow
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.component.CoffeePeekStepperRow
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitchRow
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.DayInput
import com.coffeepeek.feature.contributions.domain.rules.WeekOrder
import com.coffeepeek.feature.contributions.impl.R
import com.coffeepeek.feature.contributions.impl.shop.AddShopAction
import com.coffeepeek.feature.contributions.impl.shop.AddShopState

@Composable
internal fun StepPhotos(state: AddShopState, host: AddShopHost, onAction: (AddShopAction) -> Unit) {
    OptionalLegend()
    host.photos(
        state.photos, ContributionLimits.SHOP_PHOTOS, stringResource(R.string.contributions_shop_photos_title),
        stringResource(R.string.contributions_shop_photos_hint, ContributionLimits.SHOP_PHOTOS),
        { onAction(AddShopAction.AddPhotos(it)) }, { onAction(AddShopAction.RemovePhoto(it)) },
    )
    Spacer(Modifier.height(CoffeePeekDimens.spacing6))
    host.photos(
        state.menuPhotos, ContributionLimits.MENU_PHOTOS, stringResource(R.string.contributions_shop_menu_photos_title),
        stringResource(R.string.contributions_shop_menu_photos_hint, ContributionLimits.MENU_PHOTOS),
        { onAction(AddShopAction.AddMenuPhotos(it)) }, { onAction(AddShopAction.RemoveMenuPhoto(it)) },
    )
}

@Composable
internal fun StepFeatures(state: AddShopState, host: AddShopHost, onAddRoaster: () -> Unit, onAction: (AddShopAction) -> Unit) {
    OptionalLegend()
    Text(
        stringResource(R.string.contributions_shop_features_intro), style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = CoffeePeekDimens.spacing4),
    )
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
        val catalogs = state.catalogs
        CheckmarkSection(stringResource(R.string.contributions_shop_brew_methods), catalogs.brewMethods, state.brewMethodIds, { onAction(AddShopAction.ToggleBrewMethod(it)) },
            leading = { item -> Box(Modifier.size(22.dp)) { host.brewMethodIcon(item.name) } })
        CheckmarkSection(stringResource(R.string.contributions_shop_beans), catalogs.beans, state.beanIds, { onAction(AddShopAction.ToggleBean(it)) })
        CheckmarkSection(
            stringResource(R.string.contributions_shop_roasters), catalogs.roasters, state.roasterIds, { onAction(AddShopAction.ToggleRoaster(it)) },
            footer = stringResource(R.string.contributions_shop_roasters_footer), leading = { item -> host.roasterLogo(item) },
            extraRows = { CoffeePeekActionRow(stringResource(R.string.contributions_shop_add_roaster), onAddRoaster, CoffeePeekIcons.Add) },
        )
        CheckmarkSection(stringResource(R.string.contributions_shop_equipment), catalogs.equipment, state.equipmentIds, { onAction(AddShopAction.ToggleEquipment(it)) })
    }
}

@Composable
private fun CheckmarkSection(
    title: String,
    items: List<CatalogOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    footer: String? = null,
    leading: (@Composable (CatalogOption) -> Unit)? = null,
    extraRows: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty() && extraRows == null) return
    CoffeePeekGroupSection(title = title, footer = footer) {
        items.forEachIndexed { index, item ->
            if (index > 0) CoffeePeekRowSeparator()
            CoffeePeekCheckmarkRow(item.name, item.id in selected, { onToggle(item.id) }, leading?.let { { it(item) } })
        }
        if (extraRows != null) {
            if (items.isNotEmpty()) CoffeePeekRowSeparator()
            extraRows()
        }
    }
}

private val Presets = listOf("08:00" to "20:00", "09:00" to "21:00", "10:00" to "22:00")

@Composable
internal fun StepSchedule(state: AddShopState, onAction: (AddShopAction) -> Unit) {
    OptionalLegend()
    Text(
        stringResource(R.string.contributions_shop_schedule_intro), style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = CoffeePeekDimens.spacing4),
    )
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing6)) {
        CoffeePeekGroupSection(stringResource(R.string.contributions_shop_mode), footer = stringResource(R.string.contributions_shop_mode_footer)) {
            CoffeePeekSwitchRow(stringResource(R.string.contributions_shop_per_day), state.perDay) { onAction(AddShopAction.PerDay(it)) }
        }
        if (state.perDay) state.days.forEach { DaySection(it, onAction) } else UnifiedSchedule(state, onAction)
    }
}

@Composable
private fun UnifiedSchedule(state: AddShopState, onAction: (AddShopAction) -> Unit) {
    CoffeePeekGroupSection(stringResource(R.string.contributions_shop_hours)) {
        TimeRow(R.string.contributions_shop_opening, state.open) { onAction(AddShopAction.AdjustOpen(it)) }
        CoffeePeekRowSeparator()
        TimeRow(R.string.contributions_shop_closing, state.close) { onAction(AddShopAction.AdjustClose(it)) }
    }
    CoffeePeekGroupSection(stringResource(R.string.contributions_shop_presets)) {
        Presets.forEachIndexed { index, (open, close) ->
            if (index > 0) CoffeePeekRowSeparator()
            CoffeePeekCheckmarkRow(stringResource(R.string.contributions_shop_preset_label, open, close), state.open == open && state.close == close, onToggle = { onAction(AddShopAction.Preset(open, close)) })
        }
    }
    CoffeePeekGroupSection(
        stringResource(R.string.contributions_shop_work_days), trailing = stringResource(R.string.contributions_shop_work_days_count, 7 - state.closedDays.size),
        footer = stringResource(R.string.contributions_shop_work_days_footer),
    ) {
        WeekOrder.forEachIndexed { index, day ->
            if (index > 0) CoffeePeekRowSeparator()
            CoffeePeekCheckmarkRow(dayName(day), day !in state.closedDays, onToggle = { onAction(AddShopAction.ToggleClosedDay(day)) })
        }
    }
}

@Composable
private fun DaySection(day: DayInput, onAction: (AddShopAction) -> Unit) {
    CoffeePeekGroupSection(dayName(day.day)) {
        CoffeePeekSwitchRow(stringResource(R.string.contributions_shop_open), !day.closed) { onAction(AddShopAction.SetDayOpen(day.day, it)) }
        if (!day.closed) {
            CoffeePeekRowSeparator()
            TimeRow(R.string.contributions_shop_opening, day.open) { onAction(AddShopAction.AdjustDayOpen(day.day, it)) }
            CoffeePeekRowSeparator()
            TimeRow(R.string.contributions_shop_closing, day.close) { onAction(AddShopAction.AdjustDayClose(day.day, it)) }
        }
    }
}

/** A time moved in half-hour steps. */
@Composable
private fun TimeRow(label: Int, value: String, onAdjust: (Int) -> Unit) {
    val text = stringResource(label)
    CoffeePeekStepperRow(
        label = text, value = value, onDecrease = { onAdjust(-30) }, onIncrease = { onAdjust(30) },
        decreaseDescription = stringResource(R.string.contributions_shop_earlier, text), increaseDescription = stringResource(R.string.contributions_shop_later, text),
    )
}

@Composable
private fun dayName(day: Int) = stringResource(
    when (day) {
        1 -> R.string.contributions_shop_day_1
        2 -> R.string.contributions_shop_day_2
        3 -> R.string.contributions_shop_day_3
        4 -> R.string.contributions_shop_day_4
        5 -> R.string.contributions_shop_day_5
        6 -> R.string.contributions_shop_day_6
        else -> R.string.contributions_shop_day_0
    },
)
