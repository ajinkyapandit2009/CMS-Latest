import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import '../styles/complaint-image-modal.css'

function ComplaintImageModal({ imageUrl, alt, onClose, returnFocusTo }) {
  const closeRef = useRef(null)

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    const trigger = returnFocusTo.current
    document.body.style.overflow = 'hidden'
    closeRef.current?.focus()
    const onKeyDown = (event) => {
      if (event.key === 'Escape') { event.preventDefault(); onClose() }
      if (event.key === 'Tab') { event.preventDefault(); closeRef.current?.focus() }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = previousOverflow
      trigger?.focus()
    }
  }, [onClose, returnFocusTo])

  return createPortal(
    <div className="complaint-image-modal" role="presentation" onClick={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <div className="complaint-image-dialog" role="dialog" aria-modal="true" aria-label="Complaint photo preview">
        <button ref={closeRef} className="complaint-image-close" type="button" aria-label="Close photo preview" onClick={onClose}>×</button>
        <img src={imageUrl} alt={alt} />
      </div>
    </div>, document.body)
}

export default ComplaintImageModal
