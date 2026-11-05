import { useCallback, useEffect, useRef, useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import DashboardSidebar from './DashboardSidebar'
import DashboardTopbar from './DashboardTopbar'
import '../styles/dashboard-layout.css'

const mobileQuery = '(max-width: 900px)'

function DashboardLayout() {
  const { user, logout } = useAuth()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [isMobile, setIsMobile] = useState(() => window.matchMedia(mobileQuery).matches)
  const sidebarRef = useRef(null)
  const menuButtonRef = useRef(null)
  const closeSidebar = useCallback(() => setMobileOpen(false), [])

  useEffect(() => {
    const media = window.matchMedia(mobileQuery)
    const handleResize = () => { setIsMobile(media.matches); setMobileOpen(false) }
    media.addEventListener('change', handleResize)
    window.addEventListener('popstate', closeSidebar)
    return () => { media.removeEventListener('change', handleResize); window.removeEventListener('popstate', closeSidebar) }
  }, [closeSidebar])

  useEffect(() => {
    if (!mobileOpen) return undefined
    const previousOverflow = document.body.style.overflow
    const menuButton = menuButtonRef.current
    document.body.style.overflow = 'hidden'
    const firstControl = sidebarRef.current?.querySelector('a, button')
    firstControl?.focus()
    const handleKey = (event) => {
      if (event.key === 'Escape') closeSidebar()
      if (event.key !== 'Tab') return
      const controls = [...sidebarRef.current.querySelectorAll('a, button')].filter((element) => element.getClientRects().length)
      const first = controls[0]
      const last = controls.at(-1)
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
    }
    document.addEventListener('keydown', handleKey)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', handleKey)
      menuButton?.focus()
    }
  }, [mobileOpen, closeSidebar])

  const toggleSidebar = () => {
    if (isMobile) setMobileOpen((open) => !open)
    else setCollapsed((value) => !value)
  }

  return <div className={`dashboard-shell${collapsed ? ' sidebar-collapsed' : ''}`}>
    <a className="dashboard-skip-link" href="#dashboard-main">Skip to content</a>
    <DashboardSidebar user={user} collapsed={collapsed && !isMobile} mobileOpen={mobileOpen} onNavigate={closeSidebar} onClose={closeSidebar} sidebarRef={sidebarRef} />
    {mobileOpen && <button className="dashboard-sidebar-backdrop" type="button" aria-label="Close navigation" onClick={closeSidebar} tabIndex={-1} />}
    <div className="dashboard-workspace" inert={mobileOpen ? true : undefined}>
      <DashboardTopbar user={user} logout={logout} onToggleSidebar={toggleSidebar} sidebarExpanded={isMobile ? mobileOpen : !collapsed} isMobile={isMobile} menuButtonRef={menuButtonRef} />
      <main className="dashboard-main" id="dashboard-main" tabIndex={-1}><Outlet /></main>
      <footer className="dashboard-footer">CMS · Civic Issue Reporting Platform</footer>
    </div>
  </div>
}

export default DashboardLayout
