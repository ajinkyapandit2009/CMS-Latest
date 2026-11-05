import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { click, closeHarness, flush, key, load, mount, React } from './domHarness.mjs'

after(closeHarness)
const { default: AuthenticatedComplaintImage } = await load('/src/components/AuthenticatedComplaintImage.jsx')
const { default: CameraCapture } = await load('/src/components/CameraCapture.jsx')
const { AuthContext } = await load('/src/context/AuthStore.js')

test('authenticated photo modal reuses the protected Blob and closes by X, Escape, and backdrop', async () => {
  const priorFetch = globalThis.fetch
  const priorCreate = URL.createObjectURL
  const priorRevoke = URL.revokeObjectURL
  const requests = []
  const revoked = []
  globalThis.fetch = async (url, options) => {
    requests.push({ url, options })
    return new Response(new Blob(['fixture image'], { type: 'image/jpeg' }), { status: 200 })
  }
  URL.createObjectURL = () => 'blob:protected-fixture'
  URL.revokeObjectURL = (url) => revoked.push(url)
  const reference = '/api/complaint-photos/123e4567-e89b-12d3-a456-426614174000/content'
  const mounted = await mount(React.createElement(AuthContext.Provider, { value: { token: 'fixture-jwt' } },
    React.createElement(AuthenticatedComplaintImage, { imageReference: reference, alt: 'Safe fixture photo' })))
  try {
    await flush()
    const trigger = mounted.container.querySelector('button[aria-label^="Enlarge photo"]')
    assert.ok(trigger)
    assert.equal(requests.length, 1)
    assert.equal(requests[0].options.headers.Authorization, 'Bearer fixture-jwt')
    assert.equal(requests[0].url, `https://localhost:8080${reference}`)

    await click(trigger)
    assert.equal(document.querySelector('[role="dialog"] img').getAttribute('src'), 'blob:protected-fixture')
    assert.equal(document.activeElement.getAttribute('aria-label'), 'Close photo preview')
    assert.equal(requests.length, 1, 'opening the modal must not fetch the image twice')
    await key('Escape')
    assert.equal(document.querySelector('[role="dialog"]'), null)
    assert.equal(document.activeElement, trigger)

    await click(trigger)
    await click(document.querySelector('.complaint-image-modal'))
    assert.equal(document.querySelector('[role="dialog"]'), null)
    await click(trigger)
    await click(document.querySelector('button[aria-label="Close photo preview"]'))
    assert.equal(document.querySelector('[role="dialog"]'), null)
  } finally {
    await mounted.unmount()
    globalThis.fetch = priorFetch
    URL.createObjectURL = priorCreate
    URL.revokeObjectURL = priorRevoke
  }
  assert.deepEqual(revoked, ['blob:protected-fixture'])
})

test('mocked camera captures a live stream, retakes, confirms a Blob, and stops tracks', async () => {
  const priorCreate = URL.createObjectURL
  const priorRevoke = URL.revokeObjectURL
  const priorMedia = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices')
  const priorPlay = window.HTMLMediaElement.prototype.play
  const priorContext = window.HTMLCanvasElement.prototype.getContext
  const priorBlob = window.HTMLCanvasElement.prototype.toBlob
  const priorWidth = Object.getOwnPropertyDescriptor(window.HTMLVideoElement.prototype, 'videoWidth')
  const priorHeight = Object.getOwnPropertyDescriptor(window.HTMLVideoElement.prototype, 'videoHeight')
  let stopped = 0
  const calls = []
  const confirmed = []
  const stream = { getTracks: () => [{ stop() { stopped += 1 } }] }
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
    async getUserMedia(constraints) { calls.push(constraints); return stream },
    async enumerateDevices() { return [{ kind: 'videoinput', deviceId: 'fixture-cam', label: 'Fixture webcam' }] },
  } })
  window.HTMLMediaElement.prototype.play = async () => {}
  Object.defineProperty(window.HTMLVideoElement.prototype, 'videoWidth', { configurable: true, get: () => 640 })
  Object.defineProperty(window.HTMLVideoElement.prototype, 'videoHeight', { configurable: true, get: () => 480 })
  window.HTMLCanvasElement.prototype.getContext = () => ({ drawImage() {} })
  window.HTMLCanvasElement.prototype.toBlob = (callback) => callback(new Blob(['camera fixture'], { type: 'image/jpeg' }))
  URL.createObjectURL = () => 'blob:camera-fixture'
  URL.revokeObjectURL = () => {}
  const mounted = await mount(React.createElement(CameraCapture, { onConfirm: (blob) => confirmed.push(blob), onCancel() {} }))
  try {
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Open camera'))
    assert.equal(calls.length, 1)
    assert.deepEqual(calls[0].video, { facingMode: { ideal: 'environment' } })
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Capture photo'))
    assert.match(mounted.container.textContent, /Confirm photo/)
    assert.equal(stopped, 1)
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Retake'))
    assert.equal(calls.length, 2)
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Capture photo'))
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Confirm photo'))
    assert.equal(confirmed.length, 1)
    assert.equal(confirmed[0].type, 'image/jpeg')
    assert.equal(mounted.container.querySelector('input[type="file"]'), null)
  } finally {
    await mounted.unmount()
    if (priorMedia) Object.defineProperty(navigator, 'mediaDevices', priorMedia)
    else delete navigator.mediaDevices
    window.HTMLMediaElement.prototype.play = priorPlay
    if (priorWidth) Object.defineProperty(window.HTMLVideoElement.prototype, 'videoWidth', priorWidth)
    if (priorHeight) Object.defineProperty(window.HTMLVideoElement.prototype, 'videoHeight', priorHeight)
    window.HTMLCanvasElement.prototype.getContext = priorContext
    window.HTMLCanvasElement.prototype.toBlob = priorBlob
    URL.createObjectURL = priorCreate
    URL.revokeObjectURL = priorRevoke
  }
  assert.equal(stopped, 2)
})

