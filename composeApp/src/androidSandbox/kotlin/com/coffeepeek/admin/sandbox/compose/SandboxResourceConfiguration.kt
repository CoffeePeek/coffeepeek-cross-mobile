package com.coffeepeek.admin.sandbox.compose

import android.content.res.Configuration
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.LocalCoffeePeekOpaqueGlassOverride

/** Manual resource checks only: the wrapped context keeps its Activity parent and never changes device preferences. */
@Composable
internal fun SandboxResourceConfiguration(
    language: String?, fontScale: Float = 0f, widthDp: Int = 0, heightDp: Int = 0,
    opaqueGlass: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val parent = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scale = fontScale.takeIf { it in 1f..2f } ?: density.fontScale
    val context = remember(parent, configuration, language, scale, widthDp, heightDp) {
        ContextThemeWrapper(parent, 0).apply {
            applyOverrideConfiguration(Configuration(configuration).apply {
                if (language == "ru" || language == "en") setLocales(LocaleList.forLanguageTags(language))
                this.fontScale = scale
                if (widthDp >= 280) screenWidthDp = widthDp
                if (heightDp >= 400) screenHeightDp = heightDp
            })
        }
    }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration,
        LocalDensity provides Density(density.density, scale),
        LocalCoffeePeekOpaqueGlassOverride provides opaqueGlass) {
        SandboxViewport(widthDp, heightDp, content)
    }
}

/** App-only constrained viewport; does not resize the device or change global accessibility settings. */
@Composable
private fun SandboxViewport(widthDp: Int, heightDp: Int, content: @Composable () -> Unit) {
    val parentWindow = LocalWindowInfo.current
    val density = LocalDensity.current
    val bounds = Modifier
        .then(if (widthDp >= 280) Modifier.widthIn(max = widthDp.dp) else Modifier)
        .then(if (heightDp >= 400) Modifier.heightIn(max = heightDp.dp) else Modifier)
    BoxWithConstraints(bounds) {
        val size = with(density) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
        val window = remember(parentWindow, size) {
            object : WindowInfo by parentWindow { override val containerSize = size }
        }
        CompositionLocalProvider(LocalWindowInfo provides window) { Box { content() } }
    }
}

@PreviewLightDark
@Composable
private fun SandboxResourceConfigurationPreview() = CoffeePeekTheme {
    SandboxResourceConfiguration("ru") { Surface { Text(stringResource(R.string.sandbox_banner)) } }
}
