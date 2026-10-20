import SwiftUI
import ComposeApp
import GoogleSignIn

@main
struct CoffeePeekApp: App {
    #if canImport(FirebaseCore) && canImport(FirebaseMessaging)
    @UIApplicationDelegateAdaptor(PushAppDelegate.self) private var pushDelegate
    #endif
    init() {
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        SocialSignInProvider.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .ignoresSafeArea(.container, edges: .all)
                .ignoresSafeArea(.keyboard)
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

private struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


#if canImport(FirebaseCore) && canImport(FirebaseMessaging)
import FirebaseCore
import FirebaseMessaging
import UserNotifications

// Firebase packages and Push Notifications capability are installed on macOS; see docs/push-notifications.md.
final class PushAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var account: String?
    private var resetting = false
    private var revokePending = true

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return true }
        FirebaseApp.configure()
        Messaging.messaging().isAutoInitEnabled = false
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        let defaults = UserDefaults.standard
        let installation = defaults.string(forKey: "pushInstallation") ?? UUID().uuidString
        defaults.set(installation, forKey: "pushInstallation")
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                NativePushBridge.shared.configure(installationId: installation, available: true,
                    permissionGranted: settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional,
                    requestPermission: { [weak self] in self?.requestPermission(); return KotlinUnit() },
                    changeAccount: { [weak self] user in
                        DispatchQueue.main.async { self?.changeAccount(user) }
                        return KotlinUnit()
                    })
            }
        }
        NotificationCenter.default.addObserver(self, selector: #selector(foreground), name: UIApplication.didBecomeActiveNotification, object: nil)
        return true
    }

    private func requestPermission() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .denied {
                DispatchQueue.main.async { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
            } else {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in self.foreground() }
            }
        }
    }

    @objc private func foreground() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let permitted = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
            DispatchQueue.main.async {
                NativePushBridge.shared.updatePermission(granted: permitted)
                if self.revokePending { self.revokeToken(); return }
                if permitted && self.account != nil { UIApplication.shared.registerForRemoteNotifications(); self.fetchToken() }
            }
        }
    }

    private func changeAccount(_ user: String?) {
        if account != user { revokePending = true }
        account = user
        if revokePending { revokeToken() } else { foreground() }
    }

    private func revokeToken() {
        guard !resetting else { return }
        resetting = true
        Messaging.messaging().isAutoInitEnabled = false
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        Messaging.messaging().deleteToken { error in
            DispatchQueue.main.async {
                self.resetting = false
                if error == nil { self.revokePending = false; self.foreground() }
            }
        }
    }

    private func fetchToken() {
        guard !resetting, !revokePending, let user = account, Messaging.messaging().apnsToken != nil else { return }
        Messaging.messaging().token { token, _ in
            DispatchQueue.main.async {
                if self.account == user, let token { NativePushBridge.shared.updateToken(userId: user, token: token) }
            }
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        fetchToken()
    }
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) { DispatchQueue.main.async { self.fetchToken() } }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completion: @escaping (UNNotificationPresentationOptions) -> Void) {
        let recipient = notification.request.content.userInfo["userId"] as? String
        completion(NativePushBridge.shared.accepts(userId: recipient) ? [.banner, .list, .sound] : [])
    }
}
#endif
