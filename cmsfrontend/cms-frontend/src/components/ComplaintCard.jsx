import { Link } from 'react-router-dom'
import AuthenticatedComplaintImage from './AuthenticatedComplaintImage'
import ComplaintStatusBadge from './ComplaintStatusBadge'

function formatDate(value) { return value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(value)) : 'Date unavailable' }

function ComplaintCard({ complaint }) {
  return <article className="complaint-card">
    <div className="complaint-card-heading"><div><p className="complaint-id">Complaint #{complaint.complaintId ?? '-'}</p><h3><Link className="complaint-title-link" to={`/complaints/${complaint.complaintId}`}>{complaint.title || 'Untitled complaint'}</Link></h3></div><ComplaintStatusBadge value={complaint.status} /></div>
    <div className="complaint-evidence"><AuthenticatedComplaintImage imageReference={complaint.imageUrl} alt={`Evidence for ${complaint.title || 'civic complaint'}`} /></div>
    <p className="complaint-card-description">{complaint.description || 'No description provided.'}</p>
    <dl className="complaint-meta"><div><dt>Category</dt><dd>{complaint.category || 'Not available'}</dd></div><div><dt>Priority</dt><dd><ComplaintStatusBadge value={complaint.priority} type="priority" /></dd></div><div><dt>Submitted</dt><dd>{formatDate(complaint.createdAt)}</dd></div><div>
      <dt>Location</dt>
      <dd>{complaint.address || 'Not available'}</dd>
    </div>

      <div>
        <dt>Latitude</dt>
        <dd>{complaint.latitude ?? 'Not available'}</dd>
      </div>

      <div>
        <dt>Longitude</dt>
        <dd>{complaint.longitude ?? 'Not available'}</dd>
      </div></dl>
  </article>
}

export default ComplaintCard
