import json
import plistlib
import re
import struct
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "iosApp/iosApp/Assets.xcassets"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class AppPresentationConfigTest(unittest.TestCase):
    def test_application_minimum_versions_match_the_supported_devices(self):
        config = (ROOT / "build-logic/utilities/src/main/kotlin/com/coffeepeek/config/Config.kt").read_text()
        self.assertEqual(re.search(r"const val MIN_SDK = (\d+)", config).group(1), "31")
        xcode = (ROOT / "iosApp/Configuration/Shared.xcconfig").read_text()
        self.assertEqual(re.search(r"IPHONEOS_DEPLOYMENT_TARGET = ([\d.]+)", xcode).group(1), "18.0")
        package = (ROOT / "iosXtool/CoffeePeek/Package.swift").read_text()
        self.assertEqual(re.search(r'\.iOS\("([\d.]+)"\)', package).group(1), "18.0")
        info = plistlib.loads((ROOT / "iosXtool/CoffeePeek/Info.plist").read_bytes())
        self.assertEqual(info["MinimumOSVersion"], "18.0")

    def test_both_ios_launchers_support_only_upright_portrait(self):
        for path in ("iosApp/iosApp/Info.plist", "iosXtool/CoffeePeek/Info.plist"):
            with self.subTest(launcher=path):
                info = plistlib.loads((ROOT / path).read_bytes())
                self.assertEqual(info["UISupportedInterfaceOrientations"], ["UIInterfaceOrientationPortrait"])
                self.assertEqual(info["UILaunchScreen"]["UIImageName"], "LaunchLogo")
                self.assertEqual(info["UILaunchScreen"]["UIColorName"], "LaunchBackground")
                self.assertTrue(info["UILaunchScreen"]["UIImageRespectsSafeAreaInsets"])

    def test_android_live_and_sandbox_launchers_are_portrait(self):
        for manifest, activity in (("androidMain", ".admin.MainActivity"),
                                   ("sandbox", "com.coffeepeek.admin.sandbox.SandboxActivity")):
            with self.subTest(manifest=manifest):
                root = ET.parse(ROOT / f"composeApp/src/{manifest}/AndroidManifest.xml")
                launcher = next(item for item in root.findall("application/activity")
                                if item.get(ANDROID + "name") == activity)
                self.assertEqual(launcher.get(ANDROID + "screenOrientation"), "portrait")

        root = ET.parse(ROOT / "composeApp/src/androidMain/AndroidManifest.xml")
        opt_out = next(item for item in root.findall("application/property")
                       if item.get(ANDROID + "name") == "android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY")
        self.assertEqual(opt_out.get(ANDROID + "value"), "true")

    def test_launch_artwork_is_complete_for_light_and_dark(self):
        catalog = ASSETS / "LaunchLogo.imageset"
        images = json.loads((catalog / "Contents.json").read_text())["images"]
        self.assertEqual(len(images), 2)
        for image, resource in zip(images, ("drawable", "drawable-night")):
            with self.subTest(appearance=resource):
                png = (catalog / image["filename"]).read_bytes()
                self.assertEqual(png, (ROOT / f"composeApp/src/main/res/{resource}/ic_splash_logo.png").read_bytes())
                self.assertEqual(struct.unpack("!II", png[16:24]), (288, 288))
                self.assertEqual(image["scale"], "2x")
        self.assertEqual(images[1]["appearances"], [{"appearance": "luminosity", "value": "dark"}])
        colors = json.loads((ASSETS / "LaunchBackground.colorset/Contents.json").read_text())["colors"]
        self.assertEqual(len(colors), 2)
        self.assertEqual(colors[1]["appearances"], images[1]["appearances"])
