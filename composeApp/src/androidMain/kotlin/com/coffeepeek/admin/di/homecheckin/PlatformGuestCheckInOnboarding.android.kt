package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.Navigator

@Composable
internal actual fun PlatformGuestCheckInOnboarding(onFinish: (Navigator.Screen?) -> Unit): Boolean = false
