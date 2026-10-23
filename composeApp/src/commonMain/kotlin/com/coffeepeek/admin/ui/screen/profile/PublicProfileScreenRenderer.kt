package com.coffeepeek.admin.ui.screen.profile

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.feature.profile.ui.OtherUserProfileScreen
import com.coffeepeek.admin.ui.Navigator

/** Platform selection only. Shared/iOS keep the legacy public profile; Android composes the migrated entry. */
interface PublicProfileScreenRenderer {
    @Composable
    fun Content(slug: String, nickname: String?, avatarUrl: String?, checkInsOnly: Boolean = false)
}

internal object LegacyPublicProfileScreenRenderer : PublicProfileScreenRenderer {
    @Composable
    override fun Content(slug: String, nickname: String?, avatarUrl: String?, checkInsOnly: Boolean) = OtherUserProfileScreen(
        slug = slug,
        nickname = nickname,
        avatarUrl = avatarUrl,
        onBack = Navigator::popBack,
        onCoffeeShopClick = { Navigator.navigate(Navigator.Screen.ShopDetail(it)) },
        onRoasterClick = { Navigator.navigate(Navigator.Screen.RoasterDetail(it)) },
        onLogin = { Navigator.navigate(Navigator.Screen.Auth) },
        onCheckInsClick = { Navigator.navigate(Navigator.Screen.UserCheckIns(slug, nickname, avatarUrl)) },
        checkInsOnly = checkInsOnly,
        onUserClick = { userSlug, userNickname, avatar -> Navigator.navigate(Navigator.Screen.UserProfile(userSlug, userNickname, avatar)) },
    )
}
