package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.model.CommunityPost
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CommunityGuestTeaser(post: CommunityPost, host: CommunityHost, onSignIn: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().heightIn(max = 320.dp).clipToBounds().clearAndSetSemantics {}) {
        PostCard(
            post = post,
            isOwn = false,
            publicTimeline = true,
            showHelpfulCount = true,
            enabled = false,
            actions = PostCardActions(null, null, null, null, onPhoto = { onSignIn() }),
            host = host,
        )
        Box(
            Modifier.matchParentSize()
                .background(Brush.verticalGradient(
                    0f to background.copy(alpha = 0.60f),
                    0.55f to background.copy(alpha = 0.91f),
                    1f to background,
                ))
                .clickable(onClick = onSignIn),
        )
    }
}

@Preview
@Composable
private fun CommunityGuestTeaserLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CommunityGuestTeaser(previewPost(), CommunityHost()) {}
}

@Preview
@Composable
private fun CommunityGuestTeaserDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CommunityGuestTeaser(previewPost(), CommunityHost()) {}
}
