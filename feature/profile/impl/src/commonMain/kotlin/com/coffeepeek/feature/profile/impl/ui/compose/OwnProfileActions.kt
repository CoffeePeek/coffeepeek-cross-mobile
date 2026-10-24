package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.material3.Icon
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_actions
import com.coffeepeek.feature.profile.impl.resources.profile_menu_blocked_authors
import com.coffeepeek.feature.profile.impl.resources.profile_menu_logout
import com.coffeepeek.feature.profile.impl.resources.profile_menu_shop_changes
import com.coffeepeek.feature.profile.impl.resources.profile_menu_submitted_roasters
import com.coffeepeek.feature.profile.impl.resources.profile_menu_submitted_shops
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileContribution

@Composable
internal fun OwnProfileActions(
    onContributions: (ProfileContribution) -> Unit, onBlockedAuthors: () -> Unit, onLogout: () -> Unit,
) {
    val actions = listOf(
        CoffeePeekMenuAction(stringResource(Res.string.profile_menu_shop_changes), CoffeePeekIcons.NoteEdit,
            onClick = { onContributions(ProfileContribution.Changes) }),
        CoffeePeekMenuAction(stringResource(Res.string.profile_menu_submitted_shops), CoffeePeekIcons.Add,
            onClick = { onContributions(ProfileContribution.Shops) }),
        CoffeePeekMenuAction(stringResource(Res.string.profile_menu_submitted_roasters), CoffeePeekIcons.CoffeeBean,
            onClick = { onContributions(ProfileContribution.Roasters) }),
        CoffeePeekMenuAction(stringResource(Res.string.profile_menu_blocked_authors), CoffeePeekIcons.VisibilityOff,
            onClick = onBlockedAuthors),
        CoffeePeekMenuAction(stringResource(Res.string.profile_menu_logout), CoffeePeekIcons.Logout,
            destructive = true, onClick = onLogout),
    )
    CoffeePeekActionMenu(actions) { open ->
        IconButton(onClick = open) {
            Icon(CoffeePeekIcons.MoreHorizontal, stringResource(Res.string.profile_actions))
        }
    }
}

@Preview
@Composable
private fun OwnProfileActionsPreview() = CoffeePeekTheme {
    OwnProfileActions({}, {}, {})
}
