package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

internal sealed interface HomeCheckInEditorEvent {
    data object Close : HomeCheckInEditorEvent
    /** Caller refreshes once, reuses the saved ID and owns the localized visibility notice. */
    data class Saved(val preparationId: String, val visibilityUnconfirmed: Boolean) : HomeCheckInEditorEvent
}

internal data class HomeCheckInEditorEventEnvelope(
    val event: HomeCheckInEditorEvent,
    val sessionGeneration: Int,
    val revision: Long,
)
