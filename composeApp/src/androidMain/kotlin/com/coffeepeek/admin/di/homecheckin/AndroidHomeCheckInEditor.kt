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
import com.coffeepeek.admin.di.checkinhistory.AndroidCheckInHistorySession
import com.coffeepeek.admin.di.shelf.ShelfHomeCheckInBags
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.formatVisitDate
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import org.jetbrains.compose.resources.painterResource
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Android composition reuses the current catalogue, platform picker, artwork and consent-aware tracker. */
internal fun androidHomeCheckInEditor(
    lease: AndroidCheckInHistorySession,
    repository: HomePreparationRepository,
    picker: CoffeePicker,
    methods: BrewMethodCatalog,
    source: CheckInSource,
    inheritedTracker: CheckInTracker? = null,
    shelfBags: ShelfHomeCheckInBags? = null,
): HomeCheckInEditorEntry {
    var handoff = inheritedTracker
    return createHomeCheckInEditorEntry(AndroidHomeCheckInRepository(lease, repository),
        AndroidHomeCheckInCatalog(lease, picker, methods, shelfBags),
        { lease.isLoggedIn && lease.isCurrent() }, androidHomeCheckInDates(),
        HomeCheckInEditorMedia(
            { Image(painterResource(Res.drawable.checkin_rating_coffee), null, it, contentScale = ContentScale.Crop) },
            { Image(painterResource(Res.drawable.maskot_with_magnifying_glass), null, it) },
            { name, modifier -> Icon(painterResource(brewMethodIcon(name)), null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            { photo, modifier, fullSize ->
                CpImage(photo.bytes, modifier = modifier, contentScale = if (fullSize) ContentScale.Fit else ContentScale.Crop)
            },
            // Shelf cards are owner-only: the protected loader adds this session.
            { url, modifier -> CpImage(url, modifier = modifier, contentScale = ContentScale.Fit) }),
        AndroidHomeCheckInPhotoPicker(),
        analytics = { editing ->
            val inherited = if (!editing) handoff.also { handoff = null } else null
            AndroidHomeCheckInAnalytics({ lease.isLoggedIn && lease.isCurrent() }, editing, source, inherited)
        })
}

internal fun androidHomeCheckInDates() = HomeCheckInEditorDates(
    currentInstant = { Instant.now().truncatedTo(ChronoUnit.MILLIS).toString() },
    localDate = ::utcIsoToLocalDate,
    // The date comes from the server as text: one that cannot be read must not break the form.
    formatDate = { date -> civilDateMillis(date)?.let(::formatVisitDate) ?: date },
    civilDateToInstant = { date -> civilDateMillis(date)?.let(::datePickerMillisToUtcIsoInstant) ?: Instant.now().truncatedTo(ChronoUnit.MILLIS).toString() },
    formatMethod = { it.methodLabel() },
)

internal fun civilDateMillis(date: String): Long? =
    try { LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() } catch (_: DateTimeParseException) { null }
