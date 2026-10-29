package com.coffeepeek.feature.profile.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ProfileContribution { Changes, Shops, Roasters }

/** App-rendered pieces that only the owner's own profile shows. */
class OwnProfileSlots(
    val shared: ProfileSlots,
    /** Owns loading/retry states and omits [header] with its content when successfully loaded empty. */
    val favoritesPreview: @Composable (onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, header: @Composable () -> Unit) -> Unit,
    /**
     * The owner's personal activity calendar (private and home check-ins) with the check-ins of the selected day under it. Opening a shop or the check-in
     * history is asked through the callbacks, so the sheet that hosts the calendar can close first.
     */
    val activityCalendar: @Composable (onOpenShop: (String) -> Unit, onOpenCheckIns: () -> Unit) -> Unit,
    /** Current personal week, opening personal check-in history on tap. */
    val activityPreview: @Composable (onClick: () -> Unit) -> Unit,
    /** Mascot error content with a retry action. */
    val loadError: @Composable (onRetry: () -> Unit) -> Unit,
)

/** The Profile tab. Caller owns the tab, navigation, sign-in and the floating bar clearance. */
interface OwnProfileEntry {
    @Composable
    fun Content(
        onEditProfile: () -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onOpenRoaster: (slug: String) -> Unit,
        onLogin: () -> Unit,
        onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
        onOpenFavorites: () -> Unit,
        onOpenCheckIns: () -> Unit,
        onOpenContributions: (ProfileContribution) -> Unit,
        onOpenBlockedAuthors: () -> Unit,
        onOpenBusiness: () -> Unit = {},
        modifier: Modifier = Modifier,
        bottomClearance: Dp = 0.dp,
    )
}
