package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCompactOutlinedTextField
import com.coffeepeek.core.designsystem.component.CoffeePeekInputRole
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_forgot_password
import com.coffeepeek.feature.auth.impl.resources.auth_hide_password
import com.coffeepeek.feature.auth.impl.resources.auth_password_hide
import com.coffeepeek.feature.auth.impl.resources.auth_password_label
import com.coffeepeek.feature.auth.impl.resources.auth_password_show
import com.coffeepeek.feature.auth.impl.resources.auth_password_required
import com.coffeepeek.feature.auth.impl.resources.auth_show_password
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String,
    isPassword: Boolean = false,
    errorText: String? = null,
    next: Boolean = false,
    enabled: Boolean = true,
    text: Boolean = false,
    newPassword: Boolean = false,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    labelAction: (@Composable () -> Unit)? = null,
    fieldModifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val keyboard = authKeyboardOptions(isPassword, text, next)
    Column(modifier.fillMaxWidth()) {
        AuthFieldLabel(label, errorText != null, labelAction)
        CoffeePeekCompactOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier.fillMaxWidth().heightIn(min = 56.dp).semantics {
                contentDescription = label
                contentType = authAutofillType(isPassword, text, newPassword)
            },
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            isError = errorText != null,
            errorDescription = errorText,
            textStyle = MaterialTheme.typography.bodyLarge,
            contentPadding = PaddingValues(horizontal = CoffeePeekDimens.inputPadding, vertical = CoffeePeekDimens.spacing1),
            colors = authFieldColors(),
            placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics {}) },
            trailingIcon = if (isPassword) {
                { AuthPasswordVisibility(passwordVisible, enabled) { passwordVisible = !passwordVisible } }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboard,
            keyboardActions = KeyboardActions(
                onNext = onNext?.let { nextField -> { nextField() } },
                onDone = onDone?.let { done -> { done() } },
            ),
            inputRole = if (newPassword) CoffeePeekInputRole.NewPassword else CoffeePeekInputRole.Credential,
        )
        if (errorText != null) Text(errorText, Modifier.padding(top = CoffeePeekDimens.spacing1),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AuthFieldLabel(label: String, isError: Boolean, action: (@Composable () -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(bottom = CoffeePeekDimens.spacing1),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(label, Modifier.alignByBaseline().clearAndSetSemantics {},
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        if (action != null) {
            Box(Modifier.weight(1f).alignByBaseline(), contentAlignment = Alignment.BottomEnd) { action() }
        }
    }
}

@Composable
private fun AuthPasswordVisibility(visible: Boolean, enabled: Boolean, onClick: () -> Unit) {
    AuthLink(
        text = stringResource(if (visible) Res.string.auth_password_hide else Res.string.auth_password_show),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(end = CoffeePeekDimens.spacing1),
        accessibilityLabel = stringResource(if (visible) Res.string.auth_hide_password else Res.string.auth_show_password),
    )
}

private fun authKeyboardOptions(isPassword: Boolean, text: Boolean, next: Boolean): KeyboardOptions = KeyboardOptions(
    capitalization = if (text) KeyboardCapitalization.Words else KeyboardCapitalization.None,
    keyboardType = when {
        isPassword -> KeyboardType.Password
        text -> KeyboardType.Text
        else -> KeyboardType.Email
    },
    autoCorrectEnabled = false,
    imeAction = if (next) ImeAction.Next else ImeAction.Done,
)

private fun authAutofillType(isPassword: Boolean, text: Boolean, newPassword: Boolean): ContentType = when {
    newPassword -> ContentType.NewPassword
    isPassword -> ContentType.Password
    text -> ContentType.PersonFullName
    else -> ContentType.EmailAddress
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.coffeePeekAccentForeground,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.coffeePeekAccentForeground,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun AuthTextFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val label = stringResource(Res.string.auth_password_label)
    AuthTextField("", {}, label, label = label, isPassword = true,
        errorText = if (dark) null else stringResource(Res.string.auth_password_required),
        labelAction = { AuthFieldLabelLink(stringResource(Res.string.auth_forgot_password), {}) })
}

@Preview @Composable private fun AuthTextFieldLightPreview() = AuthTextFieldPreview(false)
@Preview @Composable private fun AuthTextFieldDarkPreview() = AuthTextFieldPreview(true)
