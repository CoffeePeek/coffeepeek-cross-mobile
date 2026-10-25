package com.coffeepeek.core.designsystem.component

/** Shared calendar semantics for Compose cells and Android RemoteViews. */
data class CoffeePeekCalendarDayStyle(
    val showDate: Boolean,
    val outlined: Boolean,
    val selectedFill: Boolean,
    val emphasized: Boolean,
    val tone: CoffeePeekCalendarDayTone,
)

enum class CoffeePeekCalendarDayTone { Content, Accent, OnAccent, Muted }

fun coffeePeekCalendarDayStyle(isToday: Boolean, isFuture: Boolean, hasPhoto: Boolean,
    isSelected: Boolean = false): CoffeePeekCalendarDayStyle {
    val fill = isSelected && !isToday && !hasPhoto
    return CoffeePeekCalendarDayStyle(
        showDate = !hasPhoto,
        outlined = isToday || isSelected && hasPhoto,
        selectedFill = fill,
        emphasized = isToday || isSelected,
        tone = when {
            isToday -> CoffeePeekCalendarDayTone.Accent
            fill -> CoffeePeekCalendarDayTone.OnAccent
            isFuture -> CoffeePeekCalendarDayTone.Muted
            else -> CoffeePeekCalendarDayTone.Content
        },
    )
}
