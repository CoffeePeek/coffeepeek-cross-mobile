package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.feature.profile.api.ProfileAuthor
import com.coffeepeek.feature.profile.api.ProfileAuthorActions
import com.coffeepeek.feature.profile.domain.PublicProfile
import com.coffeepeek.feature.profile.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val PUBLIC_PROFILE_BASE_URL = "https://coffeepeek.by"

@Composable
internal fun PublicProfileTopBar(profile: PublicProfile?, viewingOwn: Boolean, own: OwnProfileExtras?,
    authorActions: ProfileAuthorActions, callbacks: PublicProfileCallbacks) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp), contentAlignment = Alignment.Center) {
        if (callbacks.onBack != null) CoffeePeekIconButton(callbacks.onBack, Modifier.align(Alignment.CenterStart)) {
            CoffeePeekIcon(CoffeePeekIcons.ChevronLeft, stringResource(Res.string.profile_back), Modifier.size(24.dp))
        }
        Text(stringResource(Res.string.profile_title),
            Modifier.fillMaxWidth().padding(start = if (callbacks.onBack == null) 0.dp else 56.dp, end = 56.dp),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            textAlign = if (callbacks.onBack == null) TextAlign.Start else TextAlign.Center)
        Box(Modifier.align(Alignment.CenterEnd)) {
            PublicProfileTopBarActions(profile, viewingOwn, own, authorActions, callbacks.onShare)
        }
    }
}

@Composable
private fun PublicProfileTopBarActions(profile: PublicProfile?, viewingOwn: Boolean, own: OwnProfileExtras?,
    authorActions: ProfileAuthorActions, onShare: (String) -> Unit) {
    if (own != null) {
        own.topBarActions()
        return
    }
    if (profile == null) return
    val path = profile.canonicalPath?.takeIf { it.startsWith("/") && !it.startsWith("//") }
    val share = path?.let { { onShare("$PUBLIC_PROFILE_BASE_URL$it") } }
    val author = ProfileAuthor(profile.authorId, profile.slug, profile.nickname, profile.avatarUrl)
    if (!viewingOwn) PublicProfileActions(profile.isBlockedByViewer, share,
        { if (profile.isBlockedByViewer) authorActions.requestUnblock(author) else authorActions.requestBlock(author) })
    else if (share != null) CoffeePeekIconButton(share) {
        CoffeePeekIcon(CoffeePeekIcons.Share, stringResource(Res.string.profile_share), Modifier.size(22.dp))
    }
}

@Composable
private fun PublicProfileTopBarPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PublicProfileTopBar(null, false, null, ProfileAuthorActions.None,
        PublicProfileCallbacks({}, {}, { _, _, _ -> }, {}, {}, {}, { _, _, _ -> }))
}

@Preview @Composable private fun PublicProfileTopBarLightPreview() = PublicProfileTopBarPreview(false)
@Preview @Composable private fun PublicProfileTopBarDarkPreview() = PublicProfileTopBarPreview(true)
