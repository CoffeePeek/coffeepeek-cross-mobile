package com.coffeepeek.feature.homecheckin.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Body for an app-owned detail destination/modal; does not create another sheet.
 * The caller owns navigation, localized date/method labels and account generation.
 * [accessAllowed] is a reactive account-lease snapshot, not just token availability.
 * Destroy the destination's lifecycle owner when it closes. A new account needs a
 * new lease and generation; token renewal alone must not change that generation.
 * Overlays need their own ViewModelStoreOwner, not the containing history screen's
 * retained store; closing and reopening a detail must create a fresh ViewModel.
 * Block the outer sheet's swipe/outside dismissal while [onDismissAllowedChange]
 * reports false (confirmed deletion in flight).
 */
interface HomeCheckInDetailEntry {
    @Composable
    fun Content(
        preparationId: String,
        sessionGeneration: Int,
        accessAllowed: Boolean,
        formatPreparedAt: (civilDate: String, instant: String) -> String,
        formatBrewMethod: (code: String) -> String,
        onClose: () -> Unit,
        onEdit: (preparationId: String) -> Unit,
        onDeleted: (preparationId: String) -> Unit,
        onOpenPhoto: (urls: List<String>, initialIndex: Int) -> Unit,
        onDismissAllowedChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        notice: String? = null,
    )
}
