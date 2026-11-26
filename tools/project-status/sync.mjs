const projectOwner = 'CoffeePeek';
const projectNumber = 6;
export const sourceRepository = 'coffeepeek/coffeepeek-cross-mobile';
const allowedRepositories = new Set([
  sourceRepository,
  'coffeepeek/coffeepeek-tracker',
  'coffeepeek/coffeepeek-backend',
]);
export const targetStatuses = new Map([['develop', 'Preview'], ['main', 'Done']]);
const explicitMarker = /^ {0,3}(?:[-*] +)?Project-Issue:/i;
const explicitReference = /^(?:([a-z0-9_.-]+\/[a-z0-9_.-]+))?#([1-9][0-9]*)$/i;

const linkedIssuesQuery = `
  query LinkedIssues($owner: String!, $repo: String!, $number: Int!, $cursor: String) {
    repository(owner: $owner, name: $repo) {
      pullRequest(number: $number) {
        closingIssuesReferences(first: 100, after: $cursor) {
          nodes { number repository { nameWithOwner } }
          pageInfo { hasNextPage endCursor }
        }
      }
    }
  }
`;

const projectQuery = `
  query ProjectStatus($owner: String!, $number: Int!) {
    organization(login: $owner) {
      projectV2(number: $number) {
        id
        field(name: "Status") {
          ... on ProjectV2SingleSelectField { id options { id name } }
        }
      }
    }
  }
`;

const itemFields = `
  id
  isArchived
  updatedAt
  content {
    __typename
    ... on Issue {
      number state stateReason closedAt repository { nameWithOwner }
      timelineItems(last: 1, itemTypes: [REOPENED_EVENT]) {
        nodes { ... on ReopenedEvent { createdAt } }
      }
    }
  }
  fieldValueByName(name: "Status") {
    ... on ProjectV2ItemFieldSingleSelectValue { optionId updatedAt }
  }
`;

const itemQuery = `
  query ProjectIssueItem($id: ID!) {
    node(id: $id) {
      ... on ProjectV2Item { ${itemFields} project { id } }
    }
  }
`;

const itemsQuery = `
  query ProjectIssues($projectId: ID!, $cursor: String) {
    node(id: $projectId) {
      ... on ProjectV2 {
        items(first: 100, after: $cursor, archivedStates: [ARCHIVED, NOT_ARCHIVED]) {
          nodes { ${itemFields} }
          pageInfo { hasNextPage endCursor }
        }
      }
    }
  }
`;

const updateMutation = `
  mutation SetProjectStatus($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
    updateProjectV2ItemFieldValue(input: {
      projectId: $projectId, itemId: $itemId, fieldId: $fieldId,
      value: { singleSelectOptionId: $optionId }
    }) { projectV2Item { id } }
  }
`;

export function repositoryName(name) {
  return typeof name === 'string' ? name.toLowerCase() : '';
}

function mergedPullRequest(event) {
  const pull = event?.pull_request;
  const repository = repositoryName(event?.repository?.full_name);
  const status = targetStatuses.get(pull?.base?.ref);
  if (event?.action !== 'closed' || pull?.merged !== true || !status ||
      repository !== sourceRepository || repositoryName(pull.base.repo?.full_name) !== repository ||
      repositoryName(pull.head?.repo?.full_name) !== repository ||
      !Number.isSafeInteger(pull.number) || pull.number < 1) return null;
  const release = status === 'Done' && pull.head?.ref === 'develop' &&
    repositoryName(pull.head.repo?.full_name) === repository;
  const mergedAt = Date.parse(pull.merged_at);
  if (!Number.isFinite(mergedAt)) return null;
  return { repository, status, release, mergedAt, number: pull.number, body: pull.body ?? '' };
}

export function issueKey(repository, number) {
  const name = repositoryName(repository);
  if (!allowedRepositories.has(name) || !Number.isSafeInteger(number) || number < 1) return null;
  return name + '#' + number;
}

