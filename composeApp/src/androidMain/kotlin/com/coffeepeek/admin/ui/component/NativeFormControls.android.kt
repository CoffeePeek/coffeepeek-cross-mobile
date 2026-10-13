package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal actual fun NativeAuthInput(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, password: Boolean, newPassword: Boolean, next: Boolean, error: Boolean, keyboardType: KeyboardType, modifier: Modifier) = false
@Composable
internal actual fun NativeTimePickerRow(label: String, value: String, onAdjust: (Int) -> Unit) = false

@Composable
internal actual fun NativeKeyboardDoneAccessory(visible: Boolean, onDone: () -> Unit) = Unit
