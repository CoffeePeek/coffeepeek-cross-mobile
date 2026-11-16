import assert from 'node:assert/strict';
import { test } from 'node:test';
import { syncProjectStatus } from './sync.mjs';

const mobile = 'CoffeePeek/coffeepeek-cross-mobile';
const tracker = 'CoffeePeek/coffeepeek-tracker';
const backend = 'CoffeePeek/coffeepeek-backend';

function mergedEvent(branch, body = '', head = 'fix/example') {
  return {
    action: 'closed',
    repository: { full_name: mobile },
    pull_request: {
      number: 92,
      merged: true,
      body,
      base: { ref: branch, repo: { full_name: mobile } },
      head: { ref: head, repo: { full_name: mobile } },
    },
  };
}

const linkedIssue = (repo, number) => ({ repository: { nameWithOwner: repo }, number });

function card(repo, number, optionId, extra = {}) {
  return {
    id: repo + '#' + number,
    isArchived: false,
    content: { __typename: 'Issue', ...linkedIssue(repo, number) },
    fieldValueByName: optionId ? { optionId } : null,
    ...extra,
  };
}

function page(pages, cursor) {
  const index = cursor === null ? 0 : Number(cursor);
  const next = index + 1 < pages.length;
  return {
    nodes: pages[index],
    pageInfo: { hasNextPage: next, endCursor: next ? String(index + 1) : null },
  };
}

function mockProject({ links = [[]], items = [[]], project } = {}) {
  const calls = [];
  const mutations = [];
  const graphql = async (query, vars) => {
    const operation = query.match(/\b(?:query|mutation)\s+(\w+)/)[1];
    calls.push({ operation, vars });
    switch (operation) {
      case 'LinkedIssues':
        return { repository: { pullRequest: { closingIssuesReferences: page(links, vars.cursor) } } };
      case 'ProjectStatus':
        assert.deepEqual(vars, { owner: 'CoffeePeek', number: 6 });
        return { organization: { projectV2: project ?? {
          id: 'p6',
          field: { id: 'status', options: [{ id: 'preview', name: 'Preview' }, { id: 'done', name: 'Done' }] },
        } } };
      case 'ProjectIssues':
        assert.equal(vars.projectId, 'p6');
        return { node: { items: page(items, vars.cursor) } };
      case 'SetProjectStatus':
        mutations.push(vars);
        items.flat().find(item => item.id === vars.itemId).fieldValueByName = { optionId: vars.optionId };
        return { updateProjectV2ItemFieldValue: { projectV2Item: { id: vars.itemId } } };
      default:
        assert.fail('Unexpected GraphQL operation: ' + operation);
    }
  };
  return { graphql, calls, mutations };
}

test('unmerged, non-target, and unrelated repository events leave the project untouched', async () => {
  const closed = mergedEvent('develop', 'Project-Issue: #12');
  closed.pull_request.merged = false;
  const other = mergedEvent('main');
  other.repository.full_name = tracker;
  const mock = mockProject();
  for (const event of [closed, mergedEvent('feature'), other, { ...closed, action: 'opened' }]) {
    assert.deepEqual(await syncProjectStatus(event, mock), { skipped: 'unhandled event' });
  }
  assert.equal(mock.calls.length, 0);
});

