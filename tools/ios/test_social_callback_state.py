"""Execute the production OAuth callback actor without UI or live authentication."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SWIFTC = os.environ.get("SWIFTC") or shutil.which("swiftc")
CONTRACTS = r'''
@main private struct CallbackContracts {
    static func main() async {
        let parallel = AsyncStream<String>.makeStream()
        let request = SocialSignInCompletion<Void> { token, error in
            parallel.continuation.yield(token ?? error ?? "empty")
        }
        await withTaskGroup(of: Void.self) { group in
            for index in 0..<100 {
                group.addTask { await request.finish(token: "token-\(index)", error: nil) }
            }
        }
        parallel.continuation.finish()
        var results: [String] = []
        for await result in parallel.stream { results.append(result) }
        precondition(results.count == 1)

        let cancelled = AsyncStream<String>.makeStream()
        let cancelledRequest = SocialSignInCompletion<Void> { token, error in
            cancelled.continuation.yield(token ?? error ?? "empty")
        }
        await cancelledRequest.finish(token: nil, error: "cancelled")
        await cancelledRequest.finish(token: "late-token", error: nil)
        cancelled.continuation.finish()
        var cancellations: [String] = []
        for await result in cancelled.stream { cancellations.append(result) }
        precondition(cancellations == ["cancelled"])
        print("2 OAuth actor contracts passed")
    }
}
'''


@unittest.skipUnless(SWIFTC, "Swift compiler is needed for actor contracts")
class SocialCallbackStateTest(unittest.TestCase):
    def test_callback_is_single_use_under_concurrency_and_late_success(self):
        source = (ROOT / "iosXtool/CoffeePeek/Sources/CoffeePeek/SocialSignInProvider.swift").read_text(encoding="utf-8")
        actor = source[source.index("@MainActor\nprivate final class SocialSignInCompletion"):source.index("/// Kotlin dispatches")]
        with tempfile.TemporaryDirectory(prefix="coffeepeek-social-callback-") as directory:
            swift = Path(directory) / "CallbackContracts.swift"
            executable = Path(directory) / ("contracts.exe" if os.name == "nt" else "contracts")
            swift.write_text("import Foundation\n" + actor + CONTRACTS, encoding="utf-8")
            compile_result = subprocess.run([SWIFTC, "-parse-as-library", "-swift-version", "6",
                "-strict-concurrency=complete", str(swift), "-o", str(executable)], capture_output=True, text=True)
            self.assertEqual(compile_result.returncode, 0, compile_result.stdout + compile_result.stderr)
            result = subprocess.run([str(executable)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn("2 OAuth actor contracts passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
