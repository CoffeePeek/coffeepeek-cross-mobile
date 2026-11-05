package com.coffeepeek.feature.contributions.impl.ui

import com.coffeepeek.feature.contributions.impl.ui.component.ContributionShopCover
import androidx.compose.foundation.BorderStroke
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_mascot_happy
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_fallback_name
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_go_home
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_success_description
import com.coffeepeek.feature.contributions.impl.resources.contributions_shop_success_title
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold
import androidx.compose.material3.Text
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.contributions.api.AddShopHost
import com.coffeepeek.feature.contributions.impl.shop.AddShopState

@Composable
internal fun AddShopSuccess(state: AddShopState, host: AddShopHost, onGoHome: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { host.feedback(true) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { Box(Modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing4)) { CoffeePeekButton(stringResource(Res.string.contributions_shop_go_home), onGoHome) } },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = CoffeePeekDimens.spacing4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            Image(painterResource(Res.drawable.contributions_mascot_happy), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(160.dp))
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(CoffeePeekIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(CoffeePeekDimens.spacing4))
            Text(stringResource(Res.string.contributions_shop_success_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            Text(stringResource(Res.string.contributions_shop_success_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(CoffeePeekDimens.spacing6))
            SentShopCard(state)
            Spacer(Modifier.height(CoffeePeekDimens.spacing8))
        }
    }
}

@Composable
private fun SentShopCard(state: AddShopState) {
    val location = listOfNotNull(state.selectedCity?.name?.takeIf(String::isNotBlank), state.address.trim().takeIf(String::isNotBlank)).joinToString(", ")
    val shape = RoundedCornerShape(CoffeePeekDimens.cardRadius)
    CoffeePeekCard(
        shape = shape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = 0.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(CoffeePeekDimens.spacing4),
        header = { ContributionShopCover(state.photos.firstOrNull()?.bytes, Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = CoffeePeekDimens.cardRadius, topEnd = CoffeePeekDimens.cardRadius))) },
    ) {
        Text(
            state.name.trim().ifBlank { stringResource(Res.string.contributions_shop_fallback_name) }, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (location.isNotBlank()) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing2))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(CoffeePeekIcons.Location, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Text(location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun AddShopSuccessPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AddShopSuccess(AddShopState(name = "Sandbox Cafe", address = "Sandbox address"), AddShopHost(), {})
}

@Preview @Composable private fun AddShopSuccessLightPreview() = AddShopSuccessPreview(false)
@Preview @Composable private fun AddShopSuccessDarkPreview() = AddShopSuccessPreview(true)
