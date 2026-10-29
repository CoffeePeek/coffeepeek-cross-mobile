package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.SocialProvider
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Match prod: side by side when both labels fit, otherwise stack without clipping large fonts. */
@Composable
internal fun SocialSignInButtons(onGoogle: (() -> Unit)?, onApple: (() -> Unit)?, enabled: Boolean) {
    if (onGoogle == null && onApple == null) return
    val minimumWidth = minimumSocialButtonWidth()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (onGoogle != null && onApple != null && maxWidth >= minimumWidth * 2 + 8.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialSignInButton(SocialProvider.Apple, onApple, enabled, Modifier.weight(1f))
                SocialSignInButton(SocialProvider.Google, onGoogle, enabled, Modifier.weight(1f))
            }
        } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            onApple?.let { SocialSignInButton(SocialProvider.Apple, it, enabled) }
            onGoogle?.let { SocialSignInButton(SocialProvider.Google, it, enabled) }
        }
    }
}

@Composable
private fun minimumSocialButtonWidth(): androidx.compose.ui.unit.Dp {
    val measurer = rememberTextMeasurer()
    val style = socialSignInTextStyle()
    val labelWidth = SocialProvider.entries.maxOf { measurer.measure(socialSignInLabel(it), style).size.width }
    return with(LocalDensity.current) { labelWidth.toDp() } + 18.dp + 8.dp + 12.dp
}

@Composable
private fun SocialSignInButtonsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { Column(Modifier.padding(16.dp)) { SocialSignInButtons({}, {}, true) } }
}

@Preview @Composable private fun SocialSignInButtonsLightPreview() = SocialSignInButtonsPreview(false)
@Preview @Composable private fun SocialSignInButtonsDarkPreview() = SocialSignInButtonsPreview(true)
