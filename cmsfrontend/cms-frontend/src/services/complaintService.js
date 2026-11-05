import { apiRequest } from './api'
import { getAdminComplaints } from './adminService'
import { getAssignedComplaints } from './officerService'

export const getMyComplaints = (token) => apiRequest('/api/complaints/my', { token })
export const createComplaint = (details, token) => apiRequest('/api/complaints', { method: 'POST', token, body: JSON.stringify(details) })
export const getComplaint = (complaintId, token) => apiRequest(`/api/complaints/${complaintId}`, { token })

export function getAccessibleComplaints(role, token) {
  if (role === 'ROLE_CITIZEN') return getMyComplaints(token)
  if (role === 'ROLE_OFFICER') return getAssignedComplaints(token)
  if (role === 'ROLE_ADMIN') return getAdminComplaints({}, token)
  return Promise.reject(new Error('Your account cannot access complaints.'))
}
