import plistlib
import struct
import tempfile
from pathlib import Path
import unittest
from verify_signed_app import read_signed_entitlements, verify_keychain_access, verify_signed_app


def signed_fixture(entitlements):
    payload = plistlib.dumps(entitlements)
    entitlement = struct.pack('>II', 0xFADE7171, len(payload) + 8) + payload
    signature = struct.pack('>IIIII', 0xFADE0CC0, len(entitlement) + 20, 1, 5, 20) + entitlement
    header = struct.pack('<8I', 0xFEEDFACF, 0x0100000C, 0, 2, 1, 16, 0, 0)
    command = struct.pack('<4I', 0x1D, 16, 48, len(signature))
    return header + command + signature


class SignedAppEntitlementTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.app = Path(self.folder.name)
        self.entitlements = {'application-identifier': 'SEED.com.example.test',
                             'keychain-access-groups': ['SEED.com.example.test', 'com.apple.token']}

    def write_app(self, apple_enabled=False):
        info = dict(CFBundleExecutable='Runner', CFBundleIdentifier='com.example.test',
                    CoffeePeekAppleSignInEnabled=apple_enabled)
        (self.app / 'Info.plist').write_bytes(plistlib.dumps(info))
        (self.app / 'Runner').write_bytes(signed_fixture(self.entitlements))

    def test_reads_actual_signature_and_accepts_concrete_default_group(self):
        self.write_app()
        self.assertEqual(verify_signed_app(self.app), self.entitlements)

    def test_rejects_profile_wildcard_as_default_group(self):
        self.entitlements['keychain-access-groups'][0] = 'SEED.*'
        self.write_app()
        with self.assertRaisesRegex(ValueError, 'Default Keychain'):
            verify_signed_app(self.app)

    def test_rejects_wildcard_in_additional_group(self):
        self.entitlements['keychain-access-groups'].append('SEED.*')
        with self.assertRaisesRegex(ValueError, 'wildcard'):
            verify_keychain_access(self.entitlements, 'com.example.test')

    def test_rejects_another_application_identifier(self):
        with self.assertRaisesRegex(ValueError, 'application identifier'):
            verify_keychain_access(self.entitlements, 'com.example.other')

    def test_rejects_apple_button_without_signed_capability(self):
        self.write_app(apple_enabled=True)
        with self.assertRaisesRegex(ValueError, 'Apple sign-in'):
            verify_signed_app(self.app)

    def test_accepts_signed_apple_capability(self):
        self.entitlements['com.apple.developer.applesignin'] = ['Default']
        self.write_app(apple_enabled=True)
        self.assertEqual(verify_signed_app(self.app), self.entitlements)

    def test_rejects_unsigned_and_truncated_executables(self):
        executable = self.app / 'Runner'
        for data in (b'', b'not-a-mach-o', struct.pack('<8I', 0xFEEDFACF, 0, 0, 0, 1, 16, 0, 0)):
            executable.write_bytes(data)
            with self.assertRaises(ValueError):
                read_signed_entitlements(executable)
