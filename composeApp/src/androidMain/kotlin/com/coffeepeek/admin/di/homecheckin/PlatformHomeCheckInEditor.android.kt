package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPicker

@Composable
internal actual fun PlatformHomeCheckInEditor(owner: Any, preparationId: String?, generation: Int,
    allowed: Boolean, picker: HomeCheckInPhotoPicker, create: () -> NativeHomeCheckInSession,
    onClose: () -> Unit, onSaved: (String, Boolean) -> Unit, onDismissAllowed: (Boolean) -> Unit,
    modifier: Modifier, fallback: @Composable () -> Unit) {
    fallback()
}
