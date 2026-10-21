package com.coffeepeek.admin.di.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.shop_session_unavailable
import coffeepeek.composeapp.generated.resources.shop_session_retry
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.domain.repository.SessionRepository
import com.coffeepeek.feature.authorblocking.api.AuthorVisibility
import org.jetbrains.compose.resources.stringResource

/** The outer VM survives navigation/configuration; its inner feature store cannot survive account replacement. */
@Composable
internal fun AndroidShopBrowsingHost(
    ownerKey: String,
    sessions: SessionRepository,
    visibility: AuthorVisibility = AuthorVisibility.None,
    content: @Composable (ShopBrowsingHostSession) -> Unit,
) {
    val model = viewModel(key = ownerKey) { AndroidShopBrowsingHostViewModel(sessions, visibility) }
    val session by model.session.collectAsStateWithLifecycle()
    if (session.observationFailed) {
        ShopSessionUnavailable(model::retry)
    } else if (session.lease.isCurrent()) {
        key(session.generation) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides session,
                LocalContentColor provides MaterialTheme.colorScheme.onSurface) { content(session) }
        }
    }
}

@Composable
private fun ShopSessionUnavailable(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(CoffeePeekDimens.spacing6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            Text(stringResource(Res.string.shop_session_unavailable), color = MaterialTheme.colorScheme.error)
            CoffeePeekButton(stringResource(Res.string.shop_session_retry), onRetry)
        }
    }
}

@PreviewLightDark
@Composable
private fun ShopSessionUnavailablePreview() = CoffeePeekTheme { ShopSessionUnavailable {} }
