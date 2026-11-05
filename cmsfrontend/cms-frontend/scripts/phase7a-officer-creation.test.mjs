import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

globalThis.window = { location: { protocol: 'https:', hostname: 'localhost' } }
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true }, appType: 'custom' })
after(() => server.close())
const { createCaseOfficer } = await server.ssrLoadModule('/src/services/adminService.js')
const { default: OfficerCreationForm } = await server.ssrLoadModule('/src/components/OfficerCreationForm.jsx')
const { AuthContext } = await server.ssrLoadModule('/src/context/AuthStore.js')

test('atomic Admin API sends account details once and returns employee number', async () => {
  const previous = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response('{"userId":9,"employeeNumber":"CMS-OFF-1001"}', { status: 201 })
  }
  try {
    const details = { firstName: 'New', lastName: 'Officer', email: 'new@example.test', password: 'StrongPass9', confirmPassword: 'StrongPass9' }
    const result = await createCaseOfficer(details, 'fixture-token')
    assert.equal(result.employeeNumber, 'CMS-OFF-1001')
    assert.equal(calls.length, 1)
    assert.equal(calls[0].url, 'https://localhost:8080/api/admin/case-officers/with-account')
    assert.equal(calls[0].options.method, 'POST')
    assert.equal(calls[0].options.headers.Authorization, 'Bearer fixture-token')
    assert.deepEqual(JSON.parse(calls[0].options.body), details)
  } finally {
    globalThis.fetch = previous
  }
})

test('officer form describes one linked creation operation', () => {
  const html = renderToStaticMarkup(React.createElement(AuthContext.Provider,
    { value: { token: 'fixture-token' } }, React.createElement(OfficerCreationForm)))
  assert.match(html, /Creates a staff login account and its linked case-officer profile together/)
  assert.match(html, /Create case officer/)
  assert.doesNotMatch(html, /then its linked|profile creation failed/)
})
