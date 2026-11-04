package com.coffeepeek.feature.shelf.impl.ui.compose.model

internal data class ShelfProfileVisibilityState(
    val signedIn: Boolean = false,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val visible: Boolean = false,
    val error: ShelfProfileVisibilityError? = null,
)

internal enum class ShelfProfileVisibilityError { Load, Save }

internal sealed interface ShelfProfileVisibilityAction {
    data object Refresh : ShelfProfileVisibilityAction
    data object SignIn : ShelfProfileVisibilityAction
    data class SetVisible(val value: Boolean) : ShelfProfileVisibilityAction
}

internal enum class ShelfProfileVisibilityEvent { SignIn }
