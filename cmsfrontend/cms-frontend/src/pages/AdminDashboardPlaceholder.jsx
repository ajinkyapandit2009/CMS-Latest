import { useCallback, useEffect, useMemo, useState } from 'react'
import AdminComplaintCard from '../components/AdminComplaintCard'
import ComplaintStatistics from '../components/ComplaintStatistics'
import OfficerWorkloadPanel from '../components/OfficerWorkloadPanel'
import { useAuth } from '../context/useAuth'
import { assignComplaint, getAdminComplaints, getCaseOfficers, getOfficerWorkloads, updateComplaintPriority } from '../services/adminService'

const statuses = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED']
const priorities = ['LOW', 'MEDIUM', 'HIGH']
const label = (value) => value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())

function AdminDashboardPlaceholder() {
  const { user, token } = useAuth()
  const [complaints, setComplaints] = useState([])
  const [officers, setOfficers] = useState([])
  const [workloads, setWorkloads] = useState([])
  const [filters, setFilters] = useState({ status: '', priority: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [workloadsLoading, setWorkloadsLoading] = useState(true)
  const [workloadsError, setWorkloadsError] = useState('')

  const loadComplaints = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const result = await getAdminComplaints({}, token)
      setComplaints(Array.isArray(result) ? result : [])
    } catch (requestError) {
      setError(requestError.message || 'Unable to load complaints.')
    } finally {
      setIsLoading(false)
    }
  }, [token])

  const loadWorkloads = useCallback(async () => {
    setWorkloadsLoading(true)
    setWorkloadsError('')
    try {
      const result = await getOfficerWorkloads(token)
      setWorkloads(Array.isArray(result) ? result : [])
    } catch (requestError) {
      setWorkloadsError(requestError.message || 'Unable to load officer workloads.')
    } finally {
      setWorkloadsLoading(false)
    }
  }, [token])

  useEffect(() => {
    let current = true
    Promise.all([getAdminComplaints({}, token), getCaseOfficers(token)])
      .then(([complaintResult, officerResult]) => {
        if (current) {
          setComplaints(Array.isArray(complaintResult) ? complaintResult : [])
          setOfficers(Array.isArray(officerResult) ? officerResult : [])
        }
      })
      .catch((requestError) => { if (current) setError(requestError.message || 'Unable to load complaint management data.') })
      .finally(() => { if (current) setIsLoading(false) })
    getOfficerWorkloads(token)
      .then((result) => { if (current) setWorkloads(Array.isArray(result) ? result : []) })
      .catch((requestError) => { if (current) setWorkloadsError(requestError.message || 'Unable to load officer workloads.') })
      .finally(() => { if (current) setWorkloadsLoading(false) })
    return () => { current = false }
  }, [token])

  const changeFilter = (event) => setFilters((current) => ({ ...current, [event.target.name]: event.target.value }))
  const visibleComplaints = useMemo(() => complaints.filter((item) =>
    (!filters.status || item.status === filters.status) && (!filters.priority || item.priority === filters.priority)), [complaints, filters])
  const savePriority = async (complaintId, priority) => {
    await updateComplaintPriority(complaintId, priority, token)
    await loadComplaints()
  }
  const submitAssignment = async (complaintId, assignment) => {
    await assignComplaint(complaintId, assignment, token)
    await Promise.all([loadComplaints(), loadWorkloads()])
  }

  return <section className="admin-dashboard container">
    <div className="dashboard-intro"><div><p className="eyebrow">Administration</p><h1>Admin Dashboard</h1><p>Welcome back, {user.name}. Review complaints, manage official priority, and assign eligible case officers.</p></div></div>
    <OfficerWorkloadPanel workloads={workloads} isLoading={workloadsLoading} error={workloadsError} onRetry={loadWorkloads} />
    {isLoading ? <div className="dashboard-state" role="status">Loading complaint management data…</div>
      : error ? <div className="dashboard-state dashboard-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={loadComplaints}>Try again</button></div>
        : <>
          <ComplaintStatistics complaints={complaints} />
          <div className="management-header" id="complaint-management"><div><h2>Complaint management</h2><p>Filter complaints, set official priority, and assign case officers.</p></div><button className="button button-secondary" type="button" onClick={() => { void Promise.all([loadComplaints(), loadWorkloads()]) }}>Refresh</button></div>
          <div className="admin-filters"><label>Status<select name="status" value={filters.status} onChange={changeFilter}><option value="">All statuses</option>{statuses.map((status) => <option key={status} value={status}>{label(status)}</option>)}</select></label><label>Priority<select name="priority" value={filters.priority} onChange={changeFilter}><option value="">All priorities</option>{priorities.map((priority) => <option key={priority} value={priority}>{label(priority)}</option>)}</select></label></div>
          {visibleComplaints.length ? <div className="admin-complaints-list">{visibleComplaints.map((complaint) => <AdminComplaintCard key={complaint.complaintId} complaint={complaint} officers={officers} workloads={workloads} onPriorityUpdated={savePriority} onAssigned={submitAssignment} />)}</div>
            : <div className="dashboard-state"><h2>No matching complaints</h2><p>There are no complaints for the currently selected filters.</p></div>}
        </>}
  </section>
}

export default AdminDashboardPlaceholder
