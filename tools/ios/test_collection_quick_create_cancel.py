"""Execute the production late-edit guard without presenting native UI."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "iosXtool/CoffeePeek/Sources/CoffeePeek/CollectionsWorkspaceScreen.swift"
SWIFTC = os.environ.get("SWIFTC") or shutil.which("swiftc")
FIXTURE = r'''
import Foundation
private struct Draft { var quickTitle: String? }
@MainActor private final class Workspace {
    var state = Draft(quickTitle: "Draft")
    var edits = [String?]()
    func current() -> Draft { state }
    func quickTitle(value: String?) {
        state.quickTitle = value
        edits.append(value)
    }
}
@MainActor private final class Model { let workspace = Workspace() }
@MainActor private final class Host {
    let model = Model()
    func edit(_ value: String) { updateQuickTitle(value) }
    PRODUCTION_UPDATER
}
@main private struct CancelContracts {
    @MainActor static func main() {
        for target in ["CoffeeShop", "Roaster"] {
            let host = Host()
            host.edit("Edited")
            precondition(host.model.workspace.current().quickTitle == "Edited", target)
            host.model.workspace.quickTitle(value: nil)
            host.edit("Final TextField value after Cancel")
            precondition(host.model.workspace.current().quickTitle == nil, target)
            precondition(host.model.workspace.edits.count == 2, target)
            host.model.workspace.quickTitle(value: "")
            host.edit("Fresh draft")
            precondition(host.model.workspace.current().quickTitle == "Fresh draft", target)
        }
        print("Quick-create edit/cancel/reopen contracts passed for shop and roaster")
    }
}
'''


def production_updater():
    source = SOURCE.read_text(encoding="utf-8")
    start = source.index("private func updateQuickTitle(_ value: String) {")
    opening = source.index("{", start)
    depth = 1
    end = opening + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[start:end]


@unittest.skipUnless(SWIFTC, "Swift compiler is needed for native callback contracts")
class CollectionQuickCreateCancelTest(unittest.TestCase):
    def test_late_text_field_edit_cannot_reopen_cancelled_shop_or_roaster_draft(self):
        with tempfile.TemporaryDirectory(prefix="coffeepeek-collection-cancel-") as temporary:
            folder = Path(temporary)
            fixture = folder / "CancelContracts.swift"
            fixture.write_text(FIXTURE.replace("PRODUCTION_UPDATER", production_updater()), encoding="utf-8")
            executable = folder / ("cancel-contracts.exe" if os.name == "nt" else "cancel-contracts")
            compiled = subprocess.run([SWIFTC, "-swift-version", "6", "-strict-concurrency=complete",
                "-parse-as-library", str(fixture), "-o", str(executable)], capture_output=True, text=True)
            self.assertEqual(0, compiled.returncode, compiled.stdout + compiled.stderr)
            result = subprocess.run([str(executable)], capture_output=True, text=True)
            self.assertEqual(0, result.returncode, result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
