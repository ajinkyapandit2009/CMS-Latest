export const complaintStatuses = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'REJECTED']
export const complaintPriorities = ['LOW', 'MEDIUM', 'HIGH']
export const complaintSorts = { newest: 'Newest first', oldest: 'Oldest first', title: 'Title A–Z', priority: 'Highest priority first' }

export const complaintLabel = (value) => value ? value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase()) : 'Not available'

export function formatComplaintDate(value, includeTime = false) {
  const date = value ? new Date(value) : null
  if (!date || Number.isNaN(date.getTime())) return 'Date unavailable'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', ...(includeTime ? { timeStyle: 'short' } : {}) }).format(date)
}

export function selectComplaints(complaints, { search = '', status = '', priority = '', sort = 'newest' } = {}) {
  const needle = search.trim().toLowerCase()
  const selected = complaints.filter((complaint) =>
    (!status || complaint.status === status)
    && (!priority || complaint.priority === priority)
    && (!needle || [complaint.complaintId, complaint.title, complaint.category, complaint.address, complaint.officerName, complaint.citizenName]
      .some((value) => value != null && String(value).toLowerCase().includes(needle))))
  const date = (value) => Date.parse(value) || 0
  const rank = { HIGH: 3, MEDIUM: 2, LOW: 1 }
  return selected.sort((a, b) => {
    if (sort === 'title') return (a.title || '').localeCompare(b.title || '') || (b.complaintId - a.complaintId)
    if (sort === 'oldest') return date(a.createdAt) - date(b.createdAt) || (a.complaintId - b.complaintId)
    if (sort === 'priority') return (rank[b.priority] || 0) - (rank[a.priority] || 0) || date(b.createdAt) - date(a.createdAt) || (b.complaintId - a.complaintId)
    return date(b.createdAt) - date(a.createdAt) || (b.complaintId - a.complaintId)
  })
}
