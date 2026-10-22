package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.impl.R

@Composable
internal fun OwnProfileActions(
    onContributions: (ProfileContribution) -> Unit, onBlockedAuthors: () -> Unit, onLogout: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(R.string.profile_actions))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.widthIn(min = 224.dp, max = 320.dp),
            shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
            containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            OwnProfileMenuItems(
                onContributions = { open = false; onContributions(it) },
                onBlockedAuthors = { open = false; onBlockedAuthors() },
                onLogout = { open = false; onLogout() },
            )
        }
    }
}

@Composable
private fun OwnProfileMenuItems(
    onContributions: (ProfileContribution) -> Unit, onBlockedAuthors: () -> Unit, onLogout: () -> Unit,
) {
    OwnProfileMenuItem(R.string.profile_menu_shop_changes, CoffeePeekIcons.NoteEdit,
        onClick = { onContributions(ProfileContribution.Changes) })
    OwnProfileMenuItem(R.string.profile_menu_submitted_shops, CoffeePeekIcons.Add,
        onClick = { onContributions(ProfileContribution.Shops) })
    OwnProfileMenuItem(R.string.profile_menu_submitted_roasters, CoffeePeekIcons.CoffeeBean,
        onClick = { onContributions(ProfileContribution.Roasters) })
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    OwnProfileMenuItem(R.string.profile_menu_blocked_authors, CoffeePeekIcons.VisibilityOff, onClick = onBlockedAuthors)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    OwnProfileMenuItem(R.string.profile_menu_logout, CoffeePeekIcons.Logout, destructive = true, onClick = onLogout)
}

@Composable
private fun OwnProfileMenuItem(label: Int, icon: ImageVector, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, null, Modifier.size(CoffeePeekDimens.settingsIconSize)) },
        colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = color),
        onClick = onClick,
    )
}

@PreviewLightDark
@Composable
private fun OwnProfileMenuPreview() = CoffeePeekTheme {
    Surface(shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.width(304.dp).padding(vertical = CoffeePeekDimens.spacing2)) {
            OwnProfileMenuItems({}, {}, {})
        }
    }
}
