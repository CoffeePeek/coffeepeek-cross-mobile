"""Non-UI contracts: launchers register native factories and Xcode compiles their sources."""

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


def compiled_swift_names():
    project = (ROOT / "iosApp/iosApp.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
    builds = dict(re.findall(r"(\w{24}) /\* ([^*]+\.swift) in Sources \*/ = \{isa = PBXBuildFile;", project))
    phase = re.search(r"/\* Begin PBXSourcesBuildPhase section \*/(.*?)/\* End PBXSourcesBuildPhase section \*/", project, re.S)[1]
    identifiers = re.findall(r"(\w{24}) /\* [^*]+ in Sources \*/", phase)
    return {Path(builds[key]).name for key in identifiers if key in builds}


class NativeControllerRegistrationTest(unittest.TestCase):
    def test_registered_factory_implementations_are_compiled_by_xcode(self):
        names = set()
        for source in (*APPS, *SHARED_CONFIGURATION.values()):
            names.update(re.findall(r"Ios\w+Controllers\.shared\.configure\(factory:\s*(\w+)\)",
                                    (ROOT / source).read_text(encoding="utf-8")))
        self.assertTrue(names)
        compiled = compiled_swift_names()
        found = set()
        for path in (ROOT / "iosXtool/CoffeePeek/Sources/CoffeePeek").glob("*.swift"):
            definitions = set(re.findall(r"\bfunc (\w+)\(", path.read_text(encoding="utf-8")))
            for name in names & definitions:
                with self.subTest(factory=name):
                    self.assertIn(path.name, compiled, "Native factory source is missing from Xcode Compile Sources")
                found.add(name)
        self.assertEqual(names, found)

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
