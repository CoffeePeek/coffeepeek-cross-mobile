import json
import os
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import urlsplit


BUILD_WORKFLOWS = {
    "android": ("android-apk.yml", "build_apk"),
    "ios": ("ios-testflight.yml", "upload"),
}
PLATFORM_NAMES = {"android": "Android", "ios": "iOS"}


def api(repository, endpoint, *, data=None, paginated=False):
    args = ["gh", "api", f"repos/{repository}/{endpoint}"]
    if paginated:
        args.extend(["--paginate", "--slurp"])
    if data is not None:
        args.extend(["--method", "POST", "--input", "-"])
    try:
        result = subprocess.run(
            args, input=json.dumps(data), check=True, capture_output=True, encoding="utf-8"
        )
    except subprocess.CalledProcessError as error:
        method = "POST" if data is not None else "GET"
        error.stderr = f"GitHub API {method} /repos/{repository}/{endpoint} failed:\n{error.stderr or ''}"
        raise
    return json.loads(result.stdout)


def git(*args):
    return subprocess.check_output(["git", *args], encoding="utf-8").strip()


def ancestor(commit, head):
    result = subprocess.run(["git", "merge-base", "--is-ancestor", commit, head])
    if result.returncode not in (0, 1):
        result.check_returncode()
    return result.returncode == 0


def build_run_id(value, repository, server):
    value = value.strip()
    if re.fullmatch(r"[1-9][0-9]*", value):
        return value
    url = urlsplit(value)
    origin = urlsplit(server)
    path = re.fullmatch(
        rf"/{re.escape(repository)}/actions/runs/([1-9][0-9]*)(?:/attempts/[1-9][0-9]*)?/?",
        url.path, re.IGNORECASE,
    )
    if (url.scheme, url.netloc) != (origin.scheme, origin.netloc) or not path:
        raise ValueError("Enter the run ID or the Actions run URL from this repository.")
    return path[1]


def build_commit(repository, platform, run_id):
    workflow, job = BUILD_WORKFLOWS[platform]
    run = api(repository, f"actions/runs/{run_id}")
    if (run["path"].split("@", 1)[0] != f".github/workflows/{workflow}"
            or run["head_branch"] != "main" or run["status"] != "completed"
            or run["event"] not in ("push", "workflow_dispatch")
            or (run["head_repository"] or {}).get("full_name", "").casefold() != repository.casefold()):
        raise ValueError(f"Choose a completed {workflow} run from this repository's main branch.")
    commit = run["head_sha"]
    if not re.fullmatch(r"[0-9a-f]{40}", commit) or not ancestor(commit, "HEAD"):
        raise ValueError("The build commit must be in the history of main.")
    # A rerun of failed delivery jobs may leave the successful build in an earlier attempt.
    pages = api(repository, f"actions/runs/{run_id}/jobs?filter=all&per_page=100", paginated=True)
    if not any(item["name"] == job and item["conclusion"] == "success"
               for page in pages for item in page["jobs"]):
        raise ValueError(f"The selected run has no successful {job} job.")
    return commit


def build_version(platform, commit):
    # Read the selected revision's settings, including the Android version literal.
    config = git("show", f"{commit}:iosApp/Configuration/Shared.xcconfig")
    offset = re.search(r"^BUILD_NUMBER_OFFSET = ([0-9]+)$", config, re.MULTILINE)
    marketing_version = re.search(r"^MARKETING_VERSION = ([0-9]+\.[0-9]+\.[0-9]+)$", config, re.MULTILINE)
    if offset is None or marketing_version is None:
        raise ValueError("The selected commit uses an unsupported build version format.")
    number = int(git("rev-list", "--count", commit)) + int(offset[1])
    if platform == "ios":
        return marketing_version[1], number
    return android_version(commit, marketing_version[1], number), number


