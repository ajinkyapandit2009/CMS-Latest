import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createEvidenceOperations } from '../src/services/evidenceOperations.js'

function deferred() {
  let resolve
  const promise = new Promise((finish) => { resolve = finish })
  return { promise, resolve }
}

test('retake makes an older photo upload and address response unusable', async () => {
  const operations = createEvidenceOperations()
  operations.newPhoto()
  const location = operations.newLocation()
  const upload = operations.newUpload()
  const address = operations.newAddress()
  const oldUpload = deferred()
  const oldAddress = deferred()
  const acceptedUpload = oldUpload.promise.then((value) => operations.isUpload(upload) ? value : null)
  const acceptedAddress = oldAddress.promise.then((value) => operations.isAddress(address) ? value : null)
  operations.newPhoto()
  oldUpload.resolve({ imageUrl: '/api/complaint-photos/old/content' })
  oldAddress.resolve('Old address')
  assert.equal(await acceptedUpload, null)
  assert.equal(await acceptedAddress, null)
  assert.equal(operations.isLocation(location), false)
  const freshLocation = operations.newLocation()
  const freshUpload = operations.newUpload()
  assert.equal(operations.isLocation(freshLocation), true)
  assert.equal(operations.isUpload(freshUpload), true)
})

test('switching location invalidates older GPS, QR session, upload, and geocoding work', () => {
  const operations = createEvidenceOperations()
  operations.newPhoto()
  const gps = operations.newLocation()
  const upload = operations.newUpload()
  const address = operations.newAddress()
  const qrLocation = operations.newLocation()
  const qrSession = operations.newSession()
  assert.equal(operations.isLocation(gps), false)
  assert.equal(operations.isUpload(upload), false)
  assert.equal(operations.isAddress(address), false)
  assert.equal(operations.isSession(qrSession), true)
  operations.invalidateSession()
  assert.equal(operations.isSession(qrSession), false)
  assert.equal(operations.isLocation(qrLocation), true)
})

test('only one submission runs at a time and retake invalidates its result', () => {
  const operations = createEvidenceOperations()
  operations.newPhoto()
  operations.newLocation()
  operations.newUpload()
  operations.newAddress()
  const first = operations.beginSubmission()
  assert.ok(first)
  assert.equal(operations.beginSubmission(), null)
  operations.newPhoto()
  assert.equal(operations.isSubmission(first), false)
  operations.endSubmission(first)
  assert.ok(operations.beginSubmission())
})
