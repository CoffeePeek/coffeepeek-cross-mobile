package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

internal sealed interface HomeCheckInDetailEvent {
    data object Close : HomeCheckInDetailEvent
    data class Edit(val preparationId: String) : HomeCheckInDetailEvent
    data class OpenPhoto(val urls: List<String>, val index: Int, val originalUrls: List<String> = urls) : HomeCheckInDetailEvent
    /** One confirmed outcome; the caller invalidates history and dismisses once. */
    data class Deleted(val preparationId: String) : HomeCheckInDetailEvent
}

internal data class HomeCheckInDetailEventEnvelope(
    val event: HomeCheckInDetailEvent,
    val sessionGeneration: Int,
    val revision: Long,
)
