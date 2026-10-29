package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable

/** Whether the screen is only being recreated (Android configuration change), so a retained flow must survive its disposal. */
@Composable
internal expect fun rememberRecreationCheck(): () -> Boolean
