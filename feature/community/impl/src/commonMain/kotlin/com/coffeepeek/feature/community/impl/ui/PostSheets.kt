package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectOption
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.domain.model.DrinkOption
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.rules.EditIssue
import com.coffeepeek.feature.community.domain.rules.MAX_DRINK_NAME_LENGTH
import com.coffeepeek.feature.community.domain.rules.MAX_NOTE_LENGTH
import com.coffeepeek.feature.community.domain.rules.MAX_REPORT_LENGTH
import com.coffeepeek.feature.community.domain.rules.OTHER_DRINK
import com.coffeepeek.feature.community.domain.rules.ReportIssue
import com.coffeepeek.feature.community.impl.resources.*
import com.coffeepeek.feature.community.impl.timeline.CommunityAction
import com.coffeepeek.feature.community.impl.timeline.CommunityState
import com.coffeepeek.feature.community.impl.timeline.EditError
import com.coffeepeek.feature.community.impl.timeline.PostEditState

@Composable
internal fun EditSheet(state: CommunityState, onAction: (CommunityAction) -> Unit) {
    val edit = state.editing ?: return
    CoffeePeekSwipeDismissModalBottomSheet(onDismissRequest = { onAction(CommunityAction.DismissEdit) },
        dismissDescription = stringResource(Res.string.community_close), dismissEnabled = !state.isSaving) {
        EditSheetContent(edit, state.drinks, state.drinksFailed, state.isSaving, state.source().visibility == PostVisibility.Public, onAction)
    }
}

private fun CommunityState.source() = editing!!.source

@Composable
internal fun EditSheetContent(edit: PostEditState, drinks: List<DrinkOption>, drinksFailed: Boolean, saving: Boolean, public: Boolean, onAction: (CommunityAction) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.community_edit_title), style = MaterialTheme.typography.headlineSmall)
        Text(edit.source.shopName.ifBlank { stringResource(Res.string.community_shop_default) }, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.community_edit_kept), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (public) Text(stringResource(Res.string.community_edit_review), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RatingCards(edit.rating) { onAction(CommunityAction.EditRating(it)) }
        Text(stringResource(Res.string.community_note_label), style = MaterialTheme.typography.labelMedium)
        TextArea(edit.text, { onAction(CommunityAction.EditText(it)) }, stringResource(Res.string.community_note_placeholder), MAX_NOTE_LENGTH, isError = edit.error != null)
        DrinkField(edit, drinks, drinksFailed, onAction)
        edit.error?.let { Text(editErrorText(it), color = MaterialTheme.colorScheme.error) }
        CoffeePeekButton(stringResource(if (saving) Res.string.community_saving else Res.string.community_save), { onAction(CommunityAction.SaveEdit) }, Modifier.fillMaxWidth(), enabled = !saving)
        TextButton({ onAction(CommunityAction.DismissEdit) }, Modifier.fillMaxWidth(), enabled = !saving) { Text(stringResource(Res.string.community_cancel)) }
    }
}

@Composable
private fun editErrorText(error: EditError): String = when (error) {
    is EditError.Server -> error.message ?: stringResource(Res.string.community_edit_save_failed)
    is EditError.Invalid -> stringResource(
        when (error.issue) {
            EditIssue.NoteTooLong -> Res.string.community_issue_note_too_long
            EditIssue.RatingOutOfRange -> Res.string.community_issue_rating
            EditIssue.DrinkNameRequired -> Res.string.community_issue_drink_required
            EditIssue.DrinkNameNotAllowed -> Res.string.community_issue_drink_not_allowed
        },
    )
}

@Composable
private fun RatingCards(rating: PostRating, onChange: (PostRating) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(Res.string.community_ratings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(Res.string.community_ratings_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RatingCard(Res.drawable.community_rating_coffee, stringResource(Res.string.community_rating_coffee), rating.coffee) { onChange(rating.copy(coffee = it)) }
        RatingCard(Res.drawable.community_rating_service, stringResource(Res.string.community_rating_service), rating.service) { onChange(rating.copy(service = it)) }
        RatingCard(Res.drawable.community_rating_atmosphere, stringResource(Res.string.community_rating_atmosphere), rating.place) { onChange(rating.copy(place = it)) }
    }
}

@Composable
private fun RatingCard(image: DrawableResource, label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(CoffeePeekDimens.radiusMd))
            .padding(horizontal = CoffeePeekDimens.spacing2, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Image(painterResource(image), label, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusSm)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            CoffeePeekRating(value.toDouble(), size = CoffeePeekRatingSize.Large, onRatingChange = onChange,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                starDescription = { stringResource(Res.string.community_choose_rating, label, it) })
        }
    }
}

