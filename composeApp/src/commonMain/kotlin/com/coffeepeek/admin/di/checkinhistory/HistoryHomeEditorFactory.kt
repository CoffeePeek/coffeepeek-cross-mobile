package com.coffeepeek.admin.di.checkinhistory

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.analytics.CheckInSource
import com.coffeepeek.admin.di.shelf.ShelfHomeCheckInBags
import com.coffeepeek.admin.analytics.CheckInTracker
import com.coffeepeek.admin.di.homecheckin.AppHomeCheckInRepository
import com.coffeepeek.admin.di.homecheckin.appHomeCheckInEditor
import com.coffeepeek.admin.feature.catalog.api.BrewMethodCatalog
import com.coffeepeek.feature.coffee.api.CoffeePicker
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
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
        createHomeCheckInDetailEntry(AppHomeCheckInRepository(lease, repository),
            { lease.isLoggedIn && lease.isCurrent() }) { url, description, modifier, _ ->
            CoffeePeekImage(url, contentDescription = description, modifier = modifier, contentScale = ContentScale.Crop)
        },
        appHomeCheckInEditor(lease, repository, picker, methods, CheckInSource.Diary, tracker, shelfBags),
    )
}
