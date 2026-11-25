import ComposeApp
import SwiftUI
import UIKit

@MainActor
func makeCoffeePeekRootController(root: IosNativeRoot) -> UIViewController {
    CoffeePeekRootController(source: root)
}

@MainActor
func configureCoffeePeekNativeNavigation() {
    IosNativeRootControllers.shared.configure(factory: makeCoffeePeekRootController)
    IosDiscoveryRouteControllers.shared.configure(factory: makeDiscoveryRouteController)
    NativeLocalBackProtection.shared.configure(update: { controller, protected in
        coffeePeekRouteHost(controller)?.protectsBack = protected.boolValue
        return KotlinUnit()
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
    private var nativeTransitionInFlight = false
    private var transition: (command: Int64, generation: Int64)?
    private var failure: UIViewController?
    private lazy var modals = CoffeePeekRootModalPresenter(source: source, host: self)

    init(source: IosNativeRoot) {
        self.source = source
        state = source.current()
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { nil }
    isolated deinit {
        observation?.cancel()
        source.close()
        NotificationCenter.default.removeObserver(self)
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(CoffeePeekNativeStyle.background)
        attach(tabs)
        tabs.delegate = self
        observeLifecycle()
        observation = source.observe { [weak self] in self?.receive($0) }
        modals.start()
        receive(source.current())
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        modals.flush()
    }
    private func receive(_ next: IosRootState) {
        state = next
        overrideUserInterfaceStyle = next.theme == "DARK" ? .dark : next.theme == "LIGHT" ? .light : .unspecified
        view.tintColor = UIColor(CoffeePeekNativeStyle.accent)
        if !next.ready { showUnavailable(next); return }
        removeFailure()
        renderNavigation()
        updateLabels()
        syncTabBar()
        modals.flush()
    }
    private func showUnavailable(_ next: IosRootState) {
        if installedGeneration != nil {
            tabs.setViewControllers([], animated: false)
            navigations.removeAll()
            cache.removeAll()
            installedGeneration = nil
            appliedCommand = nil
            transition = nil
            nativeTransitionInFlight = false
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
        cache.removeAll()
        navigations = (0..<5).map { index in
            let navigation = UINavigationController()
            navigation.view.tag = index
            navigation.delegate = self
            navigation.setNavigationBarHidden(true, animated: false)
            navigation.view.backgroundColor = UIColor(CoffeePeekNativeStyle.background)
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
        let command = state.command
        let generation = state.generation
        let selected = Int(state.selected)
        transition = (command, generation)
        applying = true
        for stack in state.stacks {
            let navigation = navigations[Int(stack.index)]
            let controllers = stack.routes.map(controller)
            if navigation.viewControllers != controllers {
                navigation.setViewControllers(controllers, animated: stack.index == state.selected && navigation.view.window != nil && !UIAccessibility.isReduceMotionEnabled)
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
            finish(command: command, generation: generation)
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
        let controller = CoffeePeekRouteController(route: route, source: source) { [weak self] in self?.modals.flush() }
        controller.hidesBottomBarWhenPushed = !state.stacks.contains { $0.routes.first === route }
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
        guard let navigation = tabs.selectedViewController as? UINavigationController else { return }
        let hidden = navigation.viewControllers.count > 1 || (navigation === navigations.first && state.expandedMap)
        tabs.tabBar.isHidden = hidden
    }
    func tabBarController(_ tabBarController: UITabBarController, shouldSelect viewController: UIViewController) -> Bool {
        guard transition == nil, !navigations.contains(where: { $0.transitionCoordinator != nil }) else { return false }
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
        if animated { nativeTransitionInFlight = true }
        navigationController.setNavigationBarHidden((viewController as? CoffeePeekRouteController)?.route.nativeNavigationBar != true, animated: animated)
        if navigationController.transitionCoordinator?.isInteractive == true { source.setInteracting(generation: state.generation, active: true) }
    }
    func navigationController(_ navigationController: UINavigationController, didShow viewController: UIViewController, animated: Bool) {
        guard navigations.contains(where: { $0 === navigationController }) else { return }
        nativeTransitionInFlight = false
        navigationController.interactivePopGestureRecognizer?.delegate = self
        navigationController.interactivePopGestureRecognizer?.isEnabled = navigationController.viewControllers.count > 1
        guard !applying else { return }
        if transition == nil, let route = (viewController as? CoffeePeekRouteController)?.route {
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
        return transition == nil && navigation.viewControllers.count > 1 && navigation.transitionCoordinator == nil &&
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
    @objc private func becameActive() { source.setForeground(active: true); modals.flush() }
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
        view.backgroundColor = UIColor(CoffeePeekNativeStyle.background)
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
