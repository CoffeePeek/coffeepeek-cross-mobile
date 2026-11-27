import ComposeApp
import OSLog
import SwiftUI
import UIKit

@MainActor
func makeCoffeePeekRootController(root: IosNativeRoot) -> UIViewController {
    CoffeePeekRootController(source: root)
}

@MainActor
func configureCoffeePeekNativeNavigation() {
    IosUsernameSelectionControllers.shared.configure(factory: makeUsernameSelectionController)
    IosNativeRootControllers.shared.configure(factory: makeCoffeePeekRootController)
    IosDiscoveryRouteControllers.shared.configure(factory: makeDiscoveryRouteController)
    NativeLocalBackProtection.shared.configure(update: { controller, protected in
        coffeePeekRouteHost(controller)?.protectsBack = protected.boolValue
    }, identity: { coffeePeekRouteHost($0)?.route.visit })
}

@MainActor
private func coffeePeekRouteHost(_ controller: UIViewController) -> CoffeePeekRouteController? {
    var current: UIViewController? = controller
    while let candidate = current {
        if let route = candidate as? CoffeePeekRouteController { return route }
        current = candidate.parent
    }
    return nil
}

/// UIKit alone owns tab, stack, gesture and transition state. Kotlin confirms routing policy.
@MainActor
private final class CoffeePeekRootController: UIViewController, UITabBarControllerDelegate,
    UINavigationControllerDelegate, UIGestureRecognizerDelegate {
    private let source: IosNativeRoot
    private let tabs = UITabBarController()
    private let create = UIViewController()
    private var navigations: [UINavigationController] = []
    private var cache: [Int64: CoffeePeekRouteController] = [:]
    private var observation: IosRootObservation?
    private var state: IosRootState
    private var installedGeneration: Int64?
    private var appliedCommand: Int64?
    private var applying = false
    private var mapBarHidden = false
    private weak var nativeTransitionNavigation: UINavigationController?
    private var nativeTransitionInFlight: Bool { nativeTransitionNavigation != nil }
    private var transition: (command: Int64, generation: Int64)?
    private var failure: UIViewController?
    private let navigationLog = Logger(subsystem: "com.coffeepeek", category: "NativeNavigation")
    private lazy var modals = CoffeePeekRootModalPresenter(source: source, host: self)

    init(source: IosNativeRoot) {
        self.source = source
        state = source.current()
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { nil }
    override var childForStatusBarStyle: UIViewController? { tabs }
    isolated deinit {
        observation?.cancel()
        source.close()
        NotificationCenter.default.removeObserver(self)
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = CoffeePeekNativeStyle.nativeBackground
        tabs.view.backgroundColor = CoffeePeekNativeStyle.nativeBackground
        attach(tabs)
        tabs.delegate = self
        observeLifecycle()
        observation = source.observe { [weak self] in self?.receive($0) }
        modals.start()
        receive(source.current())
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        applyAppearance()
        syncTabBar()
        traceNavigation("root_appeared")
        modals.flush()
    }
    private func receive(_ next: IosRootState) {
        state = next
        applyAppearance()
        view.tintColor = UIColor(CoffeePeekNativeStyle.accent)
        if !next.ready { showUnavailable(next); return }
        removeFailure()
        renderNavigation()
        updateLabels()
        syncTabBar()
        modals.flush()
    }
    private func applyAppearance() {
        let style: UIUserInterfaceStyle = state.theme == "DARK" ? .dark : state.theme == "LIGHT" ? .light : .unspecified
        overrideUserInterfaceStyle = style
        viewIfLoaded?.window?.overrideUserInterfaceStyle = style
        setNeedsStatusBarAppearanceUpdate()
    }
    private func showUnavailable(_ next: IosRootState) {
        view.endEditing(true)
        if installedGeneration != nil {
            setMapBarHidden(false, animated: false)
            tabs.setViewControllers([], animated: false)
            navigations.removeAll()
            cache.removeAll()
            installedGeneration = nil
            appliedCommand = nil
            transition = nil
            nativeTransitionNavigation = nil
        }
        removeFailure()
        let child = UIHostingController(rootView: CoffeePeekRootStatus(failed: next.failed,
            message: next.failure, retry: next.retry, onRetry: source.retry))
        failure = child
        attach(child)
    }
    private func removeFailure() {
        guard let failure else { return }
        failure.willMove(toParent: nil)
        failure.view.removeFromSuperview()
        failure.removeFromParent()
        self.failure = nil
    }
    private func renderNavigation() {
        guard transition == nil, !nativeTransitionInFlight else { return }
        if installedGeneration != state.generation { resetNavigation() }
        guard appliedCommand != state.command else { return }
        applyNavigation()
    }
    private func resetNavigation() {
        setMapBarHidden(false, animated: false)
        cache.removeAll()
        navigations = (0..<5).map { index in
            let navigation = UINavigationController()
            navigation.view.tag = index
            navigation.delegate = self
            navigation.setNavigationBarHidden(true, animated: false)
            navigation.view.backgroundColor = CoffeePeekNativeStyle.nativeBackground
            return navigation
        }
        installedGeneration = state.generation
        appliedCommand = nil
        let indices = state.visibleTabs.map(\.intValue)
        var controllers: [UIViewController] = indices.map { navigations[$0] }
        controllers.insert(create, at: (controllers.count + 1) / 2)
        tabs.setViewControllers(controllers, animated: false)
    }
    private func applyNavigation() {
        view.endEditing(true)
        let command = state.command
        let generation = state.generation
        let selected = Int(state.selected)
        transition = (command, generation)
        applying = true
        for stack in state.stacks {
            let navigation = navigations[Int(stack.index)]
            let controllers = stack.routes.enumerated().map { index, route in
                let child = controller(route)
                child.hidesBottomBarWhenPushed = index > 0
                return child
            }
            if navigation.viewControllers != controllers {
                let animated = stack.index == state.selected && navigation.view.window != nil && !UIAccessibility.isReduceMotionEnabled
                navigation.setViewControllers(controllers, animated: animated)
            }
        }
        tabs.selectedViewController = navigations[selected]
        applying = false
        appliedCommand = command
        finishWhenShown(navigations[selected], command: command, generation: generation)
    }
    private func finishWhenShown(_ navigation: UINavigationController, command: Int64, generation: Int64) {
        navigation.view.isUserInteractionEnabled = false
        guard let coordinator = navigation.transitionCoordinator else {
            if !nativeTransitionInFlight { finish(command: command, generation: generation) }
            return
        }
        coordinator.animate(alongsideTransition: nil) { [weak self] _ in self?.finish(command: command, generation: generation) }
    }
    private func finish(command: Int64, generation: Int64) {
        guard transition?.command == command, transition?.generation == generation else { return }
        transition = nil
        navigations.forEach { $0.view.isUserInteractionEnabled = true }
        source.confirm(command: command, generation: generation)
        pruneControllers()
        renderNavigation()
        syncTabBar()
        modals.flush()
    }
    private func pruneControllers() {
        let retained = Set(navigations.flatMap { $0.viewControllers.compactMap { ($0 as? CoffeePeekRouteController)?.route.key } })
        cache = cache.filter { retained.contains($0.key) }
    }
    private func controller(_ route: IosRootRoute) -> UIViewController {
        if let existing = cache[route.key] { return existing }
        let controller = CoffeePeekRouteController(route: route, source: source) { [weak self] in self?.routeAppeared() }
        cache[route.key] = controller
        return controller
    }
    private func updateLabels() {
        let symbols = ["magnifyingglass", "person.2", "person.crop.circle", "gearshape", "building.2"]
        for index in navigations.indices where index < state.labels.count {
            navigations[index].tabBarItem = UITabBarItem(title: state.labels[index], image: UIImage(systemName: symbols[index]), tag: index)
        }
        create.tabBarItem = UITabBarItem(title: state.createTitle, image: UIImage(systemName: "plus"), tag: 5)
        tabs.tabBar.tintColor = UIColor(CoffeePeekNativeStyle.accent)
        tabs.tabBar.unselectedItemTintColor = UIColor(CoffeePeekNativeStyle.secondary)
    }
    private func syncTabBar() {
        guard state.ready, transition == nil, !nativeTransitionInFlight else { traceNavigation("bar_deferred"); return }
        guard let navigation = tabs.selectedViewController as? UINavigationController else { return }
        // UINavigationController owns push/pop visibility, including cancelled gestures.
        // This override belongs only to the expanded map, independently of stack depth.
        setMapBarHidden(navigation === navigations.first && state.expandedMap, animated: !UIAccessibility.isReduceMotionEnabled)
        traceNavigation("bar_settled")
    }
    private var tabBarIsHidden: Bool {
        if #available(iOS 18.0, *) { tabs.isTabBarHidden }
        else { tabs.tabBar.isHidden }
    }
    private func setMapBarHidden(_ hidden: Bool, animated: Bool) {
        guard mapBarHidden != hidden else { return }
        mapBarHidden = hidden
        if #available(iOS 18.0, *) { tabs.setTabBarHidden(hidden, animated: animated) }
        else { tabs.tabBar.isHidden = hidden }
    }
    private func routeAppeared() {
        syncTabBar()
        traceNavigation("route_appeared")
        modals.flush()
    }
    private func traceNavigation(_ event: String) {
        guard Bundle.main.object(forInfoDictionaryKey: "CoffeePeekDebug") as? String == "YES" else { return }
        let selected = state.selected
        let navigation = tabs.selectedViewController as? UINavigationController
        let actualTab = navigation?.view.tag ?? -1
        let depth = navigation?.viewControllers.count ?? 0
        let generation = state.generation
        let command = state.command
        let expanded = state.expandedMap
        let hidden = tabBarIsHidden
        let viewHidden = tabs.tabBar.isHidden
        let pending = transition != nil
        let native = nativeTransitionInFlight
        let barHeight = Double(tabs.tabBar.frame.height)
        let bottom = Double(navigation?.view.safeAreaInsets.bottom ?? 0)
        navigationLog.info("event=\(event, privacy: .public) generation=\(generation) command=\(command) tab=\(selected) uiTab=\(actualTab) depth=\(depth) map=\(expanded) hidden=\(hidden) viewHidden=\(viewHidden) pending=\(pending) native=\(native) barHeight=\(barHeight) bottom=\(bottom)")
    }
    func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
        guard transition == nil, !nativeTransitionInFlight, !navigations.contains(where: { $0.transitionCoordinator != nil }) else { return false }
        view.endEditing(true)
        if viewController === create { source.createCheckIn(); return false }
        if viewController === tabs.selectedViewController, let index = navigations.firstIndex(where: { $0 === viewController }),
           navigations[index].viewControllers.count == 1 { source.reselected(generation: state.generation, tab: Int32(index)) }
        return true
    }
    func tabBarController(_ tabBarController: UITabBarController, didSelect viewController: UIViewController) {
        guard let index = navigations.firstIndex(where: { $0 === viewController }) else { return }
        source.selected(generation: state.generation, tab: Int32(index))
        syncTabBar()
    }
    func navigationController(_ navigationController: UINavigationController, willShow viewController: UIViewController, animated: Bool) {
        guard navigations.contains(where: { $0 === navigationController }) else { return }
        if animated { nativeTransitionNavigation = navigationController }
        navigationController.setNavigationBarHidden((viewController as? CoffeePeekRouteController)?.route.nativeNavigationBar != true, animated: animated)
        if navigationController.transitionCoordinator?.isInteractive == true { source.setInteracting(generation: state.generation, active: true) }
    }
    func navigationController(_ navigationController: UINavigationController, didShow viewController: UIViewController, animated: Bool) {
        guard navigations.contains(where: { $0 === navigationController }) else { return }
        if nativeTransitionNavigation === navigationController { nativeTransitionNavigation = nil }
        navigationController.interactivePopGestureRecognizer?.delegate = self
        navigationController.interactivePopGestureRecognizer?.isEnabled = navigationController.viewControllers.count > 1
        guard !applying else { return }
        if let transition, tabs.selectedViewController === navigationController {
            finish(command: transition.command, generation: transition.generation)
        } else if transition == nil, let route = (viewController as? CoffeePeekRouteController)?.route {
            source.didPop(generation: route.generation, tab: Int32(navigationController.view.tag), topKey: route.key)
            pruneControllers()
        }
        source.setInteracting(generation: state.generation, active: false)
        syncTabBar()
        renderNavigation()
        modals.flush()
    }
    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard let navigation = navigations.first(where: { $0.interactivePopGestureRecognizer === gestureRecognizer }) else { return false }
        return transition == nil && !nativeTransitionInFlight && navigation.viewControllers.count > 1 && navigation.transitionCoordinator == nil &&
            navigation.presentedViewController == nil && (navigation.topViewController as? CoffeePeekRouteController)?.protectsBack != true
    }
    private func attach(_ child: UIViewController) {
        addChild(child)
        child.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(child.view)
        NSLayoutConstraint.activate([child.view.topAnchor.constraint(equalTo: view.topAnchor), child.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            child.view.leadingAnchor.constraint(equalTo: view.leadingAnchor), child.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)])
        child.didMove(toParent: self)
    }
    private func observeLifecycle() {
        NotificationCenter.default.addObserver(self, selector: #selector(becameActive), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(resignedActive), name: UIApplication.willResignActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(enteredBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(enteredForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        source.setStarted(active: UIApplication.shared.applicationState != .background)
        source.setForeground(active: UIApplication.shared.applicationState == .active)
    }
    @objc private func becameActive() { source.setForeground(active: true); syncTabBar(); modals.flush() }
    @objc private func resignedActive() { source.setForeground(active: false) }
    @objc private func enteredBackground() { source.setStarted(active: false) }
    @objc private func enteredForeground() { source.setStarted(active: true) }
}

