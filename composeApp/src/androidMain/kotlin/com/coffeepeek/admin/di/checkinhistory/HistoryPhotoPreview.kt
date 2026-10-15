package com.coffeepeek.admin.di.checkinhistory

/** Process-only account-owned viewer state; never SavedState or application-global. */
internal data class HistoryPhotoPreview(val urls: List<String>, val initialIndex: Int)
