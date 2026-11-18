@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.ownership

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import com.coffeepeek.feature.ownership.impl.createIosOwnershipActionsSession
import com.coffeepeek.feature.ownership.impl.IosOwnershipClaimContent
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import platform.UIKit.*

private lateinit var ownershipActionsFactory: (IosOwnershipActions) -> UIViewController
internal fun configureNativeOwnershipActions(factory: (IosOwnershipActions) -> UIViewController) { ownershipActionsFactory = factory }

/** Shared by Shop and Roaster native toolbars; favorite/share remain caller-owned SwiftUI. */
internal fun iosOwnershipActionsController(ownership: BusinessOwnershipEntry, language: LanguagePort,
    kind: String, id: String, slug: String, allowed: () -> Boolean,
    onSuggest: () -> Unit = {}, onOwnerEdit: () -> Unit = {}): UIViewController {
    return ownershipActionsFactory(IosOwnershipActions(ownership, language, kind, id, slug, allowed, onSuggest, onOwnerEdit))
}

class IosOwnershipActions internal constructor(private val ownership: BusinessOwnershipEntry, private val language: LanguagePort,
    val kind: String, private val id: String, private val slug: String, private val allowed: () -> Boolean,
    onSuggest: () -> Unit, onOwnerEdit: () -> Unit) {
    private val session = createIosOwnershipActionsSession(ownership, kind, id, slug, onSuggest, onOwnerEdit)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var closed = false
    fun owner() = !closed && allowed() && session.owner()
    fun active() = !closed && allowed()
    fun languageTag() = language.current().languageTag
    fun observe(receive: () -> Unit): IosOwnershipObservation = IosOwnershipObservation(listOf(
        session.observe(receive), scope.launch { language.observe().collect { receive() } }))
    fun edit() { if (active()) session.edit() }
    fun suggest() { if (active()) session.suggest() }
    fun makeClaim(onClose: () -> Unit): UIViewController? {
        if (!active() || !session.requestClaim()) return null
        return ComposeUIViewController {
            AppContent {
                IosOwnershipClaimContent(ownership, kind, slug, onClose,
                    onLogin = { onClose(); Navigator.navigate(Navigator.Screen.Auth) })
            }
        }.apply { modalInPresentation = true }
    }
    fun close() { closed = true; session.close(); scope.cancel() }
}
