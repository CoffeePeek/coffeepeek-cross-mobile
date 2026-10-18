import contextlib
import copy
import io
import json
import unittest
from unittest import mock
import urllib.error

import publish_android_release as release


TOKEN = "secret-token-never-in-output-123456789"
ENV = {"ADMIN_API_URL": "https://api.coffeepeek.by", "ADMIN_TOKEN": TOKEN,
       "VERSION_NAME": "1.0.700", "VERSION_CODE": "700", "FILE_SIZE": "123456",
       "SHA256": "a" * 64, "FILE_URL": "https://coffeepeek.by/downloads/android/releases/1.0.700-700/coffeepeek.apk"}
EXPECTED = release.ExpectedRelease.from_env(ENV)


def record(active=False, code=700):
    return {"id": "f37f0935-af60-474d-85d0-26b24ac5741b", "version": "1.0.700", "versionCode": code,
            "fileUrl": EXPECTED.url, "fileName": "coffeepeek.apk", "fileSize": EXPECTED.size,
            "sha256": EXPECTED.sha256, "isActive": active}


def public_apk():
    value = record()
    for key in ("id", "fileUrl", "isActive"):
        value.pop(key)
    return {**value, "enabled": True, "available": True, "url": "/download/apk"}


class FakeClient:
    def __init__(self, records=None):
        self.records = records if records is not None else [record()]
        self.posts = []
        self.list_reads = 0
        self.public_reads = 0
        self.error = None
        self.result = None
        self.apk = public_apk()
        self.policy = {"versionCode": 700, "minVersionCode": 440, "url": EXPECTED.url}

    def releases(self):
        self.list_reads += 1
        return copy.deepcopy(self.records)

    def publish(self, identifier):
        self.posts.append(identifier)
        if self.error:
            raise self.error
        if self.result is not None:
            return self.result
        for item in self.records:
            item["isActive"] = item["id"] == identifier
        return copy.deepcopy(self.records[0])

    def public_downloads(self):
        self.public_reads += 1
        return {"android": {"apk": self.apk}}

    def public_apk_version(self):
        self.public_reads += 1
        return self.policy


class FakeResponse:
    status = 200

    def __init__(self, payload):
        self.body = payload if isinstance(payload, bytes) else json.dumps(payload).encode()
        self.read_limit = None

    def __enter__(self): return self
    def __exit__(self, *_): pass
    def read(self, size):
        self.read_limit = size
        return self.body[:size]


class FakeOpener:
    def __init__(self, response):
        self.response = response
        self.requests = []

    def open(self, request, timeout):
        self.requests.append((request, timeout))
        if isinstance(self.response, Exception):
            raise self.response
        return self.response


