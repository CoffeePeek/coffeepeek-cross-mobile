package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.core.designsystem.modifier.GlassIconButton
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.coffeepeek.core.designsystem.icons.CpIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Single top bar used across screens: center-aligned title + circular back button in the corner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpTopBar(
    title: String,
    backDescription: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = { onBack?.let { CpCircularBackButton(onClick = it, contentDescription = backDescription) } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        modifier = modifier.padding(horizontal = CpDimens.spacing4),
    )
}

/** Circular Liquid Glass back button — used on plain bars and over hero images. */
@Composable
fun CpCircularBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    GlassIconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier,
    ) {
        Icon(
            imageVector = CpIcons.ChevronLeft,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp).scale(
                scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f,
                scaleY = 1f,
            ),
        )
    }
}

@Composable
private fun CpTopBarPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Surface { CpTopBar("CoffeePeek", "Back", onBack = {}) }
}

@Preview @Composable private fun CpTopBarLightPreview() = CpTopBarPreviewContent(false)
@Preview @Composable private fun CpTopBarDarkPreview() = CpTopBarPreviewContent(true)

// Synthetic asymmetry checks physical and logical inset behaviour in both directions.
@Composable
private fun CpTopBarInsetsPreviewContent(darkTheme: Boolean, direction: LayoutDirection) {
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        CoffeePeekTheme(darkTheme = darkTheme) {
            val insets = WindowInsets(left = 12.dp, top = 20.dp, right = 32.dp, bottom = 24.dp)
            Surface {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    CpTopBar("${direction.name} · inset layout", "Back", onBack = {})
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer)
                        .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))) {
                        // A nested consumer must not add a second horizontal gap.
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))) {
                            Text("Content: insets applied once")
                            AppButton("Continue", {})
                        }
                    }
                    Text("Logical start / end spacers")
                    Row(Modifier.fillMaxWidth().height(48.dp)) {
                        Spacer(Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.primary)
                            .windowInsetsStartWidth(insets))
                        Text("Start → content → End", Modifier.weight(1f))
                        Spacer(Modifier.fillMaxHeight().background(MaterialTheme.colorScheme.tertiary)
                            .windowInsetsEndWidth(insets))
                    }
                    Text("Bottom bars + synthetic keyboard: max, not sum")
                    val keyboard = WindowInsets(bottom = 64.dp)
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer)
                        .windowInsetsPadding(insets.union(keyboard).only(WindowInsetsSides.Bottom))) {
                        Text("Content above keyboard", Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface).padding(8.dp))
                    }
                }
            }
        }
    }
}

@Preview @Composable private fun CpTopBarInsetsLtrLightPreview() = CpTopBarInsetsPreviewContent(false, LayoutDirection.Ltr)
@Preview @Composable private fun CpTopBarInsetsLtrDarkPreview() = CpTopBarInsetsPreviewContent(true, LayoutDirection.Ltr)
@Preview @Composable private fun CpTopBarInsetsRtlLightPreview() = CpTopBarInsetsPreviewContent(false, LayoutDirection.Rtl)
@Preview @Composable private fun CpTopBarInsetsRtlDarkPreview() = CpTopBarInsetsPreviewContent(true, LayoutDirection.Rtl)
