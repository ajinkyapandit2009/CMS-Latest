// Isolated server-rendered route checks. Fixtures never authenticate against the backend.
import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

globalThis.window = {
  location: { protocol: 'https:', hostname: 'localhost' },
  matchMedia: () => ({ matches: false }),
}

const server = await createServer({
  configFile: false,
  plugins: [react()],
  server: { middlewareMode: true },
  appType: 'custom',
})
after(() => server.close())

const { default: App } = await server.ssrLoadModule('/src/App.jsx')
const { AuthContext } = await server.ssrLoadModule('/src/context/AuthStore.js')
const roles = [
  ['ROLE_CITIZEN', '/citizen/dashboard'],
  ['ROLE_OFFICER', '/officer/dashboard'],
  ['ROLE_ADMIN', '/admin/dashboard'],
]

function render(path, role = null, isInitializing = false) {
  const user = role ? { firstName: 'Navigation', lastName: 'Fixture', name: 'Navigation Fixture', email: 'fixture@example.test', role } : null
  const auth = { user, token: null, isInitializing, logout() {} }
  return renderToStaticMarkup(React.createElement(MemoryRouter, { initialEntries: [path] },
    React.createElement(AuthContext.Provider, { value: auth }, React.createElement(App))))
}

for (const [role, dashboard] of roles) {
  test(`${role}: dashboard keeps sidebar, current user, notification bell, and logout`, () => {
    const html = render(dashboard, role)
    assert.match(html, /id="dashboard-sidebar"/)
    assert.match(html, /Navigation Fixture/)
    assert.match(html, /aria-label="Notifications"/)
    assert.match(html, /aria-label="Logout"/)
    assert.match(html, /aria-current="page"/)
    assert.equal(html.includes('href="/complaints/new"'), role === 'ROLE_CITIZEN')
  })

  test(`${role}: profile page remains in the dashboard shell`, () => {
    const html = render('/profile', role)
    assert.match(html, /id="dashboard-sidebar"/)
    assert.match(html, /<h1>My Profile<\/h1>/)
    assert.match(html, /Loading your profile/)
  })

  test(`anonymous requests cannot render ${dashboard}`, () => {
    const html = render(dashboard)
    assert.doesNotMatch(html, /dashboard-sidebar|dashboard-main|Navigation Fixture/)
  })

  test(`${role}: homepage report action follows role rules`, () => {
    const html = render('/', role)
    assert.equal(html.includes('href="/complaints/new"'), role === 'ROLE_CITIZEN')
    assert.match(html, /href="\/#how-it-works"/)
  })
}

test('citizen complaint creation uses the shared layout and existing capture workflow', () => {
  const html = render('/complaints/new', 'ROLE_CITIZEN')
  assert.match(html, /id="dashboard-sidebar"/)
  assert.match(html, /<h1>Raise a Complaint<\/h1>/)
  assert.match(html, /Take required photo/)
  assert.match(html, /Verified address/)
})

test('case officer creation is available in the admin workspace only', () => {
  const path = '/admin/case-officers/new'
  const admin = render(path, 'ROLE_ADMIN')
  assert.match(admin, /id="dashboard-sidebar"/)
  assert.match(admin, /class="dashboard-nav-link is-active"[^>]*aria-current="page"[^>]*href="\/admin\/case-officers\/new"/)
  assert.match(admin, /Create a case officer/)
  assert.match(admin, /Create case officer<\/button>/)
  for (const role of [null, 'ROLE_CITIZEN', 'ROLE_OFFICER']) {
    assert.doesNotMatch(render(path, role), /officer-creation-form|Case officers are refreshed/)
  }
  for (const role of ['ROLE_CITIZEN', 'ROLE_OFFICER']) {
    assert.doesNotMatch(render('/profile', role), /href="\/admin\/case-officers\/new"/)
  }
})

for (const role of [null, 'ROLE_ADMIN', 'ROLE_OFFICER']) {
  test(`${role || 'anonymous'} cannot render the complaint creation form`, () => {
    assert.doesNotMatch(render('/complaints/new', role), /complaint-form-page|Take required photo/)
  })
}

test('restoring a session does not expose dashboard contents', () => {
  const html = render('/admin/dashboard', null, true)
  assert.match(html, /Restoring your session/)
  assert.doesNotMatch(html, /dashboard-sidebar/)
})

test('unknown roles cannot render an authenticated workspace', () => {
  assert.doesNotMatch(render('/profile', 'ROLE_UNKNOWN'), /dashboard-sidebar|profile-summary/)
})

test('phone sharing stays public and has no authenticated controls', () => {
  const html = render('/share-location/test-only-token')
  assert.match(html, /Share your current location/)
  assert.match(html, /Share My Location/)
  assert.doesNotMatch(html, /dashboard-sidebar|site-header|Notifications|Logout/)
})

test('anonymous profile requests cannot display account information', () => {
  assert.doesNotMatch(render('/profile'), /profile-summary|dashboard-sidebar/)
})
