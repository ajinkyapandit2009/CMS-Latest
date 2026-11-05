import { Link } from 'react-router-dom'
import { formatComplaintDate } from '../services/complaintListUtils'

const outcomeLabel = (status) => status === 'ACCEPTED' ? 'Accepted' : status === 'DECLINED' ? 'Declined' : 'Pending review'

function EscalationHistory({ escalations, showComplaint = false }) {
  if (!escalations.length) return <p className="escalation-empty">No escalations have been submitted yet.</p>
  return <div className="escalation-history-list">{escalations.map((item) => <article className="escalation-entry" key={item.escalationId}>
    <div className="escalation-entry-heading"><div>
      <p className="complaint-id">Escalation #{item.escalationId}</p>
      {showComplaint && <h3><Link className="complaint-title-link" to={`/complaints/${item.complaintId}`}>Complaint #{item.complaintId}: {item.complaintTitle}</Link></h3>}
    </div><span className={`escalation-status ${item.reviewStatus?.toLowerCase() || 'pending'}`}>{outcomeLabel(item.reviewStatus)}</span></div>
    <p className="escalation-reason">{item.reason}</p>
    <dl className="escalation-meta"><div><dt>Submitted</dt><dd>{formatComplaintDate(item.createdAt, true)}</dd></div>
      {item.reviewedAt && <div><dt>Reviewed</dt><dd>{formatComplaintDate(item.reviewedAt, true)}</dd></div>}
      {item.reviewerName && <div><dt>Reviewed by</dt><dd>{item.reviewerName}</dd></div>}
    </dl>
    {item.remarks && <p className="escalation-remarks"><strong>Admin remarks:</strong> {item.remarks}</p>}
  </article>)}</div>
}

export default EscalationHistory
