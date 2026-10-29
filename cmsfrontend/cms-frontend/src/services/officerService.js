import { apiRequest } from './api'

export const getAssignedComplaints = (token) => apiRequest('/api/officer/complaints/assigned', { token })
export const updateOfficerComplaintStatus = (complaintId, update, token) => apiRequest(`/api/officer/complaints/${complaintId}/status`, { method: 'PATCH', token, body: JSON.stringify(update) })
