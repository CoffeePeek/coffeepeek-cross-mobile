"""Non-UI contracts for the actual Swift photo loading model, with fake frame ports."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SWIFTC = os.environ.get('SWIFTC') or shutil.which('swiftc')

STUBS = '''
import Foundation
typealias UIImage = String
protocol ObservableObject: AnyObject {}
@propertyWrapper struct Published<Value> { var wrappedValue: Value }
struct CoffeePeekPhotoSource {
    typealias Load = (Int, Int, @escaping (UIImage?) -> Void) -> (() -> Void)
    let loadPreview: Load
    let loadOriginal: Load
}
'''

CONTRACTS = '''
@MainActor private final class Fixture {
    let model = CoffeePeekPhotoPageImages()
    var previews: [(UIImage?) -> Void] = []
    var originals: [(UIImage?) -> Void] = []
    var previewCancellations = 0
    var originalCancellations = 0
    var source: CoffeePeekPhotoSource {
        CoffeePeekPhotoSource(loadPreview: { [unowned self] _, _, receive in
            previews.append(receive)
            return { [unowned self] in previewCancellations += 1 }
        }, loadOriginal: { [unowned self] _, _, receive in
            originals.append(receive)
            return { [unowned self] in originalCancellations += 1 }
        })
    }
    func start() {
        model.loadPreview(source, page: 0, selected: 0)
        model.loadOriginal(source, page: 0, selected: 0, request: true)
    }
}

@main private struct Contracts {
    @MainActor static func main() {
        let pending = Fixture()
        pending.start()
        pending.previews[0]("preview")
        precondition(pending.model.preview == "preview" && pending.model.isLoading)

        pending.originals[0](nil)
        precondition(pending.model.preview == "preview" && !pending.model.isLoading)

        let upgraded = Fixture()
        upgraded.start()
        upgraded.previews[0]("preview")
        upgraded.originals[0]("original")
        precondition(upgraded.model.original == "original" && upgraded.model.preview == "preview")
        upgraded.originals[0](nil)
        precondition((upgraded.model.original ?? upgraded.model.preview) == "preview")

        upgraded.previews[0](nil)
        precondition(upgraded.model.original == nil && upgraded.model.preview == nil)

        let cancelled = Fixture()
        cancelled.start()
        cancelled.model.cancel()
        cancelled.previews[0]("late preview")
        cancelled.originals[0]("late original")
        precondition(cancelled.model.preview == nil && cancelled.model.original == nil && !cancelled.model.isLoading)
        precondition(cancelled.previewCancellations == 1 && cancelled.originalCancellations == 1)

        let retained = Fixture()
        retained.start()
        retained.previews[0]("ready")
        retained.model.loadPreview(retained.source, page: 0, selected: 0, retry: true)
        precondition(retained.model.preview == "ready" && retained.previews.count == 1 && retained.previewCancellations == 0)

        let retry = Fixture()
        retry.start()
        retry.previews[0](nil)
        retry.model.loadPreview(retry.source, page: 0, selected: 0, retry: true)
        retry.previews[1]("replacement")
        retry.previews[0](nil)
        precondition(retry.model.preview == "replacement")

        let adjacent = Fixture()
        adjacent.start()
        adjacent.previews[0]("preview")
        adjacent.model.loadOriginal(adjacent.source, page: 0, selected: 1, request: false)
        adjacent.originals[0]("late original")
        precondition(adjacent.model.preview == "preview" && adjacent.model.original == nil)

        let duplicate = Fixture()
        duplicate.start()
        duplicate.model.loadOriginal(duplicate.source, page: 0, selected: 0, request: true)
        precondition(duplicate.originals.count == 1)

        let previewFailure = Fixture()
        previewFailure.start()
        previewFailure.previews[0](nil)
        precondition(previewFailure.model.isLoading)
        previewFailure.originals[0]("original")
        precondition(previewFailure.model.original == "original" && !previewFailure.model.isLoading)

        for fixture in [pending, upgraded, cancelled, retained, retry, adjacent, duplicate, previewFailure] { fixture.model.cancel() }
        print("11 photo loading contracts passed")
    }
}
'''


@unittest.skipUnless(SWIFTC, 'Swift compiler is needed for non-UI model contracts')
class PhotoViewerStateTest(unittest.TestCase):
    def test_preview_upgrade_failure_cancellation_and_revocation(self):
        source = (ROOT / 'iosXtool/CoffeePeek/Sources/CoffeePeek/CoffeePeekNativePhotoViewer.swift').read_text()
        start = source.index('@MainActor\nprivate final class CoffeePeekPhotoPageImages')
        end = source.index('private struct CoffeePeekPhotoViewerStrings', start)
        with tempfile.TemporaryDirectory(prefix='coffeepeek-photo-model-') as directory:
            swift = Path(directory) / 'PhotoModelContracts.swift'
            executable = Path(directory) / ('contracts.exe' if os.name == 'nt' else 'contracts')
            swift.write_text(STUBS + source[start:end] + CONTRACTS)
            compile_result = subprocess.run([SWIFTC, '-parse-as-library', '-swift-version', '6',
                '-strict-concurrency=complete', str(swift), '-o', str(executable)], capture_output=True, text=True)
            self.assertEqual(compile_result.returncode, 0, compile_result.stdout + compile_result.stderr)
            result = subprocess.run([str(executable)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertIn('11 photo loading contracts passed', result.stdout)


if __name__ == '__main__':
    unittest.main()
