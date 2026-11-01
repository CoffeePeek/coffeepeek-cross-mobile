package com.coffeepeek.admin.ui.screen.shop

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import com.coffeepeek.admin.ui.component.CpTopBar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.platformTextInputOptions
import com.coffeepeek.admin.ui.component.rememberSyncedTextFieldValue
import com.coffeepeek.admin.ui.component.limitTextLength
import com.coffeepeek.admin.ui.component.updateFromInput
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopReportScreen(shopId: String, shopTitle: String) {
    val localizedLegacyShopReportScreenShopReportTitle = stringResource(Res.string.legacy_shop_report_screen_shop_report_title)
    val localizedLegacyReviewContentOwnershipKindShop = stringResource(Res.string.legacy_review_content_ownership_kind_shop)
    val localizedLegacyShopReportScreenShopReportPrompt = stringResource(Res.string.legacy_shop_report_screen_shop_report_prompt)
    val localizedLegacyShopReportScreenShopReportDescriptionLabel = stringResource(Res.string.legacy_shop_report_screen_shop_report_description_label)
    val localizedLegacyShopReportScreenShopReportDescriptionLimit = stringResource(Res.string.legacy_shop_report_screen_shop_report_description_limit)
    val localizedLegacyReviewReportScreenCommunityReportSend = stringResource(Res.string.legacy_review_report_screen_community_report_send)

    val vm: ShopReportViewModel = platformViewModel(parameters = { parametersOf(shopId) })
    val state by vm.state.collectAsState()
    val errorMessage = state.validation?.message(shopReportValidationLabels()) ?: state.error

    Scaffold(
        topBar = {
            CpTopBar(localizedLegacyShopReportScreenShopReportTitle)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()).coffeePeekScreenBottomPadding()
                .padding(CpDimens.spacing4),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
        ) {
            if (state.isSubmitted) {
                ReportSubmittedContent(shopTitle = shopTitle)
                return@Column
            }

            Text(
                text = shopTitle.ifBlank { localizedLegacyReviewContentOwnershipKindShop },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = localizedLegacyShopReportScreenShopReportPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(CpDimens.radius2xl),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.selectableGroup()) {
                    ShopReportReason.entries.forEachIndexed { index, reason ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = CpDimens.spacing4),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        ReasonRow(
                            reason = reason,
                            selected = state.selectedReason == reason,
                            onClick = { vm.selectReason(reason) },
                        )
                    }
                }
            }

            if (state.selectedReason == ShopReportReason.OTHER) {
                val commentField = rememberSyncedTextFieldValue(state.comment)
                OutlinedTextField(
                    value = commentField.value,
                    onValueChange = { updated ->
                        val limited = updated.limitTextLength(500)
                        commentField.updateFromInput(limited, vm::updateComment)
                    },
                    keyboardOptions = platformTextInputOptions(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(localizedLegacyShopReportScreenShopReportDescriptionLabel) },
                    supportingText = { Text(localizedLegacyShopReportScreenShopReportDescriptionLimit) },
                    minLines = 3,
                    maxLines = 6,
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(CpDimens.buttonRadius),
                )
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(CpDimens.spacing2))
            if (state.isSubmitting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CoffeePeekLoader()
                }
            } else {
                AppButton(
                    text = localizedLegacyReviewReportScreenCommunityReportSend,
                    onClick = vm::submit,
                    enabled = state.selectedReason != null,
                )
            }
        }
    }
}

@Composable
private fun ReportSubmittedContent(shopTitle: String) {
    val localizedLegacyShopReportScreenShopReportThanks = stringResource(Res.string.legacy_shop_report_screen_shop_report_thanks)
    val localizedStepDone = stringResource(Res.string.step_done)

    Spacer(Modifier.height(48.dp))
    Icon(
        imageVector = CpIcons.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = localizedLegacyShopReportScreenShopReportThanks,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
    Text(
        text = stringResource(Res.string.legacy_shop_report_submitted, shopTitle.ifBlank { stringResource(Res.string.legacy_shop_report_fallback_name) }),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(CpDimens.spacing3))
    AppButton(text = localizedStepDone, onClick = Navigator::popBack)
}

@Composable
private fun ReasonRow(
    reason: ShopReportReason,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3),
    ) {
        Text(
            text = reason.localizedTitle(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(
                    imageVector = CpIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
