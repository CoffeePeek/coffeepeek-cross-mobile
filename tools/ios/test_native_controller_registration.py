"""Non-UI contract: both launchers install every native application factory."""

import re
import unittest
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
APPS = ("iosApp/iosApp/CoffeePeekApp.swift", "iosXtool/CoffeePeek/Sources/CoffeePeek/CoffeePeekApp.swift")
REGISTRATION = re.compile(r"(MainViewControllerKt\.configureNative\w+|Ios\w+Controllers\.shared\.configure)\s*[({]")
SHARED_CONFIGURATION = {
    "configureCoffeePeekNativeGlass": "iosXtool/CoffeePeek/Sources/CoffeePeek/CoffeePeekNativeGlass.swift",
    "configureCoffeePeekNativeNavigation": "iosXtool/CoffeePeek/Sources/CoffeePeek/NativeRootController.swift",
}


def registrations(path):
    source = (ROOT / path).read_text(encoding="utf-8")
    result = Counter(REGISTRATION.findall(source))
    for configure, implementation in SHARED_CONFIGURATION.items():
        calls = len(re.findall(r"\b" + re.escape(configure) + r"\(\)", source))
        shared = Counter(REGISTRATION.findall((ROOT / implementation).read_text(encoding="utf-8")))
        result.update({name: count * calls for name, count in shared.items()})
    return result


def required_registrations():
    boundary = ROOT / "composeApp/src/iosMain/kotlin/com/coffeepeek/admin"
    main = (boundary / "MainViewController.kt").read_text(encoding="utf-8")
    forwarders = set(re.findall(r"^fun (configureNative\w+)\(", main, re.M))
    expected = {f"MainViewControllerKt.{name}" for name in forwarders}
    for path in boundary.rglob("*.kt"):
        objects = re.findall(r"^object (Ios\w+Controllers)\s*\{", path.read_text(encoding="utf-8"), re.M)
        expected.update(f"{name}.shared.configure" for name in objects)
    return expected


class NativeControllerRegistrationTest(unittest.TestCase):
    def test_both_launchers_register_the_same_native_factories(self):
        self.assertEqual(registrations(APPS[0]), registrations(APPS[1]))

    def test_public_native_factories_are_installed_exactly_once(self):
        expected = required_registrations()
        self.assertTrue(expected)
        for app in APPS:
            with self.subTest(app=app):
                actual = registrations(app)
                self.assertFalse(expected - actual.keys(), f"Missing native factories in {app}")
                self.assertTrue(all(count == 1 for count in actual.values()))


if __name__ == "__main__":
    unittest.main()
