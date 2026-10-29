import { useCallback, useEffect, useMemo, useState } from 'react'
import { dashboardForRole, getCurrentUser, loginRequest, roleFromToken } from '../services/authService'
import { AuthContext } from './AuthStore'

const TOKEN_KEY = 'cms_auth_token'
function prepareUser(user, token) {
  const role = user?.role || roleFromToken(token)
  return role ? { ...user, role, name: `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email } : null
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [isInitializing, setIsInitializing] = useState(true)

  const logout = useCallback(() => { localStorage.removeItem(TOKEN_KEY); setToken(null); setUser(null) }, [])
  const login = useCallback(async (credentials) => {
    const response = await loginRequest(credentials)
    if (!response?.token) throw new Error('The server did not return an authentication token.')
    localStorage.setItem(TOKEN_KEY, response.token)
    try {
      const verified = await getCurrentUser(response.token)
      const nextUser = prepareUser(verified, response.token)
      if (!nextUser || !dashboardForRole(nextUser.role)) throw new Error('Your account role could not be verified. Please contact an administrator.')
      setUser(nextUser)
      setToken(response.token)
      return nextUser
    } catch (error) { logout(); throw error }
  }, [logout])

  useEffect(() => {
    const restore = async () => {
      const token = localStorage.getItem(TOKEN_KEY)
      if (!token) { setIsInitializing(false); return }
      try {
        const verified = await getCurrentUser(token)
        const nextUser = prepareUser(verified, token)
        if (!nextUser || !dashboardForRole(nextUser.role)) throw new Error('Invalid account role')
        setUser(nextUser)
        setToken(token)
      } catch { logout() } finally { setIsInitializing(false) }
    }
    restore()
  }, [logout])

  const value = useMemo(() => ({ user, token, isInitializing, isAuthenticated: Boolean(user), login, logout }), [user, token, isInitializing, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