function* markerValues(body) {
  let fence = '';
  const visible = body.replace(/<!--[\s\S]*?(?:-->|$)/g, '');
  for (const line of visible.split(/\r?\n/)) {
    const delimiter = line.match(/^ {0,3}(~{3,}|`{3,})(.*)$/);
    if (delimiter) {
      const [, run, suffix] = delimiter;
      if (!fence) fence = run;
      else if (run[0] === fence[0] && run.length >= fence.length && !suffix.trim()) fence = '';
      continue;
    }
    const marker = !fence && line.match(explicitMarker);
    if (marker) yield line.slice(marker[0].length).trim();
  }
}

export function parseIssueReference(value, repository = sourceRepository) {
  const match = typeof value === 'string' && value.trim().match(explicitReference);
  const key = match && issueKey(match[1] ?? repository, Number(match[2]));
  if (!key) throw new Error('Invalid Project-Issue line. Use #123 or an issue in an approved CoffeePeek repository.');
  return key;
}

function explicitIssues(body, repository) {
  const keys = new Set();
  for (const value of markerValues(body)) {
    keys.add(parseIssueReference(value, repository));
  }
  return keys;
}

async function collectPages(loadPage) {
  const nodes = [];
  const seenCursors = new Set();
  let cursor = null;
  for (;;) {
    const page = await loadPage(cursor);
    if (!Array.isArray(page?.nodes) || typeof page.pageInfo?.hasNextPage !== 'boolean') {
      throw new Error('GitHub returned an invalid or inaccessible connection.');
    }
    nodes.push(...page.nodes);
    if (!page.pageInfo.hasNextPage) return nodes;
    cursor = page.pageInfo.endCursor;
    if (!cursor || seenCursors.has(cursor)) throw new Error('GitHub returned an invalid pagination cursor.');
    seenCursors.add(cursor);
  }
}

async function collectIssues(pull, graphql) {
  const keys = explicitIssues(pull.body, pull.repository);
  if (pull.release) return keys;
  const [owner, repo] = pull.repository.split('/');
  const links = await collectPages(async cursor => {
    const data = await graphql(linkedIssuesQuery, { owner, repo, number: pull.number, cursor });
    return data.repository?.pullRequest?.closingIssuesReferences;
  });
  for (const issue of links) {
    const key = issueKey(issue?.repository?.nameWithOwner, issue?.number);
    if (key) keys.add(key);
  }
  return keys;
}

export async function readProject(graphql, required = ['Preview', 'Done']) {
  const data = await graphql(projectQuery, { owner: projectOwner, number: projectNumber });
  const project = data.organization?.projectV2;
  const field = project?.field;
  const options = new Map(field?.options?.map(option => [option.name, option.id]) ?? []);
  if (!project?.id || !field?.id || !required.every(name => options.get(name))) {
    throw new Error('CoffeePeek Project #6 must be accessible and have a Status field with ' + required.join(' and ') + ' options.');
  }
  return { projectId: project.id, fieldId: field.id, options };
}

export async function projectItems(graphql, projectId) {
  return collectPages(async cursor => {
    const data = await graphql(itemsQuery, { projectId, cursor });
    return data.node?.items;
  });
}

export async function projectItem(graphql, projectId, id) {
  const { node } = await graphql(itemQuery, { id });
  return node?.project?.id === projectId ? node : null;
}

export function itemIssueKey(item) {
  const issue = item?.content;
  return issue?.__typename === 'Issue' ? issueKey(issue.repository?.nameWithOwner, issue.number) : null;
}

function planStatusChanges(items, issues, targetId, doneId, mergedAt) {
  const changes = new Set();
  let matched = 0;
  for (const item of items) {
    if (item?.isArchived || !issues.has(itemIssueKey(item))) continue;
    matched++;
    const reopenedAt = Date.parse(item.content.timelineItems?.nodes?.[0]?.createdAt);
    if (Number.isFinite(reopenedAt) && reopenedAt > mergedAt) continue;
    const current = item.fieldValueByName?.optionId;
    if (current !== targetId && current !== doneId) changes.add(item.id);
  }
  return { matched, ids: [...changes] };
}

export async function syncProjectStatus(event, { graphql, dryRun = false }) {
  const pull = mergedPullRequest(event);
  if (!pull) return { skipped: 'unhandled event' };
  const issues = await collectIssues(pull, graphql);
  if (!issues.size) return { skipped: pull.release ? 'release has no Project-Issue lines' : 'no linked issues' };
  return syncIssueCards(issues, pull.status, { graphql, dryRun, mergedAt: pull.mergedAt });
}

export async function writeStatus(graphql, { projectId, fieldId }, itemId, optionId) {
  const result = await graphql(updateMutation, { projectId, fieldId, itemId, optionId });
  if (result.updateProjectV2ItemFieldValue?.projectV2Item?.id !== itemId) {
    throw new Error('GitHub did not confirm the status update.');
  }
}

export async function syncIssueCards(issues, status, { graphql, dryRun = true, mergedAt = Infinity }) {
  const project = await readProject(graphql);
  const items = await projectItems(graphql, project.projectId);
  const optionId = project.options.get(status);
  const { matched, ids } = planStatusChanges(items, issues, optionId, project.options.get('Done'), mergedAt);
  if (!dryRun) {
    for (const id of ids) await writeStatus(graphql, project, id, optionId);
  }
  return { target: status, matched, planned: ids.length, updated: dryRun ? 0 : ids.length, dryRun };
}

export function githubGraphql(token) {
  return async (query, variables) => {
    const name = query.match(/\b(?:query|mutation)\s+(\w+)/)?.[1] ?? 'unknown';
    const response = await fetch('https://api.github.com/graphql', {
      method: 'POST',
      headers: { authorization: 'Bearer ' + token, 'content-type': 'application/json' },
      body: JSON.stringify({ query, variables }),
      signal: AbortSignal.timeout(30000),
    });
    if (!response.ok) throw new Error('GitHub GraphQL ' + name + ' failed (HTTP ' + response.status + ').');
    const result = await response.json();
    if (result.errors?.length || !result.data) {
      throw new Error('GitHub GraphQL ' + name + ' failed. Check GitHub App permissions and Project configuration.');
    }
    return result.data;
  };
}
