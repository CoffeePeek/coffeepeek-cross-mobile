# Shop check-in QR MVP

- Business ownership renders and exports one QR per listed coffee shop with a public slug. The code is a print-ready PNG with a white border. The app shares it through its existing system share path.
- Application navigation owns the HTTPS link and the sign-in continuation. The QR opens the existing shop check-in form for signed-in users and keeps the selected shop through login or registration. Closing sign-in cancels that request.
- Shop detail waits for the current session and loaded details, then opens the existing form once for that route. Sending a check-in still requires the existing submit action and validation.

## Verification

- Unit contracts cover link generation and parsing, ordinary share links, and sign-in continuation consumption and cancellation.
- Build Direct Debug and run the app unit suite. Review the business tab and PNG export on Android and iOS. Use Android `directSandbox` for manual app checks. Test scanning the PNG with a second device, login and registration, cancellation and rescanning, light and dark themes, and the website fallback with the app absent.
- The QR has no account identifier and can be printed for visitors. The installed app must include the QR route to open the form directly.
