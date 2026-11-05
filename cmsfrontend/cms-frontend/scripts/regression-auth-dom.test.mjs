import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import { change, click, closeHarness, flush, load, mount, React } from './domHarness.mjs'

after(closeHarness)
const { default: App } = await load('/src/App.jsx')
const { AuthProvider } = await load('/src/context/AuthContext.jsx')

let navigateTo
function RouteProbe() {
  navigateTo = useNavigate()
  const location = useLocation()
  return React.createElement('span', { 'data-path': location.pathname }, location.pathname)
}

function page(initialPath) {
  return React.createElement(MemoryRouter, { initialEntries: [initialPath] },
    React.createElement(AuthProvider, null, React.createElement(React.Fragment, null,
      React.createElement(App), React.createElement(RouteProbe))))
}

for (const [role, dashboard] of [
  ['ROLE_CITIZEN', '/citizen/dashboard'],
  ['ROLE_OFFICER', '/officer/dashboard'],
  ['ROLE_ADMIN', '/admin/dashboard'],
]) {
  test(`${role} login verifies /auth/me, reaches its own dashboard, and logout clears access`, async () => {
    localStorage.clear()
    const priorFetch = globalThis.fetch
    const calls = []
    globalThis.fetch = async (url, options) => {
      calls.push({ url, options })
      if (url.endsWith('/auth/login')) return Response.json({ token: 'fixture-jwt' })
      if (url.endsWith('/auth/me')) return Response.json({ firstName: 'Safe', lastName: 'Fixture', email: 'fixture@example.test', role })
      if (url.endsWith('/api/notifications/unread-count')) return Response.json({ unreadCount: 0 })
      if (url.includes('/api/complaints') || url.includes('/api/admin/case-officers') || url.includes('/api/officer/complaints/assigned')) return Response.json([])
      throw new Error(`Unexpected request: ${url}`)
    }
    const mounted = await mount(page('/login'))
    try {
      await flush()
      await change(mounted.container.querySelector('[name="email"]'), 'fixture@example.test')
      await change(mounted.container.querySelector('[name="password"]'), 'fixture-password')
      await React.act(async () => {
        mounted.container.querySelector('form.auth-form').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))
      })
      await flush()
      assert.equal(mounted.container.querySelector('[data-path]').textContent, dashboard)
      assert.match(mounted.container.textContent, /Safe Fixture/)
      assert.equal(localStorage.getItem('cms_auth_token'), 'fixture-jwt')
      assert.ok(calls.some((call) => call.url.endsWith('/auth/login') && call.options.method === 'POST'))
      assert.ok(calls.some((call) => call.url.endsWith('/auth/me') && call.options.headers.Authorization === 'Bearer fixture-jwt'))
      assert.equal(Boolean(mounted.container.querySelector('a[href="/complaints/new"]')), role === 'ROLE_CITIZEN')

      await React.act(async () => { navigateTo('/admin/dashboard') })
      await flush()
      assert.equal(mounted.container.querySelector('[data-path]').textContent, role === 'ROLE_ADMIN' ? '/admin/dashboard' : dashboard)
      await click(mounted.container.querySelector('button[aria-label="Logout"]'))
      await flush()
      assert.equal(localStorage.getItem('cms_auth_token'), null)
      assert.equal(mounted.container.querySelector('[data-path]').textContent, '/login')
    } finally {
      await mounted.unmount()
      globalThis.fetch = priorFetch
      localStorage.clear()
    }
  })
}

test('anonymous protected route redirects to shared sign-in before dashboard data loads', async () => {
  localStorage.clear()
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url) => { calls.push(url); throw new Error('No API call expected') }
  const mounted = await mount(page('/officer/dashboard'))
  try {
    await flush()
    assert.equal(mounted.container.querySelector('[data-path]').textContent, '/login')
    assert.equal(calls.length, 0)
    assert.equal(mounted.container.querySelector('#dashboard-sidebar'), null)
  } finally {
    await mounted.unmount()
    globalThis.fetch = priorFetch
  }
})
