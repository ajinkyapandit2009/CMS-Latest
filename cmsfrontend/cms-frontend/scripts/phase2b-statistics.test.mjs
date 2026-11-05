import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'
import { countComplaints } from '../src/services/complaintStatistics.js'

const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true, hmr: false }, appType: 'custom' })
after(() => server.close())
const { default: ComplaintStatistics } = await server.ssrLoadModule('/src/components/ComplaintStatistics.jsx')

const records = [
  { status: 'SUBMITTED' }, { status: 'SUBMITTED' }, { status: 'ASSIGNED' },
  { status: 'IN_PROGRESS' }, { status: 'RESOLVED' }, { status: 'REJECTED' },
]

function render(path, complaints) {
  return renderToStaticMarkup(React.createElement(MemoryRouter, { initialEntries: [path] },
    React.createElement(ComplaintStatistics, { complaints })))
}

test('counts all six categories from the supplied authorized records', () => {
  assert.deepEqual(countComplaints(records), { total: 6, SUBMITTED: 2, ASSIGNED: 1, IN_PROGRESS: 1, RESOLVED: 1, REJECTED: 1 })
  assert.deepEqual(countComplaints([]), { total: 0, SUBMITTED: 0, ASSIGNED: 0, IN_PROGRESS: 0, RESOLVED: 0, REJECTED: 0 })
})

test('each card links to its status and Total clears list filters', () => {
  const html = render('/complaints?search=water&priority=HIGH&status=REJECTED', records)
  for (const status of ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED']) {
    assert.match(html, new RegExp(`href="/complaints\\?status=${status}"`))
  }
  assert.match(html, /href="\/complaints"/)
  assert.match(html, /aria-label="Submitted: 2 complaints/)
  assert.match(html, /aria-current="page"[^>]*href="\/complaints\?status=REJECTED"/)
  assert.match(html, /aria-label="Total: 6 complaints/)
})

test('counts remain unchanged when the list URL has search and filters', () => {
  const unfiltered = render('/complaints', records)
  const filtered = render('/complaints?search=missing&priority=LOW&status=RESOLVED', records)
  for (const count of ['Total: 6', 'Submitted: 2', 'Assigned: 1', 'In Progress: 1', 'Resolved: 1', 'Rejected: 1']) {
    assert.ok(unfiltered.includes(count))
    assert.ok(filtered.includes(count))
  }
})

test('cards represent only the records returned for a role', () => {
  const citizen = render('/citizen/dashboard', records.slice(0, 2))
  const officer = render('/officer/dashboard', records.slice(2, 5))
  const admin = render('/admin/dashboard', records)
  assert.match(citizen, /Total: 2 complaints/)
  assert.match(officer, /Total: 3 complaints/)
  assert.match(admin, /Total: 6 complaints/)
  assert.match(citizen, /Rejected: 0 complaints/)
})
