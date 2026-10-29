import { useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { dashboardForRole } from '../services/authService'

const navItems = [{ label: 'Home', to: '/' }, { label: 'How it works', to: '/#how-it-works' }, { label: 'Civic issues', to: '/#civic-issues' }, { label: 'About', to: '/#about' }]

function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const { user, isInitializing, logout } = useAuth()
  const closeMenu = () => setIsMenuOpen(false)
  const dashboardLink = user ? dashboardForRole(user.role) : null
  const complaintLink = user?.role === 'ROLE_CITIZEN' ? '/complaints/new' : dashboardLink || '/login'
  const handleLogout = () => { logout(); closeMenu() }
  return <header className="site-header"><nav className="navbar container" aria-label="Primary navigation">
    <Link className="brand" to="/" onClick={closeMenu} aria-label="CMS home"><span className="brand-mark" aria-hidden="true">C</span><span>CMS</span></Link>
    <button className="menu-toggle" type="button" aria-expanded={isMenuOpen} aria-controls="primary-menu" onClick={() => setIsMenuOpen((open) => !open)}><span className="visually-hidden">{isMenuOpen ? 'Close' : 'Open'} menu</span><span aria-hidden="true">☰</span></button>
    <div className={`nav-content ${isMenuOpen ? 'is-open' : ''}`} id="primary-menu"><div className="nav-links">{navItems.map((item) => item.to === '/' ? <NavLink key={item.label} to={item.to} onClick={closeMenu}>{item.label}</NavLink> : <a key={item.label} href={item.to} onClick={closeMenu}>{item.label}</a>)}</div><div className="nav-actions">{isInitializing ? <span className="nav-status">Checking session…</span> : user ? <><Link className="text-link" to={dashboardLink} onClick={closeMenu}>{user.name}</Link><button className="button button-quiet" type="button" onClick={handleLogout}>Logout</button></> : <><Link className="text-link" to="/login" onClick={closeMenu}>Login</Link><Link className="button button-quiet" to="/register" onClick={closeMenu}>Register</Link></>}<Link className="button button-primary" to={complaintLink} onClick={closeMenu}>Raise a complaint</Link></div></div>
  </nav></header>
}

export default Navbar
