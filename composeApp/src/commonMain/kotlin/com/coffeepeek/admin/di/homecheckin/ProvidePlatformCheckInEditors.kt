package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable

/** Supplies native editor factories to every application host, including embedded shelf content. */
@Composable
internal expect fun ProvidePlatformCheckInEditors(content: @Composable () -> Unit)
