---
name: clean-code
description: Keep CoffeePeek changes small, source-grounded and within the repository architecture.
---

# CoffeePeek code changes

Repository [AGENTS.md](../../../AGENTS.md) and [ARCHITECTURE.md](../../../ARCHITECTURE.md) define ownership and verification. This guide supplements them; it does not replace the user's instructions.

- Inspect actual consumers, source sets, dependencies and existing abstractions before editing.
- Keep each change tied to its business owner and task. Prefer composition and meaningful names; avoid speculative wrappers, generic helpers and unrelated cleanup.
- For new/substantively changed function bodies, follow the repository limits: 40 non-blank/non-comment lines for Composable, 20 for ordinary functions. Do not pack statements to evade the limit or extend the baseline manually.
- Feature UI uses domain/api and design-system; DTOs, HTTP and DAOs remain in data. Root assembles dependencies.
- Preserve cancellation, Result errors, account/session guards, saved data and analytics parity.
- Review dependent callers and meaningful unit/contract regressions. Run the relevant repository checks, not scripts from unrelated frameworks.
- Keep light/dark fake previews beside changed shared UI. Manual Sandbox review covers UI; do not create/run automated UI/instrumented Compose/screenshot tests.
- Report the actual source/variant, executed checks, original failures/retries and remaining gates. Documentation-only changes need link/path/source verification, not app deployment.

Use [CONTRIBUTING.md](../../../CONTRIBUTING.md) for commands and [verification flow](../../../docs/VERIFICATION_FLOW.md) for test layers. The old generic TypeScript/agent-script checklist was removed because those tools and roles are not part of this repository.
