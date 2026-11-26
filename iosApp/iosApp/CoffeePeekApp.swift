import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct CoffeePeekApp: App {
    @State private var isReady = false
    @StateObject private var readiness = NativeLaunchReadiness()
    #if canImport(FirebaseCore) && canImport(FirebaseMessaging)
    @UIApplicationDelegateAdaptor(PushAppDelegate.self) private var pushDelegate
    #endif
    init() {
        configureCoffeePeekNativeGlass()
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        SocialSignInProvider.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                NativeRootView(onReady: readiness.complete)
                if !isReady { CoffeePeekLaunchView() }
            }
                // UIKit applies safe areas per screen, including full-bleed shop photos.
                .ignoresSafeArea(.container)
                .ignoresSafeArea(.keyboard)
                .background(Color(uiColor: UIColor(named: "LaunchBackground") ?? .systemBackground).ignoresSafeArea())
                .task {
                    await readiness.wait()
                    if !Task.isCancelled { isReady = true }
                }
                #if canImport(FirebaseCore) && canImport(FirebaseMessaging)
                .task { await pushDelegate.run() }
                #endif
                .onOpenURL { url in
                    if !GIDSignIn.sharedInstance.handle(url) {
                        _ = MainViewControllerKt.handleAppLink(link: url.absoluteString)
                    }
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    if let url = activity.webpageURL {
                        _ = MainViewControllerKt.handleAppLink(link: url.absoluteString)
                    }
                }
        }
    }

}

@MainActor
private final class NativeLaunchReadiness: ObservableObject {
    private var ready = false
    private var waiters: [UUID: AsyncStream<Void>.Continuation] = [:]

    func complete() {
        ready = true
        waiters.values.forEach { $0.yield(()); $0.finish() }
        waiters.removeAll()
    }

    func wait() async {
        guard !ready, !Task.isCancelled else { return }
        let id = UUID()
        let channel = AsyncStream<Void>.makeStream(bufferingPolicy: .bufferingNewest(1))
        waiters[id] = channel.continuation
        defer { waiters.removeValue(forKey: id); channel.continuation.finish() }
        for await _ in channel.stream { return }
    }
}

