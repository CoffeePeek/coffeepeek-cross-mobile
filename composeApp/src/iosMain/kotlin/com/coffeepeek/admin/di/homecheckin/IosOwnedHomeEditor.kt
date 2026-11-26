package com.coffeepeek.admin.di.homecheckin

import com.coffeepeek.admin.di.checkinhistory.CheckInHistorySession
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.utils.IosNativePhotoPicker
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.homecheckin.impl.NativeHomeCheckInSession
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInPhotoPickerController
import com.coffeepeek.feature.settings.api.LanguagePort

internal fun ownedNativeHomeEditor(session: NativeHomeCheckInSession, language: LanguagePort,
    media: IosCatalogueMedia, accounts: SessionReader,
): IosHomeCheckInEditor {
    var photos: IosNativePhotoPicker? = null
    val editor = IosHomeCheckInEditor(session, language, media, CheckInHistorySession(accounts)) {
        photos?.let(::nativeHomePhotos)
    }
    photos = IosNativePhotoPicker(editor.scope, host = editor::presentationHost)
    editor.bindDisposal {
        photos?.close()
        photos = null
    }
    return editor
}

private fun nativeHomePhotos(picker: IosNativePhotoPicker) = HomeCheckInPhotoPickerController(picker.isPreparing,
    { maximum, receive -> picker.show(false, maximum) { receive(it.toHomePreparationPhotos(maximum)) } },
    { receive -> picker.show(true, 1) { receive(it.toHomePreparationPhotos(1)) } },
)
