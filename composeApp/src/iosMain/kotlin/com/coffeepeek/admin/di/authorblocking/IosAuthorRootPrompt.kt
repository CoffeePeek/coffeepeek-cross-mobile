package com.coffeepeek.admin.di.authorblocking

import com.coffeepeek.admin.di.system.IosRootModal
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.settings.api.LanguagePort

class IosAuthorRootPrompt internal constructor(language: LanguagePort, private val authors: IosBlockedAuthors,
    val state: IosBlockedAuthorsState, private val onSignIn: () -> Unit,
) : IosRootModal(language) {
    fun confirm() { if (isActive) state.prompt?.let(authors::confirm) }
    fun cancel() {
        if (!isActive) return
        if (state.needsLogin) authors.loginHandled() else state.prompt?.let(authors::dismiss)
    }
    fun signIn() {
        if (!isActive || !state.needsLogin) return
        authors.loginHandled()
        onSignIn()
    }
}
