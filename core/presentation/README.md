# MVI presentation base

`core/presentation` contains a typed base for migrated feature ViewModels.
It owns a private `MutableStateFlow` initialized by the subclass and a buffered
one-off event channel. Consumers see read-only `StateFlow<State>` and
`Flow<Event>`. Public `onAction(Action)` launches a lifecycle-owned coroutine;
subclasses implement suspending `handleActionInternal(Action)` and can call
atomic `updateState`, read `currentState`, or suspend in `sendEvent`.

It extends the multiplatform lifecycle ViewModel but contains no action queue,
custom scope, navigation, DI, global loading/error policy, Android API or
Compose UI. Each action runs independently, so a slow action cannot block a
later navigation intent; features must guard duplicate or conflicting work.
The legacy app BaseViewModel remains untouched.

- Owner: cross-feature presentation ViewModel contract only.
- Allowed dependencies: Kotlin, kotlinx.coroutines Flow and the multiplatform
  lifecycle ViewModel.
- Allowed consumers: feature `impl` modules and application composition when it
  intentionally needs the contract. Domain, data and API modules do not need it.
- Hidden implementation: core keeps the mutable state and event channel private;
  each feature keeps action, Result and error handling private. The standard
  lifecycle ViewModel owns scope cancellation.
- Gradle boundary: independent feature modules can opt in without depending on
  `composeApp`, another feature implementation, or the visual design-system.
  This is a shared ABI, not a package inside the first feature.

The first subclass is favorites. Event-less screens can use `Nothing` as the
event type; do not invent event classes. `updateState` delegates to the existing
`kotlinx.coroutines.flow.update` extension, rather than duplicating its
compare-and-set logic. Call `sendEvent` from a lifecycle-owned coroutine so
emission order and cancellation stay explicit. An unbounded serialized action
queue and a default catch-all error handler are deliberately not included:
slow actions must not block later intents, and each feature must decide how to
surface failures. Separate actions may complete out of order; features own
any coordination they require. Events are in-memory and are not restored after
process death.
