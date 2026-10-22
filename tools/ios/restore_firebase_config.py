"""Restore the CI-only Firebase client resource without logging credentials."""
import os
import plistlib
from pathlib import Path

def validate(raw):
    config = plistlib.loads(raw.encode())
    if config.get('PROJECT_ID') != 'coffeepeek' or config.get('BUNDLE_ID') != 'com.coffeepeek':
        raise ValueError('Firebase plist must match coffeepeek / com.coffeepeek')
    if not config.get('GOOGLE_APP_ID') or not config.get('API_KEY'):
        raise ValueError('Firebase plist is missing app configuration')
    if config['API_KEY'] == 'ci-verification-only':
        raise ValueError('Synthetic Firebase configuration must not be used for TestFlight')
    return config

if __name__ == '__main__':
    raw = os.environ.get('FIREBASE_GOOGLE_SERVICE_INFO_PLIST', '')
    validate(raw)
    Path('iosApp/iosApp/GoogleService-Info.plist').write_text(raw, encoding='utf-8')
