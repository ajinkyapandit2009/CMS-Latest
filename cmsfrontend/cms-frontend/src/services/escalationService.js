import { apiRequest } from './api'

export const getMyEscalations = (token) => apiRequest('/api/escalations/my', { token })
export const submitEscalation = (complaintId, reason, token) => apiRequest(`/api/escalations/complaints/${complaintId}`, {
  method: 'POST', token, body: JSON.stringify({ reason }),
})
export const getAdminEscalations = (status, token) => apiRequest(`/api/admin/escalations${status ? `?status=${encodeURIComponent(status)}` : ''}`, { token })
export const reviewEscalation = (escalationId, status, remarks, token) => apiRequest(`/api/admin/escalations/${escalationId}/review`, {
  method: 'PATCH', token, body: JSON.stringify({ status, remarks }),
})

export function isEscalationEligible(complaint, user, now = Date.now()) {
  if (!complaint || user?.role !== 'ROLE_CITIZEN' || Number(complaint.citizenId) !== Number(user.userId)) return false
  if (complaint.status === 'RESOLVED' || complaint.status === 'REJECTED') return true
  const created = complaint.createdAt ? Date.parse(complaint.createdAt) : NaN
  return Number.isFinite(created) && created <= now - 48 * 60 * 60 * 1000
}
