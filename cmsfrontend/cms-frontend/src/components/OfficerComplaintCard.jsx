import { Link } from 'react-router-dom'
import { useState } from 'react'
import AuthenticatedComplaintImage from './AuthenticatedComplaintImage'
import ComplaintStatusBadge from './ComplaintStatusBadge'

const nextStatuses = { ASSIGNED: ['IN_PROGRESS', 'REJECTED'], IN_PROGRESS: ['RESOLVED', 'REJECTED'] }
const label = (value) => value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase())
const formatDate = (value) => value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(value)) : 'Date unavailable'

function OfficerComplaintCard({ complaint, onStatusUpdated }) {
  const allowedStatuses = nextStatuses[complaint.status] || []
  const [status, setStatus] = useState(allowedStatuses[0] || '')
  const [remarks, setRemarks] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [message, setMessage] = useState('')
  const submit = async (event) => { event.preventDefault(); if (!status) return; setIsSaving(true); setMessage(''); try { await onStatusUpdated(complaint.complaintId, { status, remarks: remarks.trim() || null }); setMessage('Status updated.'); setRemarks('') } catch (error) { setMessage(error.message || 'Unable to update this complaint.') } finally { setIsSaving(false) } }

  return <article className="officer-complaint-card">
    <div className="complaint-card-heading"><div><p className="complaint-id">Complaint #{complaint.complaintId ?? '-'}</p><h3><Link className="complaint-title-link" to={`/complaints/${complaint.complaintId}`}>{complaint.title || 'Untitled complaint'}</Link></h3></div><ComplaintStatusBadge value={complaint.status} /></div>
    <div className="complaint-evidence"><AuthenticatedComplaintImage imageReference={complaint.imageUrl} alt={`Evidence for ${complaint.title || 'civic complaint'}`} /></div>
    <p className="complaint-card-description">{complaint.description || 'No description provided.'}</p>
    <div className="officer-complaint-details"><div><span>Category</span><strong>{complaint.category || 'Not available'}</strong></div><div><span>Priority</span><strong><ComplaintStatusBadge value={complaint.priority} type="priority" /></strong></div><div><span>Citizen</span><strong>{complaint.citizenName || (complaint.citizenId ? `Citizen #${complaint.citizenId}` : 'Not available')}</strong></div><div><span>Location</span><strong>{complaint.address || 'Not available'}</strong></div><div><span>Submitted</span><strong>{formatDate(complaint.createdAt)}</strong></div></div>
    {allowedStatuses.length > 0 ? <form className="status-editor" onSubmit={submit}><label>Update status<select value={status} onChange={(event) => { setStatus(event.target.value); setMessage('') }} disabled={isSaving}>{allowedStatuses.map((option) => <option key={option} value={option}>{label(option)}</option>)}</select></label><label>Remarks <span className="optional">(optional)</span><input value={remarks} onChange={(event) => setRemarks(event.target.value)} maxLength="2000" disabled={isSaving} /></label><button className="button button-primary" type="submit" disabled={isSaving}>{isSaving ? 'Updating...' : 'Update status'}</button>{message && <p className={message === 'Status updated.' ? 'inline-success' : 'inline-error'} role="status">{message}</p>}</form> : <p className="workflow-note">No further status actions are available for this complaint.</p>}
  </article>
}

export default OfficerComplaintCard
