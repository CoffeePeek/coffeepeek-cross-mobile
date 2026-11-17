package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.feature.profile.impl.NativeEditProfileSession

@Composable
internal actual fun PlatformEditProfile(session: NativeEditProfileSession, onBack: () -> Unit,
    onDeletion: () -> Unit, modifier: Modifier): Boolean = false
