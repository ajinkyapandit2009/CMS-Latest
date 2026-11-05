export function createEvidenceOperations() {
  let photo = 0
  let location = 0
  let session = 0
  let upload = 0
  let address = 0
  let submission = null

  const newPhoto = () => { photo += 1; location += 1; session += 1; upload += 1; address += 1; return photo }
  const isPhoto = (id) => id === photo
  const newLocation = () => { location += 1; session += 1; upload += 1; address += 1; return { photo, location } }
  const isLocation = (ticket) => ticket && ticket.photo === photo && ticket.location === location
  const newSession = () => { session += 1; return { photo, location, session } }
  const isSession = (ticket) => isLocation(ticket) && ticket.session === session
  const invalidateSession = () => { session += 1 }
  const newUpload = () => { upload += 1; address += 1; return { photo, location, upload } }
  const isUpload = (ticket) => isLocation(ticket) && ticket.upload === upload
  const newAddress = () => { address += 1; return { photo, location, upload, address } }
  const isAddress = (ticket) => isUpload(ticket) && ticket.address === address
  const beginSubmission = () => {
    if (submission) return null
    submission = { photo, location, upload, address }
    return submission
  }
  const isSubmission = (ticket) => submission === ticket && isAddress(ticket)
  const endSubmission = (ticket) => { if (submission === ticket) submission = null }

  return { newPhoto, isPhoto, newLocation, isLocation, newSession, isSession, invalidateSession,
    newUpload, isUpload, newAddress, isAddress, beginSubmission, isSubmission, endSubmission }
}
