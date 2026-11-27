import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


BASH = shutil.which("bash") if os.name != "nt" else r"C:\Program Files\Git\bin\bash.exe"
SCRIPT = Path(__file__).with_name("run_android_checks.sh").resolve()


@unittest.skipUnless(BASH and Path(BASH).is_file(), "Bash is required")
class AndroidChecksTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.arguments = self.root / "arguments.txt"
        wrapper = self.root / "gradlew"
        wrapper.write_text(
            '#!/usr/bin/env bash\n'
            'printf "%s\\n" "$@" > "$CHECK_ARGS"\n'
            'case "${FAKE_MODE:-success}" in\n'
            '  failure) exit 42 ;;\n'
            '  delay) echo "Waiting in fake Gradle"; exec sleep 8 ;;\n'
            'esac\n',
            encoding="utf-8",
        )
        wrapper.chmod(0o755)

    def run_checks(self, phase, mode="success", limit="5s"):
        env = {**os.environ, "LC_ALL": "C", "CHECK_ARGS": self.arguments.as_posix(),
               "FAKE_MODE": mode, "ANDROID_CHECK_TIMEOUT": limit}
        return subprocess.run(
            [BASH, SCRIPT.as_posix(), phase], cwd=self.root, env=env,
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=40,
        )

    def test_unit_runs_all_modules_and_both_application_flavors(self):
        result = self.run_checks("unit")
        self.assertEqual(0, result.returncode, result.stderr)
        arguments = self.arguments.read_text().splitlines()
        for task in ("testDebugUnitTest", ":composeApp:testDirectDebugUnitTest",
                     ":composeApp:testPlayDebugUnitTest"):
            self.assertIn(task, arguments)
        self.assertIn("--no-configuration-cache", arguments)
        self.assertIn("--max-workers=6", arguments)

    def test_lint_is_separate_and_preserves_failure_status(self):
        result = self.run_checks("lint", mode="failure")
        self.assertEqual(42, result.returncode, result.stderr)
        arguments = self.arguments.read_text().splitlines()
        for task in ("lintDebug", ":composeApp:lintDirectDebug", ":composeApp:lintPlayDebug"):
            self.assertIn(task, arguments)
        self.assertNotIn("testDebugUnitTest", arguments)

    def test_stalled_command_is_failed_instead_of_treated_as_success(self):
        result = self.run_checks("unit", mode="delay", limit="3s")
        self.assertEqual(124, result.returncode, result.stderr)
        self.assertIn("Waiting in fake Gradle", result.stdout)
        self.assertIn("QUIT", result.stderr)

    def test_invalid_phase_cannot_start_gradle(self):
        result = self.run_checks("unknown")
        self.assertEqual(64, result.returncode, result.stderr)
        self.assertFalse(self.arguments.exists())


if __name__ == "__main__":
    unittest.main()
