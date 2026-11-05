import React, { act } from 'react'
import { JSDOM } from 'jsdom'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

const dom = new JSDOM('<!doctype html><html><body></body></html>', {
  url: 'https://localhost:5173/',
  pretendToBeVisual: true,
})

globalThis.window = dom.window
globalThis.document = dom.window.document
globalThis.localStorage = dom.window.localStorage
globalThis.HTMLElement = dom.window.HTMLElement
globalThis.HTMLInputElement = dom.window.HTMLInputElement
globalThis.HTMLSelectElement = dom.window.HTMLSelectElement
globalThis.HTMLTextAreaElement = dom.window.HTMLTextAreaElement
globalThis.MouseEvent = dom.window.MouseEvent
globalThis.KeyboardEvent = dom.window.KeyboardEvent
globalThis.IS_REACT_ACT_ENVIRONMENT = true
Object.defineProperty(globalThis, 'navigator', { configurable: true, value: dom.window.navigator })
window.matchMedia = () => ({ matches: false, addEventListener() {}, removeEventListener() {} })
window.scrollTo = () => {}

const { createRoot } = await import('react-dom/client')
const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true, hmr: false }, appType: 'custom' })

export async function load(path) { return server.ssrLoadModule(path) }
export async function closeHarness() { await server.close(); dom.window.close() }

export async function mount(element) {
  const container = document.createElement('div')
  document.body.append(container)
  const root = createRoot(container)
  await act(async () => { root.render(element) })
  return {
    container,
    async unmount() { await act(async () => { root.unmount() }); container.remove() },
  }
}

export async function click(element) {
  if (!element) throw new Error('Expected clickable element was not found')
  await act(async () => { element.dispatchEvent(new MouseEvent('click', { bubbles: true })) })
}

export async function key(keyName) {
  await act(async () => { document.dispatchEvent(new KeyboardEvent('keydown', { key: keyName, bubbles: true })) })
}

export async function flush() {
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, 0)) })
}

export async function change(element, value) {
  if (!element) throw new Error('Expected form control was not found')
  const prototype = element.tagName === 'SELECT' ? HTMLSelectElement.prototype
    : element.tagName === 'TEXTAREA' ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype
  const setter = Object.getOwnPropertyDescriptor(prototype, 'value').set
  await act(async () => {
    setter.call(element, value)
    element.dispatchEvent(new window.Event(element.tagName === 'SELECT' ? 'change' : 'input', { bubbles: true }))
  })
}

export { React }
