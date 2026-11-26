package com.coffeepeek.feature.profile.impl

import androidx.lifecycle.ViewModelStore
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.impl.ui.EditProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileAction
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CancellationException

/** Presentation adapter only: the route's ViewModel remains the sole draft and writer. */
class NativeEditProfileSession internal constructor(
    private val model: EditProfileViewModel,
    private val accounts: ProfileAccountPort,
    private val closeModel: () -> Unit,
) {
    private val account = accounts.activeAccount()
    private val accountScope = accounts.cacheScope()
    private val revoked = MutableStateFlow(false)
    private var closed = false
    val state = combine(model.state, accounts.observeActiveAccount(), accounts.observeCacheScope(), revoked) { _, _, _, _ -> current() }
        .catch {
            if (it is CancellationException) throw it
            close()
            emit(current())
        }
    val saved = model.events.filter { isCurrent() }.map { Unit }
    fun current() = model.state.value.nativeSnapshot(!isCurrent())
    fun isCurrent(): Boolean {
        if (closed || revoked.value) return false
        if (account == null || accounts.activeAccount() != account || accounts.cacheScope() != accountScope) revoked.value = true
        return !revoked.value
    }
    fun usernameChanged(value: String) = dispatch(EditProfileAction.UsernameChanged(value))
    fun aboutChanged(value: String) = dispatch(EditProfileAction.AboutChanged(value))
    fun avatarPicked(image: ProfileAvatarImage) {
        if (!isCurrent()) return
        dispatch(EditProfileAction.AvatarPicked(ProfileAvatarImage(image.fileName, image.contentType, image.bytes.copyOf())))
    }
    fun save() = dispatch(EditProfileAction.Save)
    fun confirmUsername() = dispatch(EditProfileAction.ConfirmUsernameChange)
    fun cancelUsername() = dispatch(EditProfileAction.CancelUsernameChange)
    fun dismissError() = dispatch(EditProfileAction.DismissError)
    fun reload() = dispatch(EditProfileAction.Reload)
    fun close() {
        if (closed) return
        closed = true
        revoked.value = true
        closeModel()
    }
    private fun dispatch(action: EditProfileAction) { if (isCurrent()) model.onAction(action) }
}

fun createNativeEditProfileSession(accounts: ProfileAccountPort, edit: ProfileEditPort): NativeEditProfileSession {
    val model = EditProfileViewModel(accounts, edit)
    val store = ViewModelStore().apply { put("edit-profile", model) }
    return NativeEditProfileSession(model, accounts, store::clear)
}
