package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    errorDescription: String? = null,
) {
    val fieldModifier = modifier.fillMaxWidth().heightIn(min = 80.dp)
    if (CoffeePeekNativeMultilineTextField(value, onValueChange, label, placeholder, maxLength,
            fieldModifier, enabled, errorDescription)) return
    val colors = MaterialTheme.colorScheme
    val container = colors.onSurface.copy(alpha = 0.04f)
    CoffeePeekCompactOutlinedTextField(
        value, { onValueChange(it.take(maxLength)) },
        modifier = fieldModifier.semantics { contentDescription = label },
        enabled = enabled, singleLine = false,
        isError = errorDescription != null, errorDescription = errorDescription,
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, color = colors.onSurfaceVariant) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            errorContainerColor = container,
            focusedBorderColor = colors.outline,
        ),
    )
}

@Composable
internal expect fun CoffeePeekNativeMultilineTextField(
    value: String, onValueChange: (String) -> Unit, label: String, placeholder: String,
    maxLength: Int, modifier: Modifier, enabled: Boolean, errorDescription: String?,
): Boolean

@Composable
private fun CoffeePeekMultilineTextFieldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekMultilineTextField(if (dark) "Понравился фильтр." else "", {}, "Описание", "Что понравилось?", 1000)
}

@Preview @Composable private fun CoffeePeekMultilineTextFieldLightPreview() = CoffeePeekMultilineTextFieldPreview(false)
@Preview @Composable private fun CoffeePeekMultilineTextFieldDarkPreview() = CoffeePeekMultilineTextFieldPreview(true)
