package com.coffeepeek.feature.profile.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

enum class ProfileContribution { Changes, Shops, Roasters }

/** Editable shelf supplied by the app to the owner's profile sections. */
class OwnProfileShelf(
    val content: @Composable (Modifier, @Composable () -> Unit) -> Unit,
    val openRequested: @Composable () -> Boolean,
    val onOpened: () -> Unit,
)

/** The localized month and independent navigation actions for the public profile calendar. */
typealias ProfileCalendarHeaderContent = @Composable (month: String, previous: (() -> Unit)?, next: (() -> Unit)?) -> Unit

/** Profile arranges these slots; the history feature owns their shared state and scrolling. */
class OwnProfileHistory(
    val calendar: @Composable () -> Unit,
    val list: @Composable (header: @Composable () -> Unit) -> Unit,
    val selectionRevision: Int,
)

/** A pending section intent is consumed after selection; it is not saved profile state. */
class OwnProfileSectionRequest(
    val requested: @Composable () -> Boolean,
    val onConsumed: () -> Unit,
)

/** App-rendered pieces that only the owner's own profile shows. */
class OwnProfileSlots(
    val shared: ProfileSlots,
    /** Owns loading/retry states and omits [header] with its content when successfully loaded empty. */
    val favoritesPreview: @Composable (onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, header: @Composable () -> Unit) -> Unit,
    /** One account-bound history owns the calendar and paginated list across profile tabs. */
    val history: @Composable (content: @Composable (OwnProfileHistory) -> Unit) -> Unit,
    /** Mascot error content with a retry action. */
    val loadError: @Composable (onRetry: () -> Unit) -> Unit,
    val shelf: OwnProfileShelf? = null,
    val collectionsRequest: OwnProfileSectionRequest? = null,
)

/** The Profile tab. Caller owns the tab, navigation, sign-in and the floating bar clearance. *
 * No default arguments: Kotlin/Native links an implementation in another module against the abstract Composable's exact
 * signature, and Compose makes defaulted parameters of the override nullable, so defaults would break iOS.
 */
interface OwnProfileEntry {
    @Composable
    fun GuestContent(bottomClearance: Dp, accountPrompt: @Composable () -> Unit)

    @Composable
    fun Content(
        onEditProfile: () -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onOpenRoaster: (slug: String) -> Unit,
        onLogin: () -> Unit,
        onOpenUser: (slug: String, nickname: String, avatarUrl: String?) -> Unit,
        onOpenFavorites: () -> Unit,
        onOpenContributions: (ProfileContribution) -> Unit,
        onOpenBlockedAuthors: () -> Unit,
        modifier: Modifier,
        bottomClearance: Dp,
    )
}
