package com.coffeepeek.feature.homecheckin.impl.ui.data

/** App-owned zone/format conventions. currentInstant must keep server-compatible millisecond precision. */
class HomeCheckInEditorDates(
    val currentInstant: () -> String,
    val localDate: (String) -> String,
    val formatDate: (String) -> String,
    val civilDateToInstant: (String) -> String,
    val formatMethod: (String) -> String,
)
