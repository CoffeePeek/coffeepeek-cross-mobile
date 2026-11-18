package com.coffeepeek.admin.di.system

import com.coffeepeek.admin.di.appupdate.IosAppUpdate
import com.coffeepeek.admin.di.homecheckin.IosGuestOnboarding
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.feature.appupdate.api.AppUpdateEnvironment
import com.coffeepeek.feature.appupdate.domain.AppUpdate
import com.coffeepeek.feature.appupdate.domain.AppUpdateRepository
import com.coffeepeek.feature.appupdate.domain.InstallationState
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.domain.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IosRootModalsTest {
    private val dispatcher = StandardTestDispatcher()
    private val language = object : LanguagePort {
        override fun current() = AppLanguage.RUSSIAN
        override fun observe() = MutableStateFlow(current())
        override suspend fun set(language: AppLanguage) = Result.success(Unit)
    }
    @BeforeTest fun before() = Dispatchers.setMain(dispatcher)
    @AfterTest fun after() = Dispatchers.resetMain()

    @Test fun onboardingFinishesOnlyOnceAndClosedPresentationCannotNavigate() {
        val routes = mutableListOf<Navigator.Screen?>()
        val model = IosGuestOnboarding(language, routes::add)
        model.signIn()
        model.register()
        model.continueAsGuest()
        assertEquals(listOf<Navigator.Screen?>(Navigator.Screen.Auth), routes)
        model.close()
        val closed = IosGuestOnboarding(language, routes::add)
        closed.close()
        closed.register()
        assertEquals(1, routes.size)
    }

    @Test fun requiredUpdateCannotBeDismissedAndUsesTheExistingInstaller() = runTest(dispatcher) {
        val repository = Updates(AppUpdate(2, 2, "https://example.invalid/update"))
        val installer = Installer()
        val controller = AppUpdateController(repository, Environment(), backgroundScope)
        controller.check()
        runCurrent()
        val model = IosAppUpdate(repository.update, controller, installer, language, 1)
        assertTrue(model.required)
        model.dismiss()
        runCurrent()
        assertTrue(controller.state.value.showPrompt)
        assertNull(repository.dismissed)
        model.openStore()
        assertEquals(repository.update, installer.opened)
        model.close()
    }

    @Test fun optionalUpdatePersistsDismissalAndClosedActionsAreRevoked() = runTest(dispatcher) {
        val repository = Updates(AppUpdate(2, 0, "https://example.invalid/update"))
        val installer = Installer()
        val controller = AppUpdateController(repository, Environment(), backgroundScope)
        controller.check()
        runCurrent()
        val model = IosAppUpdate(repository.update, controller, installer, language, 1)
        assertFalse(model.required)
        model.dismiss()
        runCurrent()
        assertFalse(controller.state.value.showPrompt)
        assertEquals(2L, repository.dismissed)
        model.close()
        model.openStore()
        assertNull(installer.opened)
    }

    private class Environment : AppUpdateEnvironment {
        override val versionCode = 1L
        override val platform = "ios"
        override val channel: String? = null
    }
    private class Updates(val update: AppUpdate) : AppUpdateRepository {
        var dismissed: Long? = null
        override suspend fun fetch() = update
        override suspend fun dismissedVersion() = dismissed
        override suspend fun dismiss(version: Long) { dismissed = version }
    }
    private class Installer : UpdateInstaller {
        override val state = MutableStateFlow(InstallationState())
        var opened: AppUpdate? = null
        override fun download(update: AppUpdate) { opened = update }
        override fun install() = Unit
        override fun cancel() = Unit
        override fun refresh() = Unit
    }
}
