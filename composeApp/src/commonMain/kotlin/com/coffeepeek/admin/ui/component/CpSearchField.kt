package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekSearchField
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.search_clear
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Legacy callers share the design-system field while keeping the platform input hook. */
@Composable
fun CpSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    fieldHeight: Dp = CpDimens.buttonHeight,
    shadowElevation: Dp = 0.dp,
    onFocusChanged: (Boolean) -> Unit = {},
    onCancelSearch: (() -> Unit)? = null,
) {
    if (NativeSearchField(value, onValueChange, placeholder, onSearch, onFocusChanged, onCancelSearch, modifier.height(fieldHeight))) return
    CoffeePeekSearchField(value, onValueChange, placeholder, stringResource(Res.string.search_clear),
        modifier = modifier, onSearch = onSearch, onFocusChanged = onFocusChanged,
        fieldHeight = fieldHeight, shadowElevation = shadowElevation,
        keyboardOptions = platformTextInputOptions(KeyboardOptions(imeAction = ImeAction.Search)))
}

@Preview @Composable private fun CpSearchFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CpSearchField("Coffee", {}, "Search")
}

@Preview @Composable private fun CpSearchFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CpSearchField("Coffee", {}, "Search")
}
