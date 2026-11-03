package com.coffeepeek.admin.ui.screen.addshop

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.resources.coffeePeekStringArrayResource as stringArrayResource

enum class AddShopLocationHint { Unavailable, AddressMissing, PermissionRequired }

internal data class AddShopLocationLabels(val unavailable: String, val addressMissing: String, val permissionRequired: String)

@Composable
internal fun addShopLocationLabels() = AddShopLocationLabels(
    stringResource(Res.string.legacy_location_unavailable),
    stringResource(Res.string.legacy_location_address_missing),
    stringResource(Res.string.legacy_location_permission_required),
)

@Composable
internal fun addShopLocationMessage(hint: AddShopLocationHint): String = hint.message(addShopLocationLabels())

internal fun AddShopLocationHint.message(labels: AddShopLocationLabels): String = when (this) {
    AddShopLocationHint.Unavailable -> labels.unavailable
    AddShopLocationHint.AddressMissing -> labels.addressMissing
    AddShopLocationHint.PermissionRequired -> labels.permissionRequired
}

@Composable
internal fun addShopWeekdayLabel(day: Int): String = stringArrayResource(Res.array.legacy_weekdays_full)[(day + 6) % 7]