@Composable
private fun DrinkField(edit: PostEditState, drinks: List<DrinkOption>, failed: Boolean, onAction: (CommunityAction) -> Unit) {
    val language = Locale.current.language
    fun DrinkOption.title() = if (language == "en") nameEn.ifBlank { nameRu } else nameRu.ifBlank { nameEn }
    val slug = edit.drinkSlug
    val selected = drinks.find { it.slug == slug }
    // The saved name stays while the drink is unchanged, even if the catalog no longer has it.
    val unchanged = slug == edit.source.drink?.slug
    val savedName = edit.source.drink?.takeIf { unchanged }?.let { saved ->
        if (language == "en") saved.nameEn?.takeIf(String::isNotBlank) ?: saved.nameRu else saved.nameRu?.takeIf(String::isNotBlank) ?: saved.nameEn
    }?.takeIf { !it.isNullOrBlank() }
    val none = stringResource(Res.string.community_drink_none)
    val other = stringResource(Res.string.community_drink_other)
    val label = when (slug) {
        null -> none
        OTHER_DRINK -> edit.customDrinkName?.takeIf { it.isNotBlank() } ?: selected?.title() ?: if (language == "en") "Other" else other
        else -> savedName ?: selected?.title() ?: slug
    }
    val options = buildList {
        add(CoffeePeekSelectOption("none", none, selected = slug == null) { onAction(CommunityAction.EditDrink(null, null)) })
        if (slug != null && selected == null) add(CoffeePeekSelectOption("saved:$slug", label, selected = true) { })
        drinks.forEach { drink ->
            add(CoffeePeekSelectOption("drink:${drink.slug}", drink.title(), selected = drink.slug == slug) {
                if (drink.slug != slug) onAction(CommunityAction.EditDrink(drink.slug, if (drink.slug == OTHER_DRINK) "" else null))
            })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        val title = stringResource(Res.string.community_drink_label)
        Text(title, style = MaterialTheme.typography.labelMedium)
        CoffeePeekSelectField(label, options, title, Modifier.fillMaxWidth())
        if (failed) {
            Text(stringResource(Res.string.community_drink_error), color = MaterialTheme.colorScheme.error)
            TextButton({ onAction(CommunityAction.LoadDrinks) }) { Text(stringResource(Res.string.community_drink_retry)) }
        }
        if (slug == OTHER_DRINK) {
            TextArea(edit.customDrinkName.orEmpty(), { onAction(CommunityAction.EditDrink(slug, it.take(MAX_DRINK_NAME_LENGTH))) },
                stringResource(Res.string.community_drink_custom), MAX_DRINK_NAME_LENGTH, singleLine = true)
        }
    }
}

@Composable
internal fun ReportSheet(form: ReportForm, callbacks: ReportCallbacks) {
    CoffeePeekSwipeDismissModalBottomSheet(onDismissRequest = callbacks.onDismiss,
        dismissDescription = stringResource(Res.string.community_close), dismissEnabled = !form.isSending) {
        ReportSheetContent(form, callbacks)
    }
}

/** What the report sheet shows; the feed and a single post keep their own drafts. */
internal class ReportForm(val text: String, val issue: ReportIssue?, val failed: Boolean, val failure: String?, val isSending: Boolean)

internal class ReportCallbacks(val onText: (String) -> Unit, val onSubmit: () -> Unit, val onDismiss: () -> Unit)

internal fun CommunityState.reportForm() = ReportForm(reportText, reportIssue, reportFailed, reportFailure, isReporting)

internal fun reportCallbacks(onAction: (CommunityAction) -> Unit) = ReportCallbacks(
    onText = { onAction(CommunityAction.ReportText(it)) },
    onSubmit = { onAction(CommunityAction.SubmitReport) },
    onDismiss = { onAction(CommunityAction.DismissReport) },
)

@Composable
internal fun ReportSheetContent(form: ReportForm, callbacks: ReportCallbacks) {
    Column(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(stringResource(Res.string.community_report_title), style = MaterialTheme.typography.titleLarge)
        val error = form.issue != null || form.failed
        TextArea(form.text, callbacks.onText, stringResource(Res.string.community_report_placeholder), MAX_REPORT_LENGTH, isError = error)
        val message = when {
            form.issue != null -> stringResource(Res.string.community_report_issue)
            form.failed -> form.failure ?: stringResource(Res.string.community_report_failed)
            else -> null
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(callbacks.onSubmit, enabled = !form.isSending) {
            Text(stringResource(if (form.isSending) Res.string.community_report_sending else Res.string.community_report_send))
        }
        TextButton(callbacks.onDismiss, enabled = !form.isSending) { Text(stringResource(Res.string.community_cancel)) }
    }
}

/** A multi-line (or single-line) field with a placeholder and a hard length limit, as the legacy review input. */
@Composable
private fun TextArea(value: String, onChange: (String) -> Unit, placeholder: String, maxLength: Int, modifier: Modifier = Modifier, isError: Boolean = false, singleLine: Boolean = false) {
    val shape = if (singleLine) RoundedCornerShape(percent = 50) else RoundedCornerShape(CoffeePeekDimens.buttonRadius)
    val focus = LocalFocusManager.current
    BasicTextField(
        value = value, onValueChange = { onChange(it.take(maxLength)) },
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(force = true) }),
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth().then(if (singleLine) Modifier.height(CoffeePeekDimens.buttonHeight) else Modifier)
            .clip(shape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(1.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp),
        decorationBox = { inner ->
            Box(if (singleLine) Modifier.fillMaxSize() else Modifier, contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                inner()
            }
        },
    )
}

@Composable private fun EditSheetPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    androidx.compose.material3.Surface {
        EditSheetContent(PostEditState(previewPost()), listOf(DrinkOption("cappuccino", "Капучино", "Cappuccino")), false, false, true, {})
    }
}
@Preview @Composable private fun EditSheetLightPreview() = EditSheetPreview(false)
@Preview @Composable private fun EditSheetDarkPreview() = EditSheetPreview(true)

@Composable private fun ReportSheetPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    androidx.compose.material3.Surface {
        ReportSheetContent(ReportForm("Нарушение", ReportIssue.Empty, failed = false, failure = null, isSending = false), ReportCallbacks({}, {}, {}))
    }
}
@Preview @Composable private fun ReportSheetLightPreview() = ReportSheetPreview(false)
@Preview @Composable private fun ReportSheetDarkPreview() = ReportSheetPreview(true)
