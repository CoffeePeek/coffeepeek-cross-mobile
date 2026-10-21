package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.local_data_cleanup_error
import com.coffeepeek.admin.auth.GoogleAuth
import com.coffeepeek.admin.feature.favorites.api.FavoritesProfilePreview
import com.coffeepeek.admin.feature.profile.domain.PublicProfileRepository
import com.coffeepeek.admin.settings.ReviewDraftStore
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.LoadErrorContent
import com.coffeepeek.admin.ui.component.LocalFloatingNavClearance
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.admin.ui.screen.profile.OwnProfileScreenRenderer
import com.coffeepeek.admin.ui.screen.profile.PersonalProfileActivityCalendar
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.domain.repository.AuthRepository
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.authorblocking.api.AuthorBlockActions
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import com.coffeepeek.feature.profile.api.OwnProfileEntry
import com.coffeepeek.feature.profile.api.OwnProfileSlots
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.api.ProfileSignOutPort
import com.coffeepeek.feature.profile.impl.createOwnProfileEntry
import org.jetbrains.compose.resources.getString

/** Android routes the Profile tab to the migrated entry; navigation, analytics, sign-out and legacy-rendered pieces stay app-owned. */
internal class AndroidOwnProfileScreenRenderer(private val entry: OwnProfileEntry) : OwnProfileScreenRenderer {
    @Composable
    override fun Content() {
        entry.Content(
            onEditProfile = { Navigator.navigate(Navigator.Screen.EditProfile) },
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenUser = { slug, nickname, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, nickname, avatar)) },
            onOpenFavorites = {
                AndroidProfileAnalytics.favoritesOpened()
                Navigator.navigate(Navigator.Screen.Favorites)
            },
            onOpenCheckIns = { Navigator.navigate(Navigator.Screen.VisitedPlaces) },
            onOpenContributions = { kind -> Navigator.navigate(Navigator.Screen.MyContributions(kind.toContributionKind().name)) },
            bottomClearance = LocalFloatingNavClearance.current,
        )
    }
}

private fun ProfileContribution.toContributionKind() = when (this) {
    ProfileContribution.Changes -> ContributionKind.Changes
    ProfileContribution.Shops -> ContributionKind.Shops
    ProfileContribution.Roasters -> ContributionKind.Roasters
}

/** The legacy logout order: server logout (failure shown), platform sign-out, then the signed-in user's local drafts. */
internal class AndroidProfileSignOut(
    private val auth: AuthRepository,
    private val drafts: ReviewDraftStore,
    private val platformSignOut: suspend () -> Unit = { GoogleAuth.signOut() },
) : ProfileSignOutPort {
    override suspend fun signOut() {
        auth.logout().onFailure { error ->
            ErrorHandler.showError(error.message?.takeIf(String::isNotBlank) ?: getString(Res.string.local_data_cleanup_error))
        }
        platformSignOut()
        drafts.clearAll()
    }
}

internal fun androidOwnProfileScreenRenderer(
    repository: PublicProfileRepository,
    sessions: SessionReader,
    users: UserRepository,
    auth: AuthRepository,
    drafts: ReviewDraftStore,
    visibility: AuthorVisibility,
    blocks: AuthorBlockActions,
): AndroidOwnProfileScreenRenderer {
    val checkIns = AndroidPublicCheckIns(repository)
    val slots = OwnProfileSlots(
        shared = androidProfileSlots(checkIns),
        favoritesPreview = { openShop, openRoaster -> FavoritesProfilePreview(openShop, openRoaster) },
        activityCalendar = { PersonalProfileActivityCalendar() },
        loadError = { onRetry -> LoadErrorContent(onRetry = onRetry) },
    )
    return AndroidOwnProfileScreenRenderer(createOwnProfileEntry(
        data = repository,
        checkIns = checkIns,
        accounts = AndroidProfileAccountPort(sessions, users),
        signOut = AndroidProfileSignOut(auth, drafts),
        visibility = AndroidProfileAuthorVisibility(visibility),
        authorActions = AndroidProfileAuthorActions(blocks),
        slots = slots,
    ))
}
