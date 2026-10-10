# Resource ownership and provenance

Prepared shared assets: six existing static Manrope TTFs, version 4.504, copied
byte-for-byte from composeApp/src/commonMain/composeResources/font. No downloads,
font upgrades, subsetting, renaming of internal font metadata or glyph edits.
They total approximately 567 KiB. Legacy copies remain until app integration.
During the independent phase, the app has no dependency on this library, so it
does not package both copies through this change.

Public UI surface: Manrope (Composable FontFamily), CoffeePeekTheme with packaged
Manrope default and explicit override, cpTypography(FontFamily) unchanged.
Compose resource runtime is an implementation dependency; generated Res and
accessors are internal in com.coffeepeek.core.designsystem.resources. Consumers
do not import application Res or depend on the library's generated resource paths.
The common Compose resources directory is prepared for future shared consumers;
Android alone is built/tested here, not native iOS implementation.

## Fonts and license

OS/2 metadata confirms weights 300/400/500/600/700/800 for light/regular/medium/
semibold/bold/extrabold. The name table identifies copyright 2019 The Manrope
Project Authors (https://github.com/sharanda/manrope), version 4.504 and SIL
Open Font License 1.1. Although name-table family labels say ExtraLight, explicit
weight mappings and OS/2 values identify the supplied six static files. Preserve
their bytes rather than silently normalizing or replacing the assets.

files/licenses/manrope-OFL.txt ships alongside the fonts. Its copyright notice
matches the existing binaries; the standard OFL 1.1 body was retrieved from
https://raw.githubusercontent.com/google/fonts/main/ofl/manrope/OFL.txt.
The historical sharanda/manrope repository is currently unavailable; this does
not establish that these binaries match a current Google Fonts release.

Android tests read all six packaged files through Compose Resources and compare
SHA-256 with the source copies, read the packaged notice, resolve/render every
weight including Cyrillic, check default light/dark typography and explicit font
override. This is not a pixel-parity or exhaustive glyph-coverage test.
Both debug/release AARs must include the font and license assets.

## Remaining artwork

| Existing app asset | Decision |
|---|---|
| ic_app_icon_light/dark | No commonMain Kotlin call sites found; launcher/product identity stays in app pending actual shared need |
| maskot_happy, with_photo/book/laptop/etc. | Used by feed/auth/review/contribution states; ownership must be decided with those features |
| ic_google_g | Authentication provider branding, not a generic UI icon |
| brew_* / checkin_rating_* / ic_byn_symbol | Feature presentation semantics, not core business content |

No image-loader, resource repository or new umbrella core/resources module is
introduced. Phosphor action icons remain behind CpIcons. Prepare neutral image
rendering only if actual cross-feature reuse warrants it.

## Integration checklist

After switching a consumer, verify text metrics, large font scale, Cyrillic,
light/dark/RTL and preview rendering; verify font/notice packaging in its APK.
Ensure a single long-term font owner and remove legacy copies only after all
their resource imports have migrated. Application legal-notice UI remains an
integration decision; this slice packages the notice but does not add app screens.
