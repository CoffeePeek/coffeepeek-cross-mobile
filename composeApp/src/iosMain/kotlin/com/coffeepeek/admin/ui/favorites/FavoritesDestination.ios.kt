package com.coffeepeek.admin.ui.favorites

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.screen.favorites.FavoritesScreen
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.NavigationBarAction
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.favorites_share_collection
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal actual fun FavoritesDestination() {
    FavoritesScreen(shareAction = NavigationBarAction(stringResource(Res.string.favorites_share_collection)) {
        Navigator.navigate(Navigator.Screen.CollectionsComposer)
    })
}
