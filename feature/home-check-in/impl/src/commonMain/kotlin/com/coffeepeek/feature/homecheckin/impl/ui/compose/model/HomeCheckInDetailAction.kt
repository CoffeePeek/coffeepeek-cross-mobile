package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

internal sealed interface HomeCheckInDetailAction {
    data object Reload : HomeCheckInDetailAction
    data object Close : HomeCheckInDetailAction
    data object Edit : HomeCheckInDetailAction
    data class OpenPhoto(val index: Int) : HomeCheckInDetailAction
    data object OpenDrinkPhoto : HomeCheckInDetailAction
    data object RequestDelete : HomeCheckInDetailAction
    data object CancelDelete : HomeCheckInDetailAction
    data object ConfirmDelete : HomeCheckInDetailAction
    /** This instance belongs to one account lease; another account needs a new owner/VM. */
    data object RevokeAccess : HomeCheckInDetailAction
}
