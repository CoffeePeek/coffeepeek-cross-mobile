package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** What a text field is for, so the platform can configure its keyboard (AutoFill, suggestions). */
enum class CoffeePeekInputRole {
    /** Ordinary text, numbers and links. */
    Default,

    /** A sign-in or sign-up field: e-mail, name or the current password. */
    Credential,

    /** The password being created at sign-up. */
    NewPassword,
}

interface CoffeePeekKeyboardPresenter {
    /** The platform's configuration of [options] for a field with [role]; [options] themselves when it has none. */
    fun textInputOptions(options: KeyboardOptions, role: CoffeePeekInputRole): KeyboardOptions

    /** A "Done" bar above the keyboard for keyboards that have no return key; shown while [visible]. */
    @Composable
    fun DoneAccessory(visible: Boolean, onDone: () -> Unit)
}

val LocalCoffeePeekKeyboardPresenter = staticCompositionLocalOf<CoffeePeekKeyboardPresenter?> { null }

/**
 * A scrolling column that keeps its content above the keyboard and, on screens that extend under the bottom bar,
 * clears it when [reserveBottomClearance] is set.
 */
@Composable
fun CoffeePeekScrollableColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    reserveBottomClearance: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val insets = LocalCoffeePeekScreenInsets.current
    val bottomClearance = if (reserveBottomClearance && insets.extendsToBottom) insets.bottom else 0.dp
    Column(modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement, horizontalAlignment) {
        content()
        if (bottomClearance > 0.dp) Spacer(Modifier.height(bottomClearance))
    }
}
