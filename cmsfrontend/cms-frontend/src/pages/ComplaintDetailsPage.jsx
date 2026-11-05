import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import AdminComplaintCard from '../components/AdminComplaintCard'
import ComplaintCard from '../components/ComplaintCard'
import OfficerComplaintCard from '../components/OfficerComplaintCard'
import CitizenEscalations from '../components/CitizenEscalations'
import { useAuth } from '../context/useAuth'
import { assignComplaint, getCaseOfficers, getOfficerWorkloads, updateComplaintPriority } from '../services/adminService'
import { getComplaint } from '../services/complaintService'
import { updateOfficerComplaintStatus } from '../services/officerService'
import { formatComplaintDate } from '../services/complaintListUtils'
import '../styles/complaint-pages.css'
import '../styles/escalations.css'

function ComplaintDetailRecord({ complaintId }) {
  const { user, token } = useAuth()
  const location = useLocation()
  const [result, setResult] = useState({ complaint: null, officers: [], workloads: [], officersError: false, workloadsError: false, error: '', isLoading: true })
  const [refresh, setRefresh] = useState(0)
  const [message, setMessage] = useState('')
  const backSearch = typeof location.state?.listSearch === 'string' ? new URLSearchParams(location.state.listSearch).toString() : ''

  useEffect(() => {
    let current = true
    const officers = user.role === 'ROLE_ADMIN'
      ? getCaseOfficers(token).then((data) => ({ officers: Array.isArray(data) ? data : [], officersError: false })).catch(() => ({ officers: [], officersError: true }))
      : Promise.resolve({ officers: [], officersError: false })
    const workloads = user.role === 'ROLE_ADMIN'
      ? getOfficerWorkloads(token).then((data) => ({ workloads: Array.isArray(data) ? data : [], workloadsError: false })).catch(() => ({ workloads: [], workloadsError: true }))
      : Promise.resolve({ workloads: [], workloadsError: false })
    Promise.all([getComplaint(complaintId, token), officers, workloads])
      .then(([complaint, officerResult, workloadResult]) => { if (current) setResult({ complaint, ...officerResult, ...workloadResult, error: '', isLoading: false }) })
      .catch((error) => {
        const text = error.status === 401 ? 'Your session expired. Sign out and sign in again.'
          : [403, 404].includes(error.status) ? 'This complaint is unavailable for your account.' : 'The complaint could not be loaded. Please try again.'
        if (current) setResult({ complaint: null, officers: [], workloads: [], officersError: false, workloadsError: false, error: text, isLoading: false })
      })
    return () => { current = false }
  }, [complaintId, token, user.role, refresh])

  const reload = () => {
    setResult({ complaint: null, officers: [], workloads: [], officersError: false, workloadsError: false, error: '', isLoading: true })
    setMessage('')
    setRefresh((value) => value + 1)
  }
  const refreshAfterAction = async (successMessage, refreshWorkloads = false) => {
    const [complaint, workloads] = await Promise.all([
      getComplaint(complaintId, token),
      refreshWorkloads ? getOfficerWorkloads(token).catch(() => null) : Promise.resolve(null),
    ])
    setResult((current) => ({ ...current, complaint,
      ...(refreshWorkloads ? { workloads: Array.isArray(workloads) ? workloads : current.workloads, workloadsError: !Array.isArray(workloads) } : {}),
    }))
    setMessage(successMessage)
  }
  const savePriority = async (id, priority) => { await updateComplaintPriority(id, priority, token); await refreshAfterAction('Priority updated.') }
  const saveAssignment = async (id, assignment) => { await assignComplaint(id, assignment, token); await refreshAfterAction('Officer assignment updated.', true) }
  const saveStatus = async (id, update) => { await updateOfficerComplaintStatus(id, update, token); await refreshAfterAction('Complaint status updated.') }
  const complaint = result.complaint
  const cardKey = complaint ? `${complaint.complaintId}:${complaint.status}:${complaint.priority}:${complaint.officerId}` : ''

  return <section className="complaint-details-page container">
    <Link className="complaint-back-link" to={`/complaints${backSearch ? `?${backSearch}` : ''}`}>← Back to complaints</Link>
    <div className="management-header"><div><p className="eyebrow">Complaint record</p><h1>Complaint #{complaintId}</h1></div><button className="button button-secondary" type="button" onClick={reload} disabled={result.isLoading}>Refresh</button></div>
    {result.isLoading ? <div className="dashboard-state" role="status">Loading complaint details...</div> : result.error ? <div className="dashboard-state dashboard-error" role="alert"><p>{result.error}</p><button className="button button-secondary" type="button" onClick={reload}>Try again</button></div> : complaint && <>
      {message && <p className="form-message form-success" role="status">{message}</p>}
      <dl className="complaint-detail-summary"><div><dt>Submitted</dt><dd>{formatComplaintDate(complaint.createdAt, true)}</dd></div><div><dt>Citizen</dt><dd>{complaint.citizenName || (complaint.citizenId ? `Citizen #${complaint.citizenId}` : 'Not available')}</dd></div>{complaint.citizenEmail && <div><dt>Citizen email</dt><dd>{complaint.citizenEmail}</dd></div>}<div><dt>Assigned officer</dt><dd>{complaint.officerName || (complaint.officerId ? `Officer #${complaint.officerId}` : 'Unassigned')}{complaint.officerEmployeeNumber && ` (${complaint.officerEmployeeNumber})`}</dd></div></dl>
      {result.officersError && <p className="form-message form-error" role="alert">The officer list could not be loaded. Refresh to retry assignment options.</p>}
      {user.role === 'ROLE_ADMIN' && result.workloadsError && <p className="form-message form-error" role="alert">Officer workloads are unavailable. Refresh to retry; manual assignment remains available.</p>}
      {user.role === 'ROLE_ADMIN' && <p className="complaint-detail-description">{complaint.description || 'No description provided.'}</p>}
      {user.role === 'ROLE_ADMIN' ? <AdminComplaintCard key={cardKey} complaint={complaint} officers={result.officers} workloads={result.workloadsError ? [] : result.workloads} onPriorityUpdated={savePriority} onAssigned={saveAssignment} />
        : user.role === 'ROLE_OFFICER' ? <OfficerComplaintCard key={cardKey} complaint={complaint} onStatusUpdated={saveStatus} /> : <ComplaintCard complaint={complaint} />}
      {user.role === 'ROLE_OFFICER' && <dl className="complaint-detail-summary"><div><dt>Latitude</dt><dd>{complaint.latitude ?? 'Not available'}</dd></div><div><dt>Longitude</dt><dd>{complaint.longitude ?? 'Not available'}</dd></div></dl>}
      {user.role === 'ROLE_CITIZEN' && <CitizenEscalations complaint={complaint} />}
    </>}
  </section>
}

function ComplaintDetailsPage() {
  const { complaintId } = useParams()
  if (!/^[1-9]\d*$/.test(complaintId || '') || Number(complaintId) > 2147483647) {
    return <section className="complaint-details-page container"><div className="dashboard-state dashboard-error" role="alert"><p>This complaint reference is invalid.</p><Link className="button button-secondary" to="/complaints">Back to complaints</Link></div></section>
  }
  // A new record gets a fresh request/card state when navigating between detail URLs.
  return <ComplaintDetailRecord key={complaintId} complaintId={complaintId} />
}

export default ComplaintDetailsPage
