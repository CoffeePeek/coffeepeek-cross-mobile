"""Observe the exact uploaded build without changing TestFlight groups or app state."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


class RetryableResponse(Exception):
    pass


def authorization_token():
    script = Path(__file__).with_name("app_store_token.rb")
    result = subprocess.run(["ruby", str(script)], capture_output=True, text=True, check=False)
    if result.returncode:
        raise RuntimeError("Unable to authorize App Store Connect processing check")
    return result.stdout.strip()


class AppStoreConnect:
    def __init__(self, token=authorization_token, request=urlopen):
        self.token = token
        self.request = request

    def get(self, path, query):
        url = "https://api.appstoreconnect.apple.com/v1/" + path + "?" + urlencode(query)
        request = Request(url, headers={"Authorization": "Bearer " + self.token()})
        try:
            with self.request(request, timeout=30) as response:
                return json.load(response)
        except HTTPError as failure:
            if failure.code == 429 or failure.code >= 500:
                raise RetryableResponse("Apple API HTTP " + str(failure.code)) from None
            raise RuntimeError("Apple processing check HTTP " + str(failure.code)) from None
        except (URLError, TimeoutError):
            raise RetryableResponse("Apple API connection unavailable") from None


def find_app(client):
    response = client.get("apps", {"filter[bundleId]": "com.coffeepeek", "fields[apps]": "bundleId", "limit": "1"})
    apps = [app for app in response["data"] if app["attributes"]["bundleId"] == "com.coffeepeek"]
    if len(apps) != 1:
        raise RuntimeError("com.coffeepeek is absent from the configured App Store Connect team")
    return apps[0]["id"]


def find_build(client, app, build, version):
    response = client.get("builds", {"filter[app]": app, "filter[version]": build, "include": "preReleaseVersion",
        "fields[builds]": "version,processingState,expired,preReleaseVersion", "fields[preReleaseVersions]": "version,platform", "limit": "20"})
    versions = {item["id"]: item["attributes"] for item in response.get("included", []) if item["type"] == "preReleaseVersions"}
    for item in response["data"]:
        release = versions.get(item["relationships"]["preReleaseVersion"]["data"]["id"])
        if item["attributes"]["version"] == build and release and release.get("version") == version and release.get("platform") == "IOS":
            return item
    return None


def wait_for_build(client, app, build, version, timeout=1500, now=time.monotonic, sleep=time.sleep, report=print):
    deadline = now() + timeout
    while now() < deadline:
        try:
            item = find_build(client, app, build, version)
            status = item["attributes"]["processingState"] if item else "AWAITING_UPLOAD"
            report("TestFlight build " + version + " (" + build + "): " + status)
            if status in ("FAILED", "INVALID"):
                raise RuntimeError("Apple rejected uploaded build: " + status)
            if status == "VALID":
                if item["attributes"]["expired"]:
                    raise RuntimeError("Uploaded build is expired")
                return item
        except RetryableResponse as failure:
            report(str(failure))
        sleep(min(30, max(0, deadline - now())))
    raise TimeoutError("Apple processing has not completed within 25 minutes; the upload is retained")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build", required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    client = AppStoreConnect()
    app = find_app(client)
    result = wait_for_build(client, app, args.build, args.version)
    url = "https://appstoreconnect.apple.com/apps/" + app + "/testflight/ios/" + result["id"]
    print("Apple processing completed: " + url)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
            summary.write("\n### Apple processing completed\n\n- Build: " + args.version + " (" + args.build + ")\n- State: VALID\n- [TestFlight build](" + url + ")\n")


if __name__ == "__main__":
    main()
