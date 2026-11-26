package com.coffeepeek.feature.shelf.impl

import kotlinx.coroutines.flow.MutableStateFlow

/** The finish sheet's transient presentation choices; all persistence stays in the shared ViewModel. */
internal class ShelfNativeFinishDraft {
    val state = MutableStateFlow(Selection())
    val rating get() = state.value.rating
    val buyAgain get() = state.value.buyAgain
    fun reset() { state.value = Selection() }
    fun rate(value: Int) {
        if (value !in 1..5) return
        state.value = state.value.copy(rating = value.takeUnless { it == rating } ?: 0)
    }
    fun chooseAgain(value: Boolean) { state.value = state.value.copy(buyAgain = value.takeUnless { it == buyAgain }) }
    data class Selection(val rating: Int = 5, val buyAgain: Boolean? = null)
}
