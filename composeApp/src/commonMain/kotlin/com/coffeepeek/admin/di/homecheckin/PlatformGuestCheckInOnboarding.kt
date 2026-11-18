package com.coffeepeek.admin.di.homecheckin

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.Navigator

@Composable
internal expect fun PlatformGuestCheckInOnboarding(onFinish: (Navigator.Screen?) -> Unit): Boolean
