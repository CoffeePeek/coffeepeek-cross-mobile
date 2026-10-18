"""Activate an already registered APK, or verify its public descriptor (default).

Inputs are ADMIN_API_URL, ADMIN_TOKEN, VERSION_NAME, VERSION_CODE, FILE_SIZE,
SHA256 and FILE_URL. --publish permits one activation POST; it never creates a
release or retries a write. A failed/unknown response requires a read-only
reconciliation before an operator decides whether to invoke it again.
"""

import argparse
from dataclasses import dataclass
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


class ReleaseError(Exception):
    """Contains a fixed diagnostic code, never an HTTP body or credential."""


def require(condition, code):
    if not condition:
        raise ReleaseError(code)


def field(value, name):
    require(isinstance(value, dict), "invalid_response_schema")
    pascal = name[0].upper() + name[1:]
    require(name in value or pascal in value, "missing_response_field")
    if name in value and pascal in value:
        require(value[name] == value[pascal], "ambiguous_response_field")
    return value[name] if name in value else value[pascal]


def positive_integer(value):
    return type(value) is int and value > 0


@dataclass(frozen=True)
class ExpectedRelease:
    version: str
    code: int
    size: int
    sha256: str
    url: str

    @classmethod
    def from_env(cls, env):
        version = env.get("VERSION_NAME", "")
        raw_code = env.get("VERSION_CODE", "")
        raw_size = env.get("FILE_SIZE", "")
        digest = env.get("SHA256", "").lower()
        require(re.fullmatch(r"[0-9A-Za-z][0-9A-Za-z._-]{0,79}", version), "invalid_expected_version")
        require(re.fullmatch(r"[0-9]{1,10}", raw_code), "invalid_expected_code")
        require(re.fullmatch(r"[0-9]{1,12}", raw_size), "invalid_expected_size")
        code, size = int(raw_code), int(raw_size)
        require(0 < code <= 2147483647 and 0 < size <= 512 * 1024 * 1024, "invalid_expected_bounds")
        require(re.fullmatch(r"[0-9a-f]{64}", digest), "invalid_expected_checksum")
        url = env.get("FILE_URL", "")
        require(url == f"https://coffeepeek.by/downloads/android/releases/{version}-{code}/coffeepeek.apk", "invalid_expected_url")
        return cls(version, code, size, digest, url)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ReleaseError("http_redirect_rejected")


class ApiClient:
    MAX_RESPONSE_BYTES = 1024 * 1024

    def __init__(self, base, token, opener=None):
        parsed = urllib.parse.urlsplit(base)
        require(parsed.scheme == "https" and parsed.hostname in {"api.coffeepeek.by", "coffeepeek.by"}
                and parsed.port in {None, 443} and not parsed.username and not parsed.password
                and parsed.path in {"", "/"} and not parsed.query and not parsed.fragment, "invalid_api_origin")
        require(len(token) >= 32 and not any(c in token for c in "\r\n"), "invalid_admin_token")
        self.base = base.rstrip("/")
        self.token = token
        self.opener = opener or urllib.request.build_opener(NoRedirect())

    def request(self, method, path, authenticated=True):
        headers = {"Accept": "application/json", "User-Agent": "CoffeePeek-Android-release/1"}
        if authenticated:
            headers["Authorization"] = "Bearer " + self.token
        request = urllib.request.Request(self.base + path, method=method, headers=headers)
        try:
            with self.opener.open(request, timeout=30) as response:
                require(response.status == 200, "unexpected_http_status")
                body = response.read(self.MAX_RESPONSE_BYTES + 1)
                require(len(body) <= self.MAX_RESPONSE_BYTES, "response_too_large")
                payload = json.loads(body)
        except ReleaseError:
            raise
        except urllib.error.HTTPError as error:
            raise ReleaseError(f"http_{error.code}") from None
        except Exception:
            raise ReleaseError("http_or_json_failure") from None
        require(field(payload, "isSuccess") is True, "api_response_not_success")
        return field(payload, "data")

    def releases(self):
        return self.request("GET", "/api/admin/v1/app-downloads/android/releases")

    def publish(self, identifier):
        return self.request("POST", f"/api/admin/v1/app-downloads/android/releases/{identifier}/publish")

    def public_downloads(self):
        return self.request("GET", "/api/v1/app-downloads", authenticated=False)

    def public_apk_version(self):
        return self.request("GET", "/api/v1/app-version?platform=android&channel=apk", authenticated=False)


