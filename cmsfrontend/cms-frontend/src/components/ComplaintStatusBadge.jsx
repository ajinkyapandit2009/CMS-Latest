function readable(value) { return value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) : 'Not available' }

function ComplaintStatusBadge({ value, type = 'status' }) {
  return <span className={`complaint-badge ${type} ${value ? value.toLowerCase() : ''}`}>{readable(value)}</span>
}

export default ComplaintStatusBadge
