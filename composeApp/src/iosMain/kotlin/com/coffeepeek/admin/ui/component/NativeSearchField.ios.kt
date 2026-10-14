package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Render the search pill and its background together in Compose on iOS.
@Composable
internal actual fun NativeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier,
): Boolean = false
