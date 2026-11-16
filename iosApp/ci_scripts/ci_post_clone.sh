#!/bin/sh
set -eu

if [ "${CI_XCODE_CLOUD:-}" != TRUE ]; then
    exit 0
fi

if ! /usr/libexec/java_home -F -v 17 >/dev/null 2>&1; then
    brew install openjdk@17
fi

if [ "${CI_XCODEBUILD_ACTION:-}" = build ]; then
    repository="${CI_PRIMARY_REPOSITORY_PATH:?Missing Xcode Cloud checkout path}"
    firebase_config="$repository/iosApp/iosApp/GoogleService-Info.plist"
    if [ ! -f "$firebase_config" ]; then
        cp "$repository/tools/ios/GoogleService-Info.verification.plist" "$firebase_config"
    fi
fi
