package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState
import kotlinx.coroutines.launch

/**
 * The edit form. Saves sequentially (avatar, name, about) and stops at the first failure. Unlike production, a step that succeeded is
 * remembered, so a retry after a later failure does not upload the avatar or resend the name again.
 */
internal class EditProfileViewModel(
    private val accounts: ProfileAccountPort,
    private val edit: ProfileEditPort,
) : MviViewModel<EditProfileState, EditProfileAction, EditProfileEvent>(EditProfileState()) {
    init { load() }

    override suspend fun handleActionInternal(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.UsernameChanged -> updateState { copy(username = action.value.take(EditProfileState.MAX_USERNAME)) }
            is EditProfileAction.AboutChanged -> updateState { copy(about = action.value.take(EditProfileState.MAX_ABOUT)) }
            is EditProfileAction.AvatarPicked ->
                if (action.image.bytes.size > EditProfileState.MAX_AVATAR_BYTES) updateState { copy(error = AVATAR_TOO_LARGE) }
                else updateState { copy(pendingAvatar = action.image, error = null) }
            EditProfileAction.DismissError -> updateState { copy(error = null) }
            EditProfileAction.Save -> save()
        }
    }

    private fun load() {
        viewModelScope.launch {
            val cached = accounts.cachedOwnProfile()
            if (cached != null) return@launch apply(cached)
            accounts.loadOwnProfile(forceRefresh = true)
                .onSuccess(::apply)
                .onFailure { updateState { copy(isLoading = false, error = it.userMessage() ?: LOAD_FAILED) } }
        }
    }

    private fun apply(profile: OwnProfile) = updateState {
        copy(
            username = profile.userName, about = profile.about.orEmpty(), avatarUrl = profile.avatarUrl,
            originalUsername = profile.userName, originalAbout = profile.about.orEmpty(), isLoading = false,
        )
    }

    private suspend fun save() {
        val form = currentState
        if (!form.canSave) return
        updateState { copy(isSaving = true, error = null) }

        form.pendingAvatar?.let { avatar ->
            edit.updateAvatar(avatar).onFailure { return fail(it, AVATAR_FAILED, useCause = true) }
            val uploaded = accounts.cachedOwnProfile()?.avatarUrl
            updateState { copy(pendingAvatar = pendingAvatar.takeUnless { it === avatar }, avatarUrl = uploaded ?: avatarUrl) }
        }
        val username = form.username.trim()
        if (username != form.originalUsername) {
            edit.updateUsername(username).onFailure { return fail(it, USERNAME_FAILED) }
            updateState { copy(originalUsername = username) }
        }
        val about = form.about.trim()
        if (about != form.originalAbout) {
            edit.updateAbout(about).onFailure { return fail(it, ABOUT_FAILED) }
            updateState { copy(originalAbout = about) }
        }
        updateState { copy(isSaving = false) }
        sendEvent(EditProfileEvent.Saved)
    }

    private fun fail(error: Throwable, fallback: String, useCause: Boolean = false) = updateState {
        copy(isSaving = false, error = error.userMessage(useCause) ?: fallback)
    }

    private fun Throwable.userMessage(useCause: Boolean = false): String? =
        (message ?: if (useCause) cause?.message else null)?.takeIf(String::isNotBlank)

    private companion object {
        const val LOAD_FAILED = "Ошибка загрузки профиля"
        const val AVATAR_TOO_LARGE = "Размер файла не должен превышать 5 МБ"
        const val AVATAR_FAILED = "Ошибка обновления аватара"
        const val USERNAME_FAILED = "Ошибка обновления имени"
        const val ABOUT_FAILED = "Ошибка обновления описания"
    }
}
