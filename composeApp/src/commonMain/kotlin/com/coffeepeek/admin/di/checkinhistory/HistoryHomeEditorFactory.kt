package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.di.shelf.ShelfHomeCheckInBags
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.homecheckin.HomeCheckInAppRepository
import com.coffeepeek.admin.di.homecheckin.homeCheckInEditor
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.feature.homecheckin.api.HomeCheckInDetailEntry
import com.coffeepeek.feature.homecheckin.api.HomeCheckInEditorEntry
import com.coffeepeek.feature.homecheckin.domain.repository.HomePreparationRepository
import com.coffeepeek.feature.homecheckin.impl.createHomeCheckInDetailEntry

/** App-owned assembly; history sees neither the Home ViewModel nor a legacy writer. */
internal fun interface HistoryHomeEditorFactory {
    fun create(lease: CheckInHistorySession, tracker: CheckInTracker?): HistoryHomeEntries
}

internal class HistoryHomeEntries(val detail: HomeCheckInDetailEntry, val editor: HomeCheckInEditorEntry)

internal fun historyHomeEditorFactory(repository: HomePreparationRepository, picker: CoffeePicker,
    methods: BrewMethodCatalog, shelfBags: ShelfHomeCheckInBags? = null,
) = HistoryHomeEditorFactory { lease, tracker ->
    HistoryHomeEntries(
        createHomeCheckInDetailEntry(HomeCheckInAppRepository(lease, repository),
            { lease.isLoggedIn && lease.isCurrent() }) { url, description, modifier, _ ->
            CpImage(url, contentDescription = description, modifier = modifier, contentScale = ContentScale.Crop)
        },
        homeCheckInEditor(lease, repository, picker, methods, CheckInSource.Diary, tracker, shelfBags),
    )
}
