import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

globalThis.window = { location: { protocol: 'https:', hostname: 'localhost' }, matchMedia: () => ({ matches: false }) }
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true }, appType: 'custom' })
after(() => server.close())
const { default: App } = await server.ssrLoadModule('/src/App.jsx')
const { AuthContext } = await server.ssrLoadModule('/src/context/AuthStore.js')
const { isEscalationEligible, getMyEscalations, submitEscalation, getAdminEscalations, reviewEscalation } = await server.ssrLoadModule('/src/services/escalationService.js')

const now = Date.parse('2026-11-05T12:00:00Z')
const citizen = { role: 'ROLE_CITIZEN', userId: 7, firstName: 'Test', lastName: 'Citizen' }

test('Citizen eligibility requires ownership and terminal status or 48 elapsed hours', () => {
  const base = { citizenId: 7, status: 'SUBMITTED', createdAt: '2026-11-03T12:00:00Z' }
  assert.equal(isEscalationEligible(base, citizen, now), true)
  assert.equal(isEscalationEligible({ ...base, createdAt: '2026-11-03T12:00:01Z' }, citizen, now), false)
  assert.equal(isEscalationEligible({ ...base, createdAt: null }, citizen, now), false)
  assert.equal(isEscalationEligible({ ...base, status: 'RESOLVED', createdAt: null }, citizen, now), true)
  assert.equal(isEscalationEligible({ ...base, status: 'REJECTED', createdAt: null }, citizen, now), true)
  assert.equal(isEscalationEligible({ ...base, citizenId: 8, status: 'RESOLVED' }, citizen, now), false)
  assert.equal(isEscalationEligible(base, { role: 'ROLE_ADMIN', userId: 7 }, now), false)
})

test('API calls use Phase 5B1 paths, DTO fields, and bearer token', async () => {
  const previous = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(JSON.stringify([]), { status: 200, headers: { 'Content-Type': 'application/json' } })
  }
  try {
    await getMyEscalations('fixture-token')
    await submitEscalation(42, 'Please review', 'fixture-token')
    await getAdminEscalations('PENDING', 'fixture-token')
    await reviewEscalation(9, 'DECLINED', 'Explanation', 'fixture-token')
    assert.deepEqual(calls.map((call) => new URL(call.url).pathname + new URL(call.url).search), [
      '/api/escalations/my', '/api/escalations/complaints/42', '/api/admin/escalations?status=PENDING', '/api/admin/escalations/9/review',
    ])
    assert.deepEqual(calls.map((call) => call.options.method || 'GET'), ['GET', 'POST', 'GET', 'PATCH'])
    assert.ok(calls.every((call) => call.options.headers.Authorization === 'Bearer fixture-token'))
    assert.deepEqual(JSON.parse(calls[1].options.body), { reason: 'Please review' })
    assert.deepEqual(JSON.parse(calls[3].options.body), { status: 'DECLINED', remarks: 'Explanation' })
  } finally {
    globalThis.fetch = previous
  }
})

function render(path, role) {
  const user = role ? { ...citizen, role } : null
  return renderToStaticMarkup(React.createElement(MemoryRouter, { initialEntries: [path] },
    React.createElement(AuthContext.Provider, { value: { user, token: 'fixture', isInitializing: false, logout() {} } }, React.createElement(App))))
}

test('Citizen and Admin escalation pages stay inside the sidebar and remain role-gated', () => {
  const citizenPage = render('/escalations/my', 'ROLE_CITIZEN')
  assert.match(citizenPage, /My Escalations/)
  assert.match(citizenPage, /id="dashboard-sidebar"/)
  assert.match(citizenPage, /href="\/escalations\/my"/)
  assert.doesNotMatch(citizenPage, /href="\/admin\/escalations"/)
  const adminPage = render('/admin/escalations', 'ROLE_ADMIN')
  assert.match(adminPage, /Admin workspace/)
  assert.match(adminPage, /id="dashboard-sidebar"/)
  assert.match(adminPage, /href="\/admin\/escalations"/)
  assert.doesNotMatch(adminPage, /href="\/escalations\/my"/)
  assert.doesNotMatch(render('/admin/escalations', 'ROLE_CITIZEN'), /Admin workspace/)
  assert.doesNotMatch(render('/escalations/my', 'ROLE_OFFICER'), /Track review outcomes/)
  assert.doesNotMatch(render('/admin/escalations', null), /id="dashboard-sidebar"/)
})
