import { complaintStatuses } from './complaintListUtils.js'

export function countComplaints(complaints) {
  const statusCounts = Object.fromEntries(complaintStatuses.map((status) => [status, 0]))
  for (const complaint of complaints) {
    if (Object.hasOwn(statusCounts, complaint.status)) statusCounts[complaint.status] += 1
  }
  return { total: complaints.length, ...statusCounts }
}
