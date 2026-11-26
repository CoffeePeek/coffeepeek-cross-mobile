# Collections native iOS checkpoint — 2026-11-18

Previous location: remaining collection destinations and profile slots rendered common Compose UI; saved-public's native list embedded a Compose poster.

Target: SwiftUI collection editor, owner profile tab, featured public preview, profile display preference and saved-public content; native poster preview and PNG rendering. Kotlin keeps repositories, account observation, metadata/cover validation, revision conflicts, place ordering, mutations, read-only link resolution, and export-generation guards. No module split or new domain dependency is introduced.

The application owns navigation and share-sheet adaptation. Feature-owned CollectionsWorkspaceSession adapts the existing SavedCollectionsViewModel; PublicCollectionSession keeps old snapshot links readable through CollectionsViewModel. Swift receives presentation values and cancellable image observations. Authenticated covers stay in the repository path; public and featured surfaces never fetch authenticated cover bytes. On account change, old observations are cancelled and late media is checked against account version.

Actual iOS entries:
- CollectionEditor → Editor workspace, including create/edit, native Photos picker cover preparation, visibility, remove/reorder places, save/delete, conflict reload and sharing.
- SavedPublicCollection → Public workspace and native poster.
- PublicCollection → read-only Snapshot workspace; mutable list links hand off to SavedPublicCollection.
- Own profile collections/preferences and public-profile featured slots → native workspace controllers.
- CollectionsComposer has no production navigation caller or app-link construction. Keep its serializable ID for stack compatibility; on iOS it resolves to the native library.
- The heart/membership flow is active on iOS. CollectionsPickerHost retains request leases,
  favorite synchronization and the decorated repository; PlatformCollectionsPicker presents
  the native membership and quick-create sheets through the same workspace factory.

Retired the saved-public Compose poster adapter/session after replacing its final iOS consumer. Android's feature Compose UI remains active and is retained.

Verification:
- 2026-11-18, source branch codex/ios-native-collections-ui, integration base 9861b209d.
- Windows commonMain and iosMain Kotlin metadata compilation passed.
- verifyFunctionBodyLimits found no new Collections violations; the integration base still has two violations in business-ownership and IosPublicProfileSession.
- Native unit/contract regressions cover old links, list-link handoff, editor metadata/order writes, featured selection, anonymous public covers, stale private-cover completion, old cover-picker account versions and export cancellation.

## Membership presentation — 2026-11-25

Picker mode reuses SavedCollectionsViewModel through CollectionsWorkspaceSession.
The platform-independent workspace state/session and its nine contracts now live
in commonMain/commonTest, so the host JVM executes the native presentation behavior.
SwiftUI owns search, public/private sections, covers, selection, quick creation,
keyboard, errors and busy dismissal. Validation, account revisions, membership
commit and all writes remain shared. Covers retain account-scoped loading.

88 collection contracts passed in six JVM suites, including blank-title rejection,
quick creation plus membership commit, selection after failure, close and account
replacement. This is not live persistence or device UI acceptance. Paired fake
light/dark previews are colocated with both new native sheet components.
- Xcode/Swift build and executed Kotlin/Native tests passed on 1480cda784de30246bb1c21d8773aff3f791f623 in ios-verify run 44267648608 (PR #400): 9 Collections suites, 99 tests, zero failures/errors/skips. Manual device gates below remain open.
- Colocated light/dark previews use injected fake content, no DI or network. Not rendered on this Windows host.
- Remaining manual iPhone gates: light/dark and Dynamic Type; create/edit/save/delete with a safe test account; Photos permission/cancel; private/public cover delivery; conflict reload; profile embedded scrolling; back/cancel while exporting; all multi-page chat/story exports and native share sheets. No real-account writes, UI automation, screenshot tests, or device installs were performed here.

