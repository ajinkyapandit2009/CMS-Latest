import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { formatComplaintDate } from '../services/complaintListUtils'
import { getAdminEscalations, reviewEscalation } from '../services/escalationService'
import '../styles/escalations.css'

const filters = ['PENDING', 'ACCEPTED', 'DECLINED']
const outcomeLabel = (status) => status === 'ACCEPTED' ? 'Accepted' : status === 'DECLINED' ? 'Declined' : 'Pending review'

function ReviewCard({ item, token, onReviewed }) {
  const [outcome, setOutcome] = useState('')
  const [remarks, setRemarks] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')
  const savingRef = useRef(false)

  const submit = async (event) => {
    event.preventDefault()
    if (savingRef.current) return
    setError('')
    if (!filters.slice(1).includes(outcome) || !remarks.trim() || remarks.trim().length > 4000) {
      setError('Choose Accepted or Declined and enter remarks of 1 to 4000 characters.')
      return
    }
    savingRef.current = true
    setIsSaving(true)
    try {
      const reviewed = await reviewEscalation(item.escalationId, outcome, remarks.trim(), token)
      onReviewed(reviewed)
    } catch (requestError) {
      setError(requestError.status === 409 ? 'This escalation has already been reviewed. Refresh the list.'
        : requestError.status === 401 ? 'Your session expired. Sign in again to review.'
          : (requestError.message || 'Review could not be saved. Try again.'))
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  return <article className="escalation-entry">
    <div className="escalation-entry-heading"><div><p className="complaint-id">Escalation #{item.escalationId}</p><h2><Link className="complaint-title-link" to={`/complaints/${item.complaintId}`}>Complaint #{item.complaintId}: {item.complaintTitle}</Link></h2></div><span className={`escalation-status ${item.reviewStatus?.toLowerCase() || 'pending'}`}>{outcomeLabel(item.reviewStatus)}</span></div>
    <dl className="escalation-meta"><div><dt>Citizen</dt><dd>{item.citizenName || `Citizen #${item.citizenId}`}</dd></div><div><dt>Submitted</dt><dd>{formatComplaintDate(item.createdAt, true)}</dd></div>
      {item.reviewedAt && <div><dt>Reviewed</dt><dd>{formatComplaintDate(item.reviewedAt, true)}</dd></div>}
      {item.reviewerName && <div><dt>Reviewed by</dt><dd>{item.reviewerName}</dd></div>}
    </dl>
    <div className="escalation-text"><strong>Citizen’s reason</strong><p>{item.reason}</p></div>
    {item.reviewStatus === 'PENDING' ? <form className="escalation-form" onSubmit={submit} noValidate>
      <div className="escalation-review-fields"><label htmlFor={`outcome-${item.escalationId}`}>Review outcome <span aria-hidden="true">*</span><select id={`outcome-${item.escalationId}`} value={outcome} onChange={(event) => setOutcome(event.target.value)} required disabled={isSaving}><option value="">Choose an outcome</option><option value="ACCEPTED">Accepted</option><option value="DECLINED">Declined</option></select></label>
        <label htmlFor={`remarks-${item.escalationId}`}>Admin remarks <span aria-hidden="true">*</span><textarea id={`remarks-${item.escalationId}`} required maxLength="4000" rows="3" value={remarks} onChange={(event) => setRemarks(event.target.value)} disabled={isSaving} /></label></div>
      {error && <p className="inline-error" role="alert">{error}</p>}
      <button className="button button-primary" type="submit" disabled={isSaving}>{isSaving ? 'Saving review...' : 'Save review'}</button>
    </form> : item.remarks && <p className="escalation-remarks"><strong>Admin remarks:</strong> {item.remarks}</p>}
  </article>
}

function AdminEscalationsPage() {
  const { token } = useAuth()
  const [status, setStatus] = useState('')
  const [items, setItems] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let current = true
    getAdminEscalations(status, token).then((data) => {
      if (current) { setItems(Array.isArray(data) ? data : []); setError('') }
    }).catch((requestError) => {
      if (current) setError(requestError.status === 401 ? 'Your session expired. Sign in again.' : 'Escalations could not be loaded. Try again.')
    }).finally(() => { if (current) setIsLoading(false) })
    return () => { current = false }
  }, [status, token, refresh])

  const afterReview = (reviewed) => {
    setItems((current) => current.map((item) => item.escalationId === reviewed.escalationId ? reviewed : item).filter((item) => !status || item.reviewStatus === status))
    setSuccess(`Escalation #${reviewed.escalationId} was ${reviewed.reviewStatus.toLowerCase()}.`)
    setRefresh((value) => value + 1)
  }

  return <section className="escalations-page container">
    <div className="management-header"><div><p className="eyebrow">Admin workspace</p><h1>Escalations</h1><p>Review Citizen requests without changing complaint status.</p></div><button className="button button-secondary" type="button" disabled={isLoading} onClick={() => { setIsLoading(true); setRefresh((value) => value + 1) }}>Refresh</button></div>
    <div className="escalation-filters"><label htmlFor="escalation-status-filter">Review status</label><select id="escalation-status-filter" value={status} onChange={(event) => { setStatus(event.target.value); setIsLoading(true); setSuccess('') }}><option value="">All escalations</option>{filters.map((value) => <option key={value} value={value}>{outcomeLabel(value)}</option>)}</select></div>
    {success && <p className="inline-success" role="status">{success}</p>}
    {isLoading ? <div className="dashboard-state" role="status">Loading escalations...</div> : error ? <div className="dashboard-state dashboard-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={() => { setIsLoading(true); setRefresh((value) => value + 1) }}>Try again</button></div> : items.length ? <div className="escalation-history-list">{items.map((item) => <ReviewCard key={item.escalationId} item={item} token={token} onReviewed={afterReview} />)}</div> : <div className="dashboard-state">No escalations match this review status.</div>}
  </section>
}

export default AdminEscalationsPage
