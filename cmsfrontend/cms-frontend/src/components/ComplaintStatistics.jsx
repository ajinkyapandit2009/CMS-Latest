import { Link, useLocation } from 'react-router-dom'
import { complaintStatuses } from '../services/complaintListUtils'
import { countComplaints } from '../services/complaintStatistics'
import '../styles/complaint-statistics.css'

const cards = [
  { label: 'Total', status: '' },
  ...complaintStatuses.map((status) => ({
    label: status === 'IN_PROGRESS' ? 'In Progress' : status[0] + status.slice(1).toLowerCase(),
    status,
  })),
]

function ComplaintStatistics({ complaints }) {
  const counts = countComplaints(complaints)
  const location = useLocation()
  const selectedStatus = new URLSearchParams(location.search).get('status')

  return <nav className="complaint-statistics" aria-label="Complaint statistics">
    {cards.map(({ label, status }) => {
      const count = status ? counts[status] : counts.total
      const selected = location.pathname === '/complaints' && (status ? selectedStatus === status : !selectedStatus)
      return <Link key={label} className={`complaint-stat-card${selected ? ' is-selected' : ''}`} to={status ? `/complaints?status=${status}` : '/complaints'} aria-label={`${label}: ${count} complaints. View ${status ? label.toLowerCase() : 'all'} complaints.`} aria-current={selected ? 'page' : undefined}>
        <span className="complaint-stat-label">{label}</span>
        <strong className="complaint-stat-count">{count}</strong>
      </Link>
    })}
  </nav>
}

export default ComplaintStatistics
