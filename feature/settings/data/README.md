# Settings data

Owns account analytics preference HTTP calls and the durable account-scoped pending choice.
Depends on settings/domain and core network, session and database infrastructure.
Application composition calls the factory; feature UI consumes domain/API contracts only.
DTOs, HTTP backend, migration and local keys are internal. A separate data Gradle boundary
keeps these dependencies out of settings impl/domain; packages in composeApp would leave
feature persistence in application composition.

The server initializes accounts to enabled. The old installation choice is migrated only
for the account restored at startup, with onlyIfUnset so a newer server decision wins.
Guest legacy settings cannot be attributed to a future account and are removed.
Failed writes stay pending for that account and are retried on the next load. No settled
local default can enable analytics after an unsuccessful server read.
