package com.coffeepeek.admin.di.community

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.coffeepeek.admin.ui.component.CpSnackbarHost

@Composable
internal actual fun CommunitySnackbar(state: SnackbarHostState, clearance: Dp) {
    CpSnackbarHost(state, Modifier.padding(bottom = clearance))
}