def android_version(commit, marketing_version, number):
    gradle = git("show", f"{commit}:composeApp/build.gradle.kts")
    literal = re.search(
        r'val appVersionName: Provider<String> = appVersionCode.map \{ "([0-9]+\.[0-9]+\.[0-9]+)\.\$it" \}',
        gradle,
    )
    if literal:
        return f"{literal[1]}.{number}"
    # Early release runs used the marketing version as the entire versionName.
    if re.search(r"val appVersionName: Provider<String> = appVersionConfig.map \{ config ->\s+Regex\(.*MARKETING_VERSION", gradle):
        return marketing_version
    raise ValueError("The selected commit uses an unsupported Android versionName format.")


def tag_commit(tag):
    if git("tag", "--list", tag):
        return git("rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}")
    return None


def previous_release(releases, platform, commit):
    candidates = [release for release in releases
                  if release["tag_name"].startswith(f"{platform}-v")
                  and not release["draft"] and not release["prerelease"] and release["published_at"]]
    for release in sorted(candidates, key=lambda item: item["published_at"], reverse=True):
        tag = release["tag_name"]
        base = tag_commit(tag)
        if base is None:
            raise ValueError(f"Published release {tag} is missing its Git tag.")
        if base != commit and ancestor(base, commit):
            return tag
    return None


def verify_release_token(commit):
    if os.environ.get("RELEASE_USES_GITHUB_TOKEN") != "true":
        return
    if git("diff", "--name-only", commit, "HEAD", "--", ".github/workflows"):
        raise ValueError(
            "This build has different workflow files from main. GITHUB_TOKEN cannot create its release. "
            "Set MOBILE_RELEASE_TOKEN with Actions: read, Contents: write and Workflows: write, "
            "or create the draft using an authorized local gh login and rerun this workflow. "
            "The selected build commit will not be changed."
        )


def draft_release(repository, server, platform, run_id):
    commit = build_commit(repository, platform, run_id)
    version, number = build_version(platform, commit)
    tag = f"{platform}-v{version}"
    pinned = tag_commit(tag)
    if pinned is not None and pinned != commit:
        raise ValueError(f"Tag {tag} already points to a different commit.")
    pages = api(repository, "releases?per_page=100", paginated=True)
    releases = [release for page in pages for release in page]
    existing = next((release for release in releases if release["tag_name"] == tag), None)
    if existing is not None:
        if (pinned or existing["target_commitish"]) != commit:
            raise ValueError(f"Release {tag} already targets a different commit.")
        return existing, False
    verify_release_token(commit)
    previous = previous_release(releases, platform, commit)
    body = release_body(repository, server, run_id, number, tag, commit, previous)
    return api(repository, "releases", data={
        "tag_name": tag, "target_commitish": commit, "draft": True,
        "name": f"{PLATFORM_NAMES[platform]} {version}", "body": body,
    }), True


def release_body(repository, server, run_id, number, tag, commit, previous):
    url = f"{server}/{repository}"
    header = (
        "## Что нового\n\n<!-- Добавьте описание для пользователей перед публикацией. -->\n\n"
        f"Сборка [{number}]({url}/actions/runs/{run_id}) · Коммит [`{commit[:12]}`]({url}/commit/{commit})\n\n"
    )
    if previous is None:
        return header + "Первый релиз платформы в GitHub. Следующий релиз будет сравниваться с этим тегом.\n"
    notes = api(repository, "releases/generate-notes", data={
        "tag_name": tag, "target_commitish": commit, "previous_tag_name": previous,
    })
    return header + notes["body"]


def main(platform, build_run):
    repository = os.environ["GITHUB_REPOSITORY"]
    server = os.environ["GITHUB_SERVER_URL"]
    run_id = build_run_id(build_run, repository, server)
    release, created = draft_release(repository, server, platform, run_id)
    status = "Draft created" if created else "Draft already exists"
    if not release["draft"]:
        status = "Release already published"
    summary = f'{status} — [{release["tag_name"]}]({release["html_url"]})'
    print(summary)
    if summary_path := os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(summary_path).open("a", encoding="utf-8") as output:
            output.write(summary + "\n")


if __name__ == "__main__":
    try:
        main(*sys.argv[1:])
    except (ValueError, subprocess.CalledProcessError) as error:
        sys.exit(getattr(error, "stderr", None) or str(error))
