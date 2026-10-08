package com.coffeepeek.admin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

enum class GlassControlIcon {
    Back,
    Add,
    Edit,
    Favorite,
    FavoriteFilled,
    Share,
    Close,
    Zones,
    Location,
}

/** Native controls on iOS; the existing Compose control on Android. */
@Composable
expect fun PlatformGlassIconButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    isLoading: Boolean = false,
    hazeState: HazeState? = LocalGlassHazeState.current,
    content: @Composable () -> Unit,
)

@Composable
expect fun PlatformFloatingBottomNavBar(
    items: List<FloatingNavItem>,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalGlassHazeState.current,
)

@Composable
expect fun PlatformGlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
)

@Composable
expect fun PlatformMapControlButton(
    icon: GlassControlIcon,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit,
)
