package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun NativeSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, onSearch: () -> Unit, onFocusChanged: (Boolean) -> Unit, onCancel: (() -> Unit)?, modifier: Modifier): Boolean = false
