package com.coffeepeek.admin.di.community

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

@Composable
internal expect fun CommunitySnackbar(state: SnackbarHostState, clearance: Dp)
