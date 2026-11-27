# Resource ownership and provenance

Reviewed on 2026-11-27. Shared resources live in
`src/commonMain/composeResources`; generated accessors are internal to
`com.coffeepeek.core.designsystem.resources`. Kotlin facade names use CoffeePeek;
third-party filenames, metadata and notices retain their provenance.

## Manrope

Six static TTFs are present: light, regular, medium, semibold, bold and extrabold,
with explicit mappings for weights 300/400/500/600/700/800. They were copied from
the application's existing resources; do not silently upgrade, subset or change
internal family metadata while moving consumers. Android uses these fonts; the shared Compose iOS default is the system font.
Active SwiftUI CoffeePeekNativeStyle loads the packaged Manrope weights with
Dynamic Type scaling and falls back to a system font when unavailable.

The recorded binary provenance is Manrope 4.504, copyright 2019 The Manrope
Project Authors, under SIL Open Font License 1.1. Original project:
[sharanda/manrope](https://github.com/sharanda/manrope); the recorded notice source
is [Google Fonts Manrope OFL](https://github.com/google/fonts/blob/main/ofl/manrope/OFL.txt).
That attribution does not assert equality with the latest upstream font binaries.

The older guide claimed `files/licenses/manrope-OFL.txt` shipped in this module.
The current tracked tree contains no such file or other Manrope notice. Packaging
that notice and checking the actual APK/AAR remains an unresolved resource gate;
this documentation change does not add or certify the missing asset. Preserve
this provenance when restoring the notice, and remove duplicate font resources
only after their final consumer has migrated.

## Artwork ownership

| Asset family | Owner/contract |
| --- | --- |
| Launcher icons/product identity | Application platform asset catalogs/resources |
| Interface action icons | CoffeePeekIcons; shared vectors and native symbol mapping |
| Brewing vectors | CoffeePeekBrewIcons exposes artwork; feature maps business slugs/labels |
| BYN glyph | Shared decorative currency artwork; feature owns thresholds/text |
| Google branding | Authentication feature/provider presentation |
| Rating mascots/empty-state imagery | Owning business feature |
| Notification mascot | [design/notifications](../../design/notifications/README.md) |

Do not create an umbrella `core/resources` module or move business images merely
to centralize files. Keep Russian/Belarusian resource keys aligned where supported;
English support must not be inferred from a fallback or OS-owned phrase.

## Consumer verification

Check packaged fonts/notices, Cyrillic/text metrics, light/dark, font scaling,
RTL and overrides after replacing a consumer. Keep paired previews beside the
component and remove obsolete duplicates with their final verified consumer.
Asset provenance and license verification are distinct from a rendered preview.
