import { useState } from 'react'
import ComplaintStatusBadge from './ComplaintStatusBadge'

const priorities = ['LOW', 'MEDIUM', 'HIGH']
const label = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) : 'Not available'
const formatDate = (value) => value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(value)) : 'Date unavailable'

function AdminComplaintCard({ complaint, officers, onPriorityUpdated, onAssigned }) {
  const [priority, setPriority] = useState(complaint.priority || 'MEDIUM')
  const [isSaving, setIsSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [officerId, setOfficerId] = useState('')
  const [remarks, setRemarks] = useState('')
  const [isAssigning, setIsAssigning] = useState(false)
  const [assignmentMessage, setAssignmentMessage] = useState('')
  const canAssign = ['SUBMITTED', 'ASSIGNED'].includes(complaint.status)

  const savePriority = async () => {
    if (priority === complaint.priority) return
    setIsSaving(true); setMessage('')
    try { await onPriorityUpdated(complaint.complaintId, priority); setMessage('Priority updated.') } catch (error) { setMessage(error.message || 'Unable to update priority.') } finally { setIsSaving(false) }
  }
  const submitAssignment = async (event) => {
    event.preventDefault(); setAssignmentMessage('')
    if (!officerId) { setAssignmentMessage('Choose a case officer.'); return }
    setIsAssigning(true)
    try { await onAssigned(complaint.complaintId, { officerId: Number(officerId), remarks: remarks.trim() || null }); setAssignmentMessage('Officer assigned.'); setOfficerId(''); setRemarks('') } catch (error) { setAssignmentMessage(error.message || 'Unable to assign this complaint.') } finally { setIsAssigning(false) }
  }

  return <article className="admin-complaint-card"><div className="complaint-card-heading"><div><p className="complaint-id">Complaint #{complaint.complaintId ?? '—'}</p><h3>{complaint.title || 'Untitled complaint'}</h3></div><ComplaintStatusBadge value={complaint.status} /></div><div className="admin-complaint-details"><div><span>Category</span><strong>{complaint.category || 'Not available'}</strong></div><div><span>Citizen</span><strong>{complaint.citizenName || 'Not available'}</strong></div><div><span>Assigned officer</span><strong>{complaint.officerName || 'Unassigned'}</strong></div><div><span>Submitted</span><strong>{formatDate(complaint.createdAt)}</strong></div><div className="detail-wide"><span>Location</span><strong>{complaint.address || 'Not available'}</strong></div></div><div className="admin-card-actions"><div className="priority-editor"><label>Official priority<select value={priority} onChange={(event) => { setPriority(event.target.value); setMessage('') }} disabled={isSaving}>{priorities.map((option) => <option key={option} value={option}>{label(option)}</option>)}</select></label><button className="button button-secondary" type="button" onClick={savePriority} disabled={isSaving || priority === complaint.priority}>{isSaving ? 'Saving…' : 'Save priority'}</button><ComplaintStatusBadge value={complaint.priority} type="priority" />{message && <p className={message === 'Priority updated.' ? 'inline-success' : 'inline-error'} role="status">{message}</p>}</div>{canAssign && <form className="assignment-editor" onSubmit={submitAssignment}><label>Assign officer<select value={officerId} onChange={(event) => { setOfficerId(event.target.value); setAssignmentMessage('') }} disabled={isAssigning || !officers.length}><option value="">{officers.length ? 'Select a case officer' : 'No officers available'}</option>{officers.map((officer) => <option key={officer.userId} value={officer.userId}>{officer.firstName} {officer.lastName} — {officer.employeeNumber}</option>)}</select></label><label>Remarks <span className="optional">(optional)</span><input value={remarks} onChange={(event) => setRemarks(event.target.value)} maxLength="2000" disabled={isAssigning || !officers.length} /></label><button className="button button-primary" type="submit" disabled={isAssigning || !officers.length}>{isAssigning ? 'Assigning…' : 'Assign officer'}</button>{assignmentMessage && <p className={assignmentMessage === 'Officer assigned.' ? 'inline-success' : 'inline-error'} role="status">{assignmentMessage}</p>}</form>}</div></article>
}

export default AdminComplaintCard
