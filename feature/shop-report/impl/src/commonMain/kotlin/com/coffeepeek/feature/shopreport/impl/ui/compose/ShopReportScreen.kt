package com.coffeepeek.feature.shopreport.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.impl.resources.Res
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_back
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_category_required
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_description_label
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_description_limit
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_description_required
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_done
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_unconfirmed
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_prompt
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_shop_fallback
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_submission_failed
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_submit
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_thanks
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_title
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_will_review
import com.coffeepeek.feature.shopreport.impl.ui.ShopReportViewModel
import com.coffeepeek.feature.shopreport.impl.ui.compose.component.ShopReportCategoryList
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportAction
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportError
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportEvent
import com.coffeepeek.feature.shopreport.impl.ui.compose.model.ShopReportState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReportScreen(
    viewModel: ShopReportViewModel,
    shopTitle: String,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentBack by rememberUpdatedState(onBack)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ShopReportEvent.Back -> currentBack()
            }
        }
    }
    ShopReportScreenContent(state, shopTitle, viewModel::onAction)
}

@Composable
internal fun ShopReportScreenContent(
    state: ShopReportState,
    shopTitle: String,
    onAction: (ShopReportAction) -> Unit,
) {
    Scaffold(
        topBar = {
            CoffeePeekTopBar(stringResource(Res.string.shop_report_title),
                stringResource(Res.string.shop_report_back), onBack = { onAction(ShopReportAction.Back) })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(coffeePeekScrollContentPadding(PaddingValues(CoffeePeekDimens.spacing4))),
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
        ) {
            if (state.deliveryUnconfirmed) {
                Text(stringResource(Res.string.shop_report_unconfirmed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                CoffeePeekButton(stringResource(Res.string.shop_report_back), { onAction(ShopReportAction.Back) })
                return@Column
            }
            if (state.isSubmitted) {
                Text(stringResource(Res.string.shop_report_thanks),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(stringResource(Res.string.shop_report_will_review, shopTitle.ifBlank {
                    stringResource(Res.string.shop_report_shop_fallback)
                }), color = MaterialTheme.colorScheme.onSurfaceVariant)
                CoffeePeekButton(stringResource(Res.string.shop_report_done),
                    { onAction(ShopReportAction.Back) })
                return@Column
            }

            Text(shopTitle.ifBlank { stringResource(Res.string.shop_report_shop_fallback) },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(stringResource(Res.string.shop_report_prompt), color = MaterialTheme.colorScheme.onSurfaceVariant)

            ShopReportCategoryList(state.selectedCategory,
                onSelect = { onAction(ShopReportAction.SelectCategory(it)) }, enabled = !state.isSubmitting)

            if (state.selectedCategory == ShopIssueCategory.Other) {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onAction(ShopReportAction.ChangeDescription(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.shop_report_description_label)) },
                    supportingText = { Text(stringResource(Res.string.shop_report_description_limit)) },
                    minLines = 3,
                    maxLines = 6,
                    isError = state.error == ShopReportError.DescriptionRequired,
                    shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
                )
            }

            state.error?.let { error ->
                Text(stringResource(error.label()), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            if (state.isSubmitting) {
                CircularProgressIndicator()
            } else {
                CoffeePeekButton(stringResource(Res.string.shop_report_submit),
                    { onAction(ShopReportAction.Submit) }, enabled = state.selectedCategory != null)
            }
        }
    }
}

@Composable
private fun ShopReportError.label() = when (this) {
    ShopReportError.CategoryRequired -> Res.string.shop_report_category_required
    ShopReportError.DescriptionRequired -> Res.string.shop_report_description_required
    ShopReportError.SubmissionFailed -> Res.string.shop_report_submission_failed
}

@Preview @Composable private fun ShopReportUnconfirmedLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReportScreenContent(ShopReportState(deliveryUnconfirmed = true), "Кофейня", {})
}

@Preview @Composable private fun ShopReportUnconfirmedDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReportScreenContent(ShopReportState(deliveryUnconfirmed = true), "Кофейня", {})
}

@Preview @Composable private fun ShopReportScreenLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReportScreenContent(
        ShopReportState(selectedCategory = ShopIssueCategory.Other, description = "Неверный адрес"),
        "Кофейня на углу", {},
    )
}

@Preview @Composable private fun ShopReportScreenDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReportScreenContent(
        ShopReportState(selectedCategory = ShopIssueCategory.ShopClosed, error = ShopReportError.SubmissionFailed),
        "Кофейня на углу", {},
    )
}
