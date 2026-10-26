package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.checkin_rating_coffee
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.admin.utils.datePickerMillisToUtcIsoInstant
import com.coffeepeek.admin.utils.formatVisitDate
import com.coffeepeek.admin.utils.utcIsoToLocalDate
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.impl.ui.data.*
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Instant
import com.coffeepeek.admin.utils.currentEpochMillis
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** App composition reuses the current catalogue, platform picker, artwork and consent-aware tracker. */
internal fun appHomeCheckInEditor(
    lease: CheckInHistorySession,
    repository: HomePreparationRepository,
    picker: CoffeePicker,
    methods: BrewMethodCatalog,
    source: CheckInSource,
    inheritedTracker: CheckInTracker? = null,
): HomeCheckInEditorEntry {
    var handoff = inheritedTracker
    return createHomeCheckInEditorEntry(AppHomeCheckInRepository(lease, repository),
        AppHomeCheckInCatalog(lease, picker, methods),
        { lease.isLoggedIn && lease.isCurrent() }, appHomeCheckInDates(),
        HomeCheckInEditorMedia(
            { Image(painterResource(Res.drawable.checkin_rating_coffee), null, it, contentScale = ContentScale.Crop) },
            { Image(painterResource(CoffeePeekIllustrations.CatalogSearch), null, it) },
            { name, modifier -> Icon(painterResource(brewMethodIcon(name)), null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            { photo, modifier, fullSize ->
                CoffeePeekImage(photo.bytes, modifier = modifier, contentScale = if (fullSize) ContentScale.Fit else ContentScale.Crop)
            }),
        AppHomeCheckInPhotoPicker(),
        analytics = { editing ->
            val inherited = if (!editing) handoff.also { handoff = null } else null
            AppHomeCheckInAnalytics({ lease.isLoggedIn && lease.isCurrent() }, editing, source, inherited)
        })
}

internal fun appHomeCheckInDates() = HomeCheckInEditorDates(
    currentInstant = { Instant.fromEpochMilliseconds(currentEpochMillis()).toString() },
    localDate = ::utcIsoToLocalDate,
    // The date comes from the server as text: one that cannot be read must not break the form.
    formatDate = { date -> civilDateMillis(date)?.let(::formatVisitDate) ?: date },
    civilDateToInstant = { date -> civilDateMillis(date)?.let(::datePickerMillisToUtcIsoInstant) ?: Instant.fromEpochMilliseconds(currentEpochMillis()).toString() },
    formatMethod = { it.methodLabel() },
)

internal fun civilDateMillis(date: String): Long? =
    try { LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() } catch (_: IllegalArgumentException) { null }
