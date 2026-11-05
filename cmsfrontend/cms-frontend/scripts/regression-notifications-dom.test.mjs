import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { click, closeHarness, flush, load, mount, React } from './domHarness.mjs'

after(closeHarness)
const { default: NotificationBell } = await load('/src/components/NotificationBell.jsx')
const { default: ComplaintDetailsPage } = await load('/src/pages/ComplaintDetailsPage.jsx')
const { AuthContext } = await load('/src/context/AuthStore.js')

function LocationProbe() {
  const location = useLocation()
  return React.createElement('span', { 'data-path': location.pathname }, location.pathname)
}

const user = { role: 'ROLE_CITIZEN', name: 'Fixture Citizen' }
function content(component, path = '/citizen/dashboard') {
  return React.createElement(MemoryRouter, { initialEntries: [path] },
    React.createElement(AuthContext.Provider, { value: { user, token: 'fixture-jwt', isInitializing: false } },
      React.createElement(React.Fragment, null, component, React.createElement(LocationProbe))))
}

test('bell retains 30-second polling, unread count and mark-one navigation', async () => {
  const priorFetch = globalThis.fetch
  const priorInterval = window.setInterval
  const priorClear = window.clearInterval
  const calls = []
  const intervals = []
  window.setInterval = (callback, delay) => { intervals.push(delay); return 123 }
  window.clearInterval = () => {}
  globalThis.fetch = async (url, options) => {
    calls.push({ url, method: options.method || 'GET' })
    if (url.endsWith('/unread-count')) return Response.json({ unreadCount: 1 })
    if (url.endsWith('/notifications/my')) return Response.json([{ notificationId: 8, complaintId: 17, title: 'Complaint assigned', message: 'Fixture assignment', isRead: false, createdAt: '2026-01-01T00:00:00Z' }])
    return Response.json({ notificationId: 8, isRead: true })
  }
  const mounted = await mount(content(React.createElement(NotificationBell)))
  try {
    await flush()
    assert.deepEqual(intervals, [30_000])
    assert.match(mounted.container.textContent, /1/)
    await click(mounted.container.querySelector('button[aria-label="Notifications"]'))
    await flush()
    assert.match(mounted.container.textContent, /Complaint assigned/)
    await click([...mounted.container.querySelectorAll('.notification-panel li button')][0])
    assert.equal(mounted.container.querySelector('[data-path]').textContent, '/complaints/17')
    assert.ok(calls.some((call) => call.url.endsWith('/notifications/8/read') && call.method === 'PATCH'))
    assert.equal(mounted.container.querySelector('.notification-panel'), null)
  } finally {
    await mounted.unmount()
    globalThis.fetch = priorFetch
    window.setInterval = priorInterval
    window.clearInterval = priorClear
  }
})

test('Mark all as read keeps navigation idle and clears count', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, method: options.method || 'GET' })
    if (url.endsWith('/unread-count')) return Response.json({ unreadCount: 2 })
    if (url.endsWith('/notifications/my')) return Response.json([
      { notificationId: 8, complaintId: 17, title: 'First', message: 'Fixture', isRead: false },
      { notificationId: 9, complaintId: 18, title: 'Second', message: 'Fixture', isRead: false },
    ])
    return Response.json({})
  }
  const mounted = await mount(content(React.createElement(NotificationBell)))
  try {
    await click(mounted.container.querySelector('button[aria-label="Notifications"]'))
    await flush()
    await click([...mounted.container.querySelectorAll('.notification-panel button')].find((button) => button.textContent === 'Mark all as read'))
    assert.equal(mounted.container.querySelector('.notification-count'), null)
    assert.equal(mounted.container.querySelectorAll('.notification-panel li.unread').length, 0)
    assert.equal(mounted.container.querySelector('[data-path]').textContent, '/citizen/dashboard')
    assert.ok(calls.some((call) => call.url.endsWith('/notifications/read-all') && call.method === 'PATCH'))
  } finally {
    await mounted.unmount()
    globalThis.fetch = priorFetch
  }
})

test('inaccessible complaint details show a safe unavailable state', async () => {
  const priorFetch = globalThis.fetch
  globalThis.fetch = async () => Response.json({ message: 'Complaint not found' }, { status: 404 })
  const details = React.createElement(Routes, null,
    React.createElement(Route, { path: '/complaints/:complaintId', element: React.createElement(ComplaintDetailsPage) }))
  const mounted = await mount(content(details, '/complaints/17'))
  try {
    await flush()
    assert.match(mounted.container.textContent, /unavailable for your account/i)
    assert.doesNotMatch(mounted.container.textContent, /fixture secret/i)
  } finally {
    await mounted.unmount()
    globalThis.fetch = priorFetch
  }
})
