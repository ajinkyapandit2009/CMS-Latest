import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router-dom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

globalThis.window = { location: { protocol: 'https:', hostname: 'localhost' } }
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true }, appType: 'custom' })
after(() => server.close())
const { default: Register } = await server.ssrLoadModule('/src/pages/Register.jsx')
const { AuthContext } = await server.ssrLoadModule('/src/context/AuthStore.js')
const { checkEmailAvailability, registerCitizen } = await server.ssrLoadModule('/src/services/authService.js')
const { normalizeRegistrationEmail, prepareRegistration, validateRegistrationField, validateRegistration } = await server.ssrLoadModule('/src/services/registrationValidation.js')

const valid = { firstName: ' Anaya ', lastName: ' D’Costa ', email: ' Anaya.D+tag@Example.COM ', phone: ' +919876543210 ', address: ' Road 1 ', password: 'StrongPass9', confirmPassword: 'StrongPass9' }

test('normalization and field rules match Phase 6A', () => {
  assert.equal(normalizeRegistrationEmail(valid.email), 'anaya.d+tag@example.com')
  assert.deepEqual(validateRegistration(valid), {})
  assert.deepEqual(prepareRegistration(valid), { firstName: 'Anaya', lastName: 'D’Costa', email: 'anaya.d+tag@example.com', phone: '+919876543210', address: 'Road 1', password: 'StrongPass9', confirmPassword: 'StrongPass9' })
  for (const [field, value] of [['firstName', '123'], ['lastName', '   '], ['email', 'bad@address'], ['phone', 'letters'], ['password', 'Weak123'], ['confirmPassword', 'Mismatch1']]) {
    assert.ok(validateRegistrationField(field, { ...valid, [field]: value }), field)
  }
  assert.ok(validateRegistrationField('password', { ...valid, password: 'StrongPass9' + '😀'.repeat(20) }))
  assert.ok(validateRegistrationField('address', { ...valid, address: '😀'.repeat(17000) }))
  assert.equal(validateRegistrationField('phone', { ...valid, phone: '  ' }), '')
  assert.equal(validateRegistrationField('address', { ...valid, address: '  ' }), '')
})

test('availability uses the public endpoint and preserves server field errors', async () => {
  const previous = globalThis.fetch
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    if (calls.length === 1) return new Response('{"available":false}', { status: 200 })
    return new Response('{"message":"Email is already registered","fieldErrors":{"email":"Email is already registered"}}', { status: 409 })
  }
  try {
    const controller = new AbortController()
    assert.deepEqual(await checkEmailAvailability('anaya.d+tag@example.com', controller.signal), { available: false })
    assert.equal(calls[0].url, 'https://localhost:8080/users/email-availability?email=anaya.d%2Btag%40example.com')
    assert.equal(calls[0].options.signal, controller.signal)
    await assert.rejects(registerCitizen(prepareRegistration(valid)), (error) => error.status === 409 && error.fieldErrors.email === 'Email is already registered')
    assert.equal(calls[1].url, 'https://localhost:8080/users')
    assert.equal(calls[1].options.method, 'POST')
    assert.deepEqual(JSON.parse(calls[1].options.body), prepareRegistration(valid))
  } finally {
    globalThis.fetch = previous
  }
})

test('registration renders guidance and accessible inputs without role selection', () => {
  const html = renderToStaticMarkup(React.createElement(MemoryRouter, null,
    React.createElement(AuthContext.Provider, { value: { user: null, isInitializing: false } }, React.createElement(Register))))
  for (const field of ['firstName', 'lastName', 'email', 'phone', 'address', 'password', 'confirmPassword']) {
    assert.match(html, new RegExp(`id="register-${field}"`))
    assert.match(html, new RegExp(`id="register-${field}-help"`))
  }
  assert.match(html, /8–72 characters without spaces/)
  assert.doesNotMatch(html, /6\+ characters|name="role"|name="userRole"/)
})
