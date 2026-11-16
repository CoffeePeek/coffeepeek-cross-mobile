# CoffeePeek Project status

The mobile repository can update existing issue cards in [CoffeePeek Project #6](https://github.com/orgs/CoffeePeek/projects/6) when a pull request merges. A merge into `develop` sets `Preview`. A merge into `main` sets `Done`. A card already in `Done` stays there if a preview run is delayed or retried. `Done` records the merge, so release or deployment success would be a separate signal.

## Link issues in the pull request

For a task PR, link the issue through GitHub Development when it is available, or put one marker per line in the PR description:

```text
Project-Issue: #123
Project-Issue: CoffeePeek/coffeepeek-tracker#456
```

`#123` means the mobile repository. Use a qualified reference for a tracker or backend issue. Review the marker lines before merging. The workflow changes only unarchived issue cards that already belong to Project #6 and are in `coffeepeek-cross-mobile`, `coffeepeek-tracker`, or `coffeepeek-backend`.

For a `develop` → `main` release PR, list the issues included in that release using `Project-Issue:` lines. Release PRs in this repository commonly have no linked closing issues. An empty release list leaves the project unchanged. Closing keywords targeting the default branch can still close an issue natively, so use an explicit marker for a Preview move when necessary. [GitHub issue linking](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue)

## Enable the workflow

1. Have a CoffeePeek organization admin create or approve a GitHub App installed on the three repositories above. Grant organization **Projects: Read and write**, repository **Issues: Read** and **Pull requests: Read**. [Projects GitHub App permissions](https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/automating-projects-using-actions)
2. In the mobile repository Actions configuration, set variable `PROJECT_STATUS_APP_CLIENT_ID` to the App client ID and add its private key as secret `PROJECT_STATUS_APP_PRIVATE_KEY`. Keep `PROJECT_STATUS_ENABLED` unset until the workflow is on `main`.
3. Publish this workflow and its script to the default branch `main` before testing either merge target. It uses a closed and merged `pull_request_target` event and checks out the trusted default-branch revision. [GitHub Actions events](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#pull_request_target)
4. Set `PROJECT_STATUS_ENABLED` to `true`. Begin with `PROJECT_STATUS_DRY_RUN` unset or `true`. Check the counts of matched and planned issue updates in the workflow run. Set the variable to `false` to enable status writes. Disabling `PROJECT_STATUS_ENABLED` pauses the automation.

Only the mobile repository runs this workflow. A dedicated queue serializes its runs. Rerunning a failed job is safe after restoring access. Keep any built-in workflow that manages PR cards alone if you want it. Review the board rules Item closed and Pull request linked to issue before enabling writes. They also act on issue cards and may set status after a close or link event. [GitHub Actions concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency)

## Local verification

Run `node --test tools/project-status/sync.test.mjs` with Node 20 or later. The tests use a fake GraphQL client and no credentials.
