package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.api.GeoPoint
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.model.Catalogs
import com.coffeepeek.feature.contributions.impl.R
import com.coffeepeek.feature.contributions.impl.shop.AddShopAction
import com.coffeepeek.feature.contributions.impl.shop.AddShopState
import com.coffeepeek.feature.contributions.impl.shop.AddShopStep
import com.coffeepeek.feature.contributions.impl.shop.AddShopViewModel
import com.coffeepeek.feature.contributions.impl.shop.SubmitState

internal class AddShopCallbacks(val onBack: () -> Unit, val onSignIn: () -> Unit, val onAddRoaster: () -> Unit)

@Composable
internal fun AddShopScreen(viewModel: AddShopViewModel, host: AddShopHost, callbacks: AddShopCallbacks, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AddShopContent(state, host, callbacks, viewModel::onAction, modifier)
}

@Composable
internal fun AddShopContent(state: AddShopState, host: AddShopHost, callbacks: AddShopCallbacks, onAction: (AddShopAction) -> Unit, modifier: Modifier = Modifier) {
    val submit = state.submit
    when {
        state.signedIn == null -> Box(modifier.fillMaxSize(), Alignment.Center) { CoffeePeekLoader(stringResource(R.string.contributions_loading)) }
        state.signedIn == false -> FormSignInPrompt(
            stringResource(R.string.contributions_shop_title), stringResource(R.string.contributions_shop_sign_in_prompt), callbacks.onBack, callbacks.onSignIn, modifier,
        )
        submit == SubmitState.Done -> AddShopSuccess(state, host, callbacks.onBack, modifier)
        submit == SubmitState.Sending -> SubmissionStatusScreen(
            stringResource(R.string.contributions_shop_title), SubmissionStatus.Sending, stringResource(R.string.contributions_shop_sending_title),
            stringResource(R.string.contributions_shop_sending_description), callbacks.onBack, host.feedback, modifier,
        )
        submit is SubmitState.Failed -> FailedScreen(submit, host, callbacks, onAction, modifier)
        else -> AddShopForm(state, host, callbacks, onAction, modifier)
    }
}

@Composable
private fun FailedScreen(failed: SubmitState.Failed, host: AddShopHost, callbacks: AddShopCallbacks, onAction: (AddShopAction) -> Unit, modifier: Modifier) {
    val reason = failed.serverMessage ?: stringResource(R.string.contributions_shop_failed_default)
    SubmissionStatusScreen(
        stringResource(R.string.contributions_shop_title), SubmissionStatus.Error, stringResource(R.string.contributions_shop_failed_title),
        "$reason\n\n${stringResource(R.string.contributions_shop_failed_kept)}", { onAction(AddShopAction.DismissFailure) }, host.feedback, modifier,
    ) {
        SubmissionErrorActions(
            stringResource(R.string.contributions_shop_retry_send), stringResource(R.string.contributions_shop_back_to_form),
            { onAction(AddShopAction.Next) }, { onAction(AddShopAction.DismissFailure) },
        )
    }
}

@Composable
private fun AddShopForm(state: AddShopState, host: AddShopHost, callbacks: AddShopCallbacks, onAction: (AddShopAction) -> Unit, modifier: Modifier) {
    host.locationPermission({ onAction(AddShopAction.LocationGranted) }, { onAction(AddShopAction.LocationDenied) })
    if (state.mapPickerOpen) host.mapPicker(
        state.gpsPoint?.let { GeoPoint(it.first, it.second) },
        { point, address -> onAction(AddShopAction.MapPicked(point.latitude, point.longitude, address)) },
        { onAction(AddShopAction.DismissMap) },
    )
    Scaffold(
        modifier = modifier,
        topBar = { FormHeader(state.step, callbacks.onBack, onAction) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.catalogsLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CoffeePeekLoader(stringResource(R.string.contributions_loading)) }
            state.catalogsFailed -> Box(Modifier.fillMaxSize().padding(padding).padding(CoffeePeekDimens.spacing4), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.contributions_shop_catalogs_failed), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(CoffeePeekDimens.spacing3))
                    CoffeePeekButton(stringResource(R.string.contributions_retry), { onAction(AddShopAction.RetryCatalogs) })
                }
            }
            else -> StepContent(state, host, callbacks, onAction, Modifier.padding(padding).consumeWindowInsets(padding))
        }
    }
}

