# iOS own-profile dependency crash — 2026-11-05

## Verification plan

Preserve the existing own-profile screen, account isolation, analytics, navigation,
favorite writers and loading behavior. Fix application composition only. Trace the
actual iOS bottom Profile tab in shipped main and current develop, exercise its
required cache from the production platform module list in a non-UI Koin test,
and retain a negative control reproducing the old missing-definition failure.
Run relevant profile/application unit tests and Android Debug assembly with the
packaged DEX check. Use CI for the native iOS build and Native contract tests;
this Linux host has no Xcode or iOS runtime. Do not run automated UI tests or
change real account data. Manual opening on an iPhone remains a separate gate.

## Diagnosis

The user confirmed the crash occurs on the owner's bottom Profile tab. The last
TestFlight workflow 41891202993 published main `06bceb17` (1.1.6). That source and
develop `78a49bb5` use `NativeScreenController` -> `OwnProfileScreenRenderer` ->
the migrated own-profile entry -> `PersonalProfileActivityPreview`.

The preview resolves `PersonalProfileWeekCache` from Koin. Its only production
definition belongs to `profileSnapshotModule`, included by Android but omitted
from iOS and the common root modules. Resolving it in the shipped iOS graph
therefore throws `NoDefinitionFoundException`; the UI has no exception handler
for this missing required dependency. Existing Native Koin compatibility tests
only used a synthetic ViewModel, and the shelf graph test did not resolve this
profile dependency. A successful native compilation did not cover this path.

No device crash report is available, so this is a concrete code-path defect
matching the reported entry, not a symbolicated device stack trace.

## Change

Expose the actual iOS platform module list as an internal composition function
with an injectable API base URL, and include the existing profile snapshot
module. Runtime startup consumes that same list. Native non-UI tests resolve
the cache and snapshot store, check singleton/account lifetime, and remove the
profile module for a negative control. Add the test class to the explicit Native
CI filter so it is executed rather than merely compiled. No UI, resources,
backend contracts, Gradle modules or iOS favorite bindings change.

Release metadata advances Android/iOS to 1.1.7 while retaining the shared
commit-count build-number offset. Publish through the existing main release
workflow after native/Android CI passes. The release also contains the already
accepted develop favorites fixes from PR #304; their server receipt contract is
already deployed in backend main `32e39bdd`.

## Validation

The first local gate passed 1,100 unit/contract cases (profile impl 66, application
Direct Debug 1,034), zero failures/errors/skips. Direct Debug assembly and its
packaged runtime-class verification passed on JDK 21 with `--max-workers=6`.
The final 1.1.7 metadata gate passed as well: application tests reran successfully,
the unchanged profile suite stayed up-to-date, Debug assembly/DEX verification
passed, and four release-versioning tests passed. Native CI remains pending.
Existing unrelated Preview/experimental API warnings were retained. No automated
UI tests or device/account operations ran.

Source: migration branch from develop
`78a49bb545fc0459b12816bdde01850f53ebd511`. Studio MCP is unavailable;
inspection used repository source and GitHub workflow metadata. Untracked
attachments and marketing files are preserved.
