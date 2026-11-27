@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package com.coffeepeek.admin.ui

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.analyticsPage
import com.coffeepeek.admin.analytics.correlationId
import com.coffeepeek.admin.analytics.NativeScreenAnalytics
import com.coffeepeek.admin.di.IosNativeControllerFactories
import com.coffeepeek.admin.di.IosNativeDestinations
import com.coffeepeek.admin.di.community.CommunityFeedRefresh
import com.coffeepeek.admin.di.shop.CheckInFlowHandoff
import com.coffeepeek.admin.notifications.foregroundNotificationHeartbeat
import com.coffeepeek.admin.settings.AppLanguagePreference
import com.coffeepeek.admin.theme.ThemeManager
import com.coffeepeek.admin.ui.screen.notifications.NotificationsRenderer
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.auth.api.UsernameSelectionGate
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import com.coffeepeek.feature.notifications.api.NotificationFeature
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.koin.core.Koin
import platform.UIKit.UIViewController

object IosNativeRootControllers {
    private lateinit var factory: (IosNativeRoot) -> UIViewController
    fun configure(factory: (IosNativeRoot) -> UIViewController) { this.factory = factory }
    internal fun controller(root: IosNativeRoot) = factory(root)
}

/** Root lifecycle and routing policy. This owner never hosts or renders a composition. */
class IosNativeRoot internal constructor(private val dependencies: Koin, private val ui: IosNativeControllerFactories,
    private val onReady: () -> Unit,
) {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessions = dependencies.get<SessionReader>()
    private val usernameSelection = dependencies.get<UsernameSelectionGate>()
    private val language = dependencies.get<AppLanguagePreference>()
    private val ownership = dependencies.get<BusinessOwnershipEntry>()
    private val authors = dependencies.get<AuthorBlockingPresenter>()
    private val lifecycle = RootNavigationLifecycle(Navigator::prepareRootTransition, Navigator::acceptRootTransition)
    private val output = MutableStateFlow(IosRootState())
    private val foreground = MutableStateFlow(false)
    private val started = MutableStateFlow(false)
    private val sessionReady = MutableStateFlow(false)
    private val navigation = Mutex()
    private val interacting = MutableStateFlow(false)
    private val reselections = MainTabReselections()
    internal val refreshCommunity = CommunityFeedRefresh()
    private var epoch = 0L
    private var nextCommand = 0L
    private var owner: RootOwner? = null
    private var pending: NativeRootNavigationCommand? = null
    private var sessionJob: Job? = null
    private var closed = false
    private var signalledReady = false
    internal val modals = IosRootModals(this, dependencies, ui, lifecycle, foreground)

    init {
        observeNavigation()
        observeDeferredLogin()
        observeSession()
        observeUsernameSelection()
        observeEnvironment()
        observeForeground()
        val createVisible = combine(output, foreground) { state, active -> state.ready && active &&
            state.stacks.getOrNull(state.selected)?.routes?.size == 1 && !(state.selected == 0 && state.expandedMap) }
        NativeScreenAnalytics(scope, null, createVisible).feature("check_in", "global_create", createVisible)
        scope.launch { ownership.observeSessionChanges() }
        scope.launch { output.map { if (it.ready) lifecycle.state.value.session?.isLoggedIn else null }
            .distinctUntilChanged().collectLatest { loggedIn ->
                if (loggedIn != null) dependencies.get<NotificationsRenderer>().observePendingOpen(flowOf(loggedIn))
            } }
    }
    fun current() = output.value
    fun observe(receive: (IosRootState) -> Unit) = IosRootObservation(scope.launch { output.collect(receive) })
    fun retry() { if (!closed && output.value.failed) observeSession() }
    fun setForeground(active: Boolean) { if (!closed) foreground.value = active }
    fun setStarted(active: Boolean) { if (!closed) started.value = active }
    fun close() {
        if (closed) return
        closed = true
        pending?.ack?.cancel()
        pending = null
        owner?.scope?.cancel()
        owner = null
        modals.close()
        Analytics.service?.setForeground(false)
        scope.cancel()
    }
    fun controller(route: IosRootRoute): UIViewController? {
        val current = owner ?: return null
        if (!output.value.ready || route.generation != epoch || current.routes[route.key] !== route) return null
        return current.destinations.controller(route.screen)
    }
    fun shown(route: IosRootRoute) {
        if (route.generation == epoch && output.value.ready) Analytics.service?.screenShown(route.page, route.visit)
    }
    fun confirm(command: Long, generation: Long) {
        val request = pending?.takeIf { it.id == command && it.generation == generation && generation == epoch } ?: return
        val before = owner?.model?.actual?.stacks?.flatten().orEmpty()
        if (before.any { it.screen == Navigator.Screen.CreateCheckIn } && request.stacks.stacks.flatten().none { it.screen == Navigator.Screen.CreateCheckIn })
            CheckInFlowHandoff.cancelFromBack()
        owner?.model?.confirm(request.stacks)
        pending = null
        publishActual()
        request.ack.complete(Unit)
    }
    fun didPop(generation: Long, tab: Int, topKey: Long) {
        val current = owner?.takeIf { generation == epoch && pending == null && output.value.ready } ?: return
        val before = current.model.actual
        val stack = before.stacks.getOrNull(tab) ?: return
        val index = stack.indexOfFirst { it.key == topKey }.takeIf { it >= 0 } ?: return
        if (stack.drop(index + 1).any { it.screen == Navigator.Screen.CreateCheckIn }) CheckInFlowHandoff.cancelFromBack()
        current.model.confirm(before.copy(stacks = before.stacks.mapIndexed { i, entries -> if (i == tab) entries.take(index + 1) else entries }))
        publishActual()
    }
    fun selected(generation: Long, tab: Int) {
        val current = owner?.takeIf { generation == epoch && pending == null && output.value.ready && tab in it.model.visibleTabs } ?: return
        current.model.confirm(current.model.actual.copy(selected = tab))
        publishActual()
    }
    fun reselected(generation: Long, tab: Int) {
        if (generation == epoch && output.value.ready && tab == 0) reselections.onTabPressed(Navigator.Screen.FeedTab, true)
    }
    fun setInteracting(generation: Long, active: Boolean) { if (generation == epoch) interacting.value = active }
    fun createCheckIn() { if (output.value.ready) modals.createCheckIn() }
    fun resetLogin() { scope.launch { transition { it.planLink(Navigator.Screen.Auth) } } }
    fun currentModals() = modals.current()
    fun observeModals(receive: (IosRootModalState) -> Unit) = modals.observe(receive)
    fun modalController(presentation: IosRootPresentation) = modals.controller(presentation)
    fun modalPresented(presentation: IosRootPresentation) = modals.presented(presentation)
    fun modalDismissed(presentation: IosRootPresentation) = modals.dismissed(presentation)
    fun closeModal(presentation: IosRootPresentation) = modals.requestClose(presentation)
    fun canDismissModal(presentation: IosRootPresentation) = modals.canDismiss(presentation)
    internal fun handlesAuthorPrompt() = owner?.model?.actual?.let { it.stacks[it.selected].last().screen != Navigator.Screen.BlockedAuthors } == true

    private fun observeSession() {
        sessionJob?.cancel()
        sessionJob = scope.launch {
            try { sessions.observeReadyRootNavigationSession(usernameSelection).collect {
                lifecycle.accept(it)
                sessionReady.value = true
            } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failSession() }
        }
    }
    private fun observeUsernameSelection() {
        scope.launch { usernameSelection.state.collect { setup ->
            if (setup.blocksApp) {
                output.update { it.copy(ready = false) }
                signalReady()
            }
        } }
    }
    private fun failSession() {
        sessionReady.value = false
        epoch++
        interacting.value = false
        pending?.ack?.cancel()
        pending = null
        owner?.scope?.cancel()
        owner = null
        output.value = output.value.copy(ready = false, failed = true, generation = epoch, stacks = emptyList())
        modals.revoke()
        signalReady()
    }
    private fun observeEnvironment() {
        scope.launch { combine(lifecycle.state, ownership.accountSwitchState, language.selection, ThemeManager.ready, sessionReady) {
            state, business, language, theme, session -> RootEnvironment(state, business.businessMode, language != null && theme && session)
        }.combine(usernameSelection.state) { environment, setup ->
            environment.copy(ready = environment.ready && !setup.blocksApp &&
                environment.state.session == sessions.currentRootNavigationSession())
        }.collect { environment ->
            if (environment.ready && environment.state.session != null) install(environment)
        } }
        scope.launch { language.observe().collectLatest { labels() } }
        scope.launch { ThemeManager.themeMode.collect { theme -> output.update { it.copy(theme = theme.name) } } }
    }
    private fun install(environment: RootEnvironment) {
        val session = checkNotNull(environment.state.session)
        val tabs = mainTabScreens(session.isLoggedIn, environment.businessMode)
        if (owner?.session != session || owner?.tabs != tabs) replaceOwner(session, environment.businessMode, tabs)
        publishActual()
        modals.ready(session.isLoggedIn)
        signalReady()
    }
    private fun signalReady() { if (!signalledReady) { signalledReady = true; onReady() } }
    private fun replaceOwner(session: RootNavigationSession, business: Boolean, tabs: List<Navigator.Screen>) {
        epoch++
        interacting.value = false
        pending?.ack?.cancel()
        pending = null
        owner?.scope?.cancel()
        modals.revoke()
        val child = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        val generation = epoch
        val origin = RootNavigationOrigin(session)
        val destinations = IosNativeDestinations(dependencies, ui, child, { session.isLoggedIn }, ::resetLogin,
            { expanded -> if (epoch == generation) output.update { it.copy(expandedMap = expanded) } },
            reselections.search, refreshCommunity::register, origin)
        owner = RootOwner(session, tabs, child, NativeRootNavigationModel(session.isLoggedIn, business), destinations, origin)
        output.update { it.copy(ready = false, generation = epoch, expandedMap = false, stacks = emptyList()) }
    }
    private fun observeNavigation() {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { Navigator.navigationEvents.collect { event ->
            if (output.value.ready) guardedTransition(epoch) { model -> planNavigation(event, model) }
        } }
        val readiness = output.map { it.generation.takeIf { _ -> it.ready } }.distinctUntilChanged()
        scope.launch { combine(Navigator.pendingTabSelection, readiness) { tab, ready -> tab to ready }.collectLatest { (tab, ready) ->
            if (tab != null && ready != null && guardedTransition(ready) { it.plan(Navigator.NavEvent.SelectTab(tab)) }) Navigator.consumeNativeTab(tab)
        } }
        scope.launch { combine(Navigator.nativePendingLink, readiness) { link, ready -> link to ready }.collectLatest { (link, ready) ->
            if (link != null && ready != null && guardedTransition(ready) { it.planLink(link) }) Navigator.consumeNativeLink(link)
        } }
    }
    private fun planNavigation(event: Navigator.NavEvent, model: NativeRootNavigationModel): NativeRootStacks {
        val current = owner ?: return model.actual
        if (!Navigator.prepareRootEvent(event, current.origin, sessions.currentRootNavigationSession())) return model.actual
        return model.plan(event)
    }
    private fun observeDeferredLogin() {
        scope.launch { combine(output, Navigator.nativeLoginAfterSessionEnd) { state, requested ->
            state.ready && owner?.session?.isLoggedIn == false && requested
        }.distinctUntilChanged().collect { pending ->
            if (pending && Navigator.consumeNativeLoginAfterSessionEnd()) modals.openLoginAfterSessionEnd()
        } }
    }
    private suspend fun guardedTransition(generation: Long, plan: (NativeRootNavigationModel) -> NativeRootStacks): Boolean = try {
        transition(generation, plan)
    } catch (cancelled: CancellationException) {
        currentCoroutineContext().ensureActive()
        false
    }
    private suspend fun transition(generation: Long = epoch, plan: (NativeRootNavigationModel) -> NativeRootStacks): Boolean = navigation.withLock {
        output.first { it.ready || it.failed }
        interacting.first { !it }
        if (generation != epoch) return@withLock false
        val current = owner?.takeIf { output.value.ready } ?: return@withLock false
        val stacks = plan(current.model)
        if (stacks == current.model.actual) return@withLock true
        val request = NativeRootNavigationCommand(++nextCommand, epoch, stacks)
        pending = request
        publish(stacks, request.id)
        request.awaitShown()
        current === owner
    }
    private fun publishActual() {
        val current = owner ?: return
        pending?.let { publish(it.stacks, it.id); return }
        publish(current.model.actual, output.value.command)
        val keys = current.model.actual.stacks.flatten().map { it.key }.toSet()
        current.routes.keys.retainAll(keys)
        modals.navigationChanged()
    }
    private fun publish(stacks: NativeRootStacks, command: Long) {
        val current = owner ?: return
        val views = stacks.stacks.mapIndexed { index, entries -> IosRootStack(index, entries.map { entry ->
            current.routes.getOrPut(entry.key) { route(entry, index, entries.first() == entry) }
        }) }
        output.update { it.copy(ready = true, failed = false, command = command, selected = stacks.selected, stacks = views, visibleTabs = current.model.visibleTabs) }
    }
    private fun route(entry: NativeRootEntry, tab: Int, root: Boolean): IosRootRoute {
        val screen = entry.screen
        val page = if (root) listOf("discovery", "feed", "profile", "settings", "business_ownership")[tab] else analyticsPage(screen::class.simpleName)
        return IosRootRoute(epoch, entry.key, screen, page, correlationId("screen"),
            screen is Navigator.Screen.CheckInDetail || screen is Navigator.Screen.ShopReviews,
            screen is Navigator.Screen.ShopDetail || screen is Navigator.Screen.RoasterDetail, screen is Navigator.Screen.MyBusiness)
    }
    private fun observeForeground() {
        scope.launch { started.collectLatest { active ->
            Analytics.service?.setForeground(active)
            if (active) authors.refresh()
        } }
        scope.launch { foreground.collectLatest { active -> if (active) coroutineScope {
            launch { foregroundNotificationHeartbeat(dependencies.get<NotificationFeature>()::foreground) }
            launch { ownership.refreshSession() }
            modals.foreground()
            awaitCancellation()
        } } }
    }
    private suspend fun labels() {
        val environment = getSystemResourceEnvironment()
        val labels = listOf(Res.string.navigation_search, Res.string.navigation_feed, Res.string.navigation_profile,
            Res.string.navigation_settings, Res.string.business_tab_title).map { getString(environment, it) }
        val create = getString(environment, Res.string.check_in_create_actions)
        val failure = getString(environment, Res.string.shop_session_unavailable)
        val retry = getString(environment, Res.string.shop_session_retry)
        output.update { it.copy(labels = labels, createTitle = create, failure = failure, retry = retry) }
    }
}

private data class RootEnvironment(val state: RootNavigationState, val businessMode: Boolean, val ready: Boolean)
private class RootOwner(val session: RootNavigationSession, val tabs: List<Navigator.Screen>, val scope: CoroutineScope,
    val model: NativeRootNavigationModel, val destinations: IosNativeDestinations, val origin: RootNavigationOrigin,
) { val routes = mutableMapOf<Long, IosRootRoute>() }
