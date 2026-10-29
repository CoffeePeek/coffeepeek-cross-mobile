package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_dose_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_dose_label
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCompactOutlinedTextField
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** Text and numeric variants deliberately keep the current form's 44dp/pill and 80dp/note geometry. */
@Composable
internal fun HomeCheckInEditorInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    error: String? = null,
    multiline: Boolean = false,
    maxLength: Int = Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    maximum: Int? = null,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    CoffeePeekCompactOutlinedTextField(value, { input ->
        val filtered = input.take(maxLength).filter {
            keyboardType == KeyboardType.Text || it.isDigit() ||
                keyboardType == KeyboardType.Decimal && (it == '.' || it == ',')
        }
        if (maximum == null || filtered.isEmpty() || filtered.toIntOrNull() in 0..maximum) onChange(filtered)
    }, enabled = enabled, singleLine = !multiline, isError = error != null, errorDescription = error,
        modifier = Modifier.fillMaxWidth().heightIn(min = if (multiline) 80.dp else 44.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(22.dp), textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = if (multiline) 12.dp else 0.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (multiline) ImeAction.Default else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(force = true); keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f),
            focusedBorderColor = MaterialTheme.colorScheme.outline, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error))
}

@Composable private fun HomeCheckInEditorInputPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorInput("18", {}, stringResource(Res.string.home_check_in_dose_label),
        stringResource(Res.string.home_check_in_dose_hint), true, keyboardType = KeyboardType.Decimal) }
}
@Preview @Composable private fun HomeCheckInEditorInputLightPreview() = HomeCheckInEditorInputPreview(false)
@Preview @Composable private fun HomeCheckInEditorInputDarkPreview() = HomeCheckInEditorInputPreview(true)
