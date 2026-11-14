package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.lerp
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_onboarding_step
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun GuestCheckInOnboardingIndicator(pager: PagerState) {
    val scope = rememberCoroutineScope()
    val slideCount = GuestCheckInOnboardingPage.entries.size
    Row(Modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
        repeat(slideCount) { index ->
            val selected = index == pager.currentPage % slideCount
            val label = coffeePeekStringResource(Res.string.home_check_in_onboarding_step, index + 1, slideCount)
            Box(
                Modifier.size(CoffeePeekDimens.minimumTouchTarget)
                    .clip(CircleShape)
                    .selectable(selected = selected, role = Role.Tab) {
                        scope.launch { pager.animateScrollToPage(nearestOnboardingPage(pager.currentPage, index, slideCount)) }
                    }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                GuestCheckInOnboardingDot(pager, index, slideCount)
            }
        }
    }
}

@Composable
private fun GuestCheckInOnboardingDot(pager: PagerState, index: Int, slideCount: Int) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.size(CoffeePeekDimens.spacing5, CoffeePeekDimens.spacing2).drawBehind {
        val currentPage = pager.currentPage
        val delta = nearestOnboardingPage(currentPage, index, slideCount) - currentPage
        val emphasis = (1f - abs(delta - pager.currentPageOffsetFraction)).coerceIn(0f, 1f)
        val width = lerp(CoffeePeekDimens.spacing2.toPx(), size.width, emphasis)
        drawRoundRect(
            color = lerp(colors.outlineVariant, colors.primary, emphasis),
            topLeft = Offset((size.width - width) / 2f, 0f),
            size = Size(width, size.height),
            cornerRadius = CornerRadius(size.height / 2f),
        )
    })
}

private fun nearestOnboardingPage(currentPage: Int, index: Int, slideCount: Int): Int {
    val forward = (index - currentPage % slideCount + slideCount) % slideCount
    return currentPage + if (forward <= slideCount / 2) forward else forward - slideCount
}
