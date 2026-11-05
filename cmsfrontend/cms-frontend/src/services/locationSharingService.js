import { apiRequest } from './api'

export const createLocationSession = (token) => apiRequest('/api/location-sharing/sessions', { method: 'POST', token })
export const getLocationSession = (sessionId, token, signal) => apiRequest(`/api/location-sharing/sessions/${sessionId}`, { token, signal })
export const cancelLocationSession = (sessionId, token) => apiRequest(`/api/location-sharing/sessions/${sessionId}`, { method: 'DELETE', token })
export const submitPhoneLocation = (shareToken, location) => apiRequest(`/api/location-sharing/${encodeURIComponent(shareToken)}/location`, {
  method: 'POST',
  body: JSON.stringify(location),
})
