const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status }
}

export async function apiRequest(path, { token, ...options } = {}) {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    })
    const body = await response.json().catch(() => null)
    if (!response.ok) throw new ApiError(body?.message || body?.error || 'Something went wrong. Please try again.', response.status)
    return body
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError('Unable to connect to CMS. Please check your connection and try again.', 0)
  }
}
