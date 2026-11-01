package com.coffeepeek.core.designsystem.resources

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.StringArrayResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The host applies the platform locale before publishing this tag to every Compose root. */
val LocalCoffeePeekLanguageTag = staticCompositionLocalOf { "ru" }

/** Recreates only the resource environment on a language change, preserving callers' remembered state. */
@Composable
fun coffeePeekStringResource(resource: StringResource): String = key(LocalCoffeePeekLanguageTag.current) {
    stringResource(resource)
}

@Composable
fun coffeePeekStringResource(resource: StringResource, vararg formatArgs: Any): String = key(LocalCoffeePeekLanguageTag.current) {
    stringResource(resource, *formatArgs)
}

@Composable
fun coffeePeekStringArrayResource(resource: StringArrayResource): List<String> = key(LocalCoffeePeekLanguageTag.current) {
    stringArrayResource(resource)
}

@Composable
private fun CoffeePeekLocalizedResourcesPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Column {
            Text(coffeePeekStringResource(Res.string.coffeepeek_loading))
            Text(coffeePeekStringResource(Res.string.coffeepeek_photo_description, 1, 3))
        }
    }
}

@Preview @Composable private fun CoffeePeekLocalizedResourcesLightPreview() = CoffeePeekLocalizedResourcesPreview(false)
@Preview @Composable private fun CoffeePeekLocalizedResourcesDarkPreview() = CoffeePeekLocalizedResourcesPreview(true)
