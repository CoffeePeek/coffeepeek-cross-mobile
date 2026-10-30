import os
import plistlib
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from fix_onnx_framework import binary_minimum, fix_framework


class OnnxFrameworkTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.framework = Path(self.temp.name) / "onnxruntime.framework"
        self.framework.mkdir()
        self.plist = self.framework / "Info.plist"
        self.info = {"CFBundleExecutable": "onnxruntime", "MinimumOSVersion": "15.1",
                     "CFBundleIdentifier": "com.microsoft.onnxruntime"}
        self.write_info()
        self.output = patch("fix_onnx_framework.subprocess.check_output", return_value=self.build_info())
        self.output.start()
        self.addCleanup(self.output.stop)
        self.environment = patch.dict(os.environ, {"CODE_SIGNING_ALLOWED": "NO"})
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def write_info(self, fmt=plistlib.FMT_XML):
        self.plist.write_bytes(plistlib.dumps(self.info, fmt=fmt))

    def build_info(self, minimum="18.0"):
        return f"Load command 8\n      cmd LC_BUILD_VERSION\n platform IOS\n    minos {minimum}\n      sdk 27.1\n"

    def test_xcode_stub_regression_repairs_only_minimum_in_xml_and_binary_plists(self):
        for fmt in (plistlib.FMT_XML, plistlib.FMT_BINARY):
            with self.subTest(format=fmt):
                self.write_info(fmt)
                fix_framework(self.framework, "18.0")
                self.assertEqual(plistlib.loads(self.plist.read_bytes()),
                                 {**self.info, "MinimumOSVersion": "18.0"})
                self.assertEqual(self.plist.read_bytes().startswith(b"bplist00"), fmt == plistlib.FMT_BINARY)
                fix_framework(self.framework, "18.0", check=True)

    def test_missing_or_empty_minimum_is_repaired(self):
        for value in (None, ""):
            with self.subTest(value=value):
                self.info["MinimumOSVersion"] = value
                if value is None:
                    del self.info["MinimumOSVersion"]
                self.write_info()
                fix_framework(self.framework, "18.0")
                self.assertEqual(plistlib.loads(self.plist.read_bytes())["MinimumOSVersion"], "18.0")

    def test_check_rejects_original_upload_without_mutating_it(self):
        original = self.plist.read_bytes()
        with self.assertRaisesRegex(ValueError, "15.1.*18.0"):
            fix_framework(self.framework, "18.0", check=True)
        self.assertEqual(self.plist.read_bytes(), original)

    def test_never_understates_binary_requirement_to_fit_app(self):
        original = self.plist.read_bytes()
        with self.assertRaisesRegex(ValueError, "above app minimum"):
            fix_framework(self.framework, "17.0")
        self.assertEqual(self.plist.read_bytes(), original)

    def test_unsigned_build_never_calls_codesign(self):
        with patch("fix_onnx_framework.subprocess.run") as run:
            fix_framework(self.framework, "18.0")
        run.assert_not_called()

    def test_signed_build_resigns_after_plist_update_then_verifies(self):
        def codesign(command, check):
            self.assertTrue(check)
            self.assertEqual(plistlib.loads(self.plist.read_bytes())["MinimumOSVersion"], "18.0")

        with patch.dict(os.environ, {"CODE_SIGNING_ALLOWED": "YES", "EXPANDED_CODE_SIGN_IDENTITY": "test"}):
            with patch("fix_onnx_framework.subprocess.run", side_effect=codesign) as run:
                fix_framework(self.framework, "18.0")
                self.assertEqual(run.call_count, 2)
                self.assertEqual(run.call_args_list[0].args[0][:4], ["codesign", "--force", "--sign", "test"])
                self.assertEqual(run.call_args_list[1].args[0][:3], ["codesign", "--verify", "--strict"])
                fix_framework(self.framework, "18.0")
                self.assertEqual(run.call_count, 2)

    def test_missing_signing_identity_fails_before_mutation(self):
        original = self.plist.read_bytes()
        with patch.dict(os.environ, {"CODE_SIGNING_ALLOWED": "YES", "EXPANDED_CODE_SIGN_IDENTITY": ""}):
            with self.assertRaisesRegex(ValueError, "IDENTITY is required"):
                fix_framework(self.framework, "18.0")
        self.assertEqual(self.plist.read_bytes(), original)

    def test_signing_or_signature_verification_failure_stops_build(self):
        for failed_call in (1, 2):
            with self.subTest(failed_call=failed_call):
                self.write_info()
                results = [None] * (failed_call - 1) + [subprocess.CalledProcessError(1, "codesign")]
                with patch.dict(os.environ, {"CODE_SIGNING_ALLOWED": "YES", "EXPANDED_CODE_SIGN_IDENTITY": "test"}):
                    with patch("fix_onnx_framework.subprocess.run", side_effect=results):
                        with self.assertRaises(subprocess.CalledProcessError):
                            fix_framework(self.framework, "18.0")

    def test_reads_all_slices_and_ignores_sdk_version(self):
        with patch("fix_onnx_framework.subprocess.check_output", return_value=self.build_info() * 2):
            self.assertEqual(binary_minimum(self.framework / "onnxruntime"), "18.0")

    def test_missing_or_conflicting_macho_versions_fail_closed(self):
        for output in ("", self.build_info() + self.build_info("19.0")):
            with self.subTest(output=output):
                with patch("fix_onnx_framework.subprocess.check_output", return_value=output):
                    with self.assertRaisesRegex(ValueError, "Expected one Mach-O minimum"):
                        fix_framework(self.framework, "18.0")


if __name__ == "__main__":
    unittest.main()
