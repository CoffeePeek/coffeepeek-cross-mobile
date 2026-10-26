package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Set by the platform host when content draws through the bottom system area. */
@Immutable
data class CoffeePeekScreenInsets(val bottom: Dp = 0.dp, val extendsToBottom: Boolean = false)

val LocalCoffeePeekScreenInsets = staticCompositionLocalOf { CoffeePeekScreenInsets() }

/** Keeps native bottom-action/FAB clearance while lists draw to the window edge. */
@Composable
fun CoffeePeekScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    val insets = LocalCoffeePeekScreenInsets.current
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = { CoffeePeekScaffoldBottomBar(insets, bottomBar) },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { padding ->
        val contentInsets = if (bottomBar != null) insets.copy(bottom = 0.dp) else insets
        CompositionLocalProvider(LocalCoffeePeekScreenInsets provides contentInsets) {
            content(coffeePeekScaffoldContentPadding(padding, insets, hasBottomBar = bottomBar != null))
        }
    }
}

@Composable
private fun CoffeePeekScaffoldBottomBar(insets: CoffeePeekScreenInsets, content: (@Composable () -> Unit)?) {
    if (content == null) return
    if (!insets.extendsToBottom) {
        content()
        return
    }
    Box(Modifier.padding(bottom = insets.bottom).consumeWindowInsets(PaddingValues(bottom = insets.bottom))) {
        CompositionLocalProvider(LocalCoffeePeekScreenInsets provides insets.copy(bottom = 0.dp)) { content() }
    }
}

internal fun coffeePeekScaffoldContentPadding(
    padding: PaddingValues, insets: CoffeePeekScreenInsets, hasBottomBar: Boolean,
): PaddingValues = if (insets.extendsToBottom && !hasBottomBar) CoffeePeekBottomPadding(padding, 0.dp) else padding

@Composable
fun coffeePeekScrollContentPadding(padding: PaddingValues): PaddingValues =
    coffeePeekScrollContentPadding(padding, LocalCoffeePeekScreenInsets.current)

internal fun coffeePeekScrollContentPadding(padding: PaddingValues, insets: CoffeePeekScreenInsets): PaddingValues =
    if (insets.extendsToBottom && insets.bottom > 0.dp)
        CoffeePeekBottomPadding(padding, padding.calculateBottomPadding() + insets.bottom) else padding

@Composable
fun Modifier.coffeePeekScreenBottomPadding(): Modifier =
    padding(bottom = LocalCoffeePeekScreenInsets.current.let { if (it.extendsToBottom) it.bottom else 0.dp })

/** Other hosts retain their existing viewport padding; native scroll content owns it internally. */
@Composable
fun Modifier.coffeePeekViewportBottomPadding(): Modifier =
    if (LocalCoffeePeekScreenInsets.current.extendsToBottom) this else navigationBarsPadding()

private class CoffeePeekBottomPadding(private val padding: PaddingValues, private val bottom: Dp) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = padding.calculateLeftPadding(layoutDirection)
    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = padding.calculateRightPadding(layoutDirection)
    override fun calculateTopPadding(): Dp = padding.calculateTopPadding()
    override fun calculateBottomPadding(): Dp = bottom
}

@Composable
private fun CoffeePeekScaffoldPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalCoffeePeekScreenInsets provides CoffeePeekScreenInsets(34.dp, true)) {
        CoffeePeekScaffold(topBar = { Text("CoffeePeek", Modifier.padding(16.dp)) }) { padding ->
            LazyColumn(Modifier.padding(padding), contentPadding = coffeePeekScrollContentPadding(PaddingValues(16.dp))) {
                items(12) { Text("CoffeePeek ${it + 1}", Modifier.padding(20.dp)) }
            }
        }
    }
}

@Preview @Composable
private fun CoffeePeekScaffoldLightPreview() = CoffeePeekScaffoldPreview(false)

@Preview @Composable
private fun CoffeePeekScaffoldDarkPreview() = CoffeePeekScaffoldPreview(true)
