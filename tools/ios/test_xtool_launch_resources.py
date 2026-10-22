import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASH = shutil.which("bash") if os.name != "nt" else r"C:\Program Files\Git\bin\bash.exe"


@unittest.skipUnless(BASH and Path(BASH).is_file(), "Bash is required for the launch resource script")
class XtoolLaunchResourcesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.launcher = self.root / "iosXtool/CoffeePeek"
        self.launcher.mkdir(parents=True)
        self.script = self.launcher / "prepare-launch-resources.sh"
        source = (ROOT / "iosXtool/CoffeePeek/prepare-launch-resources.sh").read_text(encoding="utf-8")
        self.script.write_text(source, encoding="utf-8", newline="\n")
        self.resources = self.launcher / ".build/launch-resources"
        self.resources.mkdir(parents=True)
        self.env = dict(os.environ)

    def run_script(self, *args):
        return subprocess.run([BASH, str(self.script), *map(str, args)], env=self.env,
                              capture_output=True, text=True)

    def mock_compiler(self, source):
        binaries = self.root / "bin"
        binaries.mkdir()
        compiler = binaries / "xcrun"
        compiler.write_text("#!/bin/bash\nset -eu\n" + source, encoding="utf-8", newline="\n")
        compiler.chmod(0o755)
        self.env["PATH"] = str(binaries) + os.pathsep + self.env["PATH"]

    def test_imports_compiled_catalog_and_removes_obsolete_loose_images(self):
        catalog = self.root / "downloaded.car"
        catalog.write_bytes(b"compiled-light-dark-image-and-color-fixture")
        for name in ("LaunchLogo.png", "LaunchLogo-dark.png"):
            (self.resources / name).write_bytes(b"obsolete")
        self.assertEqual(self.run_script(catalog).returncode, 0)
        self.assertEqual((self.resources / "Assets.car").read_bytes(), catalog.read_bytes())
        self.assertEqual([p.name for p in self.resources.iterdir()], ["Assets.car"])
        self.assertEqual(self.run_script(self.resources / "Assets.car").returncode, 0)

    def test_rejects_missing_and_empty_download_without_reusing_stale_catalog(self):
        (self.resources / "Assets.car").write_bytes(b"old")
        catalog = self.root / "missing.car"
        self.assertNotEqual(self.run_script(catalog).returncode, 0)
        catalog.touch()
        self.assertNotEqual(self.run_script(catalog).returncode, 0)

    def test_compiles_original_catalog_for_iphone_with_both_appearances(self):
        self.mock_compiler('[[ "$1 $2 $3 $4" == "--sdk iphoneos actool --compile" ]]\n'
                           '[[ "$6 $7 $8 $9 ${10} ${11}" == "--platform iphoneos --minimum-deployment-target 18.0 --target-device iphone" ]]\n'
                           '[[ "${12}" == */iosApp/iosApp/Assets.xcassets ]]\n'
                           'printf compiled > "$5/Assets.car"\n')
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.resources / "Assets.car").read_bytes(), b"compiled")

    def test_compiler_failure_is_not_hidden_by_an_older_catalog(self):
        (self.resources / "Assets.car").write_bytes(b"old")
        self.mock_compiler("exit 23\n")
        self.assertEqual(self.run_script().returncode, 23)

    def test_missing_compiler_output_fails_preparation(self):
        (self.resources / "Assets.car").write_bytes(b"old")
        self.mock_compiler("exit 0\n")
        self.assertNotEqual(self.run_script().returncode, 0)
