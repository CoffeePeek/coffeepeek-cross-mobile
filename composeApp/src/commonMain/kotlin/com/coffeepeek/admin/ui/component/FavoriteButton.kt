package com.coffeepeek.admin.ui.component

import com.coffeepeek.admin.analytics.*

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.favorite_add
import coffeepeek.composeapp.generated.resources.favorite_remove
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeButton
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overImage: Boolean = false,
) {
    val analyticsPage = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    val containerColor = if (overImage) Color.Black else Color.Transparent
    CoffeePeekLikeButton(
        selected = isFavorite,
        contentDescription = stringResource(if (isFavorite) Res.string.favorite_remove else Res.string.favorite_add),
        onClick = { platformHaptic(HapticEvent.Selection); onClick() },
        enabled = enabled,
        modifier = modifier.size(if (overImage) 40.dp else 48.dp)
            .analyticsExposure("favorites", when (analyticsPage) { "shop_detail" -> "shop_detail"; "roaster_detail" -> "roaster_card"; else -> "catalog_card" }, analyticsPage),
        containerColor = containerColor,
        overImage = overImage,
    )
}
