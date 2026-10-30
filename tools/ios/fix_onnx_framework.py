"""Align ONNX's embedded resource framework with Xcode's generated Mach-O stub.

Xcode 27 replaces the static ONNX archive with a dylib stub targeting the app's
minimum OS, but copies the vendor's older MinimumOSVersion unchanged (ITMS-90208).
Run after embedding and before the app is signed; re-sign the modified framework.
"""

import argparse
import os
import plistlib
import re
import subprocess
from pathlib import Path


def version(value):
    if not re.fullmatch(r"\d+\.\d+(?:\.\d+)?", value):
        raise ValueError(f"Invalid iOS version: {value!r}")
    parts = tuple(map(int, value.split(".")))
    return parts + (0,) * (3 - len(parts))


def binary_minimum(binary):
    output = subprocess.check_output(
        ["xcrun", "vtool", "-show-build", str(binary)], text=True
    )
    minimums = set(re.findall(r"^\s*minos\s+(\d+\.\d+(?:\.\d+)?)\s*$", output, re.MULTILINE))
    if len(minimums) != 1:
        raise ValueError(f"Expected one Mach-O minimum OS in {binary}, found {minimums}")
    return minimums.pop()


def signing_identity():
    if os.environ.get("CODE_SIGNING_ALLOWED") == "NO":
        return None
    identity = os.environ.get("EXPANDED_CODE_SIGN_IDENTITY")
    if not identity:
        raise ValueError("EXPANDED_CODE_SIGN_IDENTITY is required to re-sign ONNX")
    return identity


def sign_framework(framework, identity):
    if identity is None:
        return
    subprocess.run([
        "codesign", "--force", "--sign", identity,
        "--preserve-metadata=identifier,entitlements,flags",
        "--generate-entitlement-der", str(framework),
    ], check=True)
    subprocess.run(["codesign", "--verify", "--strict", str(framework)], check=True)


def fix_framework(framework, deployment_target, check=False):
    plist = framework / "Info.plist"
    original = plist.read_bytes()
    info = plistlib.loads(original)
    minimum = binary_minimum(framework / info["CFBundleExecutable"])
    if version(minimum) > version(deployment_target):
        raise ValueError(f"ONNX requires iOS {minimum}, above app minimum {deployment_target}")
    if info.get("MinimumOSVersion") == minimum:
        print(f"ONNX MinimumOSVersion matches Mach-O: {minimum}")
        return
    if check:
        raise ValueError(f"ONNX MinimumOSVersion {info.get('MinimumOSVersion')!r} != Mach-O {minimum}")
    identity = signing_identity()
    info["MinimumOSVersion"] = minimum
    fmt = plistlib.FMT_BINARY if original.startswith(b"bplist00") else plistlib.FMT_XML
    plist.write_bytes(plistlib.dumps(info, fmt=fmt, sort_keys=False))
    sign_framework(framework, identity)
    print(f"Aligned ONNX MinimumOSVersion with Mach-O: {minimum}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("framework", type=Path)
    parser.add_argument("--deployment-target", required=True)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        fix_framework(args.framework, args.deployment_target, args.check)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"ONNX framework validation failed: {error}\n")


if __name__ == "__main__":
    main()
