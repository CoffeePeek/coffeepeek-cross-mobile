package com.coffeepeek.admin.sandbox.compose

import android.content.res.Configuration
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.R
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

/** Manual resource checks only: the wrapped context keeps its Activity parent and never changes device preferences. */
@Composable
internal fun SandboxResourceConfiguration(language: String?, content: @Composable () -> Unit) {
    if (language != "ru" && language != "en") {
        content()
        return
    }
    val parent = LocalContext.current
    val configuration = LocalConfiguration.current
    val context = remember(parent, configuration, language) {
        ContextThemeWrapper(parent, 0).apply {
            applyOverrideConfiguration(Configuration(configuration).apply { setLocales(LocaleList.forLanguageTags(language)) })
        }
    }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides context.resources.configuration) { content() }
}

@PreviewLightDark
@Composable
private fun SandboxResourceConfigurationPreview() = CoffeePeekTheme {
    SandboxResourceConfiguration("ru") { Surface { Text(stringResource(R.string.sandbox_banner)) } }
}
