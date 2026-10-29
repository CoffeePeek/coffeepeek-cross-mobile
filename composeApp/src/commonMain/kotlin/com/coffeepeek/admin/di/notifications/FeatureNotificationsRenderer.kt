package com.coffeepeek.admin.di.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.admin.feature.notifications.ui.NotificationsRenderer
import com.coffeepeek.admin.notifications.NotificationOpens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.notifications.api.NotificationsEntry
import com.coffeepeek.feature.notifications.impl.createNotificationsEntry
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.notifications_open_error
import org.jetbrains.compose.resources.getString

/** Android and iOS compose the migrated bell and sheet; signing in is the application's navigation. */
internal class FeatureNotificationsRenderer(
    private val entry: NotificationsEntry,
    private val feature: NotificationFeature,
    private val navigation: NotificationNavigation,
    private val currentUserId: () -> String?,
) : NotificationsRenderer {
    @Composable
    override fun Bell() = entry.Bell()

    @Composable
    override fun HandlePendingOpen(isLoggedIn: Boolean) {
        val request by NotificationOpens.pending.collectAsStateWithLifecycle()
        val state by feature.state.collectAsStateWithLifecycle()
        LaunchedEffect(request, isLoggedIn, state.accountId, state.generation) {
            val pending = request ?: return@LaunchedEffect
            if (isLoggedIn && (state.accountId == null || state.accountId != currentUserId())) return@LaunchedEffect
            try {
                if (isLoggedIn && state.accountId == pending.accountId) {
                    navigation.open(pending.id, pending.accountId, state.generation)
                }
            } finally {
                NotificationOpens.consume(pending)
            }
        }
    }
}

internal fun featureNotificationsRenderer(
    feature: NotificationFeature, contributions: ContributionsRepository, sessions: SessionReader,
): NotificationsRenderer {
    val currentUser = { sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId }
    val navigation = NotificationNavigation(feature, contributions, currentUser, Navigator::openAppLink) {
        ErrorHandler.showError(getString(Res.string.notifications_open_error))
    }
    val entry = createNotificationsEntry(feature, { Navigator.navigate(Navigator.Screen.Auth) }, navigation::open)
    return FeatureNotificationsRenderer(entry, feature, navigation, currentUser)
}
