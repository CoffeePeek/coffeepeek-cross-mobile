package com.coffeepeek.admin.di.homecheckin

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import coffeepeek.composeapp.generated.resources.maskot_with_magnifying_glass
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.shelf.ShelfHomeCheckInBags
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.currentEpochMillis
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.epochMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.formatVisitDate
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import org.jetbrains.compose.resources.painterResource

/** Android composition reuses the current catalogue, platform picker, artwork and consent-aware tracker. */
internal fun homeCheckInEditor(
    lease: CheckInHistorySession,
    repository: HomePreparationRepository,
    picker: CoffeePicker,
    methods: BrewMethodCatalog,
    source: CheckInSource,
    inheritedTracker: CheckInTracker? = null,
    shelfBags: ShelfHomeCheckInBags? = null,
): HomeCheckInEditorEntry {
    var handoff = inheritedTracker
    return createHomeCheckInEditorEntry(HomeCheckInAppRepository(lease, repository),
        HomeCheckInAppCatalog(lease, picker, methods, shelfBags),
        { lease.isLoggedIn && lease.isCurrent() }, homeCheckInDates(),
        HomeCheckInEditorMedia(
            { Image(painterResource(Res.drawable.checkin_rating_coffee), null, it, contentScale = ContentScale.Crop) },
            { Image(painterResource(Res.drawable.maskot_with_magnifying_glass), null, it) },
            { name, modifier -> Icon(painterResource(brewMethodIcon(name)), null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            { photo, modifier, fullSize ->
                CpImage(photo.bytes, modifier = modifier, contentScale = if (fullSize) ContentScale.Fit else ContentScale.Crop)
            },
            // Shelf cards are owner-only: the protected loader adds this session.
            { url, modifier -> CpImage(url, modifier = modifier, contentScale = ContentScale.Fit) }),
        HomeCheckInAppPhotoPicker(),
        analytics = { editing ->
            val inherited = if (!editing) handoff.also { handoff = null } else null
            HomeCheckInTrackerAnalytics({ lease.isLoggedIn && lease.isCurrent() }, editing, source, inherited)
        })
}

internal fun homeCheckInDates() = HomeCheckInEditorDates(
    currentInstant = { epochMillisToUtcIsoInstant(currentEpochMillis()) },
    localDate = ::utcIsoToLocalDate,
    // The date comes from the server as text: one that cannot be read must not break the form.
    formatDate = { date -> civilDateMillis(date)?.let(::formatVisitDate) ?: date },
    civilDateToInstant = { date -> civilDateMillis(date)?.let(::datePickerMillisToUtcIsoInstant) ?: epochMillisToUtcIsoInstant(currentEpochMillis()) },
    formatMethod = { it.methodLabel(it) },
)

/** UTC midnight of an ISO `yyyy-MM-dd` date, or null when the text is not one. */
@OptIn(ExperimentalTime::class)
internal fun civilDateMillis(date: String): Long? = runCatching { Instant.parse("${date}T00:00:00Z") }.getOrNull()?.toEpochMilliseconds()
