package com.coffeepeek.admin.di.shop

import com.coffeepeek.admin.utils.IosNativePhotoPicker
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.shop.api.ShopDetailContext
import com.coffeepeek.feature.shop.impl.ui.data.ShopCheckInPhotoPickerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** The feature's existing account-bound form; only the native picker/presentation lifetime is new. */
internal fun nativeShopCheckInForm(entry: SessionShopCheckInEntry, context: ShopDetailContext, language: LanguagePort,
    accounts: SessionReader, callbacks: ShopCheckInNativeCallbacks, revoked: () -> Unit,
): IosShopCheckInEditor? {
    val owner = entry.nativeForm(context, callbacks)
    val form = owner.form ?: return null.also { owner.close() }
    var photos: IosNativePhotoPicker? = null
    val editor = IosShopCheckInEditor(form, language, { photos?.let(::nativePhotoController) }, { callbacks })
    photos = IosNativePhotoPicker(editor.scope, editor::photoLoading, editor::presentationHost)
    editor.bindDisposal {
        photos?.close()
        photos = null
        owner.close()
    }
    observeNativeShopAccount(editor, owner, accounts, revoked)
    if (!form.isCurrent()) {
        editor.close()
        return null
    }
    return editor
}

private fun observeNativeShopAccount(editor: IosShopCheckInEditor, owner: NativeShopCheckInForm,
    accounts: SessionReader, revoked: () -> Unit,
) {
    editor.scope.launch { accounts.observeSession().catch {
        if (it is CancellationException) throw it
        owner.close()
        revoked()
    }.collect { if (owner.form?.isCurrent() != true) revoked() } }
}

private fun nativePhotoController(picker: IosNativePhotoPicker) = ShopCheckInPhotoPickerController(picker.isPreparing,
    { remaining, receive -> picker.show(false, remaining) { receive(it.toCheckInPhotos(remaining)) } },
    { receive -> picker.show(true, 1) { receive(it.toCheckInPhotos(1)) } },
)
