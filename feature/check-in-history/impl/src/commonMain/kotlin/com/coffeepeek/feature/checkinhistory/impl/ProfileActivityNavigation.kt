package com.coffeepeek.feature.checkinhistory.impl

import com.coffeepeek.feature.checkinhistory.impl.ui.compose.model.ProfileActivityEvent

/** Both profile presentations preserve the destination carried by the selected diary row. */
internal fun routeProfileActivityEvent(event: ProfileActivityEvent, onOpenShop: (String) -> Unit, onOpenHistory: () -> Unit) {
    when (event) {
        is ProfileActivityEvent.OpenShop -> onOpenShop(event.slug)
        ProfileActivityEvent.OpenHistory, ProfileActivityEvent.OpenWeek -> onOpenHistory()
    }
}
