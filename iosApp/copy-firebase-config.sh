#!/bin/sh
set -eu

source_config="$SRCROOT/iosApp/GoogleService-Info.plist"
bundle_config="$TARGET_BUILD_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH/GoogleService-Info.plist"

if [ -f "$source_config" ]; then
    mkdir -p "$(dirname "$bundle_config")"
    cp "$source_config" "$bundle_config"
elif [ "$CONFIGURATION" = "Debug" ]; then
    # A previous configured build must not leave Firebase enabled after removing the local file.
    rm -f "$bundle_config"
    echo "note: Debug build without Firebase configuration; push notifications are unavailable."
else
    echo "error: GoogleService-Info.plist is required for $CONFIGURATION. Add the iOS Firebase configuration." >&2
    exit 1
fi
