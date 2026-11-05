import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../context/useAuth'
import { getMyEscalations, isEscalationEligible, submitEscalation } from '../services/escalationService'
import EscalationHistory from './EscalationHistory'

function CitizenEscalations({ complaint }) {
  const { token, user } = useAuth()
  const [items, setItems] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const [reason, setReason] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const savingRef = useRef(false)
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let current = true
    getMyEscalations(token).then((data) => {
      if (current) { setItems(Array.isArray(data) ? data.filter((item) => Number(item.complaintId) === Number(complaint.complaintId)) : []); setLoadError('') }
    }).catch((error) => {
      if (current) setLoadError(error.status === 401 ? 'Your session expired. Sign in again to view escalations.' : 'Escalation history could not be loaded. Try again.')
    }).finally(() => { if (current) setIsLoading(false) })
    return () => { current = false }
  }, [complaint.complaintId, token, refresh])

  const eligible = isEscalationEligible(complaint, user)
  const pending = items.some((item) => item.reviewStatus === 'PENDING')
  const submit = async (event) => {
    event.preventDefault()
    if (savingRef.current || pending) return
    const trimmed = reason.trim()
    setFormError('')
    setSuccess('')
    if (!trimmed || trimmed.length > 4000) {
      setFormError('Enter a reason of 1 to 4000 characters.')
      return
    }
    savingRef.current = true
    setIsSaving(true)
    try {
      const created = await submitEscalation(complaint.complaintId, trimmed, token)
      setItems((current) => [created, ...current])
      setReason('')
      setIsOpen(false)
      setSuccess('Your escalation was submitted for Admin review.')
    } catch (error) {
      setFormError(error.status === 409 ? 'This complaint is not currently eligible, or an escalation is already pending. Refresh to check its status.'
        : error.status === 401 ? 'Your session expired. Sign in again to submit an escalation.'
          : [403, 404].includes(error.status) ? 'This complaint is unavailable for your account.' : (error.message || 'Escalation could not be submitted. Try again.'))
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  return <section className="escalation-panel" aria-labelledby="escalation-heading">
    <div className="escalation-panel-header"><div><p className="eyebrow">Separate review process</p><h2 id="escalation-heading">Escalations</h2><p>Escalation review does not change this complaint’s status.</p></div>
      {eligible && !pending && !isOpen && !isLoading && !loadError && <button className="button button-primary" type="button" onClick={() => { setFormError(''); setSuccess(''); setIsOpen(true) }}>Escalate Complaint</button>}
    </div>
    {isLoading ? <p role="status">Loading escalation history...</p> : loadError ? <div role="alert"><p className="inline-error">{loadError}</p><button className="button button-secondary" type="button" onClick={() => { setIsLoading(true); setRefresh((value) => value + 1) }}>Try again</button></div> : <>
      {pending && <p className="escalation-note">An escalation is pending Admin review.</p>}
      {success && <p className="inline-success" role="status">{success}</p>}
      {isOpen && <form className="escalation-form" onSubmit={submit} noValidate>
        <label htmlFor="escalation-reason">Reason for escalation <span aria-hidden="true">*</span></label>
        <textarea id="escalation-reason" name="reason" required maxLength="4000" rows="5" value={reason} onChange={(event) => setReason(event.target.value)} disabled={isSaving} aria-describedby={formError ? 'escalation-form-error' : undefined} />
        {formError && <p id="escalation-form-error" className="inline-error" role="alert">{formError}</p>}
        <div className="escalation-form-actions"><button className="button button-primary" type="submit" disabled={isSaving}>{isSaving ? 'Submitting...' : 'Submit escalation'}</button><button className="button button-secondary" type="button" disabled={isSaving} onClick={() => { setIsOpen(false); setFormError('') }}>Cancel</button></div>
      </form>}
      {!isOpen && formError && <p className="inline-error" role="alert">{formError}</p>}
      <EscalationHistory escalations={items} />
    </>}
  </section>
}

export default CitizenEscalations
