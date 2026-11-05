import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'

function secondsRemaining(expiresAt) {
  return Math.max(0, Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 1000))
}

function LocationQrModal({ session, onCancel, onExpired }) {
  const [remaining, setRemaining] = useState(() => secondsRemaining(session.expiresAt))

  useEffect(() => {
    const timer = window.setInterval(() => {
      const next = secondsRemaining(session.expiresAt)
      setRemaining(next)
      if (next === 0) {
        window.clearInterval(timer)
        onExpired()
      }
    }, 1000)
    return () => window.clearInterval(timer)
  }, [session.expiresAt, onExpired])

  const minutes = Math.floor(remaining / 60)
  const seconds = String(remaining % 60).padStart(2, '0')

  return <div className="modal-backdrop" role="presentation"><section className="location-qr-modal" role="dialog" aria-modal="true" aria-labelledby="location-qr-heading"><p className="eyebrow">Secure phone handoff</p><h2 id="location-qr-heading">Get Location Using Phone</h2><p>Scan this QR code with your phone, then choose <strong>Share My Location</strong>. The code contains only a temporary, one-time link.</p><div className="qr-code-frame"><QRCodeSVG value={session.shareUrl} size={240} level="M" title="Temporary CMS location sharing QR code" /></div><p className="qr-waiting" role="status">Waiting for location from your phone...</p><p className="qr-expiry">Expires in {minutes}:{seconds}</p><button className="button button-secondary" type="button" onClick={onCancel}>Cancel</button></section></div>
}

export default LocationQrModal
