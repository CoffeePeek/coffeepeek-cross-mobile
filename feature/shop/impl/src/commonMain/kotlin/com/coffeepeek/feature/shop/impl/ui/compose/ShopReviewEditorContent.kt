package com.coffeepeek.feature.shop.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.AppButton
import com.coffeepeek.core.designsystem.component.AppTextField
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.usecase.ShopReviewFieldError
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_review_coffee
import com.coffeepeek.feature.shop.impl.resources.shop_review_comment
import com.coffeepeek.feature.shop.impl.resources.shop_review_comment_hint
import com.coffeepeek.feature.shop.impl.resources.shop_review_comment_required
import com.coffeepeek.feature.shop.impl.resources.shop_review_comment_short
import com.coffeepeek.feature.shop.impl.resources.shop_review_comment_long
import com.coffeepeek.feature.shop.impl.resources.shop_review_create_title
import com.coffeepeek.feature.shop.impl.resources.shop_review_edit_title
import com.coffeepeek.feature.shop.impl.resources.shop_review_header
import com.coffeepeek.feature.shop.impl.resources.shop_review_header_hint
import com.coffeepeek.feature.shop.impl.resources.shop_review_header_required
import com.coffeepeek.feature.shop.impl.resources.shop_review_header_short
import com.coffeepeek.feature.shop.impl.resources.shop_review_header_long
import com.coffeepeek.feature.shop.impl.resources.shop_review_place
import com.coffeepeek.feature.shop.impl.resources.shop_review_service
import com.coffeepeek.feature.shop.impl.resources.shop_review_submit_create
import com.coffeepeek.feature.shop.impl.resources.shop_review_submit_edit
import com.coffeepeek.feature.shop.impl.resources.shop_review_edit_unavailable
import com.coffeepeek.feature.shop.impl.ui.compose.component.ShopReviewRatingField
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormAction
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormMode
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewFormState
import com.coffeepeek.feature.shop.impl.ui.compose.model.ShopReviewRatingKind
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Stateless editor body. Photo picker/attachments will be added before runtime integration. */
@Composable
internal fun ShopReviewEditorContent(
    state: ShopReviewFormState,
    onAction: (ShopReviewFormAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = !state.isLoading && !state.isSubmitting && state.canEdit
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(CpDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
    ) {
        Text(
            stringResource(if (state.mode == ShopReviewFormMode.Create) Res.string.shop_review_create_title
                else Res.string.shop_review_edit_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        state.shopName?.takeIf(String::isNotBlank)?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            ShopReviewRatingField(stringResource(Res.string.shop_review_coffee), state.rating.coffee,
                editable) { onAction(ShopReviewFormAction.RatingChanged(ShopReviewRatingKind.Coffee, it)) }
            ShopReviewRatingField(stringResource(Res.string.shop_review_service), state.rating.service,
                editable) { onAction(ShopReviewFormAction.RatingChanged(ShopReviewRatingKind.Service, it)) }
            ShopReviewRatingField(stringResource(Res.string.shop_review_place), state.rating.place,
                editable) { onAction(ShopReviewFormAction.RatingChanged(ShopReviewRatingKind.Place, it)) }
            AppTextField(
                label = stringResource(Res.string.shop_review_header),
                value = state.header,
                onValueChange = { onAction(ShopReviewFormAction.HeaderChanged(it)) },
                placeholder = stringResource(Res.string.shop_review_header_hint),
                errorText = headerError(state.headerError),
                enabled = editable,
            )
            Column {
                Text(stringResource(Res.string.shop_review_comment),
                    style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = { onAction(ShopReviewFormAction.CommentChanged(it)) },
                    placeholder = { Text(stringResource(Res.string.shop_review_comment_hint)) },
                    isError = state.commentError != null,
                    minLines = 4,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth(),
                )
                commentError(state.commentError)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!state.canEdit) Text(stringResource(Res.string.shop_review_edit_unavailable),
                color = MaterialTheme.colorScheme.error)
            state.submitError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (state.isSubmitting) CircularProgressIndicator()
            else AppButton(
                text = stringResource(if (state.mode == ShopReviewFormMode.Create)
                    Res.string.shop_review_submit_create else Res.string.shop_review_submit_edit),
                onClick = { onAction(ShopReviewFormAction.Submit) },
                enabled = editable,
            )
        }
    }
}

@Composable
private fun headerError(error: ShopReviewFieldError?): String? = when (error) {
    ShopReviewFieldError.Required -> stringResource(Res.string.shop_review_header_required)
    ShopReviewFieldError.TooShort -> stringResource(Res.string.shop_review_header_short)
    ShopReviewFieldError.TooLong -> stringResource(Res.string.shop_review_header_long)
    null -> null
}

@Composable
private fun commentError(error: ShopReviewFieldError?): String? = when (error) {
    ShopReviewFieldError.Required -> stringResource(Res.string.shop_review_comment_required)
    ShopReviewFieldError.TooShort -> stringResource(Res.string.shop_review_comment_short)
    ShopReviewFieldError.TooLong -> stringResource(Res.string.shop_review_comment_long)
    null -> null
}

@Preview @Composable private fun ShopReviewEditorLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReviewEditorContent(ShopReviewFormState(shopName = "Кофейня"), onAction = {})
}

@Preview @Composable private fun ShopReviewEditorDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReviewEditorContent(ShopReviewFormState(mode = ShopReviewFormMode.Edit,
        shopName = "Кофейня", header = "Хороший кофе", comment = "Очень понравился фильтр."), onAction = {})
}
