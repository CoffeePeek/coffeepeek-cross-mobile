package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Form body for an app-owned destination/sheet, without root navigation or another form sheet.
 * Null ID creates; any non-null ID edits that exact record (including an invalid/blank ID).
 * Owns a fresh form ViewModel store while composed; removing/reopening Content resets it.
 * Caller supplies its lifecycle, reactive lease snapshot and generation, blocks outer dismissal
 * while requested, and refreshes once on Saved. Token renewal is not a generation change.
 */
interface HomeCheckInEditorEntry {
    @Composable
    fun Content(
        preparationId: String?,
        sessionGeneration: Int,
        accessAllowed: Boolean,
        onClose: () -> Unit,
        onSaved: (preparationId: String, visibilityUnconfirmed: Boolean) -> Unit,
        onDismissAllowedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
    )
}