@MainActor
private final class CoffeePeekRouteController: UIViewController {
    let route: IosRootRoute
    var protectsBack = false
    private let source: IosNativeRoot
    private let appeared: () -> Void
    init(route: IosRootRoute, source: IosNativeRoot, appeared: @escaping () -> Void) {
        self.route = route
        protectsBack = route.localBack
        self.source = source
        self.appeared = appeared
        super.init(nibName: nil, bundle: nil)
        navigationItem.largeTitleDisplayMode = .never
    }
    required init?(coder: NSCoder) { nil }
    override var preferredStatusBarStyle: UIStatusBarStyle { route.lightStatusBar ? .lightContent : .default }
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = CoffeePeekNativeStyle.nativeBackground
        guard let child = source.controller(route: route) else { return }
        addChild(child)
        child.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(child.view)
        NSLayoutConstraint.activate([child.view.topAnchor.constraint(equalTo: view.topAnchor), child.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            child.view.leadingAnchor.constraint(equalTo: view.leadingAnchor), child.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)])
        child.didMove(toParent: self)
        if route.nativeNavigationBar { title = child.title }
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        source.shown(route: route)
        appeared()
    }
}

private struct CoffeePeekRootStatus: View {
    let failed: Bool
    let message: String
    let retry: String
    let onRetry: () -> Void
    var body: some View {
        VStack(spacing: 20) {
            if failed {
                Text(message).multilineTextAlignment(.center)
                Button(retry, action: onRetry).buttonStyle(.bordered)
            } else { CoffeePeekNativeLoader(label: "") }
        }.font(CoffeePeekNativeStyle.font(16)).padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
            .foregroundStyle(CoffeePeekNativeStyle.foreground).background(CoffeePeekNativeStyle.background.ignoresSafeArea())
    }
}

#Preview("Root retry light") { CoffeePeekRootStatus(failed: true, message: "Не удалось загрузить приложение", retry: "Повторить", onRetry: {}).preferredColorScheme(.light) }
#Preview("Root retry dark") { CoffeePeekRootStatus(failed: true, message: "Не удалось загрузить приложение", retry: "Повторить", onRetry: {}).preferredColorScheme(.dark) }
