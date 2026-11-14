package com.coffeepeek.core.designsystem.component

import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

private val NumericKeyboards = listOf(KeyboardType.Number, KeyboardType.Decimal, KeyboardType.NumberPassword, KeyboardType.Phone)

/**
 * Single-line outlined field that can stay at [CoffeePeekDimens.controlHeight].
 * Material3's string [androidx.compose.material3.OutlinedTextField] always applies 16.dp of
 * vertical padding, which clips glyphs inside a 44.dp height.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeePeekCompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorDescription: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: androidx.compose.ui.graphics.Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.coffeePeekAccentForeground,
        cursorColor = MaterialTheme.colorScheme.coffeePeekAccentForeground),
    contentPadding: PaddingValues = CoffeePeekDimens.singleLineFieldContentPadding,
    inputRole: CoffeePeekInputRole = CoffeePeekInputRole.Default,
) {
    require(!isError || !errorDescription.isNullOrBlank()) {
        "Error fields require a localized error description"
    }
    val interactionSource = remember { MutableInteractionSource() }
    val focused = interactionSource.collectIsFocusedAsState().value
    val textColor = textStyle.color.takeOrElse { colors.contentColor(enabled, isError, focused) }
    val cursorColor = if (isError) colors.errorCursorColor else colors.cursorColor
    val keyboard = LocalCoffeePeekKeyboardPresenter.current
    CoffeePeekNumericKeyboardAccessory(focused, keyboardOptions)

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            // Compose draws its autofill highlight over the decoration, including the outline.
            modifier = modifier.heightIn(min = CoffeePeekDimens.inputMinHeight).clip(shape).semantics {
                errorDescription?.let { error(it) }
            },
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle.merge(TextStyle(color = textColor)),
            cursorBrush = SolidColor(cursorColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboard?.textInputOptions(keyboardOptions, inputRole) ?: keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = { innerTextField ->
                CoffeePeekOutlinedDecoration(value, visualTransformation, innerTextField, placeholder,
                    leadingIcon, trailingIcon, singleLine, enabled, isError, interactionSource, colors, contentPadding, shape)
            },
        )
    }
}

private fun TextFieldColors.contentColor(enabled: Boolean, error: Boolean, focused: Boolean): Color = when {
    !enabled -> disabledTextColor
    error -> errorTextColor
    focused -> focusedTextColor
    else -> unfocusedTextColor
}

@Composable
private fun CoffeePeekNumericKeyboardAccessory(focused: Boolean, options: KeyboardOptions) {
    val keyboard = LocalCoffeePeekKeyboardPresenter.current
    val focusManager = LocalFocusManager.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    keyboard?.DoneAccessory(focused && WindowInsets.ime.getBottom(LocalDensity.current) > 0 &&
        options.keyboardType in NumericKeyboards) {
        focusManager.clearFocus(force = true)
        softwareKeyboard?.hide()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoffeePeekOutlinedDecoration(value: String, transformation: VisualTransformation,
    inner: @Composable () -> Unit, placeholder: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?, trailing: @Composable (() -> Unit)?,
    singleLine: Boolean, enabled: Boolean, isError: Boolean, interactions: MutableInteractionSource,
    colors: TextFieldColors, padding: PaddingValues, shape: Shape) {
    OutlinedTextFieldDefaults.DecorationBox(
        value = value,
        visualTransformation = transformation,
        innerTextField = inner,
        placeholder = placeholder,
        leadingIcon = leading,
        trailingIcon = trailing,
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        interactionSource = interactions,
        colors = colors,
        contentPadding = padding,
        container = {
            OutlinedTextFieldDefaults.Container(enabled, isError, interactions, colors = colors, shape = shape)
        },
    )
}

@Composable
private fun CoffeePeekCompactOutlinedTextFieldPreviewContent(darkTheme: Boolean, fontScale: Float = 1f) = CoffeePeekTheme(darkTheme = darkTheme) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        Surface {
            CoffeePeekCompactOutlinedTextField("name@example.com", {}, modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.small)
        }
    }

}

@Preview @Composable private fun CoffeePeekCompactOutlinedTextFieldLightPreview() = CoffeePeekCompactOutlinedTextFieldPreviewContent(false)
@Preview @Composable private fun CoffeePeekCompactOutlinedTextFieldDarkPreview() = CoffeePeekCompactOutlinedTextFieldPreviewContent(true)

@Preview @Composable private fun CoffeePeekCompactOutlinedTextFieldLargeTextLightPreview() = CoffeePeekCompactOutlinedTextFieldPreviewContent(false, 2f)
@Preview @Composable private fun CoffeePeekCompactOutlinedTextFieldLargeTextDarkPreview() = CoffeePeekCompactOutlinedTextFieldPreviewContent(true, 2f)
