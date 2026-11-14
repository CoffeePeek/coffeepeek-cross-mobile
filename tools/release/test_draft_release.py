import subprocess
import unittest
from unittest.mock import patch

import draft_release as drafts


REPOSITORY = "CoffeePeek/coffeepeek-cross-mobile"
SERVER = "https://github.com"
RUN_ID = "12345"
COMMIT = "a" * 40
OLDER_COMMIT = "b" * 40


def release(tag, published_at="2026-11-06T10:00:00Z", **overrides):
    return {
        "tag_name": tag, "published_at": published_at, "draft": False, "prerelease": False,
        **overrides,
    }


class BuildSelectionTest(unittest.TestCase):
    def setUp(self):
        self.run = {
            "path": ".github/workflows/android-apk.yml@main",
            "head_sha": COMMIT,
            "head_branch": "main",
            "head_repository": {"full_name": REPOSITORY},
            "status": "completed",
            "conclusion": "failure",  # A delivery job failed after a successful build.
            "event": "push",
        }
        self.jobs = [{"jobs": [{"name": "build_apk", "conclusion": "success"}]}]

    def test_accepts_id_or_run_url(self):
        for value in (RUN_ID, f"{SERVER}/{REPOSITORY}/actions/runs/{RUN_ID}/attempts/2"):
            self.assertEqual(RUN_ID, drafts.build_run_id(value, REPOSITORY, SERVER))

    def test_rejects_url_from_another_repository(self):
        with self.assertRaises(ValueError):
            drafts.build_run_id(f"{SERVER}/elsewhere/repo/actions/runs/{RUN_ID}", REPOSITORY, SERVER)

    def test_uses_build_job_status_across_attempts(self):
        with patch.object(drafts, "api", side_effect=[self.run, self.jobs]) as api, \
                patch.object(drafts, "ancestor", return_value=True):
            self.assertEqual(COMMIT, drafts.build_commit(REPOSITORY, "android", RUN_ID))
        api.assert_called_with(REPOSITORY, f"actions/runs/{RUN_ID}/jobs?filter=all&per_page=100", paginated=True)

    def test_rejects_wrong_platform_or_non_main_run(self):
        with patch.object(drafts, "api", return_value=self.run):
            with self.assertRaisesRegex(ValueError, "ios-testflight.yml"):
                drafts.build_commit(REPOSITORY, "ios", RUN_ID)
            self.run["head_branch"] = "develop"
            with self.assertRaisesRegex(ValueError, "main branch"):
                drafts.build_commit(REPOSITORY, "android", RUN_ID)

    def test_rejects_unsuccessful_ios_upload(self):
        self.run["path"] = ".github/workflows/ios-testflight.yml"
        self.jobs[0]["jobs"] = [{"name": "upload", "conclusion": "failure"}]
        with patch.object(drafts, "api", side_effect=[self.run, self.jobs]), \
                patch.object(drafts, "ancestor", return_value=True):
            with self.assertRaisesRegex(ValueError, "no successful upload job"):
                drafts.build_commit(REPOSITORY, "ios", RUN_ID)

    def test_uses_versions_at_the_build_commit(self):
        config = "MARKETING_VERSION = 1.2.0\nBUILD_NUMBER_OFFSET = 1000\n"
        android = 'val appVersionName: Provider<String> = appVersionCode.map { "1.2.1.$it" }'
        with patch.object(drafts, "git", side_effect=[config, "1847", android]) as git:
            self.assertEqual(("1.2.1.2847", 2847), drafts.build_version("android", COMMIT))
        git.assert_any_call("show", f"{COMMIT}:composeApp/build.gradle.kts")
        git.assert_any_call("rev-list", "--count", COMMIT)
        with patch.object(drafts, "git", side_effect=[config, "1847"]):
            self.assertEqual(("1.2.0", 2847), drafts.build_version("ios", COMMIT))


