package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.AddRoasterHost
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.RoasterSubmitted
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.RoasterIssue
import com.coffeepeek.feature.contributions.impl.R
import com.coffeepeek.feature.contributions.impl.roaster.AddRoasterAction
import com.coffeepeek.feature.contributions.impl.roaster.AddRoasterState
import com.coffeepeek.feature.contributions.impl.roaster.AddRoasterViewModel
import com.coffeepeek.feature.contributions.impl.roaster.RoasterSubmitState

internal class AddRoasterCallbacks(val onBack: () -> Unit, val onSignIn: () -> Unit)

@Composable
internal fun AddRoasterScreen(viewModel: AddRoasterViewModel, host: AddRoasterHost, callbacks: AddRoasterCallbacks, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AddRoasterContent(state, host, callbacks, viewModel::onAction, modifier)
}

@Composable
internal fun AddRoasterContent(state: AddRoasterState, host: AddRoasterHost, callbacks: AddRoasterCallbacks, onAction: (AddRoasterAction) -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.contributions_roaster_title)
    val submit = state.submit
    when {
        state.signedIn == null -> Box(modifier.fillMaxSize(), Alignment.Center) { CoffeePeekLoader(stringResource(R.string.contributions_loading)) }
        state.signedIn == false -> FormSignInPrompt(title, stringResource(R.string.contributions_roaster_sign_in_prompt), callbacks.onBack, callbacks.onSignIn, modifier)
        submit is RoasterSubmitState.Done -> RoasterSuccess(submit.result, state.address.isNotBlank(), host, callbacks.onBack, modifier)
        submit == RoasterSubmitState.Sending -> SubmissionStatusScreen(
            title, SubmissionStatus.Sending, stringResource(R.string.contributions_roaster_sending_title),
            stringResource(R.string.contributions_roaster_sending_description), callbacks.onBack, host.feedback, modifier,
        )
        submit is RoasterSubmitState.Failed -> SubmissionStatusScreen(
            title, SubmissionStatus.Error, stringResource(R.string.contributions_roaster_failed_title),
            "${submit.serverMessage ?: stringResource(R.string.contributions_roaster_failed_default)}\n\n${stringResource(R.string.contributions_shop_failed_kept)}",
            { onAction(AddRoasterAction.DismissFailure) }, host.feedback, modifier,
        ) {
            SubmissionErrorActions(
                stringResource(R.string.contributions_shop_retry_send), stringResource(R.string.contributions_shop_back_to_form),
                { onAction(AddRoasterAction.Submit) }, { onAction(AddRoasterAction.DismissFailure) },
            )
        }
        else -> RoasterForm(state, host, callbacks, onAction, modifier)
    }
}

