package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.coffeepeek.admin.di.checkinhistory.FeatureProfileActivityRenderer
import com.coffeepeek.feature.notifications.api.NotificationFeature
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
import kotlinx.coroutines.CancellationException

/** Both platforms use the feature Profile tab; navigation, native integrations and shared slots remain app-owned. */
internal class FeatureOwnProfileScreenRenderer(private val entry: OwnProfileEntry) : OwnProfileScreenRenderer {
    @Composable
    override fun Content() {
        entry.Content(
            onEditProfile = { Navigator.navigate(Navigator.Screen.EditProfile) },
            onOpenShop = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
            onOpenRoaster = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
            onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
            onOpenUser = { slug, nickname, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(slug, nickname, avatar)) },
            onOpenFavorites = {
                AppProfileAnalytics.favoritesOpened()
                Navigator.navigate(Navigator.Screen.Favorites)
            },
            onOpenCheckIns = { Navigator.navigate(Navigator.Screen.VisitedPlaces) },
            onOpenContributions = { kind -> Navigator.navigate(Navigator.Screen.MyContributions(kind.toContributionKind().name)) },
            onOpenBlockedAuthors = { Navigator.navigate(Navigator.Screen.BlockedAuthors) },
            modifier = Modifier,
            bottomClearance = LocalFloatingNavClearance.current,
        )
    }
}

private fun ProfileContribution.toContributionKind() = when (this) {
    ProfileContribution.Changes -> ContributionKind.Changes
    ProfileContribution.Shops -> ContributionKind.Shops
    ProfileContribution.Roasters -> ContributionKind.Roasters
}

/** The legacy logout order: push unregistration, server logout (failure shown), platform sign-out, then the signed-in user's local drafts. */
internal class AppProfileSignOut(
    private val notifications: NotificationFeature,
    private val auth: AuthRepository,
    private val drafts: ReviewDraftStore,
    private val platformSignOut: suspend () -> Unit = { GoogleAuth.signOut() },
) : ProfileSignOutPort {
    override suspend fun signOut() {
        notifications.beforeLogout()
        auth.logout().onFailure { error ->
            if (error is CancellationException) throw error
            ErrorHandler.showError(error.message?.takeIf(String::isNotBlank) ?: getString(Res.string.local_data_cleanup_error))
        }
        platformSignOut()
        drafts.clearAll()
    }
}

internal fun featureOwnProfileScreenRenderer(
    repository: PublicProfileRepository,
    sessions: SessionReader,
    users: UserRepository,
    notifications: NotificationFeature,
    auth: AuthRepository,
    drafts: ReviewDraftStore,
    visibility: AuthorVisibility,
    blocks: AuthorBlockActions,
    activity: FeatureProfileActivityRenderer,
): FeatureOwnProfileScreenRenderer {
    val checkIns = AppPublicCheckIns(repository)
    return FeatureOwnProfileScreenRenderer(createOwnProfileEntry(
        data = repository,
        checkIns = checkIns,
        accounts = AppProfileAccountPort(sessions, users),
        signOut = AppProfileSignOut(notifications, auth, drafts),
        visibility = AppProfileAuthorVisibility(visibility),
        authorActions = AppProfileAuthorActions(blocks),
        slots = ownProfileSlots(checkIns, activity),
    ))
}

private fun ownProfileSlots(checkIns: AppPublicCheckIns, activity: FeatureProfileActivityRenderer) = OwnProfileSlots(
    shared = profileSlots(checkIns),
    favoritesPreview = { openShop, openRoaster, header -> FavoritesProfilePreview(openShop, openRoaster, header) },
    activityCalendar = { openShop, openCheckIns -> activity.Calendar(openShop, openCheckIns) },
    activityPreview = { onClick -> activity.Week(onClick) },
    loadError = { onRetry -> LoadErrorContent(onRetry = onRetry) },
)
