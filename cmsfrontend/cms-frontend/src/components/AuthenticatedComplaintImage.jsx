import { useCallback, useEffect, useRef, useState } from 'react'
import { useAuth } from '../context/useAuth'
import { getComplaintPhoto, isManagedPhotoReference } from '../services/photoService'
import ComplaintImageModal from './ComplaintImageModal'

function unavailableMessage(status) {
  if (status === 401 || status === 403) return 'Photo unavailable for this account.'
  if (status === 404) return 'Photo is no longer available.'
  if (status === 0) return 'Photo could not be loaded. Check your connection.'
  return 'Photo could not be displayed.'
}

function AuthenticatedComplaintImage({ imageReference, alt = 'Complaint evidence' }) {
  const { token } = useAuth()
  const [result, setResult] = useState({ reference: null, objectUrl: '', errorStatus: null })
  const [preview, setPreview] = useState(null)
  const triggerRef = useRef(null)
  const closePreview = useCallback(() => setPreview(null), [])
  const isManaged = isManagedPhotoReference(imageReference)
  const isPreviewOpen = preview?.reference === imageReference && preview?.objectUrl === result.objectUrl && Boolean(result.objectUrl) && result.errorStatus === null

  useEffect(() => {
    if (!isManaged || !token) return undefined
    let current = true
    let createdUrl = ''
    getComplaintPhoto(imageReference, token)
      .then((blob) => {
        createdUrl = URL.createObjectURL(blob)
        if (current) setResult({ reference: imageReference, objectUrl: createdUrl, errorStatus: null })
        else URL.revokeObjectURL(createdUrl)
      })
      .catch((error) => {
        if (current) setResult({ reference: imageReference, objectUrl: '', errorStatus: error.status ?? 0 })
      })
    return () => {
      current = false
      if (createdUrl) URL.revokeObjectURL(createdUrl)
    }
  }, [imageReference, isManaged, token])

  if (!imageReference) return <div className="complaint-photo-state">No photo available</div>
  if (!isManaged) return <div className="complaint-photo-state">Legacy photo reference unavailable</div>
  if (!token) return <div className="complaint-photo-state complaint-photo-error">Photo unavailable for this account.</div>
  if (result.reference !== imageReference) return <div className="complaint-photo-state" role="status">Loading protected photo...</div>
  if (result.errorStatus !== null) return <div className="complaint-photo-state complaint-photo-error">{unavailableMessage(result.errorStatus)}</div>
  return <>
    <button ref={triggerRef} className="complaint-photo-trigger" type="button" aria-label={`Enlarge photo: ${alt}`} onClick={() => setPreview({ reference: imageReference, objectUrl: result.objectUrl })}>
      <img className="complaint-photo-image" src={result.objectUrl} alt={alt} />
    </button>
    {isPreviewOpen && <ComplaintImageModal imageUrl={result.objectUrl} alt={alt} onClose={closePreview} returnFocusTo={triggerRef} />}
  </>
}

export default AuthenticatedComplaintImage
