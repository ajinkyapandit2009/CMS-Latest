import { useRef, useState } from 'react'
import { createCaseOfficer } from '../services/adminService'
import { useAuth } from '../context/useAuth'

const initialValues = { firstName: '', lastName: '', email: '', phone: '', address: '', password: '', confirmPassword: '' }
const passwordPattern = /^(?=\S*[A-Z])(?=\S*[a-z])(?=\S*[0-9])\S{6,}$/

function OfficerCreationForm({ onCreated }) {
  const { token } = useAuth()
  const [values, setValues] = useState(initialValues)
  const [message, setMessage] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const submittingRef = useRef(false)

  const update = (event) => setValues((current) => ({ ...current, [event.target.name]: event.target.value }))

  const submit = async (event) => {
    event.preventDefault()
    if (submittingRef.current) return
    setMessage(null)
    if (!values.firstName.trim() || !values.lastName.trim() || !values.email.trim() || !values.password || !values.confirmPassword) {
      setMessage({ type: 'error', text: 'Complete all required fields.' })
      return
    }
    if (values.password !== values.confirmPassword) {
      setMessage({ type: 'error', text: 'Passwords do not match.' })
      return
    }
    if (!passwordPattern.test(values.password)) {
      setMessage({ type: 'error', text: 'Password needs at least 6 characters, including uppercase, lowercase, and a number.' })
      return
    }

    submittingRef.current = true
    setIsSubmitting(true)
    try {
      const profile = await createCaseOfficer({
        ...values,
        firstName: values.firstName.trim(),
        lastName: values.lastName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        address: values.address.trim(),
      }, token)
      setValues(initialValues)
      setMessage({ type: 'success', text: `Case officer ${profile.employeeNumber || ''} created successfully.` })
      try {
        await onCreated?.()
      } catch (refreshError) {
        setMessage({ type: 'success', text: `Case officer ${profile.employeeNumber || ''} was created, but the officer list could not refresh: ${refreshError.message || 'please try Refresh.'}` })
      }
    } catch (error) {
      setMessage({ type: 'error', text: error.message || 'Unable to create the case officer.' })
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  return <section className="officer-creation">
    <div><p className="eyebrow">Case officer management</p><h2>Create a case officer</h2><p>Creates a staff login account and its linked case-officer profile together.</p></div>
    <form className="officer-creation-form" onSubmit={submit} noValidate>
      <div className="form-columns"><label>First name<input name="firstName" value={values.firstName} onChange={update} maxLength="100" required disabled={isSubmitting} /></label><label>Last name<input name="lastName" value={values.lastName} onChange={update} maxLength="100" required disabled={isSubmitting} /></label></div>
      <div className="form-columns"><label>Email<input name="email" type="email" value={values.email} onChange={update} required disabled={isSubmitting} /></label><label>Phone <span className="optional">(optional)</span><input name="phone" value={values.phone} onChange={update} maxLength="15" disabled={isSubmitting} /></label></div>
      <label>Address <span className="optional">(optional)</span><input name="address" value={values.address} onChange={update} disabled={isSubmitting} /></label>
      <div className="form-columns"><label>Password<input name="password" type="password" value={values.password} onChange={update} required disabled={isSubmitting} /></label><label>Confirm password<input name="confirmPassword" type="password" value={values.confirmPassword} onChange={update} required disabled={isSubmitting} /></label></div>
      <button className="button button-primary" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Creating officer…' : 'Create case officer'}</button>
      {message && <p className={message.type === 'success' ? 'inline-success' : 'inline-error'} role="status">{message.text}</p>}
    </form>
  </section>
}

export default OfficerCreationForm
