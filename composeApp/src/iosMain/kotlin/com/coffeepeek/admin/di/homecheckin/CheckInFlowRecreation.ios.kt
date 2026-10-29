package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable

/** iOS keeps the screen through rotation and appearance changes; disposal is always final. */
@Composable
internal actual fun rememberRecreationCheck(): () -> Boolean = { false }
