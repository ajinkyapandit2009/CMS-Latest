import { Outlet, Route, Routes } from 'react-router-dom'
import DashboardLayout from './components/DashboardLayout'
import Footer from './components/Footer'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import ScrollToTop from './components/ScrollToTop'
import AccessDenied from './pages/AccessDenied'
import AdminDashboardPlaceholder from './pages/AdminDashboardPlaceholder'
import AdminCreateOfficerPage from './pages/AdminCreateOfficerPage'
import AdminEscalationsPage from './pages/AdminEscalationsPage'
import CitizenDashboardPlaceholder from './pages/CitizenDashboardPlaceholder'
import ComplaintPlaceholder from './pages/ComplaintPlaceholder'
import ComplaintListPage from './pages/ComplaintListPage'
import ComplaintDetailsPage from './pages/ComplaintDetailsPage'
import Home from './pages/Home'
import Login from './pages/Login'
import NotFound from './pages/NotFound'
import OfficerDashboardPlaceholder from './pages/OfficerDashboardPlaceholder'
import Register from './pages/Register'
import ProfilePage from './pages/ProfilePage'
import MyEscalationsPage from './pages/MyEscalationsPage'
import ShareLocation from './pages/ShareLocation'
import './styles/global.css'

function PublicLayout() {
  return <div className="app-shell"><Navbar /><main className="site-main"><Outlet /></main><Footer /></div>
}

function App() {
  return <><ScrollToTop /><Routes>
    <Route element={<PublicLayout />}>
    <Route path="/" element={<Home />} /><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} />
    <Route path="/access-denied" element={<AccessDenied />} /><Route path="*" element={<NotFound />} />
    </Route>
    <Route path="/share-location/:token" element={<ShareLocation />} />
    <Route element={<ProtectedRoute><DashboardLayout /></ProtectedRoute>}>
    <Route path="/citizen/dashboard" element={<ProtectedRoute allowedRole="ROLE_CITIZEN"><CitizenDashboardPlaceholder /></ProtectedRoute>} /><Route path="/officer/dashboard" element={<ProtectedRoute allowedRole="ROLE_OFFICER"><OfficerDashboardPlaceholder /></ProtectedRoute>} /><Route path="/admin/dashboard" element={<ProtectedRoute allowedRole="ROLE_ADMIN"><AdminDashboardPlaceholder /></ProtectedRoute>} />
    <Route path="/admin/case-officers/new" element={<ProtectedRoute allowedRole="ROLE_ADMIN"><AdminCreateOfficerPage /></ProtectedRoute>} />
    <Route path="/admin/escalations" element={<ProtectedRoute allowedRole="ROLE_ADMIN"><AdminEscalationsPage /></ProtectedRoute>} />
    <Route path="/complaints/new" element={<ProtectedRoute allowedRole="ROLE_CITIZEN"><ComplaintPlaceholder /></ProtectedRoute>} />
    <Route path="/escalations/my" element={<ProtectedRoute allowedRole="ROLE_CITIZEN"><MyEscalationsPage /></ProtectedRoute>} />
    <Route path="/complaints" element={<ComplaintListPage />} />
    <Route path="/complaints/:complaintId" element={<ComplaintDetailsPage />} />
    <Route path="/profile" element={<ProfilePage />} />
    </Route>
  </Routes></>
}

export default App
