package com.coffeepeek.admin.ui.screen.contributions

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

sealed interface ContributionFieldIssue {
    data object NameRequired : ContributionFieldIssue
    data class NameTooLong(val limit: Int) : ContributionFieldIssue
    data object AddressRequired : ContributionFieldIssue
    data object CityRequired : ContributionFieldIssue
    data object CityForAddress : ContributionFieldIssue
    data object AddressForCity : ContributionFieldIssue
}

internal data class ContributionFieldLabels(
    val nameRequired: String,
    val nameTooLong: String,
    val addressRequired: String,
    val cityRequired: String,
    val cityForAddress: String,
    val addressForCity: String,
)

@Composable
internal fun contributionFieldLabels() = ContributionFieldLabels(
    stringResource(Res.string.legacy_form_name_required),
    stringResource(Res.string.legacy_form_name_too_long),
    stringResource(Res.string.legacy_form_address_required),
    stringResource(Res.string.select_city_placeholder),
    stringResource(Res.string.legacy_form_city_for_address),
    stringResource(Res.string.legacy_form_address_for_city),
)

@Composable
internal fun contributionFieldMessage(issue: ContributionFieldIssue?): String? = issue?.message(contributionFieldLabels())

internal fun ContributionFieldIssue.message(labels: ContributionFieldLabels): String = when (this) {
    ContributionFieldIssue.NameRequired -> labels.nameRequired
    is ContributionFieldIssue.NameTooLong -> labels.nameTooLong.replace("%1\$s", limit.toString())
    ContributionFieldIssue.AddressRequired -> labels.addressRequired
    ContributionFieldIssue.CityRequired -> labels.cityRequired
    ContributionFieldIssue.CityForAddress -> labels.cityForAddress
    ContributionFieldIssue.AddressForCity -> labels.addressForCity
}

enum class ContributionFailureKind { ShopCatalogLoad, ShopSubmit, RoasterCityLoad, RoasterSubmit, ChangeSubmit }

data class ContributionFormFailure(val kind: ContributionFailureKind, val provided: String? = null)

internal data class ContributionFailureLabels(
    val shopCatalogLoad: String,
    val shopSubmit: String,
    val roasterCityLoad: String,
    val roasterSubmit: String,
    val changeSubmit: String,
)

@Composable
internal fun contributionFailureLabels() = ContributionFailureLabels(
    stringResource(Res.string.legacy_form_shop_catalog_failed),
    stringResource(Res.string.legacy_form_shop_submit_failed),
    stringResource(Res.string.legacy_add_roaster_screen_settings_city_error),
    stringResource(Res.string.legacy_form_roaster_submit_failed),
    stringResource(Res.string.legacy_form_change_submit_failed),
)

@Composable
internal fun contributionFailureMessage(failure: ContributionFormFailure?): String? = failure?.message(contributionFailureLabels())

internal fun ContributionFormFailure.message(labels: ContributionFailureLabels): String = provided ?: when (kind) {
    ContributionFailureKind.ShopCatalogLoad -> labels.shopCatalogLoad
    ContributionFailureKind.ShopSubmit -> labels.shopSubmit
    ContributionFailureKind.RoasterCityLoad -> labels.roasterCityLoad
    ContributionFailureKind.RoasterSubmit -> labels.roasterSubmit
    ContributionFailureKind.ChangeSubmit -> labels.changeSubmit
}