@Composable
private fun RoasterForm(state: AddRoasterState, host: AddRoasterHost, callbacks: AddRoasterCallbacks, onAction: (AddRoasterAction) -> Unit, modifier: Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(stringResource(R.string.contributions_roaster_title), stringResource(R.string.contributions_back), onBack = callbacks.onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4)) {
            val issues = state.issues
            RequiredLegend()
            Spacer(Modifier.height(CoffeePeekDimens.spacing3))
            FormField(stringResource(R.string.contributions_roaster_name_label), required = true) {
                OutlinedField(
                    state.name, { onAction(AddRoasterAction.Name(it)) }, stringResource(R.string.contributions_roaster_name_placeholder),
                    errorText = stringResource(R.string.contributions_roaster_error_name_too_long, ContributionLimits.ROASTER_NAME).takeIf { RoasterIssue.NameTooLong in issues },
                    counter = "${state.name.length}/${ContributionLimits.ROASTER_NAME}",
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            FormField(stringResource(R.string.contributions_roaster_about_label), optional = true) {
                OutlinedField(state.about, { onAction(AddRoasterAction.About(it)) }, stringResource(R.string.contributions_roaster_about_placeholder), minLines = 3, maxLines = 6)
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Text(stringResource(R.string.contributions_roaster_location_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(CoffeePeekDimens.spacing3))
            FormField(stringResource(R.string.contributions_shop_city_label), optional = true) {
                CityFieldOrState(state, onAction, RoasterIssue.CityMissing in issues)
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            FormField(stringResource(R.string.contributions_shop_address_label), optional = true) {
                OutlinedField(
                    state.address, { onAction(AddRoasterAction.Address(it)) }, stringResource(R.string.contributions_shop_address_placeholder),
                    errorText = stringResource(R.string.contributions_shop_error_address_blank).takeIf { RoasterIssue.AddressMissing in issues },
                    leadingIcon = CoffeePeekIcons.Navigation,
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            FormField(stringResource(R.string.contributions_shop_instagram_label), optional = true) {
                OutlinedField(
                    state.instagram, { onAction(AddRoasterAction.Instagram(it)) }, stringResource(R.string.contributions_shop_instagram_placeholder),
                    errorText = stringResource(R.string.contributions_error_instagram).takeIf { RoasterIssue.Instagram in issues },
                    keyboardType = KeyboardType.Uri, leadingIcon = CoffeePeekIcons.Instagram,
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            FormField(stringResource(R.string.contributions_shop_website_label), optional = true) {
                OutlinedField(
                    state.website, { onAction(AddRoasterAction.Website(it)) }, stringResource(R.string.contributions_shop_website_placeholder),
                    errorText = stringResource(R.string.contributions_error_website).takeIf { RoasterIssue.Website in issues },
                    keyboardType = KeyboardType.Uri, leadingIcon = CoffeePeekIcons.Globe,
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            host.photos(
                state.photos, ContributionLimits.ROASTER_PHOTOS, stringResource(R.string.contributions_roaster_photos_title),
                stringResource(R.string.contributions_roaster_photos_hint, ContributionLimits.ROASTER_PHOTOS),
                { onAction(AddRoasterAction.AddPhotos(it)) }, { onAction(AddRoasterAction.RemovePhoto(it)) },
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            CoffeePeekButton(stringResource(R.string.contributions_roaster_submit), { onAction(AddRoasterAction.Submit) }, enabled = state.valid)
            Spacer(Modifier.height(CoffeePeekDimens.spacing8))
        }
    }
}

@Composable
private fun CityFieldOrState(state: AddRoasterState, onAction: (AddRoasterAction) -> Unit, missing: Boolean) {
    when {
        state.citiesLoading -> CoffeePeekLoader(stringResource(R.string.contributions_loading), Modifier.size(24.dp), strokeWidth = 2.dp)
        state.citiesFailed -> Column {
            Text(stringResource(R.string.contributions_shop_catalogs_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            CoffeePeekButton(stringResource(R.string.contributions_retry), { onAction(AddRoasterAction.RetryCities) })
        }
        state.cities.size == 1 -> ReadOnlyField(state.cities.first().name)
        else -> CityField(state.cities, state.cityId, { onAction(AddRoasterAction.City(it)) }, stringResource(R.string.contributions_shop_error_city_missing).takeIf { missing })
    }
}

@Composable
private fun RoasterSuccess(result: RoasterSubmitted, hadAddress: Boolean, host: AddRoasterHost, onDone: () -> Unit, modifier: Modifier) {
    LaunchedEffect(Unit) { host.feedback(true) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4)) { CoffeePeekButton(stringResource(R.string.contributions_roaster_done), onDone) } },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            Image(painterResource(R.drawable.contributions_mascot_happy), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(CoffeePeekIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Text(
                stringResource(R.string.contributions_roaster_success_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            Text(
                result.message.ifBlank { stringResource(R.string.contributions_roaster_success_description) }, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            if (hadAddress && !result.isAddressValidated) {
                Spacer(Modifier.height(CoffeePeekDimens.spacing3))
                Text(
                    stringResource(R.string.contributions_roaster_address_unchecked), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing8))
        }
    }
}

private val noCallbacks = AddRoasterCallbacks({}, {})
private val cities = listOf(CatalogOption("minsk", "Минск"), CatalogOption("brest", "Брест"))

@PreviewLightDark @Composable private fun AddRoasterFormPreview() = CoffeePeekTheme {
    Surface { AddRoasterContent(AddRoasterState(signedIn = true, citiesLoading = false, cities = cities, name = "Sandbox Roaster", cityId = "minsk"), AddRoasterHost(), noCallbacks, {}) }
}

@PreviewLightDark @Composable private fun AddRoasterFailedPreview() = CoffeePeekTheme {
    Surface { AddRoasterContent(AddRoasterState(signedIn = true, citiesLoading = false, submit = RoasterSubmitState.Failed(null)), AddRoasterHost(), noCallbacks, {}) }
}

@PreviewLightDark @Composable private fun AddRoasterSuccessPreview() = CoffeePeekTheme {
    Surface {
        AddRoasterContent(
            AddRoasterState(signedIn = true, address = "Улица, 1", submit = RoasterSubmitState.Done(RoasterSubmitted("id", "Pending", isAddressValidated = false, message = "Обжарщик отправлен на модерацию"))),
            AddRoasterHost(), noCallbacks, {},
        )
    }
}
