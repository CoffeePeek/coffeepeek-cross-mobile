package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.api.ProfileContribution
import com.coffeepeek.feature.profile.impl.resources.*

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

@Composable
private fun OwnProfileActionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    OwnProfileActions({}, {}, {})
}

@Preview @Composable private fun OwnProfileActionsLightPreview() = OwnProfileActionsPreview(false)
@Preview @Composable private fun OwnProfileActionsDarkPreview() = OwnProfileActionsPreview(true)
