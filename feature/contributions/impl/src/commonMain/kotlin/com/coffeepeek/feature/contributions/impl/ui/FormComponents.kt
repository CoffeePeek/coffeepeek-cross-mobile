package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.background
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_back
import com.coffeepeek.feature.contributions.impl.resources.contributions_error_email
import com.coffeepeek.feature.contributions.impl.resources.contributions_error_instagram
import com.coffeepeek.feature.contributions.impl.resources.contributions_error_phone
import com.coffeepeek.feature.contributions.impl.resources.contributions_error_website
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_city_description
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_city_placeholder
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_error_address_blank
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_error_city_missing
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_error_name_blank
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_error_name_too_long
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_field_optional
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_optional_legend
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_required_hint
import com.coffeepeek.feature.contributions.impl.resources.contributions_sign_in
import com.coffeepeek.feature.contributions.impl.resources.contributions_suggest_sign_in_prompt
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import com.coffeepeek.core.designsystem.component.coffeePeekScreenBottomPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCompactOutlinedTextField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectField
import com.coffeepeek.core.designsystem.component.CoffeePeekSelectOption
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.contributions.domain.model.CatalogOption
import com.coffeepeek.feature.contributions.domain.rules.ContactIssue
import com.coffeepeek.feature.contributions.domain.rules.ContributionLimits
import com.coffeepeek.feature.contributions.domain.rules.ShopBasicsIssue

@Composable
internal fun FormField(label: String, required: Boolean = false, optional: Boolean = false, content: @Composable () -> Unit) {
    Row(Modifier.padding(bottom = CoffeePeekDimens.spacing2), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Text(if (required) "$label *" else label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (optional) Text(
            stringResource(Res.string.contributions_shop_field_optional), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
    content()
}

/** The pill (single line) or rounded (several lines) outlined field with an optional error line and counter. */
@Composable
internal fun OutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    counter: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
) {
    val singleLine = maxLines <= 1
    Column(modifier) {
        CoffeePeekCompactOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(if (singleLine) Modifier.height(CoffeePeekDimens.buttonHeight) else Modifier),
            shape = if (singleLine) RoundedCornerShape(percent = 50) else RoundedCornerShape(CoffeePeekDimens.buttonRadius),
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = errorText != null,
            errorDescription = errorText,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            contentPadding = if (singleLine) CoffeePeekDimens.singleLineFieldContentPadding else OutlinedTextFieldDefaults.contentPadding(),
            leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
        FieldFooter(errorText, counter)
    }
}

@Composable
private fun FieldFooter(errorText: String?, counter: String?) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        if (errorText != null) Text(errorText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) else Spacer(Modifier.width(0.dp))
        if (counter != null) Text(counter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A value shown like a field but not editable, e.g. the only available city. */
@Composable
internal fun ReadOnlyField(value: String) {
    val container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    CoffeePeekCompactOutlinedTextField(
        value = value, onValueChange = {}, readOnly = true, enabled = false,
        modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight),
        textStyle = MaterialTheme.typography.bodyLarge, shape = RoundedCornerShape(percent = 50),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor = MaterialTheme.colorScheme.outline, disabledTextColor = MaterialTheme.colorScheme.onSurface, disabledContainerColor = container,
        ),
    )
}

@Composable
internal fun CityField(cities: List<CatalogOption>, selectedId: String?, onSelect: (String) -> Unit, error: String?) {
    val selected = cities.firstOrNull { it.id == selectedId }
    CoffeePeekSelectField(
        value = selected?.name ?: stringResource(Res.string.contributions_shop_city_placeholder),
        options = cities.map { city -> CoffeePeekSelectOption("city:${city.id}", city.name, city.id == selected?.id) { onSelect(city.id) } },
        contentDescription = stringResource(Res.string.contributions_shop_city_description),
        modifier = Modifier.fillMaxWidth(),
        isPlaceholder = selected == null,
        errorDescription = error,
    )
    if (error != null) Text(error, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
}

@Composable
internal fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val active = index == current
            Spacer(
                Modifier.padding(horizontal = 2.dp).height(10.dp).width(if (active) 28.dp else 10.dp).clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)),
            )
        }
    }
}

@Composable
internal fun RequiredLegend() = Text(
    stringResource(Res.string.contributions_shop_required_hint),
    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface,
)

@Composable
internal fun OptionalLegend() = Text(
    stringResource(Res.string.contributions_shop_optional_legend),
    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.padding(bottom = CoffeePeekDimens.spacing1),
)

@Composable
internal fun ShopBasicsIssue.message(): String = when (this) {
    ShopBasicsIssue.NameBlank -> stringResource(Res.string.contributions_shop_error_name_blank)
    ShopBasicsIssue.NameTooLong -> stringResource(Res.string.contributions_shop_error_name_too_long, ContributionLimits.SHOP_NAME)
    ShopBasicsIssue.AddressBlank -> stringResource(Res.string.contributions_shop_error_address_blank)
    ShopBasicsIssue.CityMissing -> stringResource(Res.string.contributions_shop_error_city_missing)
}

@Composable
internal fun ContactIssue.message(): String = stringResource(
    when (this) {
        ContactIssue.Phone -> Res.string.contributions_error_phone
        ContactIssue.Email -> Res.string.contributions_error_email
        ContactIssue.Website -> Res.string.contributions_error_website
        ContactIssue.Instagram -> Res.string.contributions_error_instagram
    },
)

/** What a form shows to a guest: the reason to sign in and the way to do it. */
@Composable
internal fun FormSignInPrompt(title: String, prompt: String, onBack: () -> Unit, onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(title, stringResource(Res.string.contributions_back), onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).coffeePeekScreenBottomPadding().padding(CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Text(prompt, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(CoffeePeekDimens.spacing3))
            CoffeePeekButton(stringResource(Res.string.contributions_sign_in), onSignIn)
        }
    }
}

@Composable
private fun FormSignInPromptPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    FormSignInPrompt(
        stringResource(Res.string.contributions_sign_in),
        stringResource(Res.string.contributions_suggest_sign_in_prompt), {}, {},
    )
}

@Preview @Composable private fun FormSignInPromptLightPreview() = FormSignInPromptPreview(false)
@Preview @Composable private fun FormSignInPromptDarkPreview() = FormSignInPromptPreview(true)
