import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap
import unittest


WORKFLOW = (Path(__file__).resolve().parents[2] / ".github/workflows/android-apk.yml").read_text(encoding="utf-8")
START = WORKFLOW.index("          import json\n", WORKFLOW.index("Restore Firebase Android configurations"))
SCRIPT = textwrap.dedent(WORKFLOW[START:WORKFLOW.index("          PY\n", START)])
RELEASE_SECRET = "FIREBASE_GOOGLE_SERVICES_JSON"
DEBUG_SECRET = "FIREBASE_GOOGLE_SERVICES_JSON_DEBUG"


def config(package, project="coffeepeek"):
    return {
        "project_info": {"project_id": project},
        "client": [{"client_info": {"android_client_info": {"package_name": package}}}],
    }


class AndroidFirebaseConfigTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.env = {
            **os.environ,
            RELEASE_SECRET: json.dumps(config("com.coffeepeek")),
            DEBUG_SECRET: json.dumps(config("com.coffeepeek.debug")),
        }

    def run_config(self):
        return subprocess.run(
            [sys.executable, "-c", SCRIPT], cwd=self.root, env=self.env,
            capture_output=True, text=True,
        )

    def test_writes_matching_clients_to_build_type_directories(self):
        result = self.run_config()
        self.assertEqual(0, result.returncode, result.stderr)
        for variant, secret in (("release", RELEASE_SECRET), ("debug", DEBUG_SECRET)):
            path = self.root / "composeApp" / "src" / variant / "google-services.json"
            self.assertEqual(json.loads(self.env[secret]), json.loads(path.read_text(encoding="utf-8")))
        self.assertFalse((self.root / "composeApp/google-services.json").exists())

    def test_missing_debug_secret_fails_before_writing_any_config(self):
        self.env.pop(DEBUG_SECRET)
        result = self.run_config()
        self.assertNotEqual(0, result.returncode)
        self.assertIn(DEBUG_SECRET, result.stderr)
        self.assertFalse((self.root / "composeApp").exists())

    def test_release_client_cannot_be_used_for_debug(self):
        self.env[DEBUG_SECRET] = self.env[RELEASE_SECRET]
        result = self.run_config()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Missing or ambiguous com.coffeepeek.debug Firebase client", result.stderr)
        self.assertFalse((self.root / "composeApp").exists())

    def test_rejects_wrong_firebase_project(self):
        self.env[RELEASE_SECRET] = json.dumps(config("com.coffeepeek", project="another-project"))
        result = self.run_config()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Incorrect Firebase project for release", result.stderr)

    def test_rejects_ambiguous_package_clients(self):
        ambiguous = config("com.coffeepeek.debug")
        ambiguous["client"] *= 2
        self.env[DEBUG_SECRET] = json.dumps(ambiguous)
        result = self.run_config()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Missing or ambiguous com.coffeepeek.debug Firebase client", result.stderr)


if __name__ == "__main__":
    unittest.main()
