#!/usr/bin/env python3
"""Import Google iOS IDs locally and prepare xtool's generated Info.plist. No Firebase SDK needed."""
import argparse
import plistlib
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOCAL = ROOT / "iosApp/Configuration/Local.xcconfig"
KEYS = ("GOOGLE_IOS_CLIENT_ID", "GOOGLE_WEB_CLIENT_ID", "GOOGLE_REVERSED_CLIENT_ID")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--google-plist", type=Path, help="GoogleService-Info.plist for com.coffeepeek")
    parser.add_argument("--web-client-id", help="The same Google Web OAuth audience as the backend")
    args = parser.parse_args()
    text = LOCAL.read_text(encoding="utf-8") if LOCAL.exists() else ""
    values = {}
    for key in KEYS:
        match = re.search(r"^" + key + r"\s*=\s*([^\r\n]*)", text, re.MULTILINE)
        values[key] = match.group(1).strip() if match else ""

    if args.google_plist:
        with args.google_plist.open("rb") as source:
            google = plistlib.load(source)
        if google.get("BUNDLE_ID") != "com.coffeepeek":
            parser.error("Google plist must belong to the iOS app com.coffeepeek")
        values[KEYS[0]] = google.get("CLIENT_ID", "")
        values[KEYS[2]] = google.get("REVERSED_CLIENT_ID", "")
        if not values[KEYS[0]] or not values[KEYS[2]]:
            parser.error("Enable Google sign-in in Firebase and download a new plist containing CLIENT_ID and REVERSED_CLIENT_ID")
    if args.web_client_id:
        values[KEYS[1]] = args.web_client_id
    configured = any(values.values())
    if configured:
        for key in KEYS[:2]:
            if not re.fullmatch(r"[A-Za-z0-9_-]+\.apps\.googleusercontent\.com", values[key]):
                parser.error(f"Missing or invalid {key}; use --web-client-id for the backend audience")
        if values[KEYS[2]] != ".".join(reversed(values[KEYS[0]].split("."))):
            parser.error("REVERSED_CLIENT_ID must correspond to CLIENT_ID")
    if args.google_plist or args.web_client_id:
        for key, value in values.items():
            line = key + " = " + value
            pattern = r"^" + key + r"\s*=[^\r\n]*"
            text = re.sub(pattern, line, text, flags=re.MULTILINE) if re.search(pattern, text, re.MULTILINE) else text.rstrip() + "\n" + line + "\n"
        LOCAL.write_text(text, encoding="utf-8")

    launcher = ROOT / "iosXtool/CoffeePeek"
    with (launcher / "Info.plist").open("rb") as source:
        info = plistlib.load(source)
    if configured:
        info.update(GIDClientID=values[KEYS[0]], GIDServerClientID=values[KEYS[1]], CoffeePeekGoogleReversedClientID=values[KEYS[2]])
        info["CFBundleURLTypes"] = [{"CFBundleTypeRole": "Editor", "CFBundleURLSchemes": [values[KEYS[2]]]}]
    output = launcher / ".build/Info.plist"
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("wb") as target:
        plistlib.dump(info, target)
    print("iOS auth configuration prepared. Google: " + ("configured" if configured else "not configured"))


if __name__ == "__main__":
    main()
