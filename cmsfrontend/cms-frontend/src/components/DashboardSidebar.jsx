import { Link, useLocation } from 'react-router-dom'
import { dashboardForRole } from '../services/authService'
import { dashboardRoleLabels } from '../services/dashboardNavigation'
import DashboardIcon from './DashboardIcon'

function DashboardSidebar({ user, collapsed, mobileOpen, onNavigate, onClose, sidebarRef }) {
  const location = useLocation()
  const dashboardPath = dashboardForRole(user.role)
  const items = [{ label: 'Dashboard', to: dashboardPath, icon: 'dashboard' }]
  items.push({ label: user.role === 'ROLE_CITIZEN' ? 'My complaints' : user.role === 'ROLE_OFFICER' ? 'Assigned complaints' : 'Manage complaints', to: '/complaints', icon: 'complaints' })
  if (user.role === 'ROLE_CITIZEN') {
    items.push({ label: 'Raise a Complaint', to: '/complaints/new', icon: 'report' })
    items.push({ label: 'My Escalations', to: '/escalations/my', icon: 'escalations' })
  }
  if (user.role === 'ROLE_ADMIN') {
    items.push({ label: 'Create Case Officer', to: '/admin/case-officers/new', icon: 'officer' })
    items.push({ label: 'Escalations', to: '/admin/escalations', icon: 'escalations' })
  }
  items.push({ label: 'My Profile', to: '/profile', icon: 'profile' })

  return <aside ref={sidebarRef} id="dashboard-sidebar" className={`dashboard-sidebar${mobileOpen ? ' is-mobile-open' : ''}`} aria-label="Dashboard sidebar">
    <div className="dashboard-brand-row"><Link className="brand dashboard-brand" to={dashboardPath} onClick={onNavigate} aria-label="CMS dashboard"><span className="brand-mark">C</span><span className="sidebar-label">CMS<span className="dashboard-brand-caption">Civic Management</span></span></Link><button className="dashboard-icon-button sidebar-mobile-close" type="button" onClick={onClose} aria-label="Close navigation"><DashboardIcon name="close" /></button></div>
    <div className="sidebar-workspace"><span className="sidebar-workspace-dot" /><span className="sidebar-label">{dashboardRoleLabels[user.role]} workspace</span></div>
    <p className="sidebar-section-label sidebar-label">Workspace</p>
    <nav className="dashboard-navigation" aria-label="Dashboard navigation">{items.map((item) => {
      const [path, fragment = ''] = item.to.split('#')
      const active = (path === '/complaints' && location.pathname !== '/complaints/new' && (location.pathname === path || location.pathname.startsWith(`${path}/`))) || (location.pathname === path && location.hash === (fragment ? `#${fragment}` : ''))
      return <Link key={item.label} className={`dashboard-nav-link${active ? ' is-active' : ''}`} to={item.to} onClick={onNavigate} aria-current={active ? 'page' : undefined} title={collapsed ? item.label : undefined}><DashboardIcon name={item.icon} /><span className="sidebar-label">{item.label}</span>{active && <span className="sidebar-active-dot" aria-hidden="true" />}</Link>
    })}</nav>
    <div className="sidebar-bottom"><Link className="dashboard-nav-link" to="/" onClick={onNavigate} title={collapsed ? 'CMS home' : undefined}><DashboardIcon name="home" /><span className="sidebar-label">CMS home</span></Link><p className="sidebar-label">Better neighbourhoods,<br />one report at a time.</p></div>
  </aside>
}

export default DashboardSidebar
