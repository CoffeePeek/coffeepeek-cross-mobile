@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.utils.PhotoPickerController
import com.coffeepeek.feature.profile.impl.NativeEditProfileSession
import com.coffeepeek.feature.profile.impl.NativeEditProfileState
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

class IosEditProfile internal constructor(
    private val session: NativeEditProfileSession, private val language: LanguagePort,
    private val media: IosCatalogueMedia, private val photos: () -> PhotoPickerController?,
    private val onBack: () -> Unit, private val onDeletion: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preparing = MutableStateFlow(false)
    fun current() = session.current().iosState(preparing.value)
    fun languageTag() = language.current().languageTag
    fun observe(onState: (IosEditProfileState) -> Unit, onLanguage: (String) -> Unit): IosEditProfileObservation {
        return IosEditProfileObservation(
            scope.launch { combine(session.state, preparing) { state, loading -> state.iosState(loading) }.collect(onState) },
            scope.launch { language.observe().collect { onLanguage(it.languageTag) } },
        )
    }
    fun usernameChanged(value: String) = session.usernameChanged(value)
    fun aboutChanged(value: String) = session.aboutChanged(value)
    fun save() { if (!preparing.value) session.save() }
    fun confirmUsername() = session.confirmUsername()
    fun cancelUsername() = session.cancelUsername()
    fun dismissError() = session.dismissError()
    fun reload() = session.reload()
    fun back() { if (!session.current().saving) onBack() }
    fun deleteAccount() { if (session.isCurrent() && !session.current().saving) onDeletion() }
    fun choosePhoto(camera: Boolean) {
        if (!session.isCurrent() || session.current().saving || preparing.value) return
        val picker = photos() ?: return
        if (camera) picker.takePhoto() else picker.pickFromGallery()
    }
    fun loadImage(url: String, onImage: (UIImage?) -> Unit): IosEditProfileObservation {
        return IosEditProfileObservation(scope.launch {
            media.observeImage(url, session::isCurrent).collect { onImage(it.takeIf { session.isCurrent() }) }
        })
    }
    internal fun photoLoading(value: Boolean) { preparing.value = value }
    internal fun close() { scope.cancel() }
}

class IosEditProfileObservation internal constructor(private vararg val jobs: Job) {
    fun cancel() = jobs.forEach { it.cancel() }
}

data class IosEditProfileState(
    val username: String, val about: String, val avatarUrl: String?, val avatar: NSData?,
    val loading: Boolean, val saving: Boolean, val canSave: Boolean, val revoked: Boolean, val photoLoading: Boolean,
    val usernameErrorKey: String?, val aboutErrorKey: String?, val usernameToConfirm: String?,
    val errorTitleKey: String?, val errorMessageKey: String?, val retryAtUtc: String?, val loadFailed: Boolean,
)

private fun NativeEditProfileState.iosState(photoLoading: Boolean): IosEditProfileState {
    val bytes = avatar?.bytes
    val data = bytes?.takeIf { it.isNotEmpty() }?.usePinned { NSData.create(it.addressOf(0), bytes.size.toULong()) }
    return IosEditProfileState(username, about, avatarUrl, data, loading, saving, canSave && !photoLoading,
        revoked, photoLoading, usernameErrorKey, aboutErrorKey, usernameToConfirm,
        errorTitleKey, errorMessageKey, retryAtUtc, loadFailed)
}
