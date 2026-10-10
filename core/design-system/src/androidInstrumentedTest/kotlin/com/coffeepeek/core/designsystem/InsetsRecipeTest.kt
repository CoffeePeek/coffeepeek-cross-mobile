package com.coffeepeek.core.designsystem

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Tests the documented Compose recipes, not real system-bar/IME dispatch. */
@OptIn(ExperimentalLayoutApi::class)
class InsetsRecipeTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun render(
        direction: LayoutDirection = LayoutDirection.Ltr,
        density: Float = 2f,
        content: @Composable () -> Unit,
    ): ActivityScenario<DesignSystemTestActivity> =
        ActivityScenario.launch(DesignSystemTestActivity::class.java).also { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides direction,
                    LocalDensity provides Density(density),
                ) { content() }
            } }
        }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag)
        .fetchSemanticsNode().boundsInRoot

    private fun px(expected: Float, actual: Float) = assertEquals(expected, actual, 0.5f)

    private val insets = WindowInsets(left = 20, top = 30, right = 60, bottom = 40)

    @Test fun horizontalPaddingPreservesPhysicalEdgesInLtrAndRtl() {
        LayoutDirection.entries.forEach { direction ->
            render(direction) {
                Box(Modifier.width(160.dp).height(100.dp).testTag("outer")
                    .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))) {
                    Box(Modifier.fillMaxSize().testTag("inner"))
                }
            }.use {
                val outer = bounds("outer")
                val inner = bounds("inner")
                px(20f, inner.left - outer.left)
                px(60f, outer.right - inner.right)
                px(0f, inner.top - outer.top)
                px(0f, outer.bottom - inner.bottom)
            }
        }
    }

    @Test fun logicalSpacersSwapWidthsWithoutDoublingInRtl() {
        LayoutDirection.entries.forEach { direction ->
            render(direction) {
                Row(Modifier.width(160.dp).height(20.dp)) {
                    Spacer(Modifier.fillMaxHeight().windowInsetsStartWidth(insets).testTag("start"))
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.fillMaxHeight().windowInsetsEndWidth(insets).testTag("end"))
                }
            }.use {
                px(if (direction == LayoutDirection.Ltr) 20f else 60f, bounds("start").width)
                px(if (direction == LayoutDirection.Ltr) 60f else 20f, bounds("end").width)
            }
        }
    }

    @Test fun nestedPaddingConsumesHorizontalInsetsOnlyOnce() {
        LayoutDirection.entries.forEach { direction ->
            render(direction) {
                Box(Modifier.width(160.dp).height(100.dp).testTag("outer")
                    .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(insets).testTag("middle")) {
                        Box(Modifier.fillMaxSize().testTag("inner"))
                    }
                }
            }.use {
                val outer = bounds("outer")
                val inner = bounds("inner")
                px(20f, inner.left - outer.left)
                px(60f, outer.right - inner.right)
                px(30f, inner.top - outer.top)
                px(40f, outer.bottom - inner.bottom)
            }
        }
    }

    @Test fun scaffoldStylePaddingIsExplicitlyConsumedForChildren() {
        LayoutDirection.entries.forEach { direction ->
            render(direction) {
                val padding = insets.asPaddingValues()
                Box(Modifier.width(160.dp).height(100.dp).testTag("outer")
                    .padding(padding).consumeWindowInsets(padding)) {
                    Box(Modifier.fillMaxSize().windowInsetsPadding(insets)) {
                        Box(Modifier.fillMaxSize().testTag("inner"))
                    }
                }
            }.use {
                val outer = bounds("outer")
                val inner = bounds("inner")
                px(20f, inner.left - outer.left)
                px(60f, outer.right - inner.right)
                px(30f, inner.top - outer.top)
                px(40f, outer.bottom - inner.bottom)
            }
        }
    }

    @Test fun consumedSideSpacersHaveZeroRemainingWidth() {
        render {
            Row(Modifier.width(160.dp).height(20.dp)
                .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))) {
                Spacer(Modifier.fillMaxHeight().windowInsetsStartWidth(insets).testTag("start"))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.fillMaxHeight().windowInsetsEndWidth(insets).testTag("end"))
            }
        }.use {
            px(0f, bounds("start").width)
            px(0f, bounds("end").width)
        }
    }

    @Test fun pixelInsetsRemainExactAtDifferentDensities() {
        listOf(1f, 2.75f).forEach { density ->
            render(density = density) {
                Column {
                    Spacer(Modifier.width(20.dp).windowInsetsTopHeight(insets).testTag("top"))
                    Spacer(Modifier.width(20.dp).windowInsetsBottomHeight(insets).testTag("bottom"))
                }
            }.use {
                px(30f, bounds("top").height)
                px(40f, bounds("bottom").height)
            }
        }
    }

    @Test fun insetChangesAreRemeasuredWithoutRememberingStalePadding() {
        val mutable = MutableWindowInsets(insets)
        render {
            Box(Modifier.width(160.dp).height(100.dp).testTag("outer")
                .windowInsetsPadding(mutable.only(WindowInsetsSides.Horizontal))) {
                Box(Modifier.fillMaxSize().testTag("inner"))
            }
        }.use {
            px(20f, bounds("inner").left - bounds("outer").left)
            compose.runOnIdle { mutable.insets = WindowInsets(left = 45, right = 10) }
            px(45f, bounds("inner").left - bounds("outer").left)
            px(10f, bounds("outer").right - bounds("inner").right)
        }
    }

    @Test fun keyboardAndNavigationInsetsUseMaximumNotSum() {
        val keyboard = MutableWindowInsets(WindowInsets(bottom = 100))
        render {
            Box(Modifier.width(160.dp).height(100.dp).testTag("outer")
                .windowInsetsPadding(insets.union(keyboard).only(WindowInsetsSides.Bottom))) {
                Column(Modifier.fillMaxSize().testTag("inner")) {
                    Spacer(Modifier.width(20.dp).windowInsetsBottomHeight(insets).testTag("remaining"))
                }
            }
        }.use {
            px(100f, bounds("outer").bottom - bounds("inner").bottom)
            px(0f, bounds("remaining").height)
            compose.runOnIdle { keyboard.insets = WindowInsets(bottom = 0) }
            px(40f, bounds("outer").bottom - bounds("inner").bottom)
        }
    }
}
