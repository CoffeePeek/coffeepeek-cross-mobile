package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.profile_actions
import coffeepeek.composeapp.generated.resources.profile_menu_blocked_authors
import coffeepeek.composeapp.generated.resources.profile_menu_logout
import coffeepeek.composeapp.generated.resources.profile_menu_shop_changes
import coffeepeek.composeapp.generated.resources.profile_menu_submitted_roasters
import coffeepeek.composeapp.generated.resources.profile_menu_submitted_shops
import com.coffeepeek.admin.ui.component.PlatformMenuButton
import com.coffeepeek.admin.ui.component.PlatformMenuIcon
import com.coffeepeek.admin.ui.component.PlatformMenuItem
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileActions(
    onContributions: (ContributionKind) -> Unit,
    onLogout: () -> Unit,
    onBlockedAuthors: () -> Unit,
) {
    val description = stringResource(Res.string.profile_actions)
    PlatformMenuButton(
        items = listOf(
            PlatformMenuItem("shop-changes", stringResource(Res.string.profile_menu_shop_changes), icon = PlatformMenuIcon.NoteEdit,
                onClick = { onContributions(ContributionKind.Changes) }),
            PlatformMenuItem("submitted-shops", stringResource(Res.string.profile_menu_submitted_shops), icon = PlatformMenuIcon.Add,
                onClick = { onContributions(ContributionKind.Shops) }),
            PlatformMenuItem("submitted-roasters", stringResource(Res.string.profile_menu_submitted_roasters), icon = PlatformMenuIcon.CoffeeBean,
                onClick = { onContributions(ContributionKind.Roasters) }),
            PlatformMenuItem("blocked-authors", stringResource(Res.string.profile_menu_blocked_authors), icon = PlatformMenuIcon.Block,
                onClick = onBlockedAuthors),
            PlatformMenuItem("logout", stringResource(Res.string.profile_menu_logout), icon = PlatformMenuIcon.Logout,
                destructive = true, onClick = onLogout),
        ),
        contentDescription = description,
    ) { openMenu ->
        IconButton(onClick = openMenu) { Icon(CpIcons.MoreHorizontal, contentDescription = description) }
    }
}

@Preview @Composable private fun ProfileActionsLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ProfileActions({}, {}, {})
}

@Preview @Composable private fun ProfileActionsDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ProfileActions({}, {}, {})
}
