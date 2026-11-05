import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { change, click, closeHarness, flush, load, mount, React } from './domHarness.mjs'

after(closeHarness)
const { default: ComplaintForm } = await load('/src/pages/ComplaintPlaceholder.jsx')
const { default: ShareLocation } = await load('/src/pages/ShareLocation.jsx')
const { AuthContext } = await load('/src/context/AuthStore.js')

function button(container, text) {
  return [...container.querySelectorAll('button')].find((item) => item.textContent.trim() === text)
}

function mockCamera() {
  const priorMedia = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices')
  const priorPlay = window.HTMLMediaElement.prototype.play
  const priorContext = window.HTMLCanvasElement.prototype.getContext
  const priorBlob = window.HTMLCanvasElement.prototype.toBlob
  const priorWidth = Object.getOwnPropertyDescriptor(window.HTMLVideoElement.prototype, 'videoWidth')
  const priorHeight = Object.getOwnPropertyDescriptor(window.HTMLVideoElement.prototype, 'videoHeight')
  const priorCreate = URL.createObjectURL
  const priorRevoke = URL.revokeObjectURL
  const tracks = []
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
    async getUserMedia() { const track = { stopped: false, stop() { this.stopped = true } }; tracks.push(track); return { getTracks: () => [track] } },
    async enumerateDevices() { return [{ kind: 'videoinput', deviceId: 'fixture-camera', label: 'Fixture camera' }] },
  } })
  window.HTMLMediaElement.prototype.play = async () => {}
  Object.defineProperty(window.HTMLVideoElement.prototype, 'videoWidth', { configurable: true, get: () => 640 })
  Object.defineProperty(window.HTMLVideoElement.prototype, 'videoHeight', { configurable: true, get: () => 480 })
  window.HTMLCanvasElement.prototype.getContext = () => ({ drawImage() {} })
  window.HTMLCanvasElement.prototype.toBlob = (callback) => callback(new Blob(['fixture camera bytes'], { type: 'image/jpeg' }))
  let nextUrl = 0
  URL.createObjectURL = () => `blob:fixture-${++nextUrl}`
  URL.revokeObjectURL = () => {}
  return { tracks, restore() {
    if (priorMedia) Object.defineProperty(navigator, 'mediaDevices', priorMedia)
    else delete navigator.mediaDevices
    window.HTMLMediaElement.prototype.play = priorPlay
    if (priorWidth) Object.defineProperty(window.HTMLVideoElement.prototype, 'videoWidth', priorWidth)
    if (priorHeight) Object.defineProperty(window.HTMLVideoElement.prototype, 'videoHeight', priorHeight)
    window.HTMLCanvasElement.prototype.getContext = priorContext
    window.HTMLCanvasElement.prototype.toBlob = priorBlob
    URL.createObjectURL = priorCreate
    URL.revokeObjectURL = priorRevoke
  } }
}

