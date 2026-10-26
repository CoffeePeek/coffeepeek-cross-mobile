package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Shared initial fallback; callers own image loading and the surrounding identity semantics. */
@Composable
fun CoffeePeekAvatar(name: String, modifier: Modifier = Modifier, image: @Composable (Modifier) -> Unit = {}) {
    Box(modifier.size(44.dp).clip(CircleShape).background(CoffeePeekColors.GoldWarmSoft), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = CoffeePeekColors.LightTextPrimary)
        image(Modifier.fillMaxSize())
    }
}

@Preview @Composable private fun CoffeePeekAvatarLightPreview() = CoffeePeekTheme(darkTheme = false) { CoffeePeekAvatar("Анна") }
@Preview @Composable private fun CoffeePeekAvatarDarkPreview() = CoffeePeekTheme(darkTheme = true) { CoffeePeekAvatar("Анна") }
