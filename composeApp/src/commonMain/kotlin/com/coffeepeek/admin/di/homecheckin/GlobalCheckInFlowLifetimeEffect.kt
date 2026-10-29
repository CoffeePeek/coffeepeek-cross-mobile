package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable

/** Android configuration replacement retains work; final removal releases the flow on both platforms. */
@Composable
internal expect fun GlobalCheckInFlowLifetimeEffect(owner: GlobalCheckInFlowStore)
