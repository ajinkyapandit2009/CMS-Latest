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
export const assignComplaint = (complaintId, assignment, token) => apiRequest(`/api/admin/complaints/${complaintId}/assign`, { method: 'POST', token, body: JSON.stringify(assignment) })
export const createOfficerAccount = (details, token) => apiRequest('/api/admin/case-officer-accounts', { method: 'POST', token, body: JSON.stringify(details) })
export const createOfficerProfile = (userId, token) => apiRequest('/api/admin/case-officers', { method: 'POST', token, body: JSON.stringify({ userId }) })