def exact_metadata(release, expected):
    require(field(release, "version") == expected.version
            and type(field(release, "versionCode")) is int and field(release, "versionCode") == expected.code
            and field(release, "fileUrl") == expected.url
            and field(release, "fileName") == "coffeepeek.apk"
            and type(field(release, "fileSize")) is int and field(release, "fileSize") == expected.size
            and isinstance(field(release, "sha256"), str)
            and field(release, "sha256").lower() == expected.sha256, "release_metadata_mismatch")


def select_release(releases, expected):
    require(isinstance(releases, list) and len(releases) <= 10000, "invalid_release_list")
    for release in releases:
        require(positive_integer(field(release, "versionCode")) and type(field(release, "isActive")) is bool, "invalid_release_list")
    matching = [r for r in releases if field(r, "versionCode") == expected.code]
    require(len(matching) == 1, "expected_single_release")
    target = matching[0]
    exact_metadata(target, expected)
    try:
        identifier = str(uuid.UUID(field(target, "id")))
    except Exception:
        raise ReleaseError("invalid_release_id") from None
    require(not any(field(r, "isActive") and field(r, "versionCode") > expected.code for r in releases), "newer_release_already_active")
    require(sum(field(r, "isActive") for r in releases) <= 1, "multiple_active_releases")
    return target, identifier


def verify_public(client, expected):
    apk = field(field(client.public_downloads(), "android"), "apk")
    require(field(apk, "enabled") is True and field(apk, "available") is True
            and field(apk, "url") == "/download/apk", "public_apk_unavailable")
    # The public DTO calls the immutable file URL a separate version-policy field.
    exact_metadata({**apk, "fileUrl": expected.url}, expected)
    policy = client.public_apk_version()
    minimum = field(policy, "minVersionCode")
    require(type(field(policy, "versionCode")) is int and field(policy, "versionCode") == expected.code
            and field(policy, "url") == expected.url and type(minimum) is int
            and 440 <= minimum <= expected.code, "public_version_policy_mismatch")


def execute(client, expected, publish=False):
    target, identifier = select_release(client.releases(), expected)
    posted = False
    if publish and not field(target, "isActive"):
        # Never retry a write, including malformed success and transport errors.
        try:
            result = client.publish(identifier)
            exact_metadata(result, expected)
            require(str(uuid.UUID(field(result, "id"))) == identifier
                    and field(result, "isActive") is True, "invalid_publish_response")
        except Exception:
            raise ReleaseError("publish_outcome_unknown_read_only_reconciliation_required") from None
        posted = True
        target, after_id = select_release(client.releases(), expected)
        require(after_id == identifier and field(target, "isActive") is True, "activation_readback_mismatch")
    require(field(target, "isActive") is True, "expected_release_not_active")
    if not publish:
        verify_public(client, expected)
    return {"status": "passed", "mode": "publish" if publish else "verify", "versionName": expected.version,
            "versionCode": expected.code, "postPerformed": posted, "publicVerified": not publish}


def main(argv=None, env=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--publish", action="store_true", help="Allow one activation POST after exact metadata checks")
    args = parser.parse_args(argv)
    environment = os.environ if env is None else env
    try:
        expected = ExpectedRelease.from_env(environment)
        client = ApiClient(environment.get("ADMIN_API_URL", ""), environment.get("ADMIN_TOKEN", ""))
        print(json.dumps(execute(client, expected, args.publish), sort_keys=True))
        return 0
    except ReleaseError as error:
        print(json.dumps({"status": "failed", "code": str(error)}, sort_keys=True))
        return 1
    except Exception:
        print(json.dumps({"status": "failed", "code": "unexpected_failure"}, sort_keys=True))
        return 1


if __name__ == "__main__":
    sys.exit(main())
