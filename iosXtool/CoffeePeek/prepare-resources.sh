#!/usr/bin/env bash
set -euo pipefail

launcher_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "$launcher_dir/../.." && pwd)"
bash "$launcher_dir/prepare-launch-resources.sh" "$@"
python3 "$repo_dir/iosApp/configure-auth.py"
"$repo_dir/gradlew" -p "$repo_dir" :composeApp:assembleIosArm64MainResources --max-workers=6

# Compose 1.9.3's static iOS framework reader resolves paths relative to
# <app>/compose-resources, then appends composeResources/<package>/... .
mkdir -p "$launcher_dir/.build/compose-resources/composeResources"
rsync -a --delete \
  "$repo_dir/composeApp/build/generated/compose/resourceGenerator/assembledResources/iosArm64Main/composeResources/" \
  "$launcher_dir/.build/compose-resources/composeResources/"
