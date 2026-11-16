"""Build an unsigned CoffeePeek Debug app with the local xcross compatibility build."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import plistlib
import re
import shutil
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
INCLUDE = re.compile(r'^#include(\?)?\s+"([^"]+)"\s*$')
SETTING = re.compile(r"\$\(([^)]*)\)|\$\{([^}]+)\}")


def flatten_config(path, chain=()):
    path = path.resolve()
    if path in chain:
        raise ValueError(f"Recursive xcconfig include: {path.name}")
    result = []
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        include = INCLUDE.fullmatch(line.strip())
        if include:
            child = path.parent / include[2]
            if child.is_file() or not include[1]:
                result.extend(flatten_config(child, (*chain, path)))
        elif line.strip() and not line.lstrip().startswith("//"):
            result.append(line)
    return result


def expand_settings(lines):
    values = {}
    for line in lines:
        key, separator, value = line.partition("=")
        if separator and "[" not in key:
            values[key.strip()] = value.strip()
    for _ in range(16):
        expanded = {key: SETTING.sub(lambda m: values.get(m[1] or m[2], ""), value)
                    for key, value in values.items()}
        if expanded == values:
            return values
        values = expanded
    raise ValueError("Cyclic xcconfig settings")


def prepare_config(root, overrides=()):
    lines = flatten_config(root / "iosApp/Configuration/Debug.xcconfig")
    settings = expand_settings(lines)
    commits = subprocess.check_output(["git", "rev-list", "--count", "HEAD"], cwd=root, text=True).strip()
    version = int(settings["BUILD_NUMBER_OFFSET"]) + int(commits)
    lines += ["COFFEEPEEK_DEBUG = YES", "APS_ENVIRONMENT = development", "DEVELOPMENT_LANGUAGE = ru",
              "EXECUTABLE_NAME = Runner", "PRODUCT_BUNDLE_PACKAGE_TYPE = APPL",
              "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon", f"CURRENT_PROJECT_VERSION = {version}"]
    lines.extend(overrides)
    (root / "iosApp/Configuration/Config.xcconfig").write_text("\n".join(lines) + "\n", encoding="utf-8")
    return expand_settings(lines)


def expand_plist(value, settings):
    if isinstance(value, dict):
        return {key: expand_plist(item, settings) for key, item in value.items()}
    if isinstance(value, list):
        return [expand_plist(item, settings) for item in value]
    if isinstance(value, str):
        return SETTING.sub(lambda match: settings[match[1] or match[2]], value)
    return value


def finalize_bundle(root, app, settings):
    info_path = app / "Info.plist"
    info = plistlib.loads(info_path.read_bytes())
    source = root / "iosApp/iosApp"
    original = plistlib.loads((source / "Info.plist").read_bytes())
    info["UILaunchScreen"] = original["UILaunchScreen"]
    info["XCrossEntitlements"] = expand_plist(plistlib.loads((source / "CoffeePeek.entitlements").read_bytes()), settings)
    if info.get("CoffeePeekDebug") != "YES" or info.get("CFBundleVersion") != settings["CURRENT_PROJECT_VERSION"]:
        raise ValueError("Built app does not match the requested Debug configuration")
    if info.get("APIBaseURL") != settings["API_BASE_URL"]:
        raise ValueError("Built app API configuration does not match the selected xcconfig")
    copy_native_resources(source, app)
    info_path.write_bytes(plistlib.dumps(info, fmt=plistlib.FMT_XML, sort_keys=False))
    validate_bundle(app, info)


def copy_native_resources(source, app):
    for name in ("GoogleService-Info.plist", "PrivacyInfo.xcprivacy"):
        resource = source / name
        if resource.is_file():
            shutil.copy2(resource, app / name)
    for localized in source.glob("*.lproj"):
        shutil.copytree(localized, app / localized.name, dirs_exist_ok=True)


def validate_bundle(app, info):
    binary = app / info["CFBundleExecutable"]
    with binary.open("rb") as stream:
        magic, cpu = struct.unpack("<II", stream.read(8))
    if (magic, cpu) != (0xFEEDFACF, 0x0100000C):
        raise ValueError("App executable must be an ARM64 Mach-O binary")
    for required in ("Assets.car", "compose-resources", "Frameworks/XcrossHostPackages.framework/XcrossHostPackages",
                     "Frameworks/MapLibre.framework/MapLibre"):
        if not (app / required).exists():
            raise ValueError(f"Missing app resource: {required}")
    if any("onnxruntime" in file.name.lower() for file in (app / "Frameworks").rglob("*")):
        raise ValueError("Scanner-disabled build unexpectedly contains ONNX Runtime")


def build_environment(config):
    environment = dict(os.environ, COFFEEPEEK_XCROSS="1", ORG_GRADLE_PROJECT_coffeepeekXcross="true")
    if config:
        environment["XCROSS_CONFIG"] = str(config.resolve(strict=True))
    return environment


def record_build(root, app, settings):
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    status = subprocess.check_output(["git", "status", "--porcelain"], cwd=root, text=True)
    with (app / "Runner").open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    identity = dict(revision=revision, dirty=bool(status), configuration="Debug", scanner=False,
                    bundle_id=settings["PRODUCT_BUNDLE_IDENTIFIER"], app_name=settings["PRODUCT_NAME"],
                    version=settings["MARKETING_VERSION"], build=settings["CURRENT_PROJECT_VERSION"],
                    executable_sha256=digest, signed=False, device_verified=False)
    (app.parent / "build-identity.json").write_text(json.dumps(identity, indent=2) + "\n", encoding="utf-8")


def package_unsigned(app, settings):
    output = app.parent / f"{app.stem}-{settings['MARKETING_VERSION']}-{settings['CURRENT_PROJECT_VERSION']}-unsigned.ipa"
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for file in sorted(app.rglob("*")):
            if not file.is_file():
                continue
            data = file.read_bytes()
            entry = zipfile.ZipInfo("Payload/" + app.name + "/" + file.relative_to(app).as_posix())
            entry.create_system = 3
            executable = data[:4] in (b"\xcf\xfa\xed\xfe", b"\xca\xfe\xba\xbe", b"\xbe\xba\xfe\xca")
            entry.external_attr = (0o100755 if executable else 0o100644) << 16
            archive.writestr(entry, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=6)
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--xcross", default=os.environ.get("XCROSS_EXECUTABLE", "xcross"))
    parser.add_argument("--config", type=Path)
    args = parser.parse_args()
    settings = prepare_config(ROOT)
    environment = build_environment(args.config)
    command = [args.xcross, "compose", "build", "--configuration", "debug"]
    subprocess.run(command, cwd=ROOT, env=environment, check=True)
    app = ROOT / "build/xcross-ios" / (settings["PRODUCT_NAME"] + ".app")
    finalize_bundle(ROOT, app, settings)
    record_build(ROOT, app, settings)
    archive = package_unsigned(app, settings)
    print(f"Unsigned Debug app (scanner disabled): {app}")
    print(f"Unsigned IPA: {archive}")


if __name__ == "__main__":
    main()
