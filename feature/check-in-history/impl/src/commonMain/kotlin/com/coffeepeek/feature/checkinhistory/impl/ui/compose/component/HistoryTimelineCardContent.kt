package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.feature.checkinhistory.domain.model.HomeHistoryEntry
import com.coffeepeek.feature.checkinhistory.domain.model.PersonalCheckIn

internal val LocalHistoryShopTimelineContent = staticCompositionLocalOf<(@Composable (PersonalCheckIn, () -> Unit) -> Unit)?> { null }
internal val LocalHistoryHomeTimelineContent = staticCompositionLocalOf<(@Composable (HomeHistoryEntry, () -> Unit) -> Unit)?> { null }
