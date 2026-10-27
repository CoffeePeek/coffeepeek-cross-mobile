import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct CoffeePeekApp: App {
    @State private var isReady = false
    #if canImport(FirebaseCore) && canImport(FirebaseMessaging)
    @UIApplicationDelegateAdaptor(PushAppDelegate.self) private var pushDelegate
    #endif
    init() {
        IosNativeMapRegistry.shared.provider = MapLibreMapProvider()
        SocialSignInProvider.shared.configure()
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                ComposeRootView(onReady: { isReady = true })
                if !isReady { CoffeePeekLaunchView() }
            }
                // UIKit applies safe areas per screen, including full-bleed shop photos.
                .ignoresSafeArea(.container)
                .ignoresSafeArea(.keyboard)
                .background(Color(uiColor: UIColor(named: "LaunchBackground") ?? .systemBackground).ignoresSafeArea())
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
    let onReady: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(onReady: {
            DispatchQueue.main.async { onReady() }
        })
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
                    requestPermission: { [weak self] in self?.requestPermission() },
                    changeAccount: { [weak self] user in
                        DispatchQueue.main.async { self?.changeAccount(user) }
                    }, requestPermissionFromSettings: { [weak self] in self?.requestPermissionFromSettings() })
            }
        }
        NotificationCenter.default.addObserver(self, selector: #selector(foreground), name: UIApplication.didBecomeActiveNotification, object: nil)
        return true
    }

    private func requestPermission() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .notDetermined {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in self.foreground() }
            }
        }
    }

    private func requestPermissionFromSettings() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .notDetermined {
                self.requestPermission()
            } else if settings.authorizationStatus == .denied {
                DispatchQueue.main.async {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
            } else {
                self.foreground()
            }
        }
    }

    @objc private func foreground() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let permitted = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
            DispatchQueue.main.async {
                NativePushBridge.shared.updatePermission(granted: permitted)
                if self.revokePending { self.revokeToken(); return }
                if permitted, let user = self.account {
                    if Messaging.messaging().apnsToken == nil {
                        NativePushBridge.shared.updateRegistrationStatus(userId: user, status: "APNS_PENDING")
                    }
                    UIApplication.shared.registerForRemoteNotifications()
                    self.fetchToken()
                }
            }
        }
    }

    private func changeAccount(_ user: String?) {
        if account != user { revokePending = true }
        account = user
        if revokePending { revokeToken() } else { foreground() }
    }

    private func revokeToken() {
        if let user = account { NativePushBridge.shared.updateRegistrationStatus(userId: user, status: "RESET_PENDING") }
        guard !resetting else { return }
        resetting = true
        Messaging.messaging().isAutoInitEnabled = false
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        Messaging.messaging().deleteToken { error in
            DispatchQueue.main.async {
                self.resetting = false
                if let user = self.account, let error {
                    NativePushBridge.shared.updateRegistrationStatus(userId: user, status: "RESET_\((error as NSError).code)")
                }
                if error == nil { self.revokePending = false; self.foreground() }
            }
        }
    }

    private func fetchToken() {
        guard !resetting, !revokePending, let user = account, Messaging.messaging().apnsToken != nil else { return }
        NativePushBridge.shared.updateRegistrationStatus(userId: user, status: "FCM_PENDING")
        Messaging.messaging().token { token, error in
            DispatchQueue.main.async {
                guard self.account == user else { return }
                if let token, !token.isEmpty { NativePushBridge.shared.updateToken(userId: user, token: token) }
                else {
                    let status = error.map { "FCM_\(($0 as NSError).code)" } ?? "FCM_EMPTY"
                    NativePushBridge.shared.updateRegistrationStatus(userId: user, status: status)
                }
            }
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        fetchToken()
    }
    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        guard let user = account else { return }
        NativePushBridge.shared.updateRegistrationStatus(userId: user, status: "APNS_\((error as NSError).code)")
    }
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) { DispatchQueue.main.async { self.fetchToken() } }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completion: @escaping (UNNotificationPresentationOptions) -> Void) {
        let recipient = notification.request.content.userInfo["userId"] as? String
        let accepted = NativePushBridge.shared.accepts(userId: recipient)
        if accepted {
            DispatchQueue.main.async { NativePushBridge.shared.notificationReceived(userId: recipient) }
        }
        completion(accepted ? [.banner, .list, .sound] : [])
    }
}
#endif