test('mocked mobile flow captures, geolocates, uploads once, resolves address, and creates once', async () => {
  const camera = mockCamera()
  const priorAgent = Object.getOwnPropertyDescriptor(navigator, 'userAgent')
  const priorLocation = Object.getOwnPropertyDescriptor(navigator, 'geolocation')
  const priorFetch = globalThis.fetch
  Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'Fixture iPhone Mobile' })
  Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
    getCurrentPosition(success) { success({ coords: { latitude: 18.52, longitude: 73.85, accuracy: 12 } }) },
  } })
  const calls = []
  const imageUrl = '/api/complaint-photos/123e4567-e89b-12d3-a456-426614174000/content'
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url.endsWith('/api/complaint-photos')) return Response.json({ uploadId: '123e4567-e89b-12d3-a456-426614174000', imageUrl, latitude: 18.52, longitude: 73.85, accuracy: 12 })
    if (url.startsWith('https://nominatim.openstreetmap.org/reverse')) return Response.json({ display_name: 'Fixture Road, Pune' })
    if (url.endsWith('/api/complaints')) return Response.json({ complaintId: 17, title: 'Fixture pothole' })
    throw new Error(`Unexpected request: ${url}`)
  }
  const element = React.createElement(MemoryRouter, { initialEntries: ['/complaints/new'] },
    React.createElement(AuthContext.Provider, { value: { token: 'fixture-jwt', user: { role: 'ROLE_CITIZEN' } } }, React.createElement(ComplaintForm)))
  const mounted = await mount(element)
  try {
    await change(mounted.container.querySelector('[name="title"]'), 'Fixture pothole')
    await change(mounted.container.querySelector('[name="description"]'), 'Safe mocked complaint description')
    await change(mounted.container.querySelector('[name="category"]'), 'Potholes and damaged roads')
    await click(button(mounted.container, 'Take required photo'))
    await click(button(mounted.container, 'Open camera'))
    await click(button(mounted.container, 'Capture photo'))
    await click(button(mounted.container, 'Confirm photo'))
    await click(button(mounted.container, "Use This Device's Location"))
    await flush()
    assert.equal(mounted.container.querySelector('[name="address"]').value, 'Fixture Road, Pune')
    assert.equal(mounted.container.querySelector('[name="address"]').readOnly, true)
    assert.equal(calls.filter((call) => call.url.endsWith('/api/complaint-photos')).length, 1)
    const upload = calls.find((call) => call.url.endsWith('/api/complaint-photos'))
    assert.equal(upload.options.body.get('latitude'), '18.52')
    assert.equal(upload.options.body.get('longitude'), '73.85')
    assert.equal(upload.options.body.get('accuracy'), '12')
    assert.equal(upload.options.headers.Authorization, 'Bearer fixture-jwt')
    assert.equal(mounted.container.querySelector('input[type="file"]'), null)
    assert.equal(button(mounted.container, 'Submit complaint').disabled, false)
    await React.act(async () => {
      mounted.container.querySelector('form.complaint-form').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))
    })
    await flush()
    const submissions = calls.filter((call) => call.url.endsWith('/api/complaints'))
    assert.equal(submissions.length, 1)
    const payload = JSON.parse(submissions[0].options.body)
    assert.deepEqual({ title: payload.title, address: payload.address, latitude: payload.latitude, longitude: payload.longitude, imageUrl: payload.imageUrl },
      { title: 'Fixture pothole', address: 'Fixture Road, Pune', latitude: 18.52, longitude: 73.85, imageUrl })
    assert.equal(payload.priority, undefined)
    assert.equal(payload.status, undefined)
    assert.match(mounted.container.textContent, /Thank you for reporting this issue/)
    assert.ok(camera.tracks.every((track) => track.stopped))
  } finally {
    await mounted.unmount()
    camera.restore()
    if (priorAgent) Object.defineProperty(navigator, 'userAgent', priorAgent)
    else delete navigator.userAgent
    if (priorLocation) Object.defineProperty(navigator, 'geolocation', priorLocation)
    else delete navigator.geolocation
    globalThis.fetch = priorFetch
  }
})

test('public phone QR page retries denied GPS and submits once without JWT', async () => {
  const priorLocation = Object.getOwnPropertyDescriptor(navigator, 'geolocation')
  const priorFetch = globalThis.fetch
  let attempts = 0
  Object.defineProperty(navigator, 'geolocation', { configurable: true, value: {
    getCurrentPosition(success, error) {
      attempts += 1
      if (attempts === 1) error({ code: 1 })
      else success({ coords: { latitude: 18.52, longitude: 73.85, accuracy: 12 } })
    },
  } })
  const calls = []
  globalThis.fetch = async (url, options) => { calls.push({ url, options }); return Response.json({ status: 'COMPLETED' }) }
  const route = React.createElement(Routes, null,
    React.createElement(Route, { path: '/share-location/:token', element: React.createElement(ShareLocation) }))
  const mounted = await mount(React.createElement(MemoryRouter, { initialEntries: ['/share-location/fixture-token'] }, route))
  try {
    await click(button(mounted.container, 'Share My Location'))
    assert.match(mounted.container.textContent, /permission was denied/i)
    await click(button(mounted.container, 'Retry Sharing'))
    assert.match(mounted.container.textContent, /Location shared successfully/i)
    assert.equal(calls.length, 1)
    assert.equal(calls[0].url, 'https://localhost:8080/api/location-sharing/fixture-token/location')
    assert.equal(calls[0].options.headers.Authorization, undefined)
    assert.deepEqual(JSON.parse(calls[0].options.body), { latitude: 18.52, longitude: 73.85, accuracy: 12 })
  } finally {
    await mounted.unmount()
    if (priorLocation) Object.defineProperty(navigator, 'geolocation', priorLocation)
    else delete navigator.geolocation
    globalThis.fetch = priorFetch
  }
})

