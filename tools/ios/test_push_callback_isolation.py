"""Invoke production KMP push callbacks off the main actor without UI or Firebase."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SWIFTC = os.environ.get("SWIFTC") or shutil.which("swiftc")
STUBS = r'''
import Foundation
private struct NotificationSettings: Sendable { let authorizationStatus = true }
private final class UNUserNotificationCenter: Sendable {
    static func current() -> UNUserNotificationCenter { UNUserNotificationCenter() }
    func notificationSettings() async -> NotificationSettings { NotificationSettings() }
}
// Kotlin exports plain escaping blocks, without a Swift Sendable annotation.
private final class NativePushBridge: Sendable {
    static let shared = NativePushBridge()
    nonisolated(unsafe) private var permission: @convention(block) () -> Void = {}
    nonisolated(unsafe) private var account: @convention(block) (String?) -> Void = { _ in }
    nonisolated(unsafe) private var settings: @convention(block) () -> Void = {}
    func configure(installationId: String, available: Bool, permissionGranted: Bool,
                   requestPermission: @escaping @convention(block) () -> Void,
                   changeAccount: @escaping @convention(block) (String?) -> Void,
                   requestPermissionFromSettings: @escaping @convention(block) () -> Void) {
        permission = requestPermission
        account = changeAccount
        settings = requestPermissionFromSettings
        account(nil)
    }
    func deliverFromWorker() {
        precondition(!Thread.isMainThread)
        permission()
        account("fixture-account")
        settings()
        account(nil)
    }
}
@MainActor private final class PushPlatformRun {
    let channel = AsyncStream<PushPlatformEvent>.makeStream()
    var continuation: AsyncStream<PushPlatformEvent>.Continuation { channel.continuation }
}
@MainActor private final class PushAppDelegate {
    private func current(_ run: PushPlatformRun) -> Bool { true }
    private func permitted(_ status: Bool) -> Bool { status }
    func start(_ run: PushPlatformRun) async { await configure("fixture-installation", run: run) }
'''
CONTRACTS = r'''
}
@main private struct PushCallbackContracts {
    @MainActor static func main() async {
        let run = PushPlatformRun()
        await PushAppDelegate().start(run)
        let continuation = run.continuation
        await Task.detached {
            NativePushBridge.shared.deliverFromWorker()
            continuation.finish()
        }.value
        var received: [String] = []
        for await event in run.channel.stream {
            switch event {
            case .permission: received.append("permission")
            case .settingsPermission: received.append("settings")
            case .account(let user): received.append(user ?? "signed-out")
            default: preconditionFailure("Unexpected push event")
            }
        }
        precondition(received == ["signed-out", "permission", "fixture-account", "settings", "signed-out"])
        print("5 background push callback events passed")
    }
}
'''


@unittest.skipUnless(SWIFTC, "Swift compiler is required")
class PushCallbackIsolationTests(unittest.TestCase):
    def test_production_callbacks_accept_background_account_and_permission_events(self):
        source = (ROOT / "iosApp/iosApp/CoffeePeekApp.swift").read_text(encoding="utf-8")
        events = source[source.index("private enum PushPlatformEvent"):source.index("private final class PushMessagingDelegate")]
        configure = source[source.index("    private func configure(_ installation:"):source.index("    private func requestPermission(fromSettings:")]
        with tempfile.TemporaryDirectory(prefix="coffeepeek-push-callback-") as directory:
            swift = Path(directory) / "PushCallbackContracts.swift"
            executable = Path(directory) / ("contracts.exe" if os.name == "nt" else "contracts")
            swift.write_text(events + STUBS + configure + CONTRACTS, encoding="utf-8")
            compiled = subprocess.run([SWIFTC, "-parse-as-library", "-swift-version", "6",
                "-strict-concurrency=complete", "-enable-actor-data-race-checks",
                str(swift), "-o", str(executable)], capture_output=True, text=True)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(executable)], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("5 background push callback events passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
