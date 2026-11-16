package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.component.coffeePeekCardBorder
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.reducedMotionDuration
import com.coffeepeek.feature.profile.api.OwnProfileActivity
import com.coffeepeek.feature.profile.api.ProfileCalendarHeaderContent
import com.coffeepeek.feature.profile.api.ProfileImage
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.domain.PublicProfileActivity
import com.coffeepeek.feature.profile.impl.resources.*
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.PublicProfileState

private const val CalendarMotionMillis = 220

@Composable
internal fun ProfileCheckInCalendar(
    state: PublicProfileState,
    profile: PublicProfile,
    image: ProfileImage,
    ownerActivity: OwnProfileActivity?,
    onAction: (PublicProfileAction) -> Unit,
    onSelectDate: (String?) -> Unit,
) {
    if (ownerActivity != null) {
        OwnerCheckInCalendar(ownerActivity, state.checkInDate, onSelectDate)
        return
    }
    ProfileCalendarFrame(profile.slug) { expanded, header ->
        PublicProfileActivityCalendar(
            profile.activity, state.activityMonth, state.isLoadingActivity, state.activityError, state.checkIns, image,
            onMonthChange = { onAction(PublicProfileAction.ChangeActivityMonth(it)) },
            onRetry = { onAction(PublicProfileAction.RetryActivity) },
            selectedDate = state.checkInDate,
            onDateSelect = onSelectDate,
            expanded = expanded,
            header = header,
            profileKey = profile.slug,
        )
    }
}

@Composable
internal fun OwnerCheckInCalendar(
    activity: OwnProfileActivity, selectedDate: String?, onSelectDate: (String?) -> Unit, modifier: Modifier = Modifier,
) {
    ProfileCalendarFrame(stateKey = null, modifier = modifier) { expanded, header ->
        activity.month(selectedDate, onSelectDate, expanded, header)
    }
}

@Composable
private fun ProfileCalendarFrame(
    stateKey: String?, modifier: Modifier = Modifier, initiallyExpanded: Boolean = false,
    content: @Composable (expanded: Boolean, header: ProfileCalendarHeaderContent) -> Unit,
) {
    var expanded by rememberSaveable(stateKey) { mutableStateOf(initiallyExpanded) }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        CoffeePeekCard(Modifier.widthIn(max = 420.dp).animateContentSize(tween(reducedMotionDuration(CalendarMotionMillis))),
            border = coffeePeekCardBorder(), elevation = 0.dp, contentPadding = PaddingValues(10.dp)) {
            content(expanded) { month, previous, next ->
                ProfileCalendarHeader(month, expanded, previous, next) { expanded = !expanded }
            }
        }
    }
}

@Composable
private fun ProfileCalendarHeader(
    month: String, expanded: Boolean, previous: (() -> Unit)?, next: (() -> Unit)?, onToggle: () -> Unit,
) {
    val description = stringResource(if (expanded) Res.string.profile_calendar_expanded else Res.string.profile_calendar_collapsed)
    val duration = reducedMotionDuration(CalendarMotionMillis)
    val disclosure = updateTransition(expanded, label = "profile calendar disclosure")
    val rotation by disclosure.animateFloat(transitionSpec = { tween(duration) }, label = "calendar chevron") {
        if (it) 180f else 0f
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            AnimatedProfileMonthArrow(disclosure, previous = true, onClick = previous, duration = duration)
            // The title stays a touch shortcut. The chevron is the sole accessible disclosure button.
            CoffeePeekTextButton(onToggle, Modifier.weight(1f, fill = false)
                .focusProperties { canFocus = false }.clearAndSetSemantics {},
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                AnimatedProfileMonthText(month, duration)
            }
            AnimatedProfileMonthArrow(disclosure, previous = false, onClick = next, duration = duration)
        }
        CoffeePeekIconButton(onToggle, Modifier.semantics { stateDescription = description },
            glass = false, contentDescription = month) {
            CoffeePeekIcon(CoffeePeekIcons.ChevronDown, null, Modifier.size(18.dp).rotate(rotation))
        }
    }
}

@Composable
private fun AnimatedProfileMonthText(month: String, duration: Int) {
    AnimatedContent(
        targetState = month,
        transitionSpec = {
            (fadeIn(tween(duration)) togetherWith fadeOut(tween(duration)))
                .using(SizeTransform { _, _ -> tween(duration) })
        },
        label = "calendar month title",
    ) { shownMonth ->
        Text(shownMonth, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AnimatedProfileMonthArrow(
    disclosure: Transition<Boolean>, previous: Boolean, onClick: (() -> Unit)?, duration: Int,
) {
    val anchor = if (previous) Alignment.End else Alignment.Start
    disclosure.AnimatedVisibility(
        visible = { it },
        enter = expandHorizontally(tween(duration), expandFrom = anchor) + fadeIn(tween(duration)),
        exit = shrinkHorizontally(tween(duration), shrinkTowards = anchor) + fadeOut(tween(duration)),
    ) {
        ProfileMonthArrow(previous, onClick)
    }
}

@Composable
private fun ProfileMonthArrow(previous: Boolean, onClick: (() -> Unit)?) {
    val pointsLeft = previous == (LocalLayoutDirection.current == LayoutDirection.Ltr)
    val label = stringResource(if (previous) Res.string.profile_previous_month else Res.string.profile_next_month)
    CoffeePeekIconButton(onClick = { onClick?.invoke() }, enabled = onClick != null, glass = false, contentDescription = label) {
        CoffeePeekIcon(if (pointsLeft) CoffeePeekIcons.ChevronLeft else CoffeePeekIcons.ChevronRight, null, Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileCheckInCalendarPreview(darkTheme: Boolean, initiallyExpanded: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    val activity = PublicProfileActivity("2026-11", setOf(10, 11, 15, 16))
    ProfileCalendarFrame("demo", Modifier.width(280.dp), initiallyExpanded = initiallyExpanded) { expanded, header ->
        PublicProfileActivityCalendar(activity, activity.month, false, null, emptyList(), { _, _, _ -> },
            {}, {}, "2026-11-16", {}, expanded, header, today = "2026-11-16")
    }
}

@Preview @Composable private fun ProfileCheckInCalendarLightPreview() = ProfileCheckInCalendarPreview(false, false)
@Preview @Composable private fun ProfileCheckInCalendarDarkPreview() = ProfileCheckInCalendarPreview(true, false)
@Preview @Composable private fun ProfileCheckInCalendarExpandedLightPreview() = ProfileCheckInCalendarPreview(false, true)
@Preview @Composable private fun ProfileCheckInCalendarExpandedDarkPreview() = ProfileCheckInCalendarPreview(true, true)
