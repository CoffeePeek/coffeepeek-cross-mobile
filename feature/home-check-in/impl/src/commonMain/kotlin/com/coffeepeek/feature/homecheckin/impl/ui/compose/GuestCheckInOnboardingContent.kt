package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal enum class GuestCheckInOnboardingPage(
    val poster: DrawableResource,
    val title: StringResource,
    val description: StringResource,
) {
    Cover(
        Res.drawable.home_check_in_onboarding_poster_cover,
        Res.string.home_check_in_onboarding_cover_title,
        Res.string.home_check_in_onboarding_poster_cover,
    ),
    Discovery(
        Res.drawable.home_check_in_onboarding_poster_discovery,
        Res.string.home_check_in_onboarding_discovery_title,
        Res.string.home_check_in_onboarding_poster_discovery,
    ),
    Atmosphere(
        Res.drawable.home_check_in_onboarding_poster_atmosphere,
        Res.string.home_check_in_onboarding_atmosphere_title,
        Res.string.home_check_in_onboarding_poster_atmosphere,
    ),
    CheckIns(
        Res.drawable.home_check_in_onboarding_poster_check_ins,
        Res.string.home_check_in_onboarding_check_ins_title,
        Res.string.home_check_in_onboarding_poster_check_ins,
    ),
    Favorites(
        Res.drawable.home_check_in_onboarding_poster_favorites,
        Res.string.home_check_in_onboarding_favorites_title,
        Res.string.home_check_in_onboarding_poster_favorites,
    ),
}

@Composable
internal fun GuestCheckInOnboardingContent(
    onContinueAsGuest: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    pager: PagerState = rememberOnboardingPager(),
    hostInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
) {
    // A long press freezes the autoplay where it is, like holding a story.
    var held by remember { mutableStateOf(false) }
    GuestCheckInOnboardingAutoAdvance(pager, held)
    val colors = MaterialTheme.colorScheme
    val insets = hostInsets.union(WindowInsets.safeDrawing).union(WindowInsets.safeContent.only(WindowInsetsSides.Bottom))
    Column(
        Modifier.fillMaxSize().background(colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.fillMaxWidth().background(colors.surfaceVariant).windowInsetsTopHeight(insets))
        Column(
            Modifier.weight(1f).windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))
                .widthIn(max = 480.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GuestCheckInOnboardingPager(pager, onHold = { held = it })
                CoffeePeekIconButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier.align(Alignment.TopEnd).padding(CoffeePeekDimens.spacing2),
                    tint = colors.surfaceVariant,
                    contentDescription = stringResource(Res.string.home_check_in_onboarding_guest),
                ) {
                    CoffeePeekIcon(CoffeePeekIcons.Close, contentDescription = null)
                }
            }
            GuestCheckInOnboardingIndicator(pager)
        }
        GuestCheckInOnboardingActions(onRegister, onSignIn, insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
    }
}

@Composable
private fun GuestCheckInOnboardingPager(pager: PagerState, onHold: (Boolean) -> Unit) {
    val pages = GuestCheckInOnboardingPage.entries
    val position = stringResource(Res.string.home_check_in_onboarding_step, pager.currentPage % pages.size + 1, pages.size)
    HorizontalPager(
        pager,
        Modifier.fillMaxSize().onboardingHold(onHold).semantics {
            collectionInfo = CollectionInfo(1, pages.size)
            stateDescription = position
        },
    ) { page ->
        GuestCheckInOnboardingSlide(pages[page % pages.size])
    }
}

/** Observes press-and-hold over the pager without consuming any pointer events. */
private fun Modifier.onboardingHold(onHold: (Boolean) -> Unit) = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onHold(true)
        waitForUpOrCancellation(PointerEventPass.Initial)
        onHold(false)
    }
}

@Composable
private fun rememberOnboardingPager(initialIndex: Int = 0): PagerState {
    val pageCount = GuestCheckInOnboardingPage.entries.size
    val middle = Int.MAX_VALUE / 2
    return rememberPagerState(initialPage = middle - middle % pageCount + initialIndex) { Int.MAX_VALUE }
}

@Composable
private fun GuestCheckInOnboardingActions(onRegister: () -> Unit, onSignIn: () -> Unit, insets: WindowInsets) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = CoffeePeekDimens.radius2xl, topEnd = CoffeePeekDimens.radius2xl),
        shadowElevation = 3.dp,
    ) {
        Box(Modifier.fillMaxWidth().windowInsetsPadding(insets), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth()
                    .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
            ) {
                CoffeePeekButton(stringResource(Res.string.home_check_in_onboarding_register), onRegister, useGlass = false)
                CoffeePeekOutlinedButton(
                    onSignIn,
                    Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.buttonHeight),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    useGlass = false,
                ) {
                    Text(stringResource(Res.string.home_check_in_onboarding_sign_in), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GuestCheckInOnboardingLightPreview() = CoffeePeekTheme(darkTheme = false) {
    GuestCheckInOnboardingContent({}, {}, {}, hostInsets = WindowInsets(top = 24.dp, bottom = 24.dp))
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GuestCheckInOnboardingDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    GuestCheckInOnboardingContent(
        {}, {}, {},
        pager = rememberOnboardingPager(initialIndex = 2),
        hostInsets = WindowInsets(top = 24.dp, bottom = 34.dp),
    )
}
