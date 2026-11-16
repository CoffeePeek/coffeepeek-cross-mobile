import { readFile } from 'node:fs/promises';
import { runProjectEvent } from './lifecycle.mjs';
import { githubGraphql } from './sync.mjs';

async function run() {
  const { PROJECT_STATUS_TOKEN: token, GITHUB_EVENT_PATH: path, PROJECT_STATUS_DRY_RUN: flag } = process.env;
  if (!token || !path) throw new Error('Missing GitHub App token or Actions event.');
  if (flag && flag !== 'true' && flag !== 'false') throw new Error('Set PROJECT_STATUS_DRY_RUN to true or false.');
  const event = JSON.parse(await readFile(path, 'utf8'));
  const result = await runProjectEvent(process.env.GITHUB_EVENT_NAME, event, {
    graphql: githubGraphql(token), dryRun: flag !== 'false',
    archiveEnabled: process.env.PROJECT_STATUS_ARCHIVE_ENABLED === 'true',
  });
  console.log(JSON.stringify(result));
}

run().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
