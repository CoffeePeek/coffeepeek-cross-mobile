package com.coffeepeek.admin.di.auth

import kotlinx.coroutines.flow.MutableStateFlow

/** Keeps form state inside the check-in route while the shared session observer resumes the editor. */
class IosCheckInAuthFlow internal constructor(
    private val destinations: IosAuthDestinations,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) : IosAuthScreen(destinations.language, onCancel, onDismiss) {
    private val login = destinations.login(::back, ::showRecovery, ::showRegistration, onSignedIn = {})
    private val registration = lazy { destinations.registration(::back, ::showLogin, onSignedIn = {}) }
    private var recovery: IosPasswordRecovery? = null
    private var recoveryEmail: String? = null
    private val active = MutableStateFlow<IosAuthScreen>(login)

    fun currentScreen(): IosAuthScreen = active.value
    fun observeScreen(onScreen: (IosAuthScreen) -> Unit) = observe(active, onScreen)

    private fun showRegistration() {
        if (active.value === login) show(registration.value)
    }

    private fun showLogin() = show(login)

    private fun showRecovery(email: String) {
        if (active.value !== login) return
        if (recoveryEmail != email) {
            recovery?.close()
            recovery = destinations.recovery(email, ::showLogin, ::showLogin)
            recoveryEmail = email
        }
        show(requireNotNull(recovery))
    }

    private fun show(screen: IosAuthScreen) {
        val previous = active.value
        if (previous === screen) return
        previous.onHidden()
        active.value = screen
    }

    override fun closeChildren() {
        login.close()
        if (registration.isInitialized()) registration.value.close()
        recovery?.close()
    }
}
