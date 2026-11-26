@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.admin.ui

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.AnalyticsEvent
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.admin.di.IosNativeControllerFactories
import com.coffeepeek.admin.di.appupdate.AppConfigUpdateEnvironment
import com.coffeepeek.admin.di.appupdate.IosAppUpdate
import com.coffeepeek.admin.di.authorblocking.*
import com.coffeepeek.admin.di.collections.*
import com.coffeepeek.admin.di.homecheckin.*
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.di.system.*
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.dialogs.globalDialogFactory
import com.coffeepeek.admin.utils.ErrorHandler
import com.coffeepeek.admin.utils.LoadingHandler
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.appupdate.domain.UpdateInstaller
import com.coffeepeek.feature.appupdate.impl.AppUpdateController
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.collections.api.CollectionTargetKind
import com.coffeepeek.feature.collections.domain.model.*
import com.coffeepeek.feature.collections.domain.repository.*
import com.coffeepeek.feature.collections.impl.*
import com.coffeepeek.feature.favorites.domain.repository.AccountFavorites
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.koin.core.Koin
import platform.UIKit.UIViewController

/** Desired native modal identity. Its finish action runs only after UIKit confirms dismissal. */
class IosRootPresentation internal constructor(val id: String, val fullScreen: Boolean,
    internal val make: () -> UIViewController, internal val closeSource: () -> Unit = {},
    internal val requestClose: () -> Unit = {}, internal val afterDismiss: () -> Unit = {}, internal val canDismiss: () -> Boolean = { true },
)
data class IosRootModalState(val primary: IosRootPresentation? = null, val blocking: IosRootPresentation? = null,
    val dismissAllowed: Boolean = true)

