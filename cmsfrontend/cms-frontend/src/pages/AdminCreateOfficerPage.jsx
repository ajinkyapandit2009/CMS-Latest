import { useCallback, useEffect, useState } from 'react'
import OfficerCreationForm from '../components/OfficerCreationForm'
import { useAuth } from '../context/useAuth'
import { getCaseOfficers } from '../services/adminService'

function AdminCreateOfficerPage() {
  const { token } = useAuth()
  const [officers, setOfficers] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const loadOfficers = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const result = await getCaseOfficers(token)
      setOfficers(Array.isArray(result) ? result : [])
    } catch (requestError) {
      setError(requestError.message || 'Unable to load case officers.')
    } finally {
      setIsLoading(false)
    }
  }, [token])

  useEffect(() => {
    let current = true
    getCaseOfficers(token)
      .then((result) => { if (current) setOfficers(Array.isArray(result) ? result : []) })
      .catch((requestError) => { if (current) setError(requestError.message || 'Unable to load case officers.') })
      .finally(() => { if (current) setIsLoading(false) })
    return () => { current = false }
  }, [token])

  return <section className="admin-dashboard container">
    <div className="dashboard-intro"><div><p className="eyebrow">Administration</p><h1>Create Case Officer</h1><p>Create a staff account and its linked case-officer profile.</p></div></div>
    <OfficerCreationForm onCreated={loadOfficers} />
    <div className="management-header"><div><h2>Case officers</h2><p>Available officers are refreshed after a successful creation.</p></div><button className="button button-secondary" type="button" onClick={loadOfficers} disabled={isLoading}>Refresh</button></div>
    {isLoading ? <div className="dashboard-state" role="status">Loading case officers…</div>
      : error ? <div className="dashboard-state dashboard-error" role="alert"><p>{error}</p><button className="button button-secondary" type="button" onClick={loadOfficers}>Try again</button></div>
        : officers.length ? <ul className="officer-directory">{officers.map((officer) => <li key={officer.userId}><strong>{officer.firstName} {officer.lastName}</strong><span>{officer.employeeNumber}</span><span>{officer.email}</span></li>)}</ul>
          : <div className="dashboard-state"><p>No case officers have been created yet.</p></div>}
  </section>
}

export default AdminCreateOfficerPage
