package com.coffeepeek.admin.ui.screen.shop

import androidx.compose.runtime.Composable
import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

enum class ShopDetailFeedback { FavoritesLoadFailed, BlocksLoadFailed, AddressMissing, SignInRequired, CheckInSubmitFailed, FavoriteChangeFailed, PhoneCopied }

internal data class ShopDetailLabels(
    val favoritesLoadFailed: String,
    val blocksLoadFailed: String,
    val addressMissing: String,
    val signInRequired: String,
    val checkInSubmitFailed: String,
    val favoriteChangeFailed: String,
    val phoneCopied: String,
)

@Composable
internal fun shopDetailLabels() = ShopDetailLabels(
    stringResource(Res.string.legacy_other_user_profile_screen_roaster_message_favorites_unavailable),
    stringResource(Res.string.legacy_shop_blocks_load_failed),
    stringResource(Res.string.checkin_error_shop_address),
    stringResource(Res.string.legacy_shop_signin_required),
    stringResource(Res.string.legacy_shop_checkin_submit_failed),
    stringResource(Res.string.legacy_shop_favorite_change_failed),
    stringResource(Res.string.shop_phone_copied),
)

internal fun ShopDetailFeedback.message(labels: ShopDetailLabels): String = when (this) {
    ShopDetailFeedback.FavoritesLoadFailed -> labels.favoritesLoadFailed
    ShopDetailFeedback.BlocksLoadFailed -> labels.blocksLoadFailed
    ShopDetailFeedback.AddressMissing -> labels.addressMissing
    ShopDetailFeedback.SignInRequired -> labels.signInRequired
    ShopDetailFeedback.CheckInSubmitFailed -> labels.checkInSubmitFailed
    ShopDetailFeedback.FavoriteChangeFailed -> labels.favoriteChangeFailed
    ShopDetailFeedback.PhoneCopied -> labels.phoneCopied
}

internal data class ShopShareLabels(val named: String, val unnamed: String) {
    fun text(title: String?, url: String): String {
        val template = if (title == null) unnamed else named
        val args = if (title == null) listOf(url) else listOf(title, url)
        return Regex("%([12])" + Regex.escape("$") + "s").replace(template) { args[it.groupValues[1].toInt() - 1] }
    }
}

@Composable
internal fun shopShareLabels() = ShopShareLabels(
    stringResource(Res.string.shop_share_named),
    stringResource(Res.string.legacy_shop_share_unnamed),
)
