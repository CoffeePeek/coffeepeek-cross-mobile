from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]


class ReleaseVersioningTest(unittest.TestCase):
    def test_android_formats_version_with_shared_build_number_offset(self):
        config = (ROOT / 'iosApp/Configuration/Shared.xcconfig').read_text(encoding='utf-8')
        version = re.search(r'^MARKETING_VERSION = (.+)$', config, re.MULTILINE).group(1)
        self.assertRegex(version, r'^[0-9]+\.[0-9]+\.[0-9]+$')
        offset = int(re.search(r'^BUILD_NUMBER_OFFSET = ([0-9]+)$', config, re.MULTILINE).group(1))
        self.assertGreaterEqual(offset, 1000)
        gradle = (ROOT / 'composeApp/build.gradle.kts').read_text(encoding='utf-8')
        self.assertIn('file("iosApp/Configuration/Shared.xcconfig")', gradle)
        self.assertIn('val appVersionName: Provider<String> = appVersionCode.map { "1.1.1.$it" }', gradle)
        self.assertIn('gitCommitCount.zip(appBuildNumberOffset) { count, offset -> count + offset }', gradle)
        self.assertNotIn('"--all"', gradle)
        self.assertIn('commandLine("git", "rev-list", "--count", "HEAD")', gradle)

    def test_release_workflows_only_publish_main_and_never_raise_server_policy(self):
        android = (ROOT / '.github/workflows/android-apk.yml').read_text(encoding='utf-8')
        ios = (ROOT / '.github/workflows/ios-testflight.yml').read_text(encoding='utf-8')
        self.assertNotIn('develop', android)
        for workflow in (android, ios):
            self.assertIn("if: github.ref == 'refs/heads/main'", workflow)
            self.assertNotIn('/android/version', workflow)
            self.assertNotIn('/ios/version', workflow)
            self.assertNotIn('publish_android_release.py --publish', workflow)
        self.assertIn('Publish to Play internal testing', android)
        self.assertIn('--request POST', android)  # APK registration remains a draft.

    def test_ios_build_is_an_integer_commit_count_and_archive_checks_both_versions(self):
        workflow = (ROOT / '.github/workflows/ios-testflight.yml').read_text(encoding='utf-8')
        self.assertIn('COMMIT_COUNT=$(git rev-list --count HEAD)', workflow)
        self.assertIn('IOS_BUILD_NUMBER=$((BUILD_NUMBER_OFFSET + COMMIT_COUNT))', workflow)
        self.assertIn("os.environ['IOS_BUILD_NUMBER']", workflow)
        self.assertNotIn('GITHUB_RUN_NUMBER', workflow)
        self.assertIn("Print :CFBundleVersion", workflow)
        self.assertIn("Print :CFBundleShortVersionString", workflow)


if __name__ == '__main__':
    unittest.main()
