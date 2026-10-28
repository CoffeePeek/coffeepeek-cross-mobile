package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.impl.R

@Composable
internal fun OwnProfileActions(
    onContributions: (ProfileContribution) -> Unit, onBlockedAuthors: () -> Unit, onLogout: () -> Unit, onBusiness: () -> Unit = {},
) {
    val actions = listOf(
        CoffeePeekMenuAction(stringResource(R.string.profile_menu_business), CoffeePeekIcons.CoffeeBean, onClick = onBusiness),
        CoffeePeekMenuAction(stringResource(R.string.profile_menu_shop_changes), CoffeePeekIcons.NoteEdit,
            onClick = { onContributions(ProfileContribution.Changes) }),
        CoffeePeekMenuAction(stringResource(R.string.profile_menu_submitted_shops), CoffeePeekIcons.Add,
            onClick = { onContributions(ProfileContribution.Shops) }),
        CoffeePeekMenuAction(stringResource(R.string.profile_menu_submitted_roasters), CoffeePeekIcons.CoffeeBean,
            onClick = { onContributions(ProfileContribution.Roasters) }),
        CoffeePeekMenuAction(stringResource(R.string.profile_menu_blocked_authors), CoffeePeekIcons.VisibilityOff,
            onClick = onBlockedAuthors),
        CoffeePeekMenuAction(stringResource(R.string.profile_menu_logout), CoffeePeekIcons.Logout,
            destructive = true, onClick = onLogout),
    )
    CoffeePeekActionMenu(actions) { open ->
        IconButton(onClick = open) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(R.string.profile_actions))
        }
    }
}

@PreviewLightDark
@Composable
private fun OwnProfileActionsPreview() = CoffeePeekTheme {
    OwnProfileActions({}, {}, {})
}
