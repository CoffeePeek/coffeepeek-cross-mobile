"""Build, sign and run a separate CoffeePeek Xcross Debug app over USB."""
import argparse
import asyncio
import os
from pathlib import Path
import subprocess

from xcross_build import ROOT, build_environment, prepare_config, scannerless_linkage_package

BUNDLE_ID = "com.coffeepeek.xcross"


def version_tuple(value):
    parts = str(value).split(".")
    if not parts or not all(part.isdecimal() for part in parts):
        raise ValueError(f"Cannot safely compare installed version: {value}")
    return tuple(int(part) for part in parts) + (0,) * (4 - len(parts))


def check_installed_version(installed, settings):
    for key, setting in (("CFBundleShortVersionString", "MARKETING_VERSION"),
                         ("CFBundleVersion", "CURRENT_PROJECT_VERSION")):
        if version_tuple(installed[key]) > version_tuple(settings[setting]):
            raise ValueError("A newer CoffeePeek Xcross is installed; refusing to downgrade or erase data")


async def preflight(udid, settings):
    from pymobiledevice3.lockdown import create_using_usbmux
    from pymobiledevice3.services.installation_proxy import InstallationProxyService
    async with await create_using_usbmux(serial=udid, connection_type="USB", pair_timeout=10) as device:
        if not device.paired or not await device.get_developer_mode_status():
            raise RuntimeError("Confirm computer trust and enable Developer Mode on the selected iPhone")
        async with InstallationProxyService(device) as service:
            installed = await service.get_apps(bundle_identifiers=[BUNDLE_ID])
        if BUNDLE_ID in installed:
            check_installed_version(installed[BUNDLE_ID], settings)
        print(f"USB preflight passed; target {BUNDLE_ID}. Existing com.coffeepeek is preserved.", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--udid", required=True)
    parser.add_argument("--xcross", default=os.environ.get("XCROSS_EXECUTABLE", "xcross"))
    parser.add_argument("--config", type=Path)
    args = parser.parse_args()
    settings = prepare_config(ROOT, [f"PRODUCT_BUNDLE_IDENTIFIER = {BUNDLE_ID}", "PRODUCT_NAME = CoffeePeek Xcross"])
    asyncio.run(asyncio.wait_for(preflight(args.udid, settings), timeout=30))
    environment = build_environment(args.config)
    environment["XCROSS_BUNDLE_ID"] = "original"
    command = [args.xcross, "compose", "run", "--udid", args.udid, "--usb", "--bundle-id", BUNDLE_ID]
    with scannerless_linkage_package(ROOT):
        subprocess.run(command, cwd=ROOT, env=environment, check=True)


if __name__ == "__main__":
    main()
