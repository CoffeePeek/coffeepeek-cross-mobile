package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal expect fun NativeAuthInput(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, password: Boolean, newPassword: Boolean, next: Boolean, error: Boolean, keyboardType: KeyboardType, modifier: Modifier): Boolean

@Composable
internal expect fun NativeTimePickerRow(label: String, value: String, onAdjust: (Int) -> Unit): Boolean

@Composable
internal expect fun NativeKeyboardDoneAccessory(visible: Boolean, onDone: () -> Unit)
