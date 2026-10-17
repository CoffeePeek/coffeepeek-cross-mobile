package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.homecheckin.AndroidHomeCheckInRepository
import com.coffeepeek.admin.di.homecheckin.androidHomeCheckInEditor
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.admin.feature.checkin.domain.DrinkPhotoExtractor
import com.coffeepeek.admin.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.ui.component.DrinkCutoutImage
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.feature.homecheckin.api.HomeCheckInDetailEntry
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInDetailEntry

/** App-owned assembly; history sees neither the Home ViewModel nor a legacy writer. */
internal fun interface HistoryHomeEditorFactory {
    fun create(lease: AndroidCheckInHistorySession, tracker: CheckInTracker?): HistoryHomeEntries
}

internal class HistoryHomeEntries(val detail: HomeCheckInDetailEntry, val editor: HomeCheckInEditorEntry)

internal fun historyHomeEditorFactory(repository: HomePreparationRepository, picker: CoffeePicker,
    methods: BrewMethodCatalog, extractor: DrinkPhotoExtractor,
) = HistoryHomeEditorFactory { lease, tracker ->
    HistoryHomeEntries(
        createHomeCheckInDetailEntry(AndroidHomeCheckInRepository(lease, repository),
            { lease.isLoggedIn && lease.isCurrent() }) { url, description, modifier, cutout ->
            if (cutout) DrinkCutoutImage(url, modifier = modifier, outlineWidth = 3.dp)
            else CpImage(url, contentDescription = description, modifier = modifier, contentScale = ContentScale.Crop)
        },
        androidHomeCheckInEditor(lease, repository, picker, methods, extractor, CheckInSource.Diary, tracker),
    )
}
