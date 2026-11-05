import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { getCurrentLocation } from '../services/locationService'
import { submitPhoneLocation } from '../services/locationSharingService'

function ShareLocation() {
  const { token } = useParams()
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')

  const shareLocation = async () => {
    setError('')
    setStatus('locating')
    try {
      const location = await getCurrentLocation()
      setStatus('sending')
      await submitPhoneLocation(token, location)
      setStatus('success')
    } catch (requestError) {
      setStatus('error')
      setError(requestError.message || 'Your location could not be shared. Please try again.')
    }
  }

  return <main className="share-location-page"><section className="share-location-card"><div className="brand share-location-brand"><span className="brand-mark">C</span> CivicCMS</div><p className="eyebrow">One-time secure sharing</p><h1>Share your current location</h1>{status === 'success' ? <div className="share-success" role="status"><h2>Location shared successfully.</h2><p>Your location has been sent to the complaint form. You can now close this page.</p></div> : <><p className="share-location-copy">Your location will be sent once to the CMS complaint form currently open on your computer. Location access is requested only after you press the button.</p>{error && <p className="form-message form-error" role="alert">{error}</p>}<button className="button button-primary share-location-button" type="button" onClick={() => void shareLocation()} disabled={status === 'locating' || status === 'sending'}>{status === 'locating' ? 'Getting location...' : status === 'sending' ? 'Sending location...' : status === 'error' ? 'Retry Sharing' : 'Share My Location'}</button><p className="share-privacy">This link cannot access your CMS account and expires after one use.</p></>}</section></main>
}

export default ShareLocation