private struct NativeRootView: UIViewControllerRepresentable {
    let onReady: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        configureNativeScreens()
        return makeNativeRootController()
    }

    @MainActor private func configureNativeScreens() {
        configureCoffeePeekNativeNavigation()
        MainViewControllerKt.configureNativeEditProfile(factory: makeEditProfileController)
        IosCommunityMessageControllers.shared.configure(factory: makeCommunityMessageController)
        MainViewControllerKt.configureNativeGlobalDialogs(factory: makeGlobalDialogController)
        MainViewControllerKt.configureNativeDiscovery(factory: makeDiscoveryController)
        IosCheckInShopPickerControllers.shared.configure(factory: makeCheckInShopPickerController)
        MainViewControllerKt.configureNativeRootModals(factory: makeRootModalController)
        MainViewControllerKt.configureNativeCommunityCards(factory: makeCommunityCardController)
        IosShopDetailControllers.shared.configure(factory: makeShopDetailRouteController)
        IosShopCheckInControllers.shared.configure(factory: makeShopCheckInRouteController)
        MainViewControllerKt.configureNativeShopDetails(
            photo: makeShopPhotoOverlayController, message: makeShopDetailNoticeController, error: makeShopDetailErrorController)
        MainViewControllerKt.configureNativeCheckInDetails(factory: makeCheckInDetailController)
        IosHomeCheckInControllers.shared.configure(factory: makeHomeCheckInRouteController)
        MainViewControllerKt.configureNativeCommunityFeed(factory: makeCommunityFeedController)
        IosCheckInPhotoControllers.shared.configure(factory: makeCheckInPhotoController)
        IosHistoryFailureControllers.shared.configure(factory: makeHistoryFailureController)
        IosRoasterControllers.shared.configure(factory: makeRoasterController)
        MainViewControllerKt.configureNativeOwnership(factory: makeOwnershipController, actions: makeOwnershipActionsController)
        MainViewControllerKt.configureNativeShelf(factory: makeShelfController)
        MainViewControllerKt.configureNativeCheckInEditors(home: makeHomeCheckInEditorController, shop: makeShopCheckInEditorController)
    }

    @MainActor private func makeNativeRootController() -> UIViewController {
        MainViewControllerKt.MainViewController(
            onReady: onReady,
            makeDiscoveryFilterController: makeDiscoveryFilterController,
            makeShopMenuGalleryController: makeShopMenuGalleryController,
            makeShopCheckInListController: makeShopCheckInListController(list:languageTag:),
            makeCoffeeDetailController: makeCoffeeDetailController,
            makeShelfArchiveController: makeShelfArchiveController,
            makeThemeSettingsController: makeThemeSettingsController,
            makeShopReportController: makeShopReportController,
            makeContributionsController: makeContributionsController,
            makeContentReportController: makeContentReportController,
            makeCollectionsLibraryController: makeCollectionsLibraryController, makeCollectionsWorkspaceController: makeCollectionsWorkspaceController,
            makeSettingsController: makeSettingsController,
            makeBlockedAuthorsController: makeBlockedAuthorsController,
            makeAuthController: makeAuthController,
            makeCheckInCreatePickerController: makeCheckInCreatePickerController,
            makeGuestProfileController: makeGuestProfileController(profile:), makeOwnProfileController: makeOwnProfileController(profile:),
            makePublicProfileController: makePublicProfileController(profile:),
            makeNotificationInboxController: makeNotificationInboxController(inbox:languageTag:)
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


#if canImport(FirebaseCore) && canImport(FirebaseMessaging)
import FirebaseCore
import FirebaseMessaging
import UserNotifications

// Firebase packages and Push Notifications capability are installed on macOS; see docs/push-notifications.md.
private enum FirebasePlist {
    /// Installations throws if API_KEY is not 39 characters starting with `A`; the unsigned CI plist is only for compile checks.
    static var isValidForConfigure: Bool {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let dict = NSDictionary(contentsOfFile: path) as? [String: Any],
              let apiKey = dict["API_KEY"] as? String else { return false }
        return apiKey.count == 39 && apiKey.hasPrefix("A")
    }
}

private enum PushPlatformEvent: Sendable {
    case permission, settingsPermission, account(String?), foreground, token
}

private final class PushMessagingDelegate: NSObject, MessagingDelegate {
    let events: AsyncStream<PushPlatformEvent>.Continuation
    init(events: AsyncStream<PushPlatformEvent>.Continuation) { self.events = events }
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        events.yield(.token)
    }
}

private final class PushNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        let recipient = notification.request.content.userInfo["userId"] as? String
        return await MainActor.run {
            guard NativePushBridge.shared.accepts(userId: recipient) else { return [] }
            NativePushBridge.shared.notificationReceived(userId: recipient)
            return [.banner, .list, .sound]
        }
    }
}

@MainActor
private final class PushPlatformRun {
    let stream: AsyncStream<PushPlatformEvent>
    let continuation: AsyncStream<PushPlatformEvent>.Continuation
    let delegate: PushMessagingDelegate

    init() {
        let channel = AsyncStream<PushPlatformEvent>.makeStream()
        stream = channel.stream
        continuation = channel.continuation
        delegate = PushMessagingDelegate(events: channel.continuation)
    }

    func close() {
        continuation.finish()
        if Messaging.messaging().delegate === delegate { Messaging.messaging().delegate = nil }
    }
}

@MainActor
final class PushAppDelegate: NSObject, UIApplicationDelegate {
    private var platformRun: PushPlatformRun?
    private var installation: String?
    private var notificationDelegate: PushNotificationDelegate?
    private var account: String?
    private var revokePending = true

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        guard FirebasePlist.isValidForConfigure else { return true }
        FirebaseApp.configure()
        Messaging.messaging().isAutoInitEnabled = false
        notificationDelegate = PushNotificationDelegate()
        UNUserNotificationCenter.current().delegate = notificationDelegate
        let defaults = UserDefaults.standard
        let installation = defaults.string(forKey: "pushInstallation") ?? UUID().uuidString
        defaults.set(installation, forKey: "pushInstallation")
        self.installation = installation
        NotificationCenter.default.addObserver(self, selector: #selector(foreground), name: UIApplication.didBecomeActiveNotification, object: nil)
        return true
    }

