package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.favorite_add
import coffeepeek.composeapp.generated.resources.favorite_remove
import com.coffeepeek.admin.ui.icons.CpIcons
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    overImage: Boolean = false,
) {
    val contentColor = if (overImage) Color.White else MaterialTheme.colorScheme.onSurface
    val containerColor = if (overImage) Color.Black else Color.Transparent
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = { onClick() },
        enabled = enabled,
        modifier = modifier.size(if (overImage) 40.dp else 48.dp).clip(CircleShape),
        colors = IconButtonDefaults.iconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            checkedContainerColor = containerColor,
            checkedContentColor = contentColor,
        ),
    ) {
        Icon(
            imageVector = if (isFavorite) CpIcons.FavoriteFilled else CpIcons.Favorite,
            contentDescription = stringResource(if (isFavorite) Res.string.favorite_remove else Res.string.favorite_add),
            modifier = Modifier.size(24.dp),
        )
    }
}
