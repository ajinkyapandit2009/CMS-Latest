import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'
import react from '@vitejs/plugin-react'

const server = await createServer({ configFile: false, plugins: [react()], server: { middlewareMode: true }, appType: 'custom' })
after(() => server.close())
const { workloadLevel, workloadThresholds } = await server.ssrLoadModule('/src/services/workloadUtils.js')
const { default: OfficerWorkloadPanel } = await server.ssrLoadModule('/src/components/OfficerWorkloadPanel.jsx')

test('workload thresholds classify boundary values', () => {
  assert.ok(workloadThresholds.yellowFrom > 0)
  assert.ok(workloadThresholds.redFrom > workloadThresholds.yellowFrom)
  assert.equal(workloadLevel(workloadThresholds.yellowFrom - 1).key, 'low')
  assert.equal(workloadLevel(workloadThresholds.yellowFrom).key, 'moderate')
  assert.equal(workloadLevel(workloadThresholds.redFrom).key, 'high')
})

test('workload panel shows active counts and officers with no complaints', () => {
  const html = renderToStaticMarkup(React.createElement(OfficerWorkloadPanel, {
    workloads: [
      { userId: 1, firstName: 'Active', lastName: 'Officer', employeeNumber: 'CMS-OFF-001', assignedCount: 2, inProgressCount: 1, totalActive: 3 },
      { userId: 2, firstName: 'Idle', lastName: 'Officer', employeeNumber: 'CMS-OFF-002', assignedCount: 0, inProgressCount: 0, totalActive: 0 },
    ],
    isLoading: false, error: '', onRetry() {},
  }))
  assert.match(html, /Active Officer/)
  assert.match(html, /Idle Officer/)
  assert.match(html, /CMS-OFF-002/)
  assert.match(html, /Total active/)
  assert.match(html, /Low workload: 0 active complaints/)
})
