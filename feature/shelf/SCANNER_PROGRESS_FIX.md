# Scanner model download progress fix

The scanner download label must show one percent sign after the numeric progress (for example, `13%`). This changes only the shelf UI resource and its argument; download state and model transfer remain unchanged. The affected layer is `feature/shelf/impl` shared Compose UI. Preserve the existing one-time download flow. No backend or persistence behavior is involved.

## Verification — 2026-10-30

- Source when checked: `origin/develop` at `c0d5a936c`; branch subsequently rebased onto map/profile checkpoint `fe12bcf7`. Execution variant: `directSandbox` (`com.coffeepeek.sandbox`).
- `:feature:shelf:impl:compileDebugKotlinAndroid` and `:composeApp:assembleDirectSandbox` passed.
- `:feature:shelf:impl:testDebugUnitTest`: 56 tests in 8 suites, 0 failures, 0 errors, 0 retries.
- `:feature:shelf:impl:compileKotlinIosArm64` executed and `:composeApp:verifyDirectSandboxRuntimeClasses` passed. The `:composeApp:compileKotlinIosArm64` task was skipped on this Windows host; native Swift build remains open.
- No device was selected or app installed. Light/dark previews were added for the downloading state, but IDE rendering and manual Android/iOS visual checks remain open.
