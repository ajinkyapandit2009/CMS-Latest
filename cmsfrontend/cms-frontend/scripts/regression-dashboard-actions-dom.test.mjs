import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { MemoryRouter } from 'react-router-dom'
import { change, click, closeHarness, flush, load, mount, React } from './domHarness.mjs'

after(closeHarness)
const { default: AdminDashboard } = await load('/src/pages/AdminDashboardPlaceholder.jsx')
const { default: OfficerDashboard } = await load('/src/pages/OfficerDashboardPlaceholder.jsx')
const { default: CitizenDashboard } = await load('/src/pages/CitizenDashboardPlaceholder.jsx')
const { AuthContext } = await load('/src/context/AuthStore.js')

function page(role, component, path) {
  const user = { role, name: 'Safe Fixture' }
  return React.createElement(MemoryRouter, { initialEntries: [path] },
    React.createElement(AuthContext.Provider, { value: { user, token: 'fixture-jwt' } }, React.createElement(component)))
}

function count(container, label) {
  const card = [...container.querySelectorAll('.complaint-stat-card')]
    .find((item) => item.querySelector('.complaint-stat-label')?.textContent === label)
  assert.ok(card, `Missing ${label} statistics card`)
  return Number(card.querySelector('.complaint-stat-count').textContent)
}

function fixture(status = 'SUBMITTED') {
  return { complaintId: 17, title: 'Safe fixture issue', category: 'Roads', description: 'Fixture description',
    address: 'Fixture Road', citizenId: 1, citizenName: 'Fixture Citizen', officerId: null, officerName: null,
    status, priority: 'MEDIUM', createdAt: '2026-01-01T00:00:00Z', imageUrl: null }
}

test('admin cards ignore management filters and update after priority and assignment actions', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  const complaint = fixture()
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url.endsWith('/api/admin/complaints') && (!options.method || options.method === 'GET')) return Response.json([complaint])
    if (url.endsWith('/api/admin/case-officers')) return Response.json([{ userId: 3, firstName: 'Case', lastName: 'Officer', employeeNumber: 'CMS-OFF-003' }])
    if (url.endsWith('/api/admin/complaints/17/priority')) { complaint.priority = JSON.parse(options.body).priority; return Response.json(complaint) }
    if (url.endsWith('/api/admin/complaints/17/assign')) { complaint.status = 'ASSIGNED'; complaint.officerId = 3; complaint.officerName = 'Case Officer'; return Response.json(complaint) }
    throw new Error(`Unexpected request: ${url}`)
  }
  const mounted = await mount(page('ROLE_ADMIN', AdminDashboard, '/admin/dashboard'))
  try {
    await flush()
    assert.equal(count(mounted.container, 'Total'), 1)
    assert.equal(count(mounted.container, 'Submitted'), 1)
    assert.equal(count(mounted.container, 'Assigned'), 0)
    await change(mounted.container.querySelector('.admin-filters select[name="status"]'), 'REJECTED')
    assert.match(mounted.container.textContent, /No matching complaints/)
    assert.equal(count(mounted.container, 'Submitted'), 1)
    assert.equal(calls.filter((call) => call.url.endsWith('/api/admin/complaints')).length, 1, 'filter should not refetch a subset')
    await change(mounted.container.querySelector('.admin-filters select[name="status"]'), '')
    await change(mounted.container.querySelector('.priority-editor select'), 'HIGH')
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Save priority'))
    await flush()
    assert.equal(complaint.priority, 'HIGH')
    const priorityCall = calls.find((call) => call.url.endsWith('/api/admin/complaints/17/priority'))
    assert.equal(priorityCall.options.method, 'PATCH')
    assert.equal(priorityCall.options.headers.Authorization, 'Bearer fixture-jwt')
    await change(mounted.container.querySelector('.assignment-editor select'), '3')
    await change(mounted.container.querySelector('.assignment-editor input'), 'Fixture assignment')
    await React.act(async () => { mounted.container.querySelector('.assignment-editor').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true })) })
    await flush()
    assert.equal(count(mounted.container, 'Submitted'), 0)
    assert.equal(count(mounted.container, 'Assigned'), 1)
    const assignment = calls.find((call) => call.url.endsWith('/api/admin/complaints/17/assign'))
    assert.deepEqual(JSON.parse(assignment.options.body), { officerId: 3, remarks: 'Fixture assignment' })
  } finally { await mounted.unmount(); globalThis.fetch = priorFetch }
})

test('officer status action refreshes assigned complaint statistics', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  const complaint = fixture('ASSIGNED')
  complaint.officerId = 3
  complaint.officerName = 'Case Officer'
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url.endsWith('/api/officer/complaints/assigned')) return Response.json([complaint])
    if (url.endsWith('/api/officer/complaints/17/status')) { complaint.status = JSON.parse(options.body).status; return Response.json(complaint) }
    throw new Error(`Unexpected request: ${url}`)
  }
  const mounted = await mount(page('ROLE_OFFICER', OfficerDashboard, '/officer/dashboard'))
  try {
    await flush()
    assert.equal(count(mounted.container, 'Assigned'), 1)
    await React.act(async () => { mounted.container.querySelector('.status-editor').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true })) })
    await flush()
    assert.equal(count(mounted.container, 'Assigned'), 0)
    assert.equal(count(mounted.container, 'In Progress'), 1)
    const statusCall = calls.find((call) => call.url.endsWith('/api/officer/complaints/17/status'))
    assert.deepEqual(JSON.parse(statusCall.options.body), { status: 'IN_PROGRESS', remarks: null })
    assert.equal(statusCall.options.headers.Authorization, 'Bearer fixture-jwt')
  } finally { await mounted.unmount(); globalThis.fetch = priorFetch }
})

test('citizen dashboard counts only its own endpoint response', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url) => { calls.push(url); return Response.json([fixture('RESOLVED')]) }
  const mounted = await mount(page('ROLE_CITIZEN', CitizenDashboard, '/citizen/dashboard'))
  try {
    await flush()
    assert.equal(count(mounted.container, 'Total'), 1)
    assert.equal(count(mounted.container, 'Resolved'), 1)
    assert.equal(count(mounted.container, 'Submitted'), 0)
    assert.ok(calls.every((url) => url.endsWith('/api/complaints/my')))
  } finally { await mounted.unmount(); globalThis.fetch = priorFetch }
})
