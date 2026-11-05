import { Link, useSearchParams } from 'react-router-dom'
import ComplaintStatusBadge from './ComplaintStatusBadge'
import { complaintLabel, complaintPriorities, complaintSorts, complaintStatuses, formatComplaintDate, selectComplaints } from '../services/complaintListUtils'

function ComplaintList({ complaints }) {
  const [params, setParams] = useSearchParams()
  const filters = {
    search: params.get('search') || '',
    status: complaintStatuses.includes(params.get('status')) ? params.get('status') : '',
    priority: complaintPriorities.includes(params.get('priority')) ? params.get('priority') : '',
    sort: Object.hasOwn(complaintSorts, params.get('sort')) ? params.get('sort') : 'newest',
  }
  const filtered = selectComplaints(complaints, filters)
  const updateFilter = (event) => {
    const next = new URLSearchParams(params)
    const { name, value } = event.target
    if (value) next.set(name, value)
    else next.delete(name)
    setParams(next, { replace: true })
  }

  return <div className="complaint-list-view">
    <div className="complaint-list-controls">
      <label className="complaint-search">Search complaints<input type="search" name="search" value={filters.search} onChange={updateFilter} placeholder="ID, title, category, address or name" /></label>
      <label>Status<select name="status" value={filters.status} onChange={updateFilter}><option value="">All statuses</option>{complaintStatuses.map((status) => <option key={status} value={status}>{complaintLabel(status)}</option>)}</select></label>
      <label>Priority<select name="priority" value={filters.priority} onChange={updateFilter}><option value="">All priorities</option>{complaintPriorities.map((priority) => <option key={priority} value={priority}>{complaintLabel(priority)}</option>)}</select></label>
      <label>Sort by<select name="sort" value={filters.sort} onChange={updateFilter}>{Object.entries(complaintSorts).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
    </div>
    <div className="complaint-list-result"><p role="status">{filtered.length} of {complaints.length} complaints</p><button className="button button-quiet" type="button" onClick={() => setParams({})}>Clear filters</button></div>
    {filtered.length ? <div className="complaint-table-scroll" tabIndex={0} role="region" aria-label="Complaint list"><table className="complaint-table"><caption className="visually-hidden">Complaints accessible to your account</caption><thead><tr>{['ID', 'Title', 'Category', 'Status', 'Priority', 'Submitted', 'Assigned officer', 'Details'].map((heading) => <th key={heading} scope="col">{heading}</th>)}</tr></thead><tbody>{filtered.map((complaint) => <tr key={complaint.complaintId}>
      <td>#{complaint.complaintId}</td><td><Link className="complaint-title-link" to={`/complaints/${complaint.complaintId}`} state={{ listSearch: params.toString() }}>{complaint.title || 'Untitled complaint'}</Link></td><td>{complaint.category || 'Not available'}</td><td><ComplaintStatusBadge value={complaint.status} /></td><td><ComplaintStatusBadge value={complaint.priority} type="priority" /></td><td>{formatComplaintDate(complaint.createdAt)}</td><td>{complaint.officerName || (complaint.officerId ? `Officer #${complaint.officerId}` : 'Unassigned')}</td><td><Link className="complaint-details-link" to={`/complaints/${complaint.complaintId}`} state={{ listSearch: params.toString() }} aria-label={`View details for complaint ${complaint.complaintId}`}>View Details</Link></td>
    </tr>)}</tbody></table></div> : <div className="dashboard-state"><h2>{complaints.length ? 'No matching complaints' : 'No complaints available'}</h2><p>{complaints.length ? 'Try another search or clear the filters.' : 'Complaints available to your account will appear here.'}</p></div>}
  </div>
}

export default ComplaintList
