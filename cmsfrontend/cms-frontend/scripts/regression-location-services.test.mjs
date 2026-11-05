import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

globalThis.window = { location: { protocol: 'https:', hostname: 'localhost' } }
Object.defineProperty(globalThis, 'navigator', { configurable: true, value: {} })
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true, hmr: false }, appType: 'custom' })
after(() => server.close())
const { getCurrentLocation, MAX_LOCATION_ACCURACY_METERS } = await server.ssrLoadModule('/src/services/locationService.js')
const { createLocationSession, getLocationSession, cancelLocationSession, submitPhoneLocation } = await server.ssrLoadModule('/src/services/locationSharingService.js')
const { uploadComplaintPhoto } = await server.ssrLoadModule('/src/services/photoService.js')

test('GPS requests high accuracy and accepts only bounded coordinates and accuracy', async () => {
  let settings
  navigator.geolocation = { getCurrentPosition(success, error, options) {
    settings = options
    success({ coords: { latitude: 18.5, longitude: 73.8, accuracy: 12 } })
  } }
  assert.deepEqual(await getCurrentLocation(), { latitude: 18.5, longitude: 73.8, accuracy: 12 })
  assert.deepEqual(settings, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 })
  navigator.geolocation = { getCurrentPosition(success) { success({ coords: { latitude: 91, longitude: 73.8, accuracy: 12 } }) } }
  await assert.rejects(getCurrentLocation(), /invalid location/i)
  navigator.geolocation = { getCurrentPosition(success) { success({ coords: { latitude: 18.5, longitude: 73.8, accuracy: MAX_LOCATION_ACCURACY_METERS + 1 } }) } }
  await assert.rejects(getCurrentLocation(), /accuracy/i)
})

test('GPS permission and timeout failures are actionable', async () => {
  navigator.geolocation = { getCurrentPosition(success, error) { error({ code: 1 }) } }
  await assert.rejects(getCurrentLocation(), /permission was denied/i)
  navigator.geolocation = { getCurrentPosition(success, error) { error({ code: 3 }) } }
  await assert.rejects(getCurrentLocation(), /timed out/i)
  delete navigator.geolocation
  await assert.rejects(getCurrentLocation(), /not supported/i)
})

test('QR session APIs use owner bearer token; phone submission has no account token', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => { calls.push({ url, options }); return Response.json({ status: 'PENDING' }) }
  try {
    await createLocationSession('fixture-jwt')
    await getLocationSession('fixture-session', 'fixture-jwt')
    await cancelLocationSession('fixture-session', 'fixture-jwt')
    await submitPhoneLocation('secret/fixture', { latitude: 18.5, longitude: 73.8, accuracy: 12 })
    assert.deepEqual(calls.map((call) => [call.options.method || 'GET', new URL(call.url).pathname]), [
      ['POST', '/api/location-sharing/sessions'],
      ['GET', '/api/location-sharing/sessions/fixture-session'],
      ['DELETE', '/api/location-sharing/sessions/fixture-session'],
      ['POST', '/api/location-sharing/secret%2Ffixture/location'],
    ])
    for (const call of calls.slice(0, 3)) assert.equal(call.options.headers.Authorization, 'Bearer fixture-jwt')
    assert.equal(calls[3].options.headers.Authorization, undefined)
    assert.deepEqual(JSON.parse(calls[3].options.body), { latitude: 18.5, longitude: 73.8, accuracy: 12 })
  } finally { globalThis.fetch = priorFetch }
})

test('photo upload sends the captured Blob and coordinates in one authenticated multipart request', async () => {
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => { calls.push({ url, options }); return Response.json({ imageUrl: '/api/complaint-photos/123e4567-e89b-12d3-a456-426614174000/content' }) }
  try {
    const blob = new Blob(['fixture'], { type: 'image/jpeg' })
    await uploadComplaintPhoto(blob, { latitude: 18.5, longitude: 73.8, accuracy: 12 }, 'fixture-jwt')
    assert.equal(calls.length, 1)
    assert.equal(calls[0].url, 'https://localhost:8080/api/complaint-photos')
    assert.equal(calls[0].options.headers.Authorization, 'Bearer fixture-jwt')
    assert.equal(calls[0].options.headers['Content-Type'], undefined)
    assert.equal(calls[0].options.body.get('photo').type, 'image/jpeg')
    assert.equal(calls[0].options.body.get('latitude'), '18.5')
    assert.equal(calls[0].options.body.get('longitude'), '73.8')
    assert.equal(calls[0].options.body.get('accuracy'), '12')
  } finally { globalThis.fetch = priorFetch }
})
