#!/usr/bin/env bash
set -euo pipefail

launcher_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "$launcher_dir/../.." && pwd)"
resources_dir="$launcher_dir/.build/launch-resources"
mkdir -p "$resources_dir"

if [[ $# -gt 0 ]]; then
  [[ -s "$1" ]] || { echo "Missing compiled launch catalog: $1" >&2; exit 1; }
  [[ "$1" -ef "$resources_dir/Assets.car" ]] || cp "$1" "$resources_dir/Assets.car"
elif command -v xcrun >/dev/null 2>&1; then
  rm -f "$resources_dir/Assets.car"
  xcrun --sdk iphoneos actool --compile "$resources_dir" \
    --platform iphoneos --minimum-deployment-target 18.0 --target-device iphone \
    "$repo_dir/iosApp/iosApp/Assets.xcassets"
fi

[[ -s "$resources_dir/Assets.car" ]] || {
  echo "Download ios-launch-resources from iOS build verification into $resources_dir, or run this script on macOS with Xcode." >&2
  exit 1
}
# Loose images override catalog lookup and discard its appearance metadata.
rm -f "$resources_dir/LaunchLogo.png" "$resources_dir/LaunchLogo-dark.png"
