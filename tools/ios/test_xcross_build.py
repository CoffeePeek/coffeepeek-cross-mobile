import plistlib
from pathlib import Path
import struct
import tempfile
import unittest

from xcross_build import expand_settings, finalize_bundle, flatten_config, scannerless_linkage_package, validate_bundle


class XcrossBuildTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def write(self, relative, text):
        file = self.root / relative
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8")
        return file

    def test_nested_optional_include_and_local_url_override(self):
        self.write("Shared.xcconfig", 'API_BASE_URL = https:/$()/example.invalid\n#include? "Local.xcconfig"\n')
        debug = self.write("Debug.xcconfig", '#include "Shared.xcconfig"\nCOFFEEPEEK_DEBUG = YES\n')
        self.assertEqual(expand_settings(flatten_config(debug))["API_BASE_URL"], "https://example.invalid")
        self.write("Local.xcconfig", "API_BASE_URL = https:/$()/local.invalid\n")
        values = expand_settings(flatten_config(debug))
        self.assertEqual(values["API_BASE_URL"], "https://local.invalid")
        self.assertEqual(values["COFFEEPEEK_DEBUG"], "YES")

    def test_required_missing_include_and_cycles_fail(self):
        debug = self.write("Debug.xcconfig", '#include "Missing.xcconfig"\n')
        with self.assertRaises(FileNotFoundError):
            flatten_config(debug)
        self.write("Missing.xcconfig", '#include "Debug.xcconfig"\n')
        with self.assertRaisesRegex(ValueError, "Recursive"):
            flatten_config(debug)

    def test_restores_kotlin_manifest_when_xcross_fails(self):
        path = self.root / "iosApp/KotlinMultiplatformLinkedPackage/Package.swift"
        path.parent.mkdir(parents=True)
        repository_manifest = Path(__file__).resolve().parents[2] / path.relative_to(self.root)
        original = repository_manifest.read_bytes()
        path.write_bytes(original)
        with self.assertRaisesRegex(RuntimeError, "build failed"):
            with scannerless_linkage_package(self.root):
                filtered = path.read_bytes()
                self.assertNotIn(b"_feature_shelf_data", filtered)
                self.assertIn(b"com_posthog_posthog_kmp_0_5_1", filtered)
                raise RuntimeError("build failed")
        self.assertEqual(path.read_bytes(), original)

    def make_bundle(self):
        app = self.root / "CoffeePeek.app"
        (app / "Frameworks/XcrossHostPackages.framework").mkdir(parents=True)
        (app / "Frameworks/XcrossHostPackages.framework/XcrossHostPackages").touch()
        (app / "Frameworks/MapLibre.framework").mkdir()
        (app / "Frameworks/MapLibre.framework/MapLibre").touch()
        (app / "compose-resources").mkdir()
        (app / "Assets.car").touch()
        (app / "Runner").write_bytes(struct.pack("<II", 0xFEEDFACF, 0x0100000C))
        info = dict(CFBundleExecutable="Runner", CFBundleVersion="1234", CoffeePeekDebug="YES",
                    APIBaseURL="https://example.invalid", UILaunchScreen={})
        (app / "Info.plist").write_bytes(plistlib.dumps(info))
        return app, info

    def test_finalization_keeps_launch_resources_and_development_entitlements(self):
        app, _ = self.make_bundle()
        source = self.root / "iosApp/iosApp"
        source.mkdir(parents=True)
        launch = dict(UIColorName="LaunchBackground", UIImageName="LaunchLogo")
        (source / "Info.plist").write_bytes(plistlib.dumps(dict(UILaunchScreen=launch)))
        (source / "CoffeePeek.entitlements").write_bytes(plistlib.dumps({"aps-environment": "$(APS_ENVIRONMENT)"}))
        (source / "PrivacyInfo.xcprivacy").write_bytes(plistlib.dumps({}))
        self.write("iosApp/iosApp/be.lproj/InfoPlist.strings", 'NSCameraUsageDescription = "Camera";')
        settings = dict(APS_ENVIRONMENT="development", CURRENT_PROJECT_VERSION="1234", API_BASE_URL="https://example.invalid")
        finalize_bundle(self.root, app, settings)
        info = plistlib.loads((app / "Info.plist").read_bytes())
        self.assertEqual(info["UILaunchScreen"], launch)
        self.assertEqual(info["XCrossEntitlements"]["aps-environment"], "development")
        self.assertTrue((app / "PrivacyInfo.xcprivacy").is_file())
        self.assertTrue((app / "be.lproj/InfoPlist.strings").is_file())

    def test_bundle_rejects_wrong_architecture_and_onnx(self):
        app, info = self.make_bundle()
        (app / "Frameworks/onnxruntime.framework").mkdir()
        with self.assertRaisesRegex(ValueError, "ONNX"):
            validate_bundle(app, info)
        (app / "Runner").write_bytes(struct.pack("<II", 0xFEEDFACF, 0x01000007))
        with self.assertRaisesRegex(ValueError, "ARM64"):
            validate_bundle(app, info)


if __name__ == "__main__":
    unittest.main()