internal class IosRootModals(private val root: IosNativeRoot, private val dependencies: Koin, private val ui: IosNativeControllerFactories,
    private val lifecycle: RootNavigationLifecycle, foreground: StateFlow<Boolean>,
) {
    private val scope = CoroutineScope(root.scope.coroutineContext + SupervisorJob(root.scope.coroutineContext[Job]))
    private val language = dependencies.get<LanguagePort>()
    private val sessions = dependencies.get<SessionReader>()
    private val updates = dependencies.get<AppUpdateController>()
    private val installer = dependencies.get<UpdateInstaller>()
    private val requests = dependencies.get<CollectionPickerRequests>()
    private val authors = dependencies.get<AuthorBlockingPresenter>()
    private val blockedAuthors = IosBlockedAuthors(authors)
    private val output = MutableStateFlow(IosRootModalState())
    private val revision = MutableStateFlow(0)
    private var active = false
    private var loggedIn = false
    private var flow: IosCheckInFlow? = null
    private var flowObservation: IosHomeCheckInObservation? = null
    private var flowPhase: Any? = null
    private var flowPresentation: IosRootPresentation? = null
    private var baseKey: Any? = null
    private val retired = mutableMapOf<String, IosRootPresentation>()
    private val claimed = mutableSetOf<String>()
    private var guestOnboarding: IosGuestOnboarding? = null
    private var errorKey: Triple<String?, Boolean, String>? = null
    private var lifetime = 0L
    private var globalDialog: IosRootPresentation? = null
    private var authorDialog: IosRootPresentation? = null
    private var authorKey: Any? = null

    init {
        scope.launch { combine(lifecycle.state, Navigator.nativeOpenedLink, updates.state, requests.state, revision) {
            state, link, update, request, _ -> RootModalInputs(state, link, update.showPrompt, update.update, request)
        }.collect(::updatePrimary) }
        scope.launch { combine(ErrorHandler.errorMessage, LoadingHandler.isLoading, language.observe()) { message, loading, _ ->
            message to loading
        }.collect { updateGlobal(it.first, it.second) } }
        scope.launch { synchronizeCollectionFavorites(dependencies.get<SavedCollectionsRepository>().observeChanges(), dependencies.get<AccountFavorites>()) }
        scope.launch { foreground.collect { if (it) revision.value++ } }
        scope.launch { combine(authors.prompt, authors.needsLogin, revision) { _, _, _ -> blockedAuthors.currentState() }
            .collect(::updateAuthors) }
    }
    fun current() = output.value
    fun observe(receive: (IosRootModalState) -> Unit) = IosRootObservation(scope.launch { output.collect(receive) })
    fun ready(loggedIn: Boolean) {
        this.loggedIn = loggedIn
        active = true
        revision.value++
        scope.launch { updateGlobal(ErrorHandler.errorMessage.value, LoadingHandler.isLoading.value) }
    }
    fun foreground() { if (updates.checksEnabled) updates.check() }
    fun navigationChanged() { revision.value++ }
    fun revoke() {
        active = false
        lifetime++
        clearFlow()
        baseKey = null
        errorKey = null
        authorKey = null
        globalDialog?.closeSource?.invoke()
        authorDialog?.closeSource?.invoke()
        globalDialog = null
        authorDialog = null
        val previous = output.value
        output.value = IosRootModalState()
        listOfNotNull(previous.primary, previous.blocking).forEach { retire(it, finish = false) }
        retired.values.forEach { it.closeSource() }
        retired.clear()
        claimed.clear()
        guestOnboarding = null
    }
    private fun clearFlow() {
        flowObservation?.cancel()
        flowObservation = null
        flow?.close()
        flow = null
        flowPhase = null
        flowPresentation = null
    }
    fun close() { revoke(); scope.cancel() }
    fun controller(presentation: IosRootPresentation): UIViewController? {
        if (!output.value.let { it.primary === presentation || it.blocking === presentation }) return null
        claimed += presentation.id
        return presentation.make()
    }
    fun requestClose(presentation: IosRootPresentation) {
        if (!canDismiss(presentation)) return
        setPrimary(null)
        presentation.requestClose()
    }
    fun canDismiss(presentation: IosRootPresentation) = output.value.primary === presentation && output.value.dismissAllowed && presentation.canDismiss()
    fun dismissed(presentation: IosRootPresentation) {
        val old = retired.remove(presentation.id) ?: return
        claimed -= presentation.id
        Analytics.service?.overlay(presentation.id, false)
        old.closeSource()
        if (active) old.afterDismiss()
    }
    fun presented(presentation: IosRootPresentation) {
        if (output.value.let { it.primary === presentation || it.blocking === presentation }) Analytics.service?.overlay(presentation.id, true)
    }
    fun createCheckIn() {
        if (!active || flow != null) return
        Analytics.track(AnalyticsEvent.CHECK_IN_START_ATTEMPTED, "source" to "global_create")
        if (!loggedIn) { Analytics.gate("check_in"); Navigator.navigate(Navigator.Screen.Auth); return }
        val current = IosCheckInFlow(dependencies.get(), null, language, IosCatalogueMedia(dependencies.get(), Constants.BASE_URL),
            sessions, root.refreshCommunity::refresh) { finishFlow() }
        flow = current
        flowObservation = current.observe { if (flow === current && active) updateFlow(current, it) }
        revision.value++
    }
    private fun finishFlow() {
        flowObservation?.cancel()
        flowObservation = null
        flow?.close()
        flow = null
        flowPhase = null
        flowPresentation = null
        baseKey = null
        revision.value++
    }
    private fun updateFlow(current: IosCheckInFlow, state: IosCheckInFlowState) {
        val phase = state.editor ?: state.picker
        if (phase !== flowPhase || state.finished) {
            flowPhase = phase
            flowPresentation = if (state.finished) null else phase?.let {
                IosRootPresentation(correlationId("create"), state.editor != null,
                    { state.editor?.let { homeEditorFactory(it) } ?: ui.makeCheckInCreatePickerController(checkNotNull(state.picker)) },
                    requestClose = current::requestClose, afterDismiss = { if (flow === current && current.current().finished) current.finishDismissal() })
            }
        }
        val previous = output.value.primary
        if (previous !== flowPresentation) setPrimary(flowPresentation)
        output.update { it.copy(dismissAllowed = state.dismissAllowed) }
        if (state.finished && previous == null) current.finishDismissal()
    }
    private fun updatePrimary(input: RootModalInputs) {
        if (!active || flow != null) return
        val onboarding = !loggedIn && !input.openedLink && !input.root.onboardingDismissed
        val key: Any? = when {
            onboarding -> "onboarding"
            updates.checksEnabled && input.showUpdate && input.update != null -> input.update
            input.request != null -> input.request
            else -> null
        }
        if (key == baseKey) return
        baseKey = key
        setPrimary(when (key) {
            "onboarding" -> onboarding()
            is com.coffeepeek.feature.appupdate.domain.AppUpdate -> update(key)
            is CollectionPickerRequests.Request -> picker(key)
            else -> null
        })
    }
    private fun onboarding(): IosRootPresentation {
        var destination: Navigator.Screen? = null
        val model = IosGuestOnboarding(language) { route ->
            destination = route
            lifecycle.dismissOnboarding()
        }
        guestOnboarding = model
        return IosRootPresentation(correlationId("onboarding"), true, { nativeRootModal(model) }, model::close,
            model::continueAsGuest, { destination?.let(Navigator::navigate) })
    }
    private fun update(update: com.coffeepeek.feature.appupdate.domain.AppUpdate): IosRootPresentation? {
        val version = AppConfigUpdateEnvironment.versionCode ?: return null
        val model = IosAppUpdate(update, updates, installer, language, version)
        return IosRootPresentation(correlationId("app-update"), true, { nativeRootModal(model) }, model::close, model::dismiss)
    }
    private fun picker(request: CollectionPickerRequests.Request): IosRootPresentation {
        val kind = if (request.target.kind == CollectionTargetKind.CoffeeShop) CollectionPlaceKind.CoffeeShop else CollectionPlaceKind.Roaster
        var signIn = false
        val source = IosCollectionsWorkspace(CollectionsWorkspaceSession(dependencies.get(), dependencies.get(), dependencies.get(),
            CollectionsWorkspaceMode.Picker, null, null, SavedCollectionTarget(kind, request.target.slug), { requests.dismiss(request) },
            { signIn = true; requests.dismiss(request) }, {}, {}, {}, {}, {}), photos = dependencies.get())
        requests.presented(request)
        return IosRootPresentation(correlationId("collections"), false, { ui.makeCollectionsWorkspaceController(source, language.current().languageTag) },
            source::close, { requests.dismiss(request) }, { if (signIn) Navigator.navigate(Navigator.Screen.Auth) }, { !source.current().busy })
    }
    private fun setPrimary(next: IosRootPresentation?) {
        val previous = output.value.primary
        previous?.takeIf { it !== next }?.let(::retire)
        output.update { it.copy(primary = next, dismissAllowed = true) }
        finishUnclaimed()
    }
    private fun retire(presentation: IosRootPresentation, finish: Boolean = true) {
        Analytics.service?.overlay(presentation.id, false)
        if (finish) retired[presentation.id] = presentation else presentation.closeSource()
    }
    private fun finishUnclaimed() {
        retired.values.filter { it.id !in claimed }.toList().forEach(::dismissed)
    }
    fun openLoginAfterSessionEnd() {
        if (baseKey == "onboarding" && output.value.primary != null) guestOnboarding?.signIn()
        else {
            lifecycle.dismissOnboarding()
            Navigator.navigate(Navigator.Screen.Auth)
        }
    }
    private suspend fun updateGlobal(message: String?, loading: Boolean) {
        val key = Triple(message, loading, language.current().languageTag)
        if (!active || key == errorKey) return
        errorKey = key
        val generation = lifetime
        val model = globalDialog(message, loading)
        if (!active || generation != lifetime || errorKey != key) { model?.close(); return }
        val next = model?.let { IosRootPresentation(correlationId("global-dialog"), true, { globalDialogFactory(it) }, it::close) }
        globalDialog = next
        if (next != null) { authorKey = null; authorDialog = null }
        updateBlocking()
        if (next == null) revision.value++
    }
    private fun updateAuthors(state: IosBlockedAuthorsState) {
        if (!active || globalDialog != null) return
        val key = if (root.handlesAuthorPrompt()) state.prompt?.let { Triple(it.id, it.working, it.failed) } ?: state.needsLogin.takeIf { it } else null
        if (key == authorKey) return
        authorKey = key
        var signIn = false
        val model = key?.let { IosAuthorRootPrompt(language, blockedAuthors, state) { signIn = true } }
        authorDialog = model?.let { IosRootPresentation(correlationId("author-operation"), true, { nativeRootModal(it) }, it::close,
            afterDismiss = { if (signIn) Navigator.navigate(Navigator.Screen.Auth) }) }
        updateBlocking()
    }
    private fun updateBlocking() {
        val next = globalDialog ?: authorDialog
        val previous = output.value.blocking
        if (previous === next) return
        previous?.let(::retire)
        output.update { it.copy(blocking = next) }
        finishUnclaimed()
    }
    private suspend fun globalDialog(message: String?, loading: Boolean): IosGlobalDialog? {
        if (message == null && !loading) return null
        val environment = getSystemResourceEnvironment()
        val title = getString(environment, if (message != null) Res.string.error_dialog_unexpected_error else Res.string.loader_loading)
        val close = getString(environment, Res.string.error_dialog_shelf_camera_tips_close)
        return IosGlobalDialog(message != null, title, message, close, ErrorHandler::clearError)
    }
}

private data class RootModalInputs(val root: RootNavigationState, val openedLink: Boolean, val showUpdate: Boolean,
    val update: com.coffeepeek.feature.appupdate.domain.AppUpdate?, val request: CollectionPickerRequests.Request?)
