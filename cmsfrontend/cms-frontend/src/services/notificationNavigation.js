export function notificationDestination(item, fallbackPath) {
  const id = item?.complaintId
  return Number.isInteger(id) && id > 0 && id <= 2147483647
    ? `/complaints/${id}`
    : fallbackPath
}
