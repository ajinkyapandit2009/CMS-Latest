import { apiRequest } from './api'

export const loginRequest = (credentials) => apiRequest('/auth/login', { method: 'POST', body: JSON.stringify(credentials) })
export const registerCitizen = (details) => apiRequest('/users', { method: 'POST', body: JSON.stringify(details) })
export const checkEmailAvailability = (email, signal) => apiRequest(`/users/email-availability?email=${encodeURIComponent(email)}`, { signal })
export const getCurrentUser = (token) => apiRequest('/auth/me', { token })
export const updateCurrentUser = (details, token) => apiRequest('/auth/me', { method: 'PATCH', token, body: JSON.stringify(details) })

export function roleFromToken(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.roles?.split(',').find((role) => ['ROLE_CITIZEN', 'ROLE_OFFICER', 'ROLE_ADMIN'].includes(role)) || null
  } catch { return null }
}

export const dashboardForRole = (role) => ({ ROLE_CITIZEN: '/citizen/dashboard', ROLE_OFFICER: '/officer/dashboard', ROLE_ADMIN: '/admin/dashboard' }[role] || null)
