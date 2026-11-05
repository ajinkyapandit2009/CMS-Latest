import { useEffect, useState } from 'react'
import EscalationHistory from '../components/EscalationHistory'
import { useAuth } from '../context/useAuth'
import { getMyEscalations } from '../services/escalationService'
import '../styles/escalations.css'

function MyEscalationsPage() {
  const { token } = useAuth()
  const [items, setItems] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let current = true
    getMyEscalations(token).then((data) => {
      if (current) { setItems(Array.isArray(data) ? data : []); setError('') }
    }).catch((requestError) => {
      if (current) setError(requestError.status === 401 ? 'Your session expired. Sign in again.' : 'Escalations could not be loaded. Try again.')
    }).finally(() => { if (current) setIsLoading(false) })
    return () => { current = false }
  }, [token, refresh])

  return <section className="escalations-page container">
    <div className="management-header"><div><p className="eyebrow">Citizen workspace</p><h1>My Escalations</h1><p>Track review outcomes separately from complaint status.</p></div><button className="button button-secondary" type="button" disabled={isLoading} onClick={() => { setIsLoading(true); setRefresh((value) => value + 1) }}>Refresh</button></div>
    {isLoading ? <div className="dashboard-state" role="status">Loading escalations...</div> : error ? <div className="dashboard-state dashboard-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={() => { setIsLoading(true); setRefresh((value) => value + 1) }}>Try again</button></div> : <EscalationHistory escalations={items} showComplaint />}
  </section>
}

export default MyEscalationsPage
