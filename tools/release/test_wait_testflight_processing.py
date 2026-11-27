import io
import json
import unittest
from urllib.error import HTTPError
from wait_testflight_processing import AppStoreConnect, RetryableResponse, find_build, wait_for_build


def response(state="VALID", version="1.2.2", platform="IOS", expired=False):
    return {"data": [{"id": "build", "attributes": {"version": "123", "processingState": state, "expired": expired},
        "relationships": {"preReleaseVersion": {"data": {"id": "release"}}}}],
        "included": [{"id": "release", "type": "preReleaseVersions", "attributes": {"version": version, "platform": platform}}]}


class FakeClient:
    def __init__(self, *responses):
        self.responses = iter(responses)
    def get(self, path, query):
        result = next(self.responses)
        if isinstance(result, Exception):
            raise result
        return result


class ProcessingTests(unittest.TestCase):
    def test_checks_marketing_version_and_platform(self):
        for value in (response(version="1.2.1"), response(platform="MAC_OS")):
            self.assertIsNone(find_build(FakeClient(value), "app", "123", "1.2.2"))

    def test_waits_for_upload_and_processing_before_accepting_exact_build(self):
        clock = [0]
        client = FakeClient({"data": []}, response("PROCESSING"), response())
        result = wait_for_build(client, "app", "123", "1.2.2", now=lambda: clock[0],
            sleep=lambda seconds: clock.__setitem__(0, clock[0] + seconds), report=lambda _: None)
        self.assertEqual("build", result["id"])
        self.assertEqual(60, clock[0])

    def test_rejection_and_expiry_fail_without_reporting_ready(self):
        for value in (response("FAILED"), response("INVALID"), response(expired=True)):
            with self.assertRaises(RuntimeError):
                wait_for_build(FakeClient(value), "app", "123", "1.2.2", report=lambda _: None)

    def test_retry_is_bounded_and_does_not_repeat_upload(self):
        clock = [0]
        with self.assertRaises(TimeoutError):
            wait_for_build(FakeClient(RetryableResponse("retry"), response("PROCESSING")), "app", "123", "1.2.2",
                timeout=31, now=lambda: clock[0], sleep=lambda seconds: clock.__setitem__(0, clock[0] + seconds), report=lambda _: None)
        self.assertEqual(31, clock[0])

    def test_api_failures_never_include_response_body_or_authorization(self):
        for status in (400, 401, 403, 429, 500):
            def reject(request, timeout):
                raise HTTPError(request.full_url, status, "private body", {}, io.BytesIO(b"private body"))
            client = AppStoreConnect(token=lambda: "private-token", request=reject)
            with self.assertRaises((RuntimeError, RetryableResponse)) as caught:
                client.get("builds", {})
            self.assertNotIn("private", str(caught.exception))

    def test_get_only_request_refreshes_authorization_per_attempt(self):
        tokens = iter(["first", "second"])
        seen = []
        def accept(request, timeout):
            seen.append((request.get_method(), request.get_header("Authorization")))
            return io.BytesIO(json.dumps({"data": []}).encode())
        client = AppStoreConnect(token=lambda: next(tokens), request=accept)
        client.get("apps", {})
        client.get("builds", {})
        self.assertEqual([("GET", "Bearer first"), ("GET", "Bearer second")], seen)


if __name__ == "__main__":
    unittest.main()
