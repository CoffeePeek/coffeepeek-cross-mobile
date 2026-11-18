package com.coffeepeek.feature.profile.impl

import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.impl.ui.EditProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileAction
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Presentation adapter only: the route's ViewModel remains the sole draft and writer. */
class NativeEditProfileSession internal constructor(
    private val model: EditProfileViewModel,
    private val accounts: ProfileAccountPort,
) {
    private val account = accounts.activeAccount()
    private val accountScope = accounts.cacheScope()
    private var revoked = false
    val state = combine(model.state, accounts.observeCacheScope()) { value, _ -> value.nativeSnapshot(!isCurrent()) }
    val saved = model.events.map { Unit }
    fun current() = model.state.value.nativeSnapshot(!isCurrent())
    fun isCurrent(): Boolean {
        if (account == null || accounts.activeAccount() != account || accounts.cacheScope() != accountScope) revoked = true
        return !revoked
    }
    fun usernameChanged(value: String) = dispatch(EditProfileAction.UsernameChanged(value))
    fun aboutChanged(value: String) = dispatch(EditProfileAction.AboutChanged(value))
    fun avatarPicked(image: ProfileAvatarImage) = dispatch(EditProfileAction.AvatarPicked(image))
    fun save() = dispatch(EditProfileAction.Save)
    fun confirmUsername() = dispatch(EditProfileAction.ConfirmUsernameChange)
    fun cancelUsername() = dispatch(EditProfileAction.CancelUsernameChange)
    fun dismissError() = dispatch(EditProfileAction.DismissError)
    fun reload() = dispatch(EditProfileAction.Reload)
    private fun dispatch(action: EditProfileAction) { if (isCurrent()) model.onAction(action) }
}
