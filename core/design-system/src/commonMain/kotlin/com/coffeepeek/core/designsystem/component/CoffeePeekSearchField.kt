package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val SearchFieldHeight = CoffeePeekDimens.buttonHeight

@Composable
fun CoffeePeekSearchField(
    value: String, onValueChange: (String) -> Unit, placeholder: String, clearDescription: String,
    enabled: Boolean = true, readOnly: Boolean = false, modifier: Modifier = Modifier,
    onSearch: () -> Unit = {}, fieldHeight: Dp = SearchFieldHeight, shadowElevation: Dp = 0.dp,
    clearButtonSize: Dp = 48.dp,
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(percent = 50)
    val field = remember { SyncedTextFieldState(value) }
    SideEffect { field.syncExternal(value) }
    BasicTextField(value = field.value, onValueChange = { updated ->
        val changed = field.value.text != updated.text
        field.value = updated
        if (changed) onValueChange(updated.text)
    }, modifier = modifier.heightIn(min = fieldHeight).shadow(shadowElevation, shape, clip = false)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape).clip(shape).background(MaterialTheme.colorScheme.surface),
        enabled = enabled, readOnly = readOnly, singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focusManager.clearFocus()
            onSearch()
        }), decorationBox = { inner ->
            SearchFieldDecoration(field.value.text, placeholder, clearDescription, fieldHeight, clearButtonSize, enabled && !readOnly,
                onClear = {
                    field.value = TextFieldValue("")
                    onValueChange("")
                }, inner = inner)
        })
}

@Composable
private fun SearchFieldDecoration(value: String, placeholder: String, clearDescription: String, fieldHeight: Dp,
    clearButtonSize: Dp, canEdit: Boolean, onClear: () -> Unit, inner: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = fieldHeight).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(CoffeePeekIcons.Search, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            inner()
        }
        if (value.isNotEmpty()) SearchClearButton(clearDescription, clearButtonSize, canEdit, onClear)
    }
}

@Composable
private fun SearchClearButton(description: String, size: Dp, enabled: Boolean, onClear: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).clickable(enabled = enabled, onClick = onClear), contentAlignment = Alignment.Center) {
        Icon(CoffeePeekIcons.Close, description, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoffeePeekSearchFieldPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    var query by remember { mutableStateOf("Coffee") }
    Surface {
        CoffeePeekSearchField(query, { query = it }, "Search", "Clear search", modifier = Modifier.padding(16.dp))
    }
}

@Preview @Composable private fun CoffeePeekSearchFieldLightPreview() = CoffeePeekSearchFieldPreviewContent(false)
@Preview @Composable private fun CoffeePeekSearchFieldDarkPreview() = CoffeePeekSearchFieldPreviewContent(true)
