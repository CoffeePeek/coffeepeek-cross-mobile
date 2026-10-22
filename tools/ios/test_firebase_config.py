import plistlib
import unittest
from pathlib import Path
from restore_firebase_config import validate

class FirebaseConfigTest(unittest.TestCase):
    def test_requires_matching_project_and_bundle(self):
        config = dict(PROJECT_ID='coffeepeek', BUNDLE_ID='com.coffeepeek', GOOGLE_APP_ID='test', API_KEY='test')
        self.assertEqual(validate(plistlib.dumps(config).decode()), config)
        for field in ('PROJECT_ID', 'BUNDLE_ID', 'GOOGLE_APP_ID', 'API_KEY'):
            invalid = {**config, field: ''}
            with self.assertRaises(ValueError):
                validate(plistlib.dumps(invalid).decode())

    def test_rejects_unsigned_verification_fixture_for_testflight(self):
        raw = Path(__file__).with_name('GoogleService-Info.verification.plist').read_text(encoding='utf-8')
        with self.assertRaisesRegex(ValueError, 'must not be used for TestFlight'):
            validate(raw)
