#!/usr/bin/env bash
set -euo pipefail

launcher_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "$launcher_dir/../.." && pwd)"
python3 "$repo_dir/iosApp/configure-auth.py"
"$repo_dir/gradlew" -p "$repo_dir" :composeApp:assembleIosArm64MainResources

# Compose 1.9.3's static iOS framework reader resolves paths relative to
# <app>/compose-resources, then appends composeResources/<package>/... .
mkdir -p "$launcher_dir/.build/compose-resources/composeResources"
rsync -a --delete \
  "$repo_dir/composeApp/build/generated/compose/resourceGenerator/assembledResources/iosArm64Main/composeResources/" \
  "$launcher_dir/.build/compose-resources/composeResources/"

# xtool copies raw launch images; Xcode compiles the same light/dark asset catalog.
mkdir -p "$launcher_dir/.build/launch-resources"
cp "$repo_dir/iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/"*.png \
  "$launcher_dir/.build/launch-resources/"