test('camera stream granted after unmount still has its tracks stopped', async () => {
  const priorMedia = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices')
  let grant
  let stopped = 0
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
    getUserMedia() { return new Promise((resolve) => { grant = resolve }) },
    async enumerateDevices() { return [] },
  } })
  const mounted = await mount(React.createElement(CameraCapture, { onConfirm() {}, onCancel() {} }))
  try {
    await click([...mounted.container.querySelectorAll('button')].find((button) => button.textContent === 'Open camera'))
    await mounted.unmount()
    grant({ getTracks: () => [{ stop() { stopped += 1 } }] })
    await flush()
    assert.equal(stopped, 1)
  } finally {
    if (mounted.container.isConnected) await mounted.unmount()
    if (priorMedia) Object.defineProperty(navigator, 'mediaDevices', priorMedia)
    else delete navigator.mediaDevices
  }
})

test('camera falls back to a default webcam and reports permission denial', async () => {
  const priorMedia = Object.getOwnPropertyDescriptor(navigator, 'mediaDevices')
  const priorPlay = window.HTMLMediaElement.prototype.play
  const calls = []
  let stopped = 0
  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
    async getUserMedia(options) {
      calls.push(options)
      if (options.video !== true) throw Object.assign(new Error('Rear camera unavailable'), { name: 'NotFoundError' })
      return { getTracks: () => [{ stop() { stopped += 1 } }] }
    },
    async enumerateDevices() { return [] },
  } })
  window.HTMLMediaElement.prototype.play = async () => {}
  const first = await mount(React.createElement(CameraCapture, { onConfirm() {}, onCancel() {} }))
  try {
    await click([...first.container.querySelectorAll('button')].find((button) => button.textContent === 'Open camera'))
    assert.equal(calls.length, 2)
    assert.equal(calls[1].video, true)
    assert.equal([...first.container.querySelectorAll('button')].find((button) => button.textContent === 'Capture photo').disabled, false)
    await click([...first.container.querySelectorAll('button')].find((button) => button.textContent === 'Close camera'))
    assert.equal(stopped, 1)
  } finally { await first.unmount() }

  Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
    async getUserMedia() { throw Object.assign(new Error('Denied'), { name: 'NotAllowedError' }) },
    async enumerateDevices() { return [] },
  } })
  const second = await mount(React.createElement(CameraCapture, { onConfirm() {}, onCancel() {} }))
  try {
    await click([...second.container.querySelectorAll('button')].find((button) => button.textContent === 'Open camera'))
    assert.match(second.container.textContent, /Camera permission was denied/)
    assert.equal(second.container.querySelector('input[type="file"]'), null)
  } finally {
    await second.unmount()
    if (priorMedia) Object.defineProperty(navigator, 'mediaDevices', priorMedia)
    else delete navigator.mediaDevices
    window.HTMLMediaElement.prototype.play = priorPlay
  }
})
