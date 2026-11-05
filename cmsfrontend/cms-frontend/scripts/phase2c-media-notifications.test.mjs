import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'
import { notificationDestination } from '../src/services/notificationNavigation.js'

globalThis.window = { location: { protocol: 'https:', hostname: 'localhost' } }
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true, hmr: false }, appType: 'custom' })
after(() => server.close())
const { getComplaintPhoto, isManagedPhotoReference } = await server.ssrLoadModule('/src/services/photoService.js')

test('complaint notifications target the protected detail route', () => {
  for (const id of [1, 23, 2147483647]) {
    assert.equal(notificationDestination({ complaintId: id }, '/citizen/dashboard'), `/complaints/${id}`)
  }
})

test('general or malformed notifications fall back to the role dashboard', () => {
  for (const id of [null, undefined, 0, -1, 2147483648, '23', '../admin']) {
    assert.equal(notificationDestination({ complaintId: id }, '/officer/dashboard'), '/officer/dashboard')
  }
  assert.equal(notificationDestination(null, '/admin/dashboard'), '/admin/dashboard')
})

test('photo fetch accepts only managed references and sends the bearer token', async () => {
  const reference = '/api/complaint-photos/123e4567-e89b-12d3-a456-426614174000/content'
  assert.equal(isManagedPhotoReference(reference), true)
  const priorFetch = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return new Response(new Blob(['private image'], { type: 'image/jpeg' }), { status: 200 })
  }
  try {
    const blob = await getComplaintPhoto(reference, 'fixture-token')
    assert.equal(blob.type, 'image/jpeg')
    assert.equal(calls.length, 1)
    assert.equal(calls[0].url, `https://localhost:8080${reference}`)
    assert.equal(calls[0].options.headers.Authorization, 'Bearer fixture-token')
    await assert.rejects(getComplaintPhoto('https://example.test/private.jpg', 'fixture-token'), { status: 400 })
    assert.equal(calls.length, 1)
  } finally {
    globalThis.fetch = priorFetch
  }
})
