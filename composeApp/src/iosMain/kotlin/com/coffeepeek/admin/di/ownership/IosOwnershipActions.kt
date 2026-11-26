@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.ownership

import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.utils.IosNativePhotoPicker
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.ownership.impl.createIosOwnershipActionsSession
import com.coffeepeek.feature.ownership.impl.createIosOwnershipClaimSession
import com.coffeepeek.feature.ownership.impl.NativeOwnershipPhoto
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import platform.UIKit.*

private lateinit var ownershipActionsFactory: (IosOwnershipActions) -> UIViewController
internal fun configureNativeOwnershipActions(factory: (IosOwnershipActions) -> UIViewController) { ownershipActionsFactory = factory }

/** Shared by Shop and Roaster native toolbars; favorite/share remain caller-owned SwiftUI. */
internal fun iosOwnershipActionsController(ownership: BusinessOwnershipEntry, language: LanguagePort, media: IosCatalogueMedia,
    kind: String, id: String, slug: String, allowed: () -> Boolean,
    onSuggest: () -> Unit = {}, onOwnerEdit: () -> Unit = {}): UIViewController {
    return ownershipActionsFactory(IosOwnershipActions(ownership, language, media, kind, id, slug, allowed, onSuggest, onOwnerEdit))
}

class IosOwnershipActions internal constructor(private val ownership: BusinessOwnershipEntry, private val language: LanguagePort,
    private val media: IosCatalogueMedia,
    val kind: String, private val id: String, private val slug: String, private val allowed: () -> Boolean,
    onSuggest: () -> Unit, onOwnerEdit: () -> Unit) {
    private val session = createIosOwnershipActionsSession(ownership, kind, id, slug, onSuggest, onOwnerEdit)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var closed = false
    private var claim: IosOwnershipScreen? = null
    private var claimController: UIViewController? = null
    private val photos = IosNativePhotoPicker(scope) { claimController }
    fun owner() = !closed && allowed() && session.owner()
    fun active() = !closed && allowed()
    fun languageTag() = language.current().languageTag
    fun observe(receive: () -> Unit): IosOwnershipObservation = IosOwnershipObservation(listOf(
        session.observe(receive), scope.launch { language.observe().collect { receive() } }))
    fun edit() { if (active()) session.edit() }
    fun suggest() { if (active()) session.suggest() }
    fun makeClaim(onClose: () -> Unit): UIViewController? {
        if (!active() || !session.requestClaim()) return null
        releaseClaim()
        val session = createIosOwnershipClaimSession(ownership, kind, slug,
            photos = { maximum, receive -> photos.show(false, maximum) { selected ->
                receive(selected.map { NativeOwnershipPhoto(it.fileName, it.contentType, it.bytes) })
            } }, onClose = onClose, onLogin = { onClose(); Navigator.navigate(Navigator.Screen.Auth) })
        val screen = IosOwnershipScreen(session, language, media)
        claim = screen
        return ownershipFactory(screen).apply { modalInPresentation = true }.also { claimController = it }
    }
    fun releaseClaim() { photos.cancel(); claim?.close(); claim = null; claimController = null }
    fun close() { releaseClaim(); photos.close(); closed = true; session.close(); scope.cancel() }
}
