# CoffeePeek Project issue workflow

Reviewed against `.github/workflows/project-merge-status.yml` and the local sync/lifecycle implementation on 2026-11-27. Repository variables, App permissions and live project settings must be checked separately; no writing run is implied by this guide.

The workflow acts on [CoffeePeek Project #6](https://github.com/orgs/CoffeePeek/projects/6). It keeps issue cards separate from pull request cards.

| Event | Change |
| --- | --- |
| Same-repository PR merged into `develop` | Linked issue cards move to `Preview`. |
| Same-repository PR merged into `main` | Linked issue cards move to `Done`. |
| Mobile issue opened with `project:coffeepeek` or labeled while open | Add a card if absent and initialize its empty status to `Backlog`. |
| Mobile issue reopened with an existing `Done` card | Restore it if archived, then move to `Todo`. |
| Daily archive job, after separate opt-in | Archive existing `Done` cards for issues closed as completed. The issue close, card update and Status update must all be at least 30 days old. |

A delayed merge does not downgrade `Done` or advance a card reopened after that merge. The archive job checks the current card before applying the update. It can archive older eligible cards from the mobile, tracker and backend repositories on its first writing run.

## Link issues in the pull request

For a task PR, link the issue through GitHub Development or add one marker per line in the description:

```text
Project-Issue: #350
Project-Issue: CoffeePeek/coffeepeek-tracker#456
```

`#350` means the mobile repository. Use qualified references for tracker or backend issues. For a `develop` → `main` release PR, add a `Project-Issue:` line for each issue included in the release. An empty release list leaves existing statuses as they are. [GitHub issue linking](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue)

## Enable and verify

1. Publish the workflow and `tools/project-status` to `main` for issue events, scheduled archival, manual checks and `main` merges. Sync these same files to `develop` for preview merges. The standard `pull_request` closed trigger works for same-repository merges. GitHub withholds repository App secrets from fork PR workflows.
2. Ask a CoffeePeek organization admin to create or approve a GitHub App installed on `coffeepeek-cross-mobile`, `coffeepeek-tracker` and `coffeepeek-backend`. Grant organization **Projects: Read and write**, repository **Issues: Read** and **Pull requests: Read**. Maintain the mobile `project:coffeepeek` label through repository settings. [Projects GitHub App permissions](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/automating-projects-using-actions)
3. Set mobile repository Actions variable `PROJECT_STATUS_APP_CLIENT_ID` to the App client ID and secret `PROJECT_STATUS_APP_PRIVATE_KEY` to its private key. Set `PROJECT_STATUS_ENABLED=true` and leave `PROJECT_STATUS_DRY_RUN` unset or `true`.
4. From `main`, manually run **Project issue workflow** for an existing card, such as `#350`. Select `develop`, then repeat with `main`. Both runs always report planned changes without writing. [Run a workflow manually](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)
5. Review the project’s **Item added to project**, **Item closed**, and **Pull request linked to issue** rules. Their current Status changes should agree with Backlog intake and the meaning of Preview and Done. Set `PROJECT_STATUS_DRY_RUN=false` when ready to write. Removing `PROJECT_STATUS_ENABLED` pauses the workflow. If intake adds a card but the initial Status write fails, finish setting its empty status to Backlog manually.
6. To verify archiving, set `PROJECT_STATUS_ARCHIVE_ENABLED=true` after setting the dry-run variable back to true. Review the next scheduled counts before enabling writes again. The daily job only writes after both opt-ins and `PROJECT_STATUS_DRY_RUN=false`. Leave the archive flag unset to keep scheduled archiving off.

The mobile repository is the trigger source. Its issue label and reopen events apply to mobile issues only. The scheduled archive can act on existing Project #6 cards in the three allowlisted repositories. The queue serializes this workflow’s runs within the mobile repository. [GitHub Actions concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency)

## Local verification

Run `node --test tools/project-status/sync.test.mjs tools/project-status/lifecycle.test.mjs` with Node 20 or later. The tests use a fake GraphQL client without credentials.
