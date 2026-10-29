import ComplaintStatusBadge from './ComplaintStatusBadge'

function formatDate(value) { return value ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(value)) : 'Date unavailable' }

function ComplaintCard({ complaint }) {
  return <article className="complaint-card"><div className="complaint-card-heading"><div><p className="complaint-id">Complaint #{complaint.complaintId ?? '—'}</p><h3>{complaint.title || 'Untitled complaint'}</h3></div><ComplaintStatusBadge value={complaint.status} /></div><p className="complaint-card-description">{complaint.description || 'No description provided.'}</p><dl className="complaint-meta"><div><dt>Category</dt><dd>{complaint.category || 'Not available'}</dd></div><div><dt>Priority</dt><dd><ComplaintStatusBadge value={complaint.priority} type="priority" /></dd></div><div><dt>Submitted</dt><dd>{formatDate(complaint.createdAt)}</dd></div><div><dt>Location</dt><dd>{complaint.address || 'Not available'}</dd></div></dl></article>
}

export default ComplaintCard
