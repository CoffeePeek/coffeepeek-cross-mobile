# Native iOS exit: current state and remaining gates

Reviewed against current `develop` on 2026-11-27. This canonical checklist replaces
superseded staged SwiftUI plans, historical Compose reachability audits and local
build/install diaries. Prior evidence and failures remain in Git history and the
corresponding PR/CI records; they are not repeated as current acceptance.

## Implemented source boundary

- Both launchers enter `MainViewController` → ordinary `IosNativeRoot` → registered
  Swift UIKit root; the active root does not create a Compose controller.
- UIKit owns tabs, stacks, native transition completion and root modal presentation.
  SwiftUI/UIKit render active features, membership/quick-create, global operation
  status, editors, reports, photo viewer and platform pickers.
- Feature-owned Kotlin sessions retain repositories, shared ViewModels, leases,
  validation, analytics and writers. Constructor/factory injection replaces the
  old composition-owned lifetimes on active native routes.
- Android Compose feature implementations remain supported. Their presence in
  commonMain is not evidence of an active iOS route and does not authorize deletion.
- Compatibility platform hooks and shared dependencies are not proof that every
  obsolete implementation/alias or dependency has been removed. Track their final
  consumers in [the repository legacy exit plan](../feature/LEGACY_EXIT_PLAN.md).

The ownership details are in [root/session contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md),
[native navigation](IOS_NATIVE_NAVIGATION.md) and each feature's current guide.
A native-looking screen alone is not lifetime, privacy or migration acceptance.

## Remaining acceptance checklist

- [ ] When replacing/removing a remaining adapter, trace every reachable route,
  profile preview, picker and modal variant from both bootstraps; remove obsolete
  fallback code with its last verified consumer. Do not add hidden/headless roots.
- [ ] Execute affected JVM and selected Kotlin/Native contracts on the actual
  revision, including observer disposal, account changes, token rotation, late
  completion, terminal retry and exactly-once handoff. Check report case names,
  not only task success. Keep initial failures and distinguish cache/compiled runs.
- [ ] For source changes, assemble the Android app with packaged runtime-class
  checks and complete the native Kotlin framework + Swift app link. A skipped
  Native compile on Windows or standalone Swift probe is not a complete iOS build.
- [ ] Manually verify light/dark, larger text, VoiceOver, keyboard/safe areas,
  modal placement, scroll state, back/swipe cancellation, temporary photo/camera
  coverage and foreground/background on explicitly selected native devices.
- [ ] Verify real integration gates separately: OAuth, push callbacks, Universal
  Links, session persistence, protected photos, scanner and real backend writes
  require suitable configuration and an authorized test setup. Scannerless xcross
  builds cannot certify ONNX scanning or normal signed-app capabilities.
- [ ] For an authorized release, use the existing signed archive/upload workflow
  and observe the exact version/build's Apple processing status. Dispatch/upload,
  unsigned CI and a locally signed xcross install are separate outcomes.

These boxes describe ongoing acceptance obligations, not claims that already
merged code is unimplemented. No new build, device action or release dispatch
occurred in this documentation refresh. Current delivery configuration is in
[IOS_SETUP](IOS_SETUP.md); this checklist does not carry forward a past owner's
one-time release/install instruction as a standing command.
