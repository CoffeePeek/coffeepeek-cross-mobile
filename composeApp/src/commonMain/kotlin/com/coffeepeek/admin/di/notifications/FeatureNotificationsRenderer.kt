package com.coffeepeek.admin.di.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.screen.notifications.NotificationsRenderer
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Android and iOS compose the migrated bell and sheet; signing in is the application's navigation. */
internal class FeatureNotificationsRenderer(
    private val entry: NotificationsEntry,
    private val feature: NotificationFeature,
    private val navigation: NotificationNavigation,
    private val currentUserId: () -> String?,
) : NotificationsRenderer {
    private val pending = PendingNotificationOpens(feature.state, currentUserId,
        { request, generation -> navigation.open(request.id, request.accountId, generation) })
    override suspend fun open(id: String, accountId: String, generation: Long): Boolean =
        navigation.open(id, accountId, generation)
    override suspend fun observePendingOpen(loggedIn: Flow<Boolean>) = pending.observe(loggedIn)

    @Composable
    override fun Bell() = entry.Bell()

    @Composable
    override fun Preference(label: String, description: String) = entry.Preference(label, description)

    @Composable
    override fun HandlePendingOpen(isLoggedIn: Boolean) {
        LaunchedEffect(isLoggedIn) { observePendingOpen(flowOf(isLoggedIn)) }
    }
}

internal fun featureNotificationsRenderer(
    feature: NotificationFeature, contributions: ContributionsRepository, sessions: SessionReader,
): NotificationsRenderer {
    val currentUser = { sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId }
    val navigation = NotificationNavigation(feature, contributions, currentUser, { openNotificationDestination(it) }) {
        ErrorHandler.showError(getString(Res.string.notifications_open_error))
    }
    val entry = createNotificationsEntry(feature, { Navigator.navigate(Navigator.Screen.Auth) }, navigation::open)
    return FeatureNotificationsRenderer(entry, feature, navigation, currentUser)
}
