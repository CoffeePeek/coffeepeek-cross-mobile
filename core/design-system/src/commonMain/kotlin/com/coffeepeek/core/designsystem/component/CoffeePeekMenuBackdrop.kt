package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekUsesNativeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.ui.tooling.preview.Preview

// Only popup menus consume this capture. In-window controls must not sample their own parent.
internal val LocalCoffeePeekMenuBackdrop = compositionLocalOf<HazeState?> { null }

/** Captures the screen behind popup menus; native iOS menus own their own backdrop. */
@Composable
fun CoffeePeekMenuBackdrop(
    modifier: Modifier = Modifier,
    state: HazeState = rememberHazeState(),
    content: @Composable () -> Unit,
) {
    if (coffeePeekUsesNativeGlass) {
        Box(modifier) { content() }
        return
    }
    CompositionLocalProvider(LocalCoffeePeekMenuBackdrop provides state) {
        Box(modifier.hazeSource(state)) { content() }
    }
}

@Composable
private fun CoffeePeekMenuBackdropPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        CoffeePeekMenuBackdrop {
            Column(Modifier.padding(16.dp)) {
                Text("CoffeePeek")
                CoffeePeekActionMenu(listOf(CoffeePeekMenuAction("CoffeePeek", CoffeePeekIcons.Share) {})) { open ->
                    CoffeePeekTextButton(onClick = open) { Text("CoffeePeek") }
                }
            }
        }
    }
}

@Preview @Composable private fun CoffeePeekMenuBackdropLightPreview() = CoffeePeekMenuBackdropPreview(false)
@Preview @Composable private fun CoffeePeekMenuBackdropDarkPreview() = CoffeePeekMenuBackdropPreview(true)
