package com.coffeepeek.feature.homecheckin.impl.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun GuestCheckInOnboardingSlide(page: GuestCheckInOnboardingPage) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val posterHeight = maxHeight * 0.8f
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GuestCheckInOnboardingPoster(page, Modifier.fillMaxWidth().height(posterHeight))
            Text(
                coffeePeekStringResource(page.title),
                modifier = Modifier.padding(top = CoffeePeekDimens.spacing2).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                coffeePeekStringResource(page.description),
                modifier = Modifier.padding(top = CoffeePeekDimens.spacing1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GuestCheckInOnboardingPoster(page: GuestCheckInOnboardingPage, modifier: Modifier) {
    val poster = painterResource(page.poster)
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = poster,
            contentDescription = null,
            modifier = Modifier.aspectRatio(poster.intrinsicSize.width / poster.intrinsicSize.height)
                .clip(RoundedCornerShape(CoffeePeekDimens.radius2xl)),
            contentScale = ContentScale.Fit,
        )
    }
}
