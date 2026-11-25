"""Execute production app lifetime models with fake SDK ports, without UI."""
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
protocol ObservableObject: AnyObject {}
protocol MessagingDelegate: AnyObject {}
@MainActor final class Messaging {
    private static let shared = Messaging()
    static func messaging() -> Messaging { shared }
    var delegate: MessagingDelegate?
}
'''
CONTRACTS = r'''
@MainActor private final class ReadinessFixture {
    let signal = NativeLaunchReadiness()
    let entered = AsyncStream<Void>.makeStream()
    var completed = 0
    func wait() async -> Bool {
        entered.continuation.yield(())
        await signal.wait()
        guard !Task.isCancelled else { return false }
        completed += 1
        return true
    }
}
@main private struct LifetimeContracts {
    @MainActor static func main() async {
        let fixture = ReadinessFixture()
        var entered = fixture.entered.stream.makeAsyncIterator()
        await withTaskGroup(of: Bool.self) { group in
            group.addTask { await fixture.wait() }
            _ = await entered.next(isolation: MainActor.shared)
            group.cancelAll()
            for await value in group { precondition(!value) }
        }
        precondition(fixture.completed == 0)
        await withTaskGroup(of: Bool.self) { group in
            group.addTask { await fixture.wait() }
            group.addTask { await fixture.wait() }
            _ = await entered.next(isolation: MainActor.shared)
            _ = await entered.next(isolation: MainActor.shared)
            precondition(fixture.completed == 0)
            fixture.signal.complete()
            for await value in group { precondition(value) }
        }
        precondition(fixture.completed == 2)
        let remembered = await fixture.wait()
        precondition(remembered)

        let old = PushPlatformRun()
        Messaging.messaging().delegate = old.delegate
        let current = PushPlatformRun()
        Messaging.messaging().delegate = current.delegate
        old.close()
        precondition(Messaging.messaging().delegate === current.delegate)
        var ended = old.stream.makeAsyncIterator()
        let closed = await ended.next()
        precondition(closed == nil)
        current.delegate.messaging(Messaging.messaging(), didReceiveRegistrationToken: nil)
        var active = current.stream.makeAsyncIterator()
        guard case .token? = await active.next() else { fatalError("Fresh push stream did not receive its SDK event") }
        current.close()
        precondition(Messaging.messaging().delegate == nil)
        print("5 native lifetime contracts passed")
    }
}
'''


@unittest.skipUnless(SWIFTC, "Swift compiler is needed for lifetime contracts")
class NativeAppLifetimesTest(unittest.TestCase):
    def test_hidden_and_backgrounded_native_controllers_keep_one_visibility_lifetime(self):
        source = (ROOT / "iosXtool/CoffeePeek/Sources/CoffeePeek/CommunitySheetHost.swift").read_text(encoding="utf-8")
        visibility = source[source.index("@MainActor\nfinal class CoffeePeekNativeControllerVisibility"):source.index("@MainActor\nfunc makeCommunitySheetController")]
        fixture = r'''
import Foundation
@MainActor final class UIApplication {
    enum State { case active, background }
    static let shared = UIApplication()
    var applicationState: State = .active
}
@main struct VisibilityContracts {
    @MainActor static func main() {
        var events: [Bool] = []
        let lifetime = CoffeePeekNativeControllerVisibility { events.append($0) }
        precondition(events.isEmpty)
        lifetime.setShowing(true)
        lifetime.setForeground(false)
        lifetime.setForeground(false)
        precondition(events == [true, false])
        lifetime.setShowing(false)
        lifetime.setForeground(true)
        precondition(events == [true, false], "A hidden navigation entry must not resume with its scene")
        lifetime.setShowing(true)
        lifetime.setShowing(true)
        lifetime.setForeground(true)
        precondition(events == [true, false, true], "Resume must not repeat for the same visibility")
        lifetime.setShowing(false)
        lifetime.setShowing(true)
        precondition(events == [true, false, true, false, true], "Returning from a cover must resume the same owner")
        UIApplication.shared.applicationState = .background
        var delayed: [Bool] = []
        let background = CoffeePeekNativeControllerVisibility { delayed.append($0) }
        background.setShowing(true)
        precondition(delayed.isEmpty)
        background.setForeground(true)
        precondition(delayed == [true])
        print("3 native controller visibility contracts passed")
    }
}
'''
        with tempfile.TemporaryDirectory(prefix="coffeepeek-controller-visibility-") as directory:
            swift = Path(directory) / "VisibilityContracts.swift"
            executable = Path(directory) / ("contracts.exe" if os.name == "nt" else "contracts")
            swift.write_text(visibility + fixture, encoding="utf-8")
            compiled = subprocess.run([SWIFTC, "-parse-as-library", "-swift-version", "6",
                "-strict-concurrency=complete", str(swift), "-o", str(executable)], capture_output=True, text=True)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(executable)], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("3 native controller visibility contracts passed", result.stdout)

    def test_readiness_restart_and_push_delegate_ownership(self):
        source = (ROOT / "iosApp/iosApp/CoffeePeekApp.swift").read_text(encoding="utf-8")
        readiness = source[source.index("@MainActor\nprivate final class NativeLaunchReadiness"):source.index("private struct NativeRootView")]
        events = source[source.index("private enum PushPlatformEvent"):source.index("private final class PushNotificationDelegate")]
        push = source[source.index("@MainActor\nprivate final class PushPlatformRun"):source.index("@MainActor\nfinal class PushAppDelegate")]
        with tempfile.TemporaryDirectory(prefix="coffeepeek-native-lifetimes-") as directory:
            swift = Path(directory) / "LifetimeContracts.swift"
            executable = Path(directory) / ("contracts.exe" if os.name == "nt" else "contracts")
            swift.write_text(STUBS + readiness + events + push + CONTRACTS, encoding="utf-8")
            compiled = subprocess.run([SWIFTC, "-parse-as-library", "-swift-version", "6",
                "-strict-concurrency=complete", str(swift), "-o", str(executable)], capture_output=True, text=True)
            self.assertEqual(compiled.returncode, 0, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(executable)], capture_output=True, text=True, timeout=30)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("5 native lifetime contracts passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