    func run() async {
        guard let installation else { return }
        let run = PushPlatformRun()
        platformRun?.close()
        platformRun = run
        Messaging.messaging().delegate = run.delegate
        defer {
            run.close()
            if platformRun === run { platformRun = nil }
        }
        await configure(installation, run: run)
        for await event in run.stream {
            guard current(run) else { return }
            await handle(event, run: run)
        }
    }

    private func current(_ run: PushPlatformRun) -> Bool { platformRun === run && !Task.isCancelled }

    private func handle(_ event: PushPlatformEvent, run: PushPlatformRun) async {
        switch event {
        case .permission: await requestPermission(fromSettings: false, run: run)
        case .settingsPermission: await requestPermission(fromSettings: true, run: run)
        case .account(let user):
            if account != user { revokePending = true }
            account = user
            await refreshPermission(run: run)
        case .foreground: await refreshPermission(run: run)
        case .token: await fetchToken(run: run)
        }
    }

    private func configure(_ installation: String, run: PushPlatformRun) async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        guard current(run) else { return }
        let events = run.continuation
        // Kotlin invokes these blocks from worker threads; only the stream consumer is main-actor isolated.
        NativePushBridge.shared.configure(installationId: installation, available: true,
            permissionGranted: permitted(settings.authorizationStatus),
            requestPermission: { @Sendable in events.yield(.permission) },
            changeAccount: { @Sendable user in events.yield(.account(user)) },
            requestPermissionFromSettings: { @Sendable in events.yield(.settingsPermission) })
    }

    private func requestPermission(fromSettings: Bool, run: PushPlatformRun) async {
        let center = UNUserNotificationCenter.current()
        let status = await center.notificationSettings().authorizationStatus
        guard current(run) else { return }
        if status == .notDetermined {
            _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
        } else if fromSettings && status == .denied,
                  let url = URL(string: UIApplication.openSettingsURLString) {
            await UIApplication.shared.open(url)
        }
        if current(run) { await refreshPermission(run: run) }
    }

    @objc private func foreground() {
        platformRun?.continuation.yield(.foreground)
    }

    private func refreshPermission(run: PushPlatformRun) async {
        let status = await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
        guard current(run) else { return }
        let granted = permitted(status)
        NativePushBridge.shared.updatePermission(granted: granted)
        if revokePending { await revokeToken(run: run) }
        guard current(run), !revokePending, granted, account != nil else { return }
        UIApplication.shared.registerForRemoteNotifications()
        await fetchToken(run: run)
    }

    private func permitted(_ status: UNAuthorizationStatus) -> Bool {
        status == .authorized || status == .provisional
    }

    private func revokeToken(run: PushPlatformRun) async {
        Messaging.messaging().isAutoInitEnabled = false
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        // Token-only deletion requires check-in data that a fresh installation does not have.
        do {
            try await Messaging.messaging().deleteData()
            if current(run) { revokePending = false }
        } catch {
            // A failed revocation must never enable registration for another account.
            if current(run) { revokePending = true }
        }
    }

    private func fetchToken(run: PushPlatformRun) async {
        guard current(run), !revokePending, let user = account, Messaging.messaging().apnsToken != nil else { return }
        do {
            let token = try await Messaging.messaging().token()
            guard current(run), account == user, NativePushBridge.shared.accepts(userId: user), !token.isEmpty else { return }
            NativePushBridge.shared.updateToken(userId: user, token: token)
        } catch {
            // Existing foreground/token events retry without retaining stale account data.
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        platformRun?.continuation.yield(.token)
    }
}
#endif
