package com.coffeepeek.feature.checkinhistory.impl.ui.compose.model

internal sealed interface CheckInHistoryEvent {
    data object Back : CheckInHistoryEvent
    data object SignIn : CheckInHistoryEvent
    data class OpenShop(val slug: String) : CheckInHistoryEvent
    data class OpenPhoto(val urls: List<String>, val index: Int) : CheckInHistoryEvent
}

internal data class CheckInHistoryEventEnvelope(val event: CheckInHistoryEvent, val sessionGeneration: Int) {
    fun isDeliverableTo(generation: Int, isLoggedIn: Boolean): Boolean = sessionGeneration == generation && when (event) {
        CheckInHistoryEvent.Back -> true
        CheckInHistoryEvent.SignIn -> !isLoggedIn
        is CheckInHistoryEvent.OpenShop, is CheckInHistoryEvent.OpenPhoto -> isLoggedIn
    }
}
