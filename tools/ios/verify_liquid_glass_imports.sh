#!/usr/bin/env bash
set -euo pipefail

app_path="$1"
binaries=("$app_path/CoffeePeek")
# Xcode can put the application code in a dylib in Debug builds.
if [[ -f "$app_path/CoffeePeek.debug.dylib" ]]; then
    binaries+=("$app_path/CoffeePeek.debug.dylib")
fi

symbols=$(xcrun nm -um "${binaries[@]}")
if grep -E 'undefined\) external _OBJC_CLASS_\$_(UIGlassEffect|UICornerConfiguration)([[:space:]]|$)' <<< "$symbols"; then
    echo 'Liquid Glass UIKit classes must be weak imports for iOS 17 and 18.' >&2
    exit 1
fi