@Composable
private fun FormHeader(step: AddShopStep, onBack: () -> Unit, onAction: (AddShopAction) -> Unit) {
    Column(Modifier.padding(bottom = CoffeePeekDimens.spacing2)) {
        CoffeePeekTopBar(
            step.title(), stringResource(R.string.contributions_back),
            onBack = if (step == AddShopStep.Basic) onBack else ({ onAction(AddShopAction.Previous) }),
        )
        StepDots(step.ordinal, AddShopStep.entries.size, Modifier.fillMaxWidth().padding(top = CoffeePeekDimens.spacing1))
    }
}

@Composable
private fun StepContent(state: AddShopState, host: AddShopHost, callbacks: AddShopCallbacks, onAction: (AddShopAction) -> Unit, modifier: Modifier) {
    AnimatedContent(
        targetState = state.step, modifier = modifier,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            slideInHorizontally { if (forward) it else -it } togetherWith slideOutHorizontally { if (forward) -it else it }
        },
        label = "add-shop-step",
    ) { step ->
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4)) {
            when (step) {
                AddShopStep.Basic -> StepBasic(state, host, onAction)
                AddShopStep.Contacts -> StepContacts(state, onAction)
                AddShopStep.Photos -> StepPhotos(state, host, onAction)
                AddShopStep.Features -> StepFeatures(state, host, callbacks.onAddRoaster, onAction)
                AddShopStep.Schedule -> StepSchedule(state, onAction)
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            CoffeePeekButton(
                stringResource(if (step == AddShopStep.Schedule) R.string.contributions_shop_submit else R.string.contributions_shop_next),
                { onAction(AddShopAction.Next) },
                enabled = when (step) {
                    AddShopStep.Basic -> state.basicsValid
                    AddShopStep.Contacts -> state.contactsValid
                    else -> true
                },
            )
            Spacer(Modifier.height(CoffeePeekDimens.spacing8))
        }
    }
}

@Composable
private fun AddShopStep.title() = stringResource(
    when (this) {
        AddShopStep.Basic -> R.string.contributions_shop_step_basic
        AddShopStep.Contacts -> R.string.contributions_shop_step_contacts
        AddShopStep.Photos -> R.string.contributions_shop_step_photos
        AddShopStep.Features -> R.string.contributions_shop_step_features
        AddShopStep.Schedule -> R.string.contributions_shop_step_schedule
    },
)

private val noCallbacks = AddShopCallbacks({}, {}, {})
private val previewCatalogs = Catalogs(
    cities = listOf(CatalogOption("minsk", "Минск"), CatalogOption("brest", "Брест")),
    brewMethods = listOf(CatalogOption("v60", "Hario V60"), CatalogOption("aero", "AeroPress")),
    roasters = listOf(CatalogOption("roast", "Sandbox Roaster")),
)

@PreviewLightDark @Composable private fun AddShopBasicPreview() = CoffeePeekTheme {
    androidx.compose.material3.Surface { AddShopContent(AddShopState(signedIn = true, catalogsLoading = false, catalogs = previewCatalogs, name = "Surf Coffee"), AddShopHost(), noCallbacks, {}) }
}

@PreviewLightDark @Composable private fun AddShopFeaturesPreview() = CoffeePeekTheme {
    androidx.compose.material3.Surface {
        AddShopContent(AddShopState(signedIn = true, catalogsLoading = false, catalogs = previewCatalogs, step = AddShopStep.Features, brewMethodIds = setOf("v60")), AddShopHost(), noCallbacks, {})
    }
}

@PreviewLightDark @Composable private fun AddShopSchedulePreview() = CoffeePeekTheme {
    androidx.compose.material3.Surface {
        AddShopContent(AddShopState(signedIn = true, catalogsLoading = false, catalogs = previewCatalogs, step = AddShopStep.Schedule, closedDays = setOf(0)), AddShopHost(), noCallbacks, {})
    }
}

@PreviewLightDark @Composable private fun AddShopFailedPreview() = CoffeePeekTheme {
    androidx.compose.material3.Surface { AddShopContent(AddShopState(signedIn = true, catalogsLoading = false, submit = SubmitState.Failed(null)), AddShopHost(), noCallbacks, {}) }
}
