package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekGlassHazeState
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CoffeePeekGlassButtonIcon { Check }

/** Native Liquid Glass on iOS 26+, backdrop blur on older iOS and Android. */
@Composable
expect fun CoffeePeekGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingDescription: String? = null,
    leadingIcon: CoffeePeekGlassButtonIcon? = null,
    hazeState: HazeState? = LocalCoffeePeekGlassHazeState.current,
)

@Composable
private fun CoffeePeekGlassButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    val haze = rememberHazeState()
    Box(Modifier.fillMaxWidth().height(140.dp)) {
        Column(Modifier.fillMaxSize().hazeSource(haze)) {
            Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFC78244)))
            Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF46664D)))
        }
        CoffeePeekGlassButton("Check in", {}, leadingIcon = CoffeePeekGlassButtonIcon.Check, hazeState = haze,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(16.dp))
    }
}

@Preview @Composable private fun CoffeePeekGlassButtonLightPreview() = CoffeePeekGlassButtonPreview(false)
@Preview @Composable private fun CoffeePeekGlassButtonDarkPreview() = CoffeePeekGlassButtonPreview(true)
