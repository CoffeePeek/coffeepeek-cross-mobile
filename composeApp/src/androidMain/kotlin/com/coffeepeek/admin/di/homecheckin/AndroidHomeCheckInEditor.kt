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
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.catalog.api.methodLabel
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.ui.component.DrinkCutoutImage
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
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Android composition reuses the current catalogue, platform picker, artwork and consent-aware tracker. */
internal fun androidHomeCheckInEditor(
    lease: AndroidCheckInHistorySession,
    repository: HomePreparationRepository,
    picker: CoffeePicker,
    methods: BrewMethodCatalog,
    extractor: DrinkPhotoExtractor,
    source: CheckInSource,
    inheritedTracker: CheckInTracker? = null,
): HomeCheckInEditorEntry {
    var handoff = inheritedTracker
    return createHomeCheckInEditorEntry(AndroidHomeCheckInRepository(lease, repository),
        AndroidHomeCheckInCatalog(lease, picker, methods), AndroidHomeCheckInPhotoProcessor(lease, extractor),
        { lease.isLoggedIn && lease.isCurrent() }, androidHomeCheckInDates(),
        HomeCheckInEditorMedia(
            { Image(painterResource(Res.drawable.checkin_rating_coffee), null, it, contentScale = ContentScale.Crop) },
            { Image(painterResource(Res.drawable.maskot_with_magnifying_glass), null, it) },
            { name, modifier -> Icon(painterResource(brewMethodIcon(name)), null, modifier, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            { photo, modifier, cutout ->
                if (cutout) DrinkCutoutImage(photo.bytes, modifier = modifier, outlineWidth = 2.dp)
                else CpImage(photo.bytes, modifier = modifier, contentScale = ContentScale.Crop)
            }),
        AndroidHomeCheckInPhotoPicker(),
        analytics = { editing ->
            val inherited = if (!editing) handoff.also { handoff = null } else null
            AndroidHomeCheckInAnalytics({ lease.isLoggedIn && lease.isCurrent() }, editing, source, inherited)
        })
}

internal fun androidHomeCheckInDates() = HomeCheckInEditorDates(
    currentInstant = { Instant.now().truncatedTo(ChronoUnit.MILLIS).toString() },
    localDate = ::utcIsoToLocalDate,
    formatDate = { date -> formatVisitDate(LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) },
    civilDateToInstant = { date -> datePickerMillisToUtcIsoInstant(LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) },
    formatMethod = { it.methodLabel() },
)
