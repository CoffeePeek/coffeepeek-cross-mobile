# Password sign-in after registration

## Scope and verification plan — 2026-11-12

`feature/auth` owns password sign-in on iOS and Android. Preserve the submitted
password, email normalization, session handling and analytics. Verify both HTTP
401 responses in the repository tests, the corresponding login states in the
ViewModel tests and the packaged Android Debug app. On iOS, verify sign-in before
and after confirming a test account, including keyboard input and AutoFill.

## Cause

Registration hashes the submitted password. Login verifies it before checking
email confirmation. The backend returns `Invalid credentials` for invalid
credentials and `Email is not confirmed` for a correct password awaiting
confirmation. Both responses have HTTP status 401. The shared client previously
used its generic email/password prompt for the unconfirmed response. The login
screen now asks the user to confirm their email. Its generic error asks the user
to retry. Confirmation links expire after 10 minutes, and web sign-in can request
another confirmation email.

## Verification

- Backend `245aef033c1656c2a6686afc2b86e04719f3b6a3`, with a successful deployment
  job in run `43004140749`. Local suites passed 28 application tests for login and
  confirmation and 32 infrastructure tests for password hashing and account
  email handlers. Tests used fixtures and did not send production mail.
- Mobile `e2e51fff983470cde23beffab9087f529e1d72bf` plus this patch. Gradle passed
  12 auth domain tests, 29 auth data tests, 39 auth presentation tests and one
  filtered app transport test, all without retries. The data regression verifies
  both 401 envelopes and confirms neither creates a local session.
- `assembleDirectDebug`, `verifyDirectDebugRuntimeClasses` and
  `verifyFunctionBodyLimits` passed. The APK targets `com.coffeepeek` and was
  built without installation or launch. The build reported existing Compose
  preview deprecations. Android Studio MCP was unavailable.
- The login preview covers the unconfirmed state in both themes. Device and
  preview rendering, iOS compilation and end-to-end email delivery and sign-in
  still need verification. The frequency of this failure in production is unknown.
