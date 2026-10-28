package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Form body for an app-owned destination/sheet, without root navigation or another form sheet.
 * Null ID creates; any non-null ID edits that exact record (including an invalid/blank ID).
 * Caller provides a destination-owned ViewModelStoreOwner, retained across Activity recreation
 * and cleared on final close/replacement. Reopening must use a fresh store.
 * Caller supplies its lifecycle, reactive lease snapshot and generation, blocks outer dismissal
 * while requested, and refreshes once on Saved. Token renewal is not a generation change.
 * A new preparation may start with [bean] already chosen; editing ignores it.
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
        bean: HomeCheckInBean? = null,
    )
}

/** A coffee to start a new preparation with: a catalogue coffee by [slug] (shown as [name]), or else one of the user's own named [name]. */
data class HomeCheckInBean(val slug: String?, val name: String?)
