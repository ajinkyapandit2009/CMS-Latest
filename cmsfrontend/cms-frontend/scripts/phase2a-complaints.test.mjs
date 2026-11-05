import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'
import { formatComplaintDate, selectComplaints } from '../src/services/complaintListUtils.js'

globalThis.window = { location: { protocol: 'https:', hostname: 'localhost' }, matchMedia: () => ({ matches: false }) }
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true }, appType: 'custom' })
after(() => server.close())
const { default: App } = await server.ssrLoadModule('/src/App.jsx')
const { default: ComplaintList } = await server.ssrLoadModule('/src/components/ComplaintList.jsx')
const { AuthContext } = await server.ssrLoadModule('/src/context/AuthStore.js')
const { getAccessibleComplaints, getComplaint } = await server.ssrLoadModule('/src/services/complaintService.js')

const complaints = [
  { complaintId: 1, title: 'Zeta drain', category: 'Drain', address: 'North Road', status: 'SUBMITTED', priority: 'HIGH', createdAt: '2026-01-01T10:00:00Z' },
  { complaintId: 2, title: 'Alpha road', category: 'Road', address: 'South Road', status: 'ASSIGNED', priority: 'MEDIUM', officerId: 7, officerName: 'Officer Fixture', createdAt: '2026-01-02T10:00:00Z' },
  { complaintId: 3, title: 'Beta water', category: 'Water', address: 'East Road', status: 'RESOLVED', priority: 'HIGH', createdAt: '2026-01-03T10:00:00Z' },
]
const ids = (items) => items.map((item) => item.complaintId)

test('search is case-insensitive and covers ID, title, category, address, and officer', () => {
  for (const [search, expected] of [['  ALPHA ', [2]], ['3', [3]], ['Drain', [1]], ['north road', [1]], ['officer fixture', [2]]]) {
    assert.deepEqual(ids(selectComplaints(complaints, { search })), expected)
  }
})

test('status, priority and search are applied together', () => {
  assert.deepEqual(ids(selectComplaints(complaints, { status: 'SUBMITTED', priority: 'HIGH', search: 'north' })), [1])
  assert.deepEqual(ids(selectComplaints(complaints, { status: 'ASSIGNED', priority: 'HIGH' })), [])
})

test('sorting supports newest, oldest, title and descending priority without mutating API data', () => {
  assert.deepEqual(ids(selectComplaints(complaints)), [3, 2, 1])
  assert.deepEqual(ids(selectComplaints(complaints, { sort: 'oldest' })), [1, 2, 3])
  assert.deepEqual(ids(selectComplaints(complaints, { sort: 'title' })), [2, 3, 1])
  assert.deepEqual(ids(selectComplaints(complaints, { sort: 'priority' })), [3, 1, 2])
  assert.deepEqual(ids(complaints), [1, 2, 3])
})

test('missing and malformed dates render a usable fallback', () => {
  assert.equal(formatComplaintDate(null), 'Date unavailable')
  assert.equal(formatComplaintDate('not-a-date'), 'Date unavailable')
})

function render(path, role, component = React.createElement(App)) {
  const user = role ? { firstName: 'List', lastName: 'Fixture', email: 'fixture@example.test', role } : null
  return renderToStaticMarkup(React.createElement(MemoryRouter, { initialEntries: [path] },
    React.createElement(AuthContext.Provider, { value: { user, token: null, isInitializing: false, logout() {} } }, component)))
}

test('list applies URL filters and renders accessible detail links', () => {
  const html = render('/complaints?status=ASSIGNED&priority=MEDIUM', 'ROLE_ADMIN', React.createElement(ComplaintList, { complaints }))
  assert.match(html, /Alpha road/)
  assert.match(html, /href="\/complaints\/2"/)
  assert.match(html, /View details for complaint 2/)
  assert.doesNotMatch(html, /Zeta drain|Beta water/)
  assert.match(html, /scope="col"/)
})

test('unknown URL filter values safely fall back to all complaints', () => {
  const html = render('/complaints?status=UNSUPPORTED&priority=INVALID', 'ROLE_CITIZEN', React.createElement(ComplaintList, { complaints }))
  assert.match(html, /Alpha road/)
  assert.match(html, /Zeta drain/)
  assert.match(html, /Beta water/)
})

test('empty and filtered-empty lists show distinct states', () => {
  assert.match(render('/complaints', 'ROLE_CITIZEN', React.createElement(ComplaintList, { complaints: [] })), /No complaints available/)
  assert.match(render('/complaints?search=missing', 'ROLE_CITIZEN', React.createElement(ComplaintList, { complaints })), /No matching complaints/)
})

for (const role of ['ROLE_CITIZEN', 'ROLE_OFFICER', 'ROLE_ADMIN']) {
  test(`${role} list and details retain the authenticated sidebar and active navigation`, () => {
    for (const path of ['/complaints', '/complaints/17']) {
      const html = render(path, role)
      assert.match(html, /id="dashboard-sidebar"/)
      assert.match(html, /class="dashboard-nav-link is-active"[^>]*aria-current="page"[^>]*href="\/complaints"/)
      assert.equal(html.includes('href="/complaints/new"'), role === 'ROLE_CITIZEN')
    }
  })
}

test('anonymous users cannot render either new page', () => {
  for (const path of ['/complaints', '/complaints/17']) assert.doesNotMatch(render(path, null), /dashboard-sidebar|Loading complaint/)
})

test('malformed and out-of-range record IDs display an invalid reference', () => {
  for (const id of ['invalid', '-1', '0', '9999999999']) assert.match(render(`/complaints/${id}`, 'ROLE_ADMIN'), /This complaint reference is invalid/)
})

test('list requests reuse the correct existing role endpoint and bearer header', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => { calls.push({ url, options }); return new Response('[]', { status: 200 }) }
  try {
    for (const [role, path] of [['ROLE_CITIZEN', '/api/complaints/my'], ['ROLE_OFFICER', '/api/officer/complaints/assigned'], ['ROLE_ADMIN', '/api/admin/complaints']]) {
      await getAccessibleComplaints(role, 'test-bearer')
      assert.equal(calls.at(-1).url, `https://localhost:8080${path}`)
      assert.equal(calls.at(-1).options.headers.Authorization, 'Bearer test-bearer')
    }
    const count = calls.length
    await assert.rejects(getAccessibleComplaints('ROLE_UNKNOWN', 'test-bearer'))
    assert.equal(calls.length, count)
    await getComplaint(17, 'test-bearer')
    assert.equal(calls.at(-1).url, 'https://localhost:8080/api/complaints/17')
    assert.equal(calls.at(-1).options.headers.Authorization, 'Bearer test-bearer')
  } finally {
    globalThis.fetch = priorFetch
  }
})
