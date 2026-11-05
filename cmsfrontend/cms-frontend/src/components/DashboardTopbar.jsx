import { Link, useLocation } from 'react-router-dom'
import DashboardIcon from './DashboardIcon'
import { dashboardRoleLabels } from '../services/dashboardNavigation'
import NotificationBell from './NotificationBell'

function DashboardTopbar({ user, logout, onToggleSidebar, sidebarExpanded, isMobile, menuButtonRef }) {
  const { pathname } = useLocation()
  const title = pathname === '/complaints/new' ? 'Raise a Complaint' : pathname === '/complaints' ? 'Complaints' : pathname.startsWith('/complaints/') ? 'Complaint details' : pathname === '/admin/case-officers/new' ? 'Create Case Officer' : pathname === '/admin/escalations' ? 'Escalations' : pathname === '/escalations/my' ? 'My Escalations' : pathname === '/profile' ? 'My Profile' : 'Dashboard'
  const name = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.name || user.email
  const initials = [user.firstName?.[0], user.lastName?.[0]].filter(Boolean).join('') || 'C'

  return <header className="dashboard-topbar">
    <div className="dashboard-topbar-start"><button ref={menuButtonRef} className="dashboard-icon-button" type="button" onClick={onToggleSidebar} aria-label={isMobile ? 'Open navigation' : sidebarExpanded ? 'Collapse sidebar' : 'Expand sidebar'} aria-expanded={sidebarExpanded} aria-controls="dashboard-sidebar"><DashboardIcon name="menu" /></button><div className="dashboard-breadcrumb"><span>CMS workspace</span><strong>{title}</strong></div></div>
    <div className="dashboard-topbar-actions"><NotificationBell /><Link className="dashboard-user" to="/profile" aria-label={`My Profile: ${name}`}><span className="dashboard-avatar" aria-hidden="true">{initials}</span><span className="dashboard-user-copy"><strong>{name}</strong><span>{dashboardRoleLabels[user.role]}</span></span></Link><Link className="dashboard-profile-link" to="/profile">My Profile</Link><button className="dashboard-logout" type="button" onClick={logout} aria-label="Logout"><DashboardIcon name="logout" /><span>Logout</span></button></div>
  </header>
}

export default DashboardTopbar
