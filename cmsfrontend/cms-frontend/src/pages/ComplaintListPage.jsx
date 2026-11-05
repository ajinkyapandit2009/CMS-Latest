import { useEffect, useState } from 'react'
import ComplaintList from '../components/ComplaintList'
import ComplaintStatistics from '../components/ComplaintStatistics'
import { useAuth } from '../context/useAuth'
import { getAccessibleComplaints } from '../services/complaintService'
import '../styles/complaint-pages.css'

function ComplaintListPage() {
  const { user, token } = useAuth()
  const [result, setResult] = useState({ complaints: [], error: '', isLoading: true })
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let current = true
    getAccessibleComplaints(user.role, token)
      .then((complaints) => { if (current) setResult({ complaints: Array.isArray(complaints) ? complaints : [], error: '', isLoading: false }) })
      .catch((error) => { if (current) setResult({ complaints: [], error: error.status === 401 ? 'Your session expired. Sign out and sign in again.' : 'Complaints could not be loaded. Please try again.', isLoading: false }) })
    return () => { current = false }
  }, [user.role, token, refresh])

  const reload = () => { setResult({ complaints: [], error: '', isLoading: true }); setRefresh((value) => value + 1) }
  const description = { ROLE_CITIZEN: 'Review your reported civic issues.', ROLE_OFFICER: 'Review complaints currently assigned to you.', ROLE_ADMIN: 'Review and manage all reported complaints.' }[user.role]

  return <section className="complaint-list-page container"><div className="management-header"><div><p className="eyebrow">Complaint records</p><h1>Complaints</h1><p>{description}</p></div><button className="button button-secondary" type="button" onClick={reload} disabled={result.isLoading}>Refresh</button></div>{result.isLoading ? <div className="dashboard-state" role="status">Loading complaints...</div> : result.error ? <div className="dashboard-state dashboard-error" role="alert"><p>{result.error}</p><button className="button button-secondary" type="button" onClick={reload}>Try again</button></div> : <><ComplaintStatistics complaints={result.complaints} /><ComplaintList complaints={result.complaints} /></>}</section>
}

export default ComplaintListPage
