import { Route, Routes } from 'react-router-dom'
import Footer from './components/Footer'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import AccessDenied from './pages/AccessDenied'
import AdminDashboardPlaceholder from './pages/AdminDashboardPlaceholder'
import CitizenDashboardPlaceholder from './pages/CitizenDashboardPlaceholder'
import ComplaintPlaceholder from './pages/ComplaintPlaceholder'
import Home from './pages/Home'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import OfficerDashboardPlaceholder from './pages/OfficerDashboardPlaceholder'
import Register from './pages/Register'
import './styles/global.css'

function App() {
  return <div className="app-shell"><Navbar /><main className="site-main"><Routes>
    <Route path="/" element={<Home />} /><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} />
    <Route path="/citizen/dashboard" element={<ProtectedRoute allowedRole="ROLE_CITIZEN"><CitizenDashboardPlaceholder /></ProtectedRoute>} /><Route path="/officer/dashboard" element={<ProtectedRoute allowedRole="ROLE_OFFICER"><OfficerDashboardPlaceholder /></ProtectedRoute>} /><Route path="/admin/dashboard" element={<ProtectedRoute allowedRole="ROLE_ADMIN"><AdminDashboardPlaceholder /></ProtectedRoute>} />
    <Route path="/complaints/new" element={<ProtectedRoute allowedRole="ROLE_CITIZEN"><ComplaintPlaceholder /></ProtectedRoute>} /><Route path="/access-denied" element={<AccessDenied />} /><Route path="*" element={<NotFound />} />
  </Routes></main><Footer /></div>
}

export default App
