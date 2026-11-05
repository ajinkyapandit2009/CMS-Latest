import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../context/useAuth'
import { dashboardRoleLabels } from '../services/dashboardNavigation'
import '../styles/profile-page.css'

const phonePattern = /^(?=.*\d)[+0-9() .-]+$/
const editableValues = (profile) => ({ phone: profile.phone || '', address: profile.address || '' })

function ProfilePage() {
  const { user, refreshUser, updateProfile } = useAuth()
  const [values, setValues] = useState({ phone: '', address: '' })
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const savingRef = useRef(false)

  useEffect(() => {
    let current = true
    refreshUser()
      .then((profile) => { if (current) setValues(editableValues(profile)) })
      .catch((error) => { if (current) setLoadError(error.message || 'Unable to load your profile.') })
      .finally(() => { if (current) setIsLoading(false) })
    return () => { current = false }
  }, [refreshUser])

  const retry = async () => {
    setIsLoading(true)
    setLoadError('')
    try {
      const profile = await refreshUser()
      setValues(editableValues(profile))
    } catch (error) {
      setLoadError(error.message || 'Unable to load your profile.')
    } finally {
      setIsLoading(false)
    }
  }

  const save = async (event) => {
    event.preventDefault()
    if (savingRef.current) return
    setFormError('')
    setSuccess('')
    const phone = values.phone.trim()
    const address = values.address.trim()
    if (phone.length > 15 || (phone && !phonePattern.test(phone))) {
      setFormError('Enter a phone number of up to 15 characters using digits, spaces, +, parentheses, dots, or hyphens.')
      return
    }
    if (address.length > 65535 || new TextEncoder().encode(address).length > 65535) {
      setFormError('Address is too long for storage. Please shorten it.')
      return
    }
    savingRef.current = true
    setIsSaving(true)
    try {
      const updated = await updateProfile({ phone: phone || null, address: address || null })
      setValues(editableValues(updated))
      setSuccess('Your profile was updated.')
    } catch (error) {
      setFormError(error.status === 401 ? 'Your session expired. Sign in again to update your profile.' : (error.message || 'Unable to update your profile.'))
    } finally {
      savingRef.current = false
      setIsSaving(false)
    }
  }

  return <section className="profile-page container">
    <div className="form-page-intro"><p className="eyebrow">Your account</p><h1>My Profile</h1><p>Review your CMS account and update your contact details.</p></div>
    {isLoading ? <div className="dashboard-state" role="status">Loading your profile…</div>
      : loadError ? <div className="dashboard-state dashboard-error" role="alert"><p>{loadError}</p><button className="button button-secondary" type="button" onClick={retry}>Try again</button></div>
        : <>
          <dl className="profile-summary">
            <div><dt>First name</dt><dd>{user.firstName || 'Not provided'}</dd></div>
            <div><dt>Last name</dt><dd>{user.lastName || 'Not provided'}</dd></div>
            <div><dt>Email</dt><dd>{user.email || 'Not provided'}</dd></div>
            <div><dt>Role</dt><dd>{dashboardRoleLabels[user.role] || 'Not available'}</dd></div>
            <div><dt>User ID</dt><dd>{user.userId ?? 'Not available'}</dd></div>
            {user.role === 'ROLE_OFFICER' && <div><dt>Employee number</dt><dd>{user.employeeNumber || 'Not available'}</dd></div>}
            <div><dt>Phone</dt><dd>{user.phone || 'Not provided'}</dd></div>
            <div><dt>Address</dt><dd>{user.address || 'Not provided'}</dd></div>
          </dl>
          <form className="profile-edit-form" onSubmit={save} noValidate>
            <div><h2>Edit contact details</h2><p>Only your phone number and address can be changed here.</p></div>
            <label>Phone number <span className="optional">(optional)</span><input name="phone" type="tel" autoComplete="tel" maxLength="15" value={values.phone} onChange={(event) => setValues((current) => ({ ...current, phone: event.target.value }))} disabled={isSaving} /></label>
            <label>Address <span className="optional">(optional)</span><textarea name="address" autoComplete="street-address" maxLength="65535" rows="4" value={values.address} onChange={(event) => setValues((current) => ({ ...current, address: event.target.value }))} disabled={isSaving} /></label>
            {formError && <p className="inline-error" role="alert">{formError}</p>}
            {success && <p className="inline-success" role="status">{success}</p>}
            <button className="button button-primary" type="submit" disabled={isSaving}>{isSaving ? 'Saving…' : 'Save changes'}</button>
          </form>
        </>}
  </section>
}

export default ProfilePage
