import { apiRequest } from './api'

export function getAdminComplaints(filters, token) {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.priority) params.set('priority', filters.priority)
  const query = params.toString()
  return apiRequest(`/api/admin/complaints${query ? `?${query}` : ''}`, { token })
}

export const updateComplaintPriority = (complaintId, priority, token) => apiRequest(`/api/admin/complaints/${complaintId}/priority`, { method: 'PATCH', token, body: JSON.stringify({ priority }) })
export const getCaseOfficers = (token) => apiRequest('/api/admin/case-officers', { token })
export const getOfficerWorkloads = (token) => apiRequest('/api/admin/case-officers/workloads', { token })
export const assignComplaint = (complaintId, assignment, token) => apiRequest(`/api/admin/complaints/${complaintId}/assign`, { method: 'POST', token, body: JSON.stringify(assignment) })
export const createCaseOfficer = (details, token) => apiRequest('/api/admin/case-officers/with-account', { method: 'POST', token, body: JSON.stringify(details) })