class PublishAndroidReleaseTests(unittest.TestCase):
    def assert_failure(self, code, client, publish=True):
        with self.assertRaisesRegex(release.ReleaseError, "^" + code + "$"):
            release.execute(client, EXPECTED, publish)

    def test_publish_once_and_read_back_without_recreating_release(self):
        client = FakeClient()
        result = release.execute(client, EXPECTED, publish=True)
        self.assertEqual(len(client.posts), 1)
        self.assertEqual(client.list_reads, 2)
        self.assertEqual(client.public_reads, 0)
        self.assertTrue(result["postPerformed"])
        self.assertFalse(result["publicVerified"])

    def test_publish_same_active_is_idempotent_without_write(self):
        client = FakeClient([record(True)])
        result = release.execute(client, EXPECTED, publish=True)
        self.assertEqual(client.posts, [])
        self.assertFalse(result["postPerformed"])

    def test_default_verify_only_reads_active_and_public_state(self):
        client = FakeClient([record(True)])
        result = release.execute(client, EXPECTED)
        self.assertEqual(client.posts, [])
        self.assertEqual(client.public_reads, 2)
        self.assertTrue(result["publicVerified"])

    def test_verify_cannot_activate_inactive_release(self):
        client = FakeClient()
        self.assert_failure("expected_release_not_active", client, publish=False)
        self.assertEqual(client.posts, [])

    def test_every_metadata_mismatch_prevents_post(self):
        for key, value in {"version": "different", "fileUrl": "https://other.invalid/file.apk",
                           "fileName": "other.apk", "fileSize": 42, "sha256": "b" * 64}.items():
            with self.subTest(key=key):
                target = record(); target[key] = value
                client = FakeClient([target])
                self.assert_failure("release_metadata_mismatch", client)
                self.assertEqual(client.posts, [])

    def test_duplicate_or_missing_version_code_prevents_post(self):
        for records in ([], [record(), record()]):
            client = FakeClient(records)
            self.assert_failure("expected_single_release", client)
            self.assertEqual(client.posts, [])

    def test_higher_active_prevents_rollback(self):
        client = FakeClient([record(), record(True, code=701)])
        self.assert_failure("newer_release_already_active", client)
        self.assertEqual(client.posts, [])

    def test_multiple_active_releases_are_rejected(self):
        client = FakeClient([record(True), record(True, code=699)])
        self.assert_failure("multiple_active_releases", client)
        self.assertEqual(client.posts, [])

    def test_publish_transport_failure_is_unknown_without_retry(self):
        client = FakeClient(); client.error = RuntimeError(TOKEN)
        self.assert_failure("publish_outcome_unknown_read_only_reconciliation_required", client)
        self.assertEqual(len(client.posts), 1)
        self.assertEqual(client.list_reads, 1)

    def test_malformed_or_mismatched_publish_response_never_retries(self):
        for response in ({}, record(False), {**record(True), "sha256": "b" * 64},
                         {**record(True), "id": "c39bcaef-9379-4009-981c-b8ad23c18210"}):
            with self.subTest(response=response):
                client = FakeClient(); client.result = response
                self.assert_failure("publish_outcome_unknown_read_only_reconciliation_required", client)
                self.assertEqual(len(client.posts), 1)

    def test_success_response_requires_committed_admin_readback(self):
        client = FakeClient(); client.result = record(True)
        self.assert_failure("activation_readback_mismatch", client)
        self.assertEqual(len(client.posts), 1)

    def test_stale_public_descriptor_fails_after_active_admin_read(self):
        client = FakeClient([record(True)]); client.apk["versionCode"] = 618
        self.assert_failure("release_metadata_mismatch", client, publish=False)
        self.assertEqual(client.posts, [])

    def test_public_version_policy_must_match_code_immutable_url_and_floor(self):
        for key, value in (("versionCode", 618), ("url", "https://coffeepeek.by/old.apk"),
                           ("minVersionCode", 439), ("minVersionCode", 701)):
            with self.subTest(key=key, value=value):
                client = FakeClient([record(True)]); client.policy[key] = value
                self.assert_failure("public_version_policy_mismatch", client, publish=False)

    def test_pascal_case_response_fields_and_uppercase_digest_are_supported(self):
        target = {key[0].upper() + key[1:]: value for key, value in record(True).items()}
        target["Sha256"] = EXPECTED.sha256.upper()
        client = FakeClient([target])
        self.assertTrue(release.execute(client, EXPECTED)["publicVerified"])

    def test_ambiguous_case_fields_are_rejected(self):
        client = FakeClient([{**record(), "VersionCode": 999}])
        self.assert_failure("ambiguous_response_field", client)
        self.assertEqual(client.posts, [])

    def test_expected_url_must_be_exact_version_and_code(self):
        for key, value in (("FILE_URL", "https://coffeepeek.by/downloads/android/main/coffeepeek.apk"),
                           ("VERSION_CODE", "0"), ("SHA256", "secret"), ("FILE_SIZE", "-1")):
            with self.subTest(key=key):
                with self.assertRaises(release.ReleaseError):
                    release.ExpectedRelease.from_env({**ENV, key: value})

    def test_http_reads_are_bounded_and_public_requests_have_no_authorization(self):
        response = FakeResponse({"isSuccess": True, "data": {}})
        opener = FakeOpener(response)
        client = release.ApiClient(ENV["ADMIN_API_URL"], TOKEN, opener)
        client.releases(); client.public_downloads(); client.public_apk_version()
        self.assertEqual(response.read_limit, 1024 * 1024 + 1)
        self.assertEqual(opener.requests[0][0].get_header("Authorization"), "Bearer " + TOKEN)
        self.assertTrue(all(r.get_header("Authorization") is None for r, _ in opener.requests[1:]))
        self.assertTrue(all(timeout == 30 for _, timeout in opener.requests))

    def test_http_publish_flow_uses_exact_id_one_post_and_committed_readback(self):
        responses = iter([FakeResponse({"isSuccess": True, "data": [record()]}),
                          FakeResponse({"isSuccess": True, "data": record(True)}),
                          FakeResponse({"isSuccess": True, "data": [record(True)]})])
        requests = []
        opener = mock.Mock()
        def open_response(request, timeout):
            requests.append(request)
            return next(responses)
        opener.open.side_effect = open_response
        client = release.ApiClient(ENV["ADMIN_API_URL"], TOKEN, opener)
        self.assertTrue(release.execute(client, EXPECTED, publish=True)["postPerformed"])
        self.assertEqual([request.method for request in requests], ["GET", "POST", "GET"])
        self.assertEqual(requests[1].full_url,
                         ENV["ADMIN_API_URL"] + "/api/admin/v1/app-downloads/android/releases/" + record()["id"] + "/publish")
        self.assertIsNone(requests[1].data)

    def test_http_publish_failure_is_not_retried_by_transport(self):
        opener = mock.Mock()
        opener.open.side_effect = [FakeResponse({"isSuccess": True, "data": [record()]}),
                                  urllib.error.URLError(TOKEN)]
        client = release.ApiClient(ENV["ADMIN_API_URL"], TOKEN, opener)
        self.assert_failure("publish_outcome_unknown_read_only_reconciliation_required", client)
        self.assertEqual(opener.open.call_count, 2)

    def test_oversized_invalid_and_failed_api_payloads_are_sanitized(self):
        for payload, code in ((b"x" * (1024 * 1024 + 1), "response_too_large"),
                              (TOKEN.encode(), "http_or_json_failure"),
                              ({"isSuccess": False, "message": TOKEN, "data": {}}, "api_response_not_success")):
            with self.subTest(code=code):
                client = release.ApiClient(ENV["ADMIN_API_URL"], TOKEN, FakeOpener(FakeResponse(payload)))
                with self.assertRaisesRegex(release.ReleaseError, "^" + code + "$"):
                    client.releases()

    def test_http_errors_do_not_expose_body_or_token(self):
        error = urllib.error.HTTPError("https://api.coffeepeek.by", 403, TOKEN, {}, io.BytesIO(TOKEN.encode()))
        client = release.ApiClient(ENV["ADMIN_API_URL"], TOKEN, FakeOpener(error))
        with self.assertRaisesRegex(release.ReleaseError, "^http_403$"):
            client.releases()

    def test_redirects_and_untrusted_origins_are_rejected(self):
        with self.assertRaisesRegex(release.ReleaseError, "^http_redirect_rejected$"):
            release.NoRedirect().redirect_request(None, None, 302, TOKEN, {}, "https://other.invalid")
        for origin in ("http://api.coffeepeek.by", "https://other.invalid", "https://user@api.coffeepeek.by", "https://api.coffeepeek.by?token=secret"):
            with self.subTest(origin=origin):
                with self.assertRaises(release.ReleaseError):
                    release.ApiClient(origin, TOKEN)

    def test_cli_failure_output_never_includes_secrets_or_exception_text(self):
        output = io.StringIO()
        with mock.patch.object(release, "ApiClient", side_effect=RuntimeError(TOKEN)), contextlib.redirect_stdout(output):
            self.assertEqual(release.main([], ENV), 1)
        self.assertNotIn(TOKEN, output.getvalue())
        self.assertEqual(json.loads(output.getvalue()), {"status": "failed", "code": "unexpected_failure"})


if __name__ == "__main__":
    unittest.main()
