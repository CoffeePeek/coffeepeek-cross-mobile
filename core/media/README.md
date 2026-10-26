# Shared image infrastructure

Owns URL/byte loading, public and protected image caches, session/access
revocation, bounded platform decoding, progressive image resources and bounded
prefetch. No feature, legacy module, application configuration or Koin dependency.
There is no second authenticated client: application composition supplies the
existing clients, SessionReader, API base URL and visibility invalidation flow.

Allowed dependencies: core/session, Ktor, coroutines, Compose/Kamel resource and
lifecycle APIs, Android bitmap/EXIF and iOS ImageIO. Consumers: design-system
rendering, root image configuration and platform photo preparation. Features use
design-system components or their narrow image slots; data/domain do not render.
Design-system owns visuals, placeholders, loaders, gallery gestures and controls.

Public surface: image source/fetcher configuration, resource loading, environment,
prefetch and the viewer's speculative-work suspension. Byte/painter caches,
permissions and protocol validation are internal. Routes under check-in photo
delivery are a shared access protocol, not a dependency on feature models.

A separate Gradle boundary is needed because image loading is used by independent
features and both platforms, but must not depend on application startup/DI or
design-system visuals. Packages inside composeApp cannot enforce that direction;
putting network/decode/session code in design-system would mix ownership.

Migration starts from composeApp/utils/CpImage, Fetcher, PhotoPrefetch and image/,
the Android bitmap codec and iOS decoder/config. Preserve private cache bounds,
ETag/304 permissions and TTL, mutation/session revocation, cancellation, timeouts,
preview-first loading, alpha/vector/EXIF and original-on-zoom behavior. Root only
assembles the environment. Unit/transport regressions move with their owner;
no new UI tests or test host are introduced. Device and full native-app acceptance
remain separate gates recorded in feature/LEGACY_EXIT_PLAN.md.
