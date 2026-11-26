import unittest

from xcross_run import check_installed_version


class DeviceVersionTest(unittest.TestCase):
    def test_newer_marketing_or_build_version_is_preserved(self):
        settings = dict(MARKETING_VERSION="1.2.0", CURRENT_PROJECT_VERSION="2949")
        for version, build in (("1.2.1", "2949"), ("1.2.0", "3050")):
            with self.subTest(version=version, build=build), self.assertRaisesRegex(ValueError, "downgrade"):
                check_installed_version(dict(CFBundleShortVersionString=version, CFBundleVersion=build), settings)

    def test_equal_or_older_version_can_be_updated_without_uninstall(self):
        settings = dict(MARKETING_VERSION="1.2.0", CURRENT_PROJECT_VERSION="2949")
        for version, build in (("1.2", "2949"), ("1.1.9", "2948")):
            check_installed_version(dict(CFBundleShortVersionString=version, CFBundleVersion=build), settings)

    def test_unknown_version_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "safely compare"):
            check_installed_version(dict(CFBundleShortVersionString="unknown", CFBundleVersion="2949"),
                                    dict(MARKETING_VERSION="1.2.0", CURRENT_PROJECT_VERSION="2949"))


if __name__ == "__main__":
    unittest.main()