class DraftReleaseTest(unittest.TestCase):
    def setUp(self):
        patch.object(drafts, "build_commit", return_value=COMMIT).start()
        patch.object(drafts, "build_version", return_value=("1.2.0", 2847)).start()
        self.api = patch.object(drafts, "api").start()
        self.tag = patch.object(drafts, "tag_commit", return_value=None).start()
        self.addCleanup(patch.stopall)

    def test_creates_draft_from_previous_published_ancestor_of_same_platform(self):
        previous = "ios-v1.1.11"
        self.api.side_effect = [[[
            release("android-v1.2.0.2847"),
            release("ios-v1.1.12", "2026-11-11T10:00:00Z", draft=True),
            release("ios-v1.1.13", "2026-11-12T10:00:00Z", prerelease=True),
        ], [release(previous), release("ios-v1.2.1", "2026-11-13T10:00:00Z")]],
            {"body": "## What's Changed\n* Fix by @dev in #12"},
            {"draft": True},
        ]
        self.tag.side_effect = [None, "c" * 40, OLDER_COMMIT]
        with patch.object(drafts, "ancestor", side_effect=[False, True]):
            _, created = drafts.draft_release(REPOSITORY, SERVER, "ios", RUN_ID)
        self.assertTrue(created)
        self.api.assert_any_call(REPOSITORY, "releases/generate-notes", data={
            "tag_name": "ios-v1.2.0", "target_commitish": COMMIT, "previous_tag_name": previous,
        })
        payload = self.api.call_args.kwargs["data"]
        self.assertEqual("releases", self.api.call_args.args[1])
        self.assertTrue(payload["draft"])
        self.assertEqual(COMMIT, payload["target_commitish"])
        self.assertIn("#12", payload["body"])
        self.assertIn(f"/actions/runs/{RUN_ID}", payload["body"])

    def test_first_release_does_not_compare_to_other_platform(self):
        self.api.side_effect = [[[release("android-v1.1.11.2800")]], {"draft": True}]
        drafts.draft_release(REPOSITORY, SERVER, "ios", RUN_ID)
        self.assertEqual(2, self.api.call_count)
        self.assertIn("Первый релиз платформы", self.api.call_args.kwargs["data"]["body"])

    def test_repeat_returns_existing_draft_without_overwriting_notes(self):
        existing = release("ios-v1.2.0", draft=True, target_commitish=COMMIT, body="Edited notes")
        self.api.return_value = [[existing]]
        self.assertEqual((existing, False), drafts.draft_release(REPOSITORY, SERVER, "ios", RUN_ID))
        self.api.assert_called_once_with(REPOSITORY, "releases?per_page=100", paginated=True)

    def test_rejects_existing_tag_or_draft_for_different_commit(self):
        self.tag.return_value = OLDER_COMMIT
        with self.assertRaisesRegex(ValueError, "Tag .* different commit"):
            drafts.draft_release(REPOSITORY, SERVER, "ios", RUN_ID)
        self.api.assert_not_called()
        self.tag.return_value = None
        self.api.return_value = [[release("ios-v1.2.0", draft=True, target_commitish=OLDER_COMMIT)]]
        with self.assertRaisesRegex(ValueError, "Release .* different commit"):
            drafts.draft_release(REPOSITORY, SERVER, "ios", RUN_ID)

    def test_failed_notes_generation_does_not_create_draft(self):
        self.api.side_effect = [[[release("ios-v1.1.11")]], subprocess.CalledProcessError(1, "gh")]
        self.tag.side_effect = [None, OLDER_COMMIT]
        with patch.object(drafts, "ancestor", return_value=True), self.assertRaises(subprocess.CalledProcessError):
            drafts.draft_release(REPOSITORY, SERVER, "ios", RUN_ID)
        self.assertEqual("releases/generate-notes", self.api.call_args.args[1])


if __name__ == "__main__":
    unittest.main()