test('mocked computer QR flow receives phone coordinates and uploads the webcam photo once', async () => {
  const camera = mockCamera()
  const priorAgent = Object.getOwnPropertyDescriptor(navigator, 'userAgent')
  const priorFetch = globalThis.fetch
  const priorTimeout = window.setTimeout
  const priorClear = window.clearTimeout
  Object.defineProperty(navigator, 'userAgent', { configurable: true, value: 'Fixture desktop browser' })
  const polls = []
  window.setTimeout = (callback, delay) => {
    if (delay === 2000) { polls.push(callback); return 42 }
    return priorTimeout(callback, delay)
  }
  window.clearTimeout = (id) => { if (id !== 42) priorClear(id) }
  const calls = []
  const sessionId = '123e4567-e89b-12d3-a456-426614174001'
  const imageUrl = '/api/complaint-photos/123e4567-e89b-12d3-a456-426614174000/content'
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (url.endsWith('/api/location-sharing/sessions')) return Response.json({ sessionId, shareUrl: 'https://localhost:5173/share-location/fixture-token', expiresAt: new Date(Date.now() + 300000).toISOString() })
    if (url.endsWith(`/api/location-sharing/sessions/${sessionId}`)) return Response.json({ status: 'COMPLETED', latitude: 18.52, longitude: 73.85, accuracy: 12 })
    if (url.endsWith('/api/complaint-photos')) return Response.json({ imageUrl, latitude: 18.52, longitude: 73.85, accuracy: 12 })
    if (url.startsWith('https://nominatim.openstreetmap.org/reverse')) return Response.json({ display_name: 'Fixture QR Road, Pune' })
    if (url.endsWith('/api/complaints')) return Response.json({ complaintId: 18, title: 'Fixture QR issue' })
    throw new Error(`Unexpected request: ${url}`)
  }
  const element = React.createElement(MemoryRouter, { initialEntries: ['/complaints/new'] },
    React.createElement(AuthContext.Provider, { value: { token: 'fixture-jwt', user: { role: 'ROLE_CITIZEN' } } }, React.createElement(ComplaintForm)))
  const mounted = await mount(element)
  try {
    await change(mounted.container.querySelector('[name="title"]'), 'Fixture QR issue')
    await change(mounted.container.querySelector('[name="description"]'), 'Safe mocked QR issue')
    await change(mounted.container.querySelector('[name="category"]'), 'Clogged drains')
    await click(button(mounted.container, 'Take required photo'))
    await click(button(mounted.container, 'Open camera'))
    await click(button(mounted.container, 'Capture photo'))
    await click(button(mounted.container, 'Confirm photo'))
    assert.equal(button(mounted.container, "Use This Device's Location"), undefined)
    await click(button(mounted.container, 'Get Location Using Phone'))
    assert.match(mounted.container.textContent, /Waiting for location from your phone/)
    assert.equal(polls.length, 1)
    await React.act(async () => { await polls.shift()() })
    await flush()
    assert.equal(mounted.container.querySelector('[name="address"]').value, 'Fixture QR Road, Pune')
    assert.equal(mounted.container.querySelector('.location-qr-modal'), null)
    assert.equal(calls.filter((call) => call.url.endsWith('/api/complaint-photos')).length, 1)
    assert.equal(calls.filter((call) => call.url.endsWith(`/api/location-sharing/sessions/${sessionId}`)).length, 1)
    assert.equal(button(mounted.container, 'Submit complaint').disabled, false)
    await React.act(async () => {
      mounted.container.querySelector('form.complaint-form').dispatchEvent(new window.Event('submit', { bubbles: true, cancelable: true }))
    })
    assert.equal(calls.filter((call) => call.url.endsWith('/api/complaints')).length, 1)
  } finally {
    await mounted.unmount()
    camera.restore()
    if (priorAgent) Object.defineProperty(navigator, 'userAgent', priorAgent)
    else delete navigator.userAgent
    window.setTimeout = priorTimeout
    window.clearTimeout = priorClear
    globalThis.fetch = priorFetch
  }
})
