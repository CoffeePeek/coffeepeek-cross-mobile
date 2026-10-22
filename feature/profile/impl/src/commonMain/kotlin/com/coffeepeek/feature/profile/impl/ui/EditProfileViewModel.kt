package com.coffeepeek.feature.profile.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.profile.api.OwnProfile
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.api.UsernameChangeFailure
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileError
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState
import kotlinx.coroutines.launch

/** Saves only after an explicit username confirmation and remembers completed steps on partial failure. */
internal class EditProfileViewModel(
    private val accounts: ProfileAccountPort,
    private val edit: ProfileEditPort,
) : MviViewModel<EditProfileState, EditProfileAction, EditProfileEvent>(EditProfileState()) {
    init { load() }

    override suspend fun handleActionInternal(action: EditProfileAction) {
        when (action) {
            is EditProfileAction.UsernameChanged -> if (!currentState.isSaving) updateState {
                copy(username = action.value.take(EditProfileState.MAX_USERNAME), usernameToConfirm = null)
            }
            is EditProfileAction.AboutChanged -> if (!currentState.isSaving) updateState {
                copy(about = action.value.take(EditProfileState.MAX_ABOUT))
            }
            is EditProfileAction.AvatarPicked -> if (!currentState.isSaving) pickAvatar(action.image)
            EditProfileAction.DismissError -> updateState { copy(error = null, usernameRetryAtUtc = null) }
            EditProfileAction.CancelUsernameChange -> updateState { copy(usernameToConfirm = null) }
            EditProfileAction.ConfirmUsernameChange -> confirmUsernameChange()
            EditProfileAction.Save -> requestSave()
        }
    }

    private fun pickAvatar(image: ProfileAvatarImage) = updateState {
        if (image.bytes.size > EditProfileState.MAX_AVATAR_BYTES) copy(error = EditProfileError.AvatarTooLarge)
        else copy(pendingAvatar = image, error = null)
    }

    private fun load() {
        viewModelScope.launch {
            val cached = accounts.cachedOwnProfile()
            if (cached != null) return@launch apply(cached)
            accounts.loadOwnProfile(forceRefresh = true)
                .onSuccess(::apply)
                .onFailure { updateState { copy(isLoading = false, error = EditProfileError.Load) } }
        }
    }

    private fun apply(profile: OwnProfile) = updateState {
        copy(
            username = profile.userName, about = profile.about.orEmpty(), avatarUrl = profile.avatarUrl,
            originalUsername = profile.userName, originalAbout = profile.about.orEmpty(), isLoading = false,
        )
    }

    private suspend fun requestSave() {
        val form = currentState
        if (!form.canSave || form.usernameToConfirm != null) return
        if (form.username.trim() != form.originalUsername) {
            updateState { copy(usernameToConfirm = username.trim(), error = null) }
        } else save(form)
    }

    private suspend fun confirmUsernameChange() {
        val form = currentState
        val confirmedUsername = form.usernameToConfirm ?: return
        if (!form.canSave || form.username.trim() != confirmedUsername) return
        save(form)
    }

    private suspend fun save(form: EditProfileState) {
        updateState { copy(isSaving = true, error = null, usernameRetryAtUtc = null, usernameToConfirm = null) }
        try {
            if (!saveAvatar(form.pendingAvatar)) return
            if (!saveUsername(form)) return
            if (!saveAbout(form)) return
            sendEvent(EditProfileEvent.Saved)
        } finally {
            updateState { copy(isSaving = false) }
        }
    }

    private suspend fun saveAvatar(avatar: ProfileAvatarImage?): Boolean {
        if (avatar == null) return true
        edit.updateAvatar(avatar).onFailure { return fail(EditProfileError.Avatar) }
        val uploaded = accounts.cachedOwnProfile()?.avatarUrl
        updateState { copy(pendingAvatar = null, avatarUrl = uploaded ?: avatarUrl) }
        return true
    }

    private suspend fun saveUsername(form: EditProfileState): Boolean {
        val username = form.username.trim()
        if (username == form.originalUsername) return true
        edit.updateUsername(username).onFailure { return failUsername(it) }
        updateState { copy(originalUsername = username) }
        return true
    }

    private suspend fun saveAbout(form: EditProfileState): Boolean {
        val about = form.about.trim()
        if (about == form.originalAbout) return true
        edit.updateAbout(about).onFailure { return fail(EditProfileError.About) }
        updateState { copy(originalAbout = about) }
        return true
    }

    private fun failUsername(error: Throwable): Boolean {
        val failure = error as? UsernameChangeFailure
        val reason = when (failure?.reason) {
            UsernameChangeFailure.Reason.LimitReached -> EditProfileError.UsernameLimit
            UsernameChangeFailure.Reason.Taken -> EditProfileError.UsernameTaken
            UsernameChangeFailure.Reason.Invalid -> EditProfileError.InvalidUsername
            UsernameChangeFailure.Reason.SignInRequired -> EditProfileError.SignInRequired
            else -> EditProfileError.Username
        }
        updateState { copy(error = reason, usernameRetryAtUtc = failure?.retryAtUtc) }
        return false
    }

    private fun fail(reason: EditProfileError): Boolean {
        updateState { copy(error = reason) }
        return false
    }
}
