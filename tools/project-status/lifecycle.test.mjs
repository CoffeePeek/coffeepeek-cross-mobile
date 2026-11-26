import assert from 'node:assert/strict';
import { test } from 'node:test';
import { runProjectEvent } from './lifecycle.mjs';

const mobile = 'CoffeePeek/coffeepeek-cross-mobile';
const now = Date.parse('2026-11-16T12:00:00Z');
const old = '2026-10-17T12:00:00Z';
const recent = '2026-10-17T12:00:01Z';
const event = (action, label = 'project:coffeepeek') => ({
  repository: { full_name: mobile },
  action,
  issue: { number: 350 },
  label: { name: label },
});

function item(number, options = {}) {
  return {
    id: 'item-' + number,
    project: { id: 'p6' },
    isArchived: options.archived ?? false,
    updatedAt: options.updatedAt ?? old,
    fieldValueByName: options.status === null ? null
      : { optionId: options.status ?? 'done', updatedAt: options.statusAt ?? old },
    content: {
      __typename: 'Issue', number, state: options.state ?? 'CLOSED',
      stateReason: options.reason ?? 'COMPLETED', closedAt: options.closedAt ?? old,
      repository: { nameWithOwner: mobile },
    },
  };
}

function mock({ initial = [], issueState = 'OPEN', labels = ['project:coffeepeek'], latest = {} } = {}) {
  const items = [...initial];
  const mutations = [];
  const calls = [];
  const graphql = async (query, vars) => {
    const operation = query.match(/\b(?:query|mutation)\s+(\w+)/)[1];
    calls.push(operation);
    if (operation === 'CurrentIssue') return { repository: { issue: {
      id: 'issue-350', number: 350, state: issueState, repository: { nameWithOwner: mobile },
      labels: { nodes: labels.map(name => ({ name })) },
    } } };
    if (operation === 'ProjectStatus') return { organization: { projectV2: {
      id: 'p6', field: { id: 'status', options: ['Backlog', 'Todo', 'Preview', 'Done']
        .map(name => ({ name, id: name.toLowerCase() })) },
    } } };
    if (operation === 'ProjectIssues') {
      assert.ok(query.includes('archivedStates: [ARCHIVED, NOT_ARCHIVED]'));
      return { node: { items: { nodes: items, pageInfo: { hasNextPage: false } } } };
    }
    const current = items.find(row => row.id === (vars.id ?? vars.itemId));
    if (operation === 'ProjectIssueItem') return { node: latest[vars.id] ?? current };
    mutations.push({ operation, vars });
    if (operation === 'AddIssueCard') {
      const added = item(350, { state: 'OPEN', status: null });
      items.push(added);
      return { addProjectV2ItemById: { item: { id: added.id } } };
    }
    if (operation === 'SetProjectStatus') {
      current.fieldValueByName = { optionId: vars.optionId };
      return { updateProjectV2ItemFieldValue: { projectV2Item: { id: current.id } } };
    }
    if (operation === 'RestoreIssueCard') {
      current.isArchived = false;
      return { unarchiveProjectV2Item: { item: { id: current.id } } };
    }
    if (operation === 'ArchiveIssueCard') {
      current.isArchived = true;
      return { archiveProjectV2Item: { item: { id: current.id } } };
    }
    assert.fail('Unexpected operation ' + operation);
  };
  return { graphql, calls, mutations, items, now };
}

test('labeled open mobile issue is added to Backlog once, with existing status preserved on replay', async () => {
  const graph = mock();
  await runProjectEvent('issues', event('labeled'), { ...graph, dryRun: false });
  assert.deepEqual(graph.mutations.map(change => change.operation), ['AddIssueCard', 'SetProjectStatus']);
  assert.equal(graph.items[0].fieldValueByName.optionId, 'backlog');
  graph.items[0].fieldValueByName.optionId = 'preview';
  await runProjectEvent('issues', event('opened'), { ...graph, dryRun: false });
  assert.equal(graph.mutations.length, 2);
  assert.equal(graph.items[0].fieldValueByName.optionId, 'preview');
});

test('intake leaves a pre-existing archived issue on the board', async () => {
  const exists = mock({ initial: [item(350, { state: 'OPEN', archived: true })] });
  const result = await runProjectEvent('issues', event('labeled'), { ...exists, dryRun: false });
  assert.deepEqual(result, { skipped: 'issue already on project' });
  assert.equal(exists.mutations.length, 0);
});

test('intake checks the current label and the issue is still open', async () => {
  for (const settings of [{ issueState: 'CLOSED' }, { labels: [] }]) {
    const graph = mock(settings);
    await runProjectEvent('issues', event('labeled'), { ...graph, dryRun: false });
    assert.equal(graph.mutations.length, 0);
  }
  const graph = mock();
  await runProjectEvent('issues', event('labeled', 'unrelated'), { ...graph, dryRun: false });
  assert.equal(graph.calls.length, 0);
});

test('reopen restores an archived Done card to Todo while open and never creates an absent card', async () => {
  const done = item(350, { state: 'OPEN', archived: true });
  const graph = mock({ initial: [done] });
  await runProjectEvent('issues', event('reopened'), { ...graph, dryRun: false });
  assert.deepEqual(graph.mutations.map(change => change.operation), ['RestoreIssueCard', 'SetProjectStatus']);
  assert.equal(done.isArchived, false);
  assert.equal(done.fieldValueByName.optionId, 'todo');
  const absent = mock();
  await runProjectEvent('issues', event('reopened'), { ...absent, dryRun: false });
  assert.equal(absent.mutations.length, 0);
});

test('scheduled archival enforces full age and current completed Done state', async () => {
  const eligible = item(350);
  const nowOpen = item(358, { state: 'OPEN' });
  const graph = mock({ initial: [
    eligible, item(351, { state: 'OPEN' }), item(352, { reason: 'NOT_PLANNED' }),
    item(353, { closedAt: recent }), item(354, { updatedAt: recent }),
    item(355, { statusAt: recent }), item(356, { status: 'preview' }),
    item(357, { archived: true }), item(358),
  ], latest: { [nowOpen.id]: nowOpen } });
  const result = await runProjectEvent('schedule', event(), { ...graph, archiveEnabled: true, dryRun: false });
  assert.deepEqual(result, { target: 'Archive', planned: 2, archived: 1, dryRun: false });
  assert.equal(eligible.isArchived, true);
  assert.equal(graph.mutations[0].vars.itemId, eligible.id);
});

test('archival defaults disabled and respects dry run when opted in', async () => {
  const graph = mock({ initial: [item(350)] });
  assert.deepEqual(await runProjectEvent('schedule', event(), graph), { skipped: 'event disabled or unhandled' });
  assert.equal(graph.calls.length, 0);
  const result = await runProjectEvent('schedule', event(), { ...graph, archiveEnabled: true });
  assert.deepEqual(result, { target: 'Archive', planned: 1, archived: 0, dryRun: true });
  assert.equal(graph.mutations.length, 0);
});

test('manual branch preview is always read-only', async () => {
  const graph = mock({ initial: [item(350, { state: 'OPEN', status: 'todo' })] });
  for (const [target, status] of [['develop', 'Preview'], ['main', 'Done']]) {
    const payload = { repository: { full_name: mobile }, inputs: { issue: '#350', target } };
    const result = await runProjectEvent('workflow_dispatch', payload, { ...graph, dryRun: false });
    assert.deepEqual(result, { target: status, matched: 1, planned: 1, updated: 0, dryRun: true });
  }
  assert.equal(graph.mutations.length, 0);
});