test('develop moves existing linked and explicit issue cards across both pages only', async () => {
  const existingMobile = card(mobile, 12, null);
  const existingTracker = card(tracker, 7, 'working');
  const mock = mockProject({
    links: [[linkedIssue(tracker, 7), linkedIssue(tracker, 9)], [linkedIssue(backend, 10), linkedIssue(tracker, 999)]],
    items: [
      [card(mobile, 99, 'working'), card(tracker, 9, null, { isArchived: true }), existingMobile],
      [card(backend, 10, null, { content: { __typename: 'PullRequest', ...linkedIssue(backend, 10) } }), existingTracker],
    ],
  });
  const result = await syncProjectStatus(mergedEvent('develop',
    'Project-Issue: #12\n- Project-Issue: CoffeePeek/coffeepeek-tracker#7'), mock);
  assert.deepEqual(result, { target: 'Preview', matched: 2, planned: 2, updated: 2, dryRun: false });
  assert.deepEqual(mock.mutations, [existingMobile, existingTracker].map(item => ({
    projectId: 'p6', fieldId: 'status', itemId: item.id, optionId: 'preview',
  })));
  assert.equal(mock.calls.filter(call => call.operation === 'LinkedIssues').length, 2);
  assert.equal(mock.calls.filter(call => call.operation === 'ProjectIssues').length, 2);
});

test('Done is idempotent and a later develop event cannot return the card to Preview', async () => {
  const item = card(tracker, 7, 'preview');
  const mock = mockProject({ items: [[item]], links: [[linkedIssue(tracker, 7)]] });
  const main = mergedEvent('main');
  await syncProjectStatus(main, mock);
  assert.equal((await syncProjectStatus(main, mock)).updated, 0);
  assert.equal((await syncProjectStatus(mergedEvent('develop'), mock)).updated, 0);
  assert.equal(item.fieldValueByName.optionId, 'done');
  assert.deepEqual(mock.mutations.map(update => update.optionId), ['done']);
});

test('develop release reads only visible explicit Project-Issue lines', async () => {
  const item = card(tracker, 7, 'preview');
  const body = [
    'Notes mentioning #23',
    '<!-- Project-Issue: #15 -->',
    '~~~text',
    'Project-Issue: #16',
    '~~~',
    'Project-Issue: CoffeePeek/coffeepeek-tracker#7',
  ].join('\n');
  const mock = mockProject({
    links: [[linkedIssue(mobile, 23)]],
    items: [[item, card(mobile, 15, 'preview'), card(mobile, 16, 'preview'), card(mobile, 23, 'preview')]],
  });
  assert.equal((await syncProjectStatus(mergedEvent('main', body, 'develop'), mock)).updated, 1);
  assert.equal(mock.calls.some(call => call.operation === 'LinkedIssues'), false);
  assert.equal(mock.mutations[0].itemId, item.id);
});

test('a release without explicit issues does not advance the whole Preview column', async () => {
  const mock = mockProject({ items: [[card(tracker, 7, 'preview')]] });
  assert.deepEqual(await syncProjectStatus(mergedEvent('main', 'Release notes with #7', 'develop'), mock),
    { skipped: 'release has no Project-Issue lines' });
  assert.equal(mock.calls.length, 0);
});

test('dry run resolves cards and reports proposed changes without mutating them', async () => {
  const item = card(tracker, 7, 'preview');
  const mock = mockProject({ links: [[linkedIssue(tracker, 7)]], items: [[item]] });
  const result = await syncProjectStatus(mergedEvent('main'), { ...mock, dryRun: true });
  assert.deepEqual(result, { target: 'Done', matched: 1, planned: 1, updated: 0, dryRun: true });
  assert.equal(item.fieldValueByName.optionId, 'preview');
  assert.equal(mock.mutations.length, 0);
});

test('an invalid explicit line cannot silently move an issue in another organization', async () => {
  const mock = mockProject();
  await assert.rejects(syncProjectStatus(mergedEvent('develop',
    'Project-Issue: #7\nProject-Issue: SomeoneElse/tracker#7'), mock), /Invalid Project-Issue line/);
  assert.equal(mock.calls.length, 0);
});

test('a misconfigured Status field stops before any write', async () => {
  const mock = mockProject({
    links: [[linkedIssue(tracker, 7)]],
    project: { id: 'p6', field: { id: 'status', options: [{ id: 'done', name: 'Done' }] } },
  });
  await assert.rejects(syncProjectStatus(mergedEvent('main'), mock), /Status field with Preview and Done/);
  assert.equal(mock.mutations.length, 0);
});
