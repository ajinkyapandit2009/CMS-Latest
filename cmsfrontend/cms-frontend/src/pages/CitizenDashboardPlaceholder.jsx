import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ComplaintCard from '../components/ComplaintCard'
import ComplaintStatistics from '../components/ComplaintStatistics'
import { useAuth } from '../context/useAuth'
import { getMyComplaints } from '../services/complaintService'

function CitizenDashboardPlaceholder() {
  const { user, token } = useAuth(); const [complaints, setComplaints] = useState([]); const [error, setError] = useState(''); const [isLoading, setIsLoading] = useState(true)
  const loadComplaints = useCallback(async () => { setIsLoading(true); setError(''); try { const result = await getMyComplaints(token); setComplaints(Array.isArray(result) ? result : []) } catch (requestError) { setError(requestError.message || 'Unable to load your complaints.') } finally { setIsLoading(false) } }, [token])
  useEffect(() => {
    let isCurrent = true
    getMyComplaints(token)
      .then((result) => { if (isCurrent) setComplaints(Array.isArray(result) ? result : []) })
      .catch((requestError) => { if (isCurrent) setError(requestError.message || 'Unable to load your complaints.') })
      .finally(() => { if (isCurrent) setIsLoading(false) })
    return () => { isCurrent = false }
  }, [token])
  return <section className="dashboard-page container"><div className="dashboard-intro"><div><p className="eyebrow">Citizen area</p><h1>Your Civic Dashboard</h1><p>Welcome back, {user.name}. Review your reported issues and raise a new complaint when something needs attention.</p></div><Link className="button button-primary" to="/complaints/new">Raise a Complaint <span aria-hidden="true">→</span></Link></div>{isLoading ? <div className="dashboard-state" role="status">Loading your complaints…</div> : error ? <div className="dashboard-state dashboard-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={loadComplaints}>Try again</button></div> : <><ComplaintStatistics complaints={complaints} /><div className="complaints-header"><div><h2>My complaints</h2><p>Your most recently submitted complaints appear first.</p></div><button className="button button-secondary" type="button" onClick={loadComplaints}>Refresh</button></div>{complaints.length ? <div className="complaints-list">{complaints.map((complaint) => <ComplaintCard key={complaint.complaintId} complaint={complaint} />)}</div> : <div className="dashboard-state"><h2>No complaints yet</h2><p>When you report a civic issue, it will appear here for you to review.</p><Link className="button button-primary" to="/complaints/new">Raise your first complaint</Link></div>}</>}</section>
}
export default CitizenDashboardPlaceholder
