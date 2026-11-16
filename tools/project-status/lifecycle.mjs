import {
  issueKey, itemIssueKey, parseIssueReference, projectItem, projectItems, readProject,
  repositoryName, sourceRepository, syncIssueCards, syncProjectStatus, targetStatuses, writeStatus,
} from './sync.mjs';

const intakeLabel = 'project:coffeepeek';
const archiveAge = 30 * 24 * 60 * 60 * 1000;

const issueQuery = `
  query CurrentIssue($owner: String!, $repo: String!, $number: Int!) {
    repository(owner: $owner, name: $repo) {
      issue(number: $number) {
        id number state repository { nameWithOwner }
        labels(first: 100) { nodes { name } }
      }
    }
  }
`;
const addMutation = `
  mutation AddIssueCard($projectId: ID!, $issueId: ID!) {
    addProjectV2ItemById(input: { projectId: $projectId, contentId: $issueId }) { item { id } }
  }
`;
const unarchiveMutation = `
  mutation RestoreIssueCard($projectId: ID!, $itemId: ID!) {
    unarchiveProjectV2Item(input: { projectId: $projectId, itemId: $itemId }) { item { id } }
  }
`;
const archiveMutation = `
  mutation ArchiveIssueCard($projectId: ID!, $itemId: ID!) {
    archiveProjectV2Item(input: { projectId: $projectId, itemId: $itemId }) { item { id } }
  }
`;

function isMobileEvent(event) {
  return repositoryName(event?.repository?.full_name) === sourceRepository;
}

async function currentMobileIssue(graphql, number) {
  const [owner, repo] = sourceRepository.split('/');
  const data = await graphql(issueQuery, { owner, repo, number });
  const issue = data.repository?.issue;
  return issue?.state === 'OPEN' && issueKey(issue.repository?.nameWithOwner, issue.number) === issueKey(sourceRepository, number)
    ? issue : null;
}

async function addToBacklog(issue, key, project, { graphql, dryRun }) {
  if (!issue.labels?.nodes?.some(label => label?.name?.toLowerCase() === intakeLabel)) {
    return { skipped: 'intake label absent' };
  }
  const items = await projectItems(graphql, project.projectId);
  if (items.some(item => itemIssueKey(item) === key)) return { skipped: 'issue already on project' };
  if (dryRun) return { target: 'Backlog', planned: 1, added: 0, dryRun };
  const added = await graphql(addMutation, { projectId: project.projectId, issueId: issue.id });
  const item = await projectItem(graphql, project.projectId, added.addProjectV2ItemById?.item?.id);
  if (itemIssueKey(item) !== key || item.isArchived) throw new Error('GitHub did not return the expected issue card.');
  if (item.content.state === 'OPEN' && !item.fieldValueByName?.optionId) {
    await writeStatus(graphql, project, item.id, project.options.get('Backlog'));
  }
  return { target: 'Backlog', planned: 1, added: 1, dryRun };
}

async function reopenIntoTodo(key, project, { graphql, dryRun }) {
  const items = await projectItems(graphql, project.projectId);
  const done = project.options.get('Done');
  const candidate = items.find(item => itemIssueKey(item) === key && item.fieldValueByName?.optionId === done);
  if (!candidate) return { skipped: 'no Done card for the reopened issue' };
  const current = await projectItem(graphql, project.projectId, candidate.id);
  if (itemIssueKey(current) !== key || current.content.state !== 'OPEN' || current.fieldValueByName?.optionId !== done) {
    return { skipped: 'reopened card no longer eligible' };
  }
  if (dryRun) return { target: 'Todo', planned: 1, updated: 0, dryRun };
  if (current.isArchived) {
    const result = await graphql(unarchiveMutation, { projectId: project.projectId, itemId: current.id });
    if (result.unarchiveProjectV2Item?.item?.id !== current.id) throw new Error('GitHub did not restore the issue card.');
  }
  await writeStatus(graphql, project, current.id, project.options.get('Todo'));
  return { target: 'Todo', planned: 1, updated: 1, dryRun };
}

async function applyIssueEvent(event, options) {
  const action = event?.action;
  if (!isMobileEvent(event) || !['opened', 'labeled', 'reopened'].includes(action)) return { skipped: 'unhandled issue event' };
  if (action === 'labeled' && event.label?.name?.toLowerCase() !== intakeLabel) return { skipped: 'unrelated label' };
  const number = event.issue?.number;
  const key = issueKey(sourceRepository, number);
  if (!key || event.issue.pull_request) return { skipped: 'invalid issue' };
  const issue = await currentMobileIssue(options.graphql, number);
  if (!issue?.id) return { skipped: 'issue is not open' };
  const project = await readProject(options.graphql, action === 'reopened' ? ['Done', 'Todo'] : ['Backlog']);
  return action === 'reopened' ? reopenIntoTodo(key, project, options) : addToBacklog(issue, key, project, options);
}

function atLeastMonthOld(timestamp, now) {
  const time = Date.parse(timestamp);
  return Number.isFinite(time) && now - time >= archiveAge;
}

function canArchive(item, done, now) {
  const status = item?.fieldValueByName;
  const issue = item?.content;
  return itemIssueKey(item) && !item.isArchived && status?.optionId === done &&
    issue.state === 'CLOSED' && issue.stateReason === 'COMPLETED' &&
    [issue.closedAt, item.updatedAt, status.updatedAt].every(value => atLeastMonthOld(value, now));
}

async function archiveCompleted({ graphql, dryRun, now }) {
  const project = await readProject(graphql, ['Done']);
  const done = project.options.get('Done');
  const items = await projectItems(graphql, project.projectId);
  const stale = items.filter(item => canArchive(item, done, now));
  let archived = 0;
  if (!dryRun) {
    for (const item of stale) {
      const current = await projectItem(graphql, project.projectId, item.id);
      if (!canArchive(current, done, now)) continue;
      const result = await graphql(archiveMutation, { projectId: project.projectId, itemId: item.id });
      if (result.archiveProjectV2Item?.item?.id !== item.id) throw new Error('GitHub did not archive the issue card.');
      archived++;
    }
  }
  return { target: 'Archive', planned: stale.length, archived, dryRun };
}

export async function runProjectEvent(name, event, { graphql, dryRun = true, archiveEnabled = false, now = Date.now() }) {
  if (!isMobileEvent(event)) return { skipped: 'unhandled repository' };
  if (name === 'pull_request') return syncProjectStatus(event, { graphql, dryRun });
  if (name === 'issues') return applyIssueEvent(event, { graphql, dryRun });
  if (name === 'schedule' && archiveEnabled) return archiveCompleted({ graphql, dryRun, now });
  if (name === 'workflow_dispatch') {
    const status = targetStatuses.get(event.inputs?.target);
    if (!status) throw new Error('Choose develop or main for the read-only preview.');
    const key = parseIssueReference(event.inputs?.issue);
    return syncIssueCards(new Set([key]), status, { graphql, dryRun: true });
  }
  return { skipped: 'event disabled or unhandled' };
}
