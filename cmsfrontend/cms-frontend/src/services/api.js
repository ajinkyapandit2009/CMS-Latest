const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  `${window.location.protocol}//${window.location.hostname}:8080`

export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}) { super(message); this.status = status; this.fieldErrors = fieldErrors }
}

export async function apiRequest(path, { token, ...options } = {}) {
  try {
    const isMultipart = options.body instanceof FormData
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: { ...(isMultipart ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    })
    const body = await response.json().catch(() => null)
    if (!response.ok) throw new ApiError(body?.message || body?.error || 'Something went wrong. Please try again.', response.status, body?.fieldErrors || {})
    return body
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError('Unable to connect to CMS. Please check your connection and try again.', 0)
  }
}

export async function apiBlobRequest(path, { token, ...options } = {}) {
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    })
    if (!response.ok) {
      const body = await response.json().catch(() => null)
      throw new ApiError(body?.message || body?.error || 'The requested file is unavailable.', response.status)
    }
    return await response.blob()
  } catch (error) {
    if (error instanceof ApiError) throw error
    throw new ApiError('Unable to connect to CMS. Please check your connection and try again.', 0)
  }
}
