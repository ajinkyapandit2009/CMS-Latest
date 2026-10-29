import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { dashboardForRole } from '../services/authService'

function ProtectedRoute({ allowedRole, children }) {
  const { user, isInitializing } = useAuth()
  const location = useLocation()
  if (isInitializing) return <div className="route-loading" role="status">Restoring your session…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (user.role !== allowedRole) return <Navigate to={dashboardForRole(user.role) || '/access-denied'} replace />
  return children
}

export default ProtectedRoute
