import { apiRequest } from './api'

export const getMyComplaints = (token) => apiRequest('/api/complaints/my', { token })
export const createComplaint = (details, token) => apiRequest('/api/complaints', { method: 'POST', token, body: JSON.stringify(details) })
