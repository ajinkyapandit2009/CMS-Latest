import { useEffect, useRef, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { checkEmailAvailability, dashboardForRole, registerCitizen } from '../services/authService'
import { normalizeRegistrationEmail, prepareRegistration, registrationFields, validateRegistration, validateRegistrationField } from '../services/registrationValidation'
import '../styles/register.css'

const emptyForm = { firstName: '', lastName: '', email: '', phone: '', address: '', password: '', confirmPassword: '' }

function RegistrationField({ name, label, guidance, value, error, onChange, onBlur, type = 'text', autoComplete, optional = false, maxLength, disabled, children }) {
  const id = `register-${name}`
  const helpId = `${id}-help`
  const errorId = `${id}-error`
  return <div className="registration-field">
    <label htmlFor={id}>{label}{optional && <span className="optional"> (optional)</span>}</label>
    <input id={id} name={name} type={type} value={value} onChange={onChange} onBlur={onBlur} autoComplete={autoComplete} maxLength={maxLength} required={!optional} disabled={disabled} aria-invalid={Boolean(error)} aria-describedby={`${helpId}${error ? ` ${errorId}` : ''}`} />
    <small id={helpId}>{guidance}</small>
    {children}
    {error && <span id={errorId} className="registration-field-error" role="alert">{error}</span>}
  </div>
}

function Register() {
  const { user, isInitializing } = useAuth()
  const [values, setValues] = useState(emptyForm)
  const [touched, setTouched] = useState({})
  const [attempted, setAttempted] = useState(false)
  const [serverErrors, setServerErrors] = useState({})
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [availability, setAvailability] = useState({ email: '', kind: 'idle' })
  const [checkRetry, setCheckRetry] = useState(0)
  const availabilityCache = useRef(new Map())
  const rateLimitedUntil = useRef(0)
  const submittingRef = useRef(false)
  const normalizedEmail = normalizeRegistrationEmail(values.email)
  const emailValid = !validateRegistrationField('email', values)
  const currentAvailability = availability.email === normalizedEmail ? availability.kind : 'idle'

  useEffect(() => {
    if (!emailValid) return undefined
    const cached = availabilityCache.current.get(normalizedEmail)
    if (cached) {
      setAvailability({ email: normalizedEmail, kind: cached })
      return undefined
    }
    let active = true
    let retryTimer
    const wait = rateLimitedUntil.current - Date.now()
    if (wait > 0) {
      setAvailability({ email: normalizedEmail, kind: 'limited' })
      retryTimer = setTimeout(() => { if (active) setAvailability({ email: normalizedEmail, kind: 'error' }) }, wait)
      return () => { active = false; clearTimeout(retryTimer) }
    }
    const controller = new AbortController()
    const timer = setTimeout(async () => {
      setAvailability({ email: normalizedEmail, kind: 'checking' })
      try {
        const result = await checkEmailAvailability(normalizedEmail, controller.signal)
        if (!active) return
        if (typeof result?.available !== 'boolean') throw new Error('Invalid availability response')
        const kind = result.available ? 'available' : 'taken'
        availabilityCache.current.set(normalizedEmail, kind)
        setAvailability({ email: normalizedEmail, kind })
      } catch (error) {
        if (active) {
          if (error.status === 429) {
            rateLimitedUntil.current = Date.now() + 60_000
            retryTimer = setTimeout(() => { if (active) setAvailability({ email: normalizedEmail, kind: 'error' }) }, 60_000)
          }
          setAvailability({ email: normalizedEmail, kind: error.status === 429 ? 'limited' : 'error' })
        }
      }
    }, 500)
    return () => { active = false; clearTimeout(timer); clearTimeout(retryTimer); controller.abort() }
  }, [normalizedEmail, emailValid, checkRetry])

  if (!isInitializing && user) return <Navigate to={dashboardForRole(user.role)} replace />

  const update = (event) => {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setServerErrors((current) => ({ ...current, [name]: '' }))
    setFormError('')
    setSuccess('')
  }
  const blur = (event) => setTouched((current) => ({ ...current, [event.target.name]: true }))
  const errorFor = (name) => serverErrors[name]
    || ((touched[name] || attempted) ? validateRegistrationField(name, values) : '')
    || (name === 'email' && emailValid && currentAvailability === 'taken' ? 'Email is already registered' : '')

  const submit = async (event) => {
    event.preventDefault()
    if (submittingRef.current) return
    setAttempted(true)
    setFormError('')
    setSuccess('')
    const errors = validateRegistration(values)
    if (emailValid && currentAvailability === 'taken') errors.email = 'Email is already registered'
    if (Object.keys(errors).length) return

    submittingRef.current = true
    setIsSubmitting(true)
    const details = prepareRegistration(values)
    try {
      await registerCitizen(details)
      availabilityCache.current.set(details.email, 'taken')
      setValues(emptyForm)
      setTouched({})
      setAttempted(false)
      setServerErrors({})
      setSuccess('Registration successful. You can now sign in with your new account.')
    } catch (error) {
      const fields = error.fieldErrors || {}
      const recognized = Object.fromEntries(registrationFields.filter((field) => fields[field]).map((field) => [field, fields[field]]))
      setServerErrors(recognized)
      if (error.status === 409 && fields.email) {
        availabilityCache.current.set(details.email, 'taken')
        setAvailability({ email: details.email, kind: 'taken' })
      }
      if (!Object.keys(recognized).length) setFormError(error.message || 'Unable to create your account. Please try again.')
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  const availabilityNote = emailValid && currentAvailability !== 'idle' && currentAvailability !== 'taken' && <span className={`registration-availability ${currentAvailability === 'available' ? 'is-available' : ''}`} role="status">
    {currentAvailability === 'checking' ? 'Checking email availability...'
      : currentAvailability === 'available' ? 'Email is available. Registration will confirm it.'
        : currentAvailability === 'limited' ? 'Too many email checks. Wait a minute before retrying; you can still submit.'
          : 'Email availability could not be checked. You can still submit.'}
    {currentAvailability === 'error' && <button className="registration-retry" type="button" onClick={() => setCheckRetry((count) => count + 1)}>Try again</button>}
  </span>

  return <section className="auth-page"><div className="auth-card auth-card-wide"><Link className="auth-brand" to="/">CMS</Link><p className="eyebrow">Citizen registration</p><h1>Create your account</h1><p className="auth-intro">Public registration is for citizens only. Case officer accounts are created through administration.</p>
    {formError && <p className="form-message form-error" role="alert">{formError}</p>}
    {success && <p className="form-message form-success" role="status">{success}</p>}
    <form className="auth-form" onSubmit={submit} noValidate>
      <div className="form-columns">
        <RegistrationField name="firstName" label="First name" guidance="Letters and common name punctuation; up to 100 characters." value={values.firstName} error={errorFor('firstName')} onChange={update} onBlur={blur} maxLength={100} autoComplete="given-name" disabled={isSubmitting} />
        <RegistrationField name="lastName" label="Last name" guidance="Letters and common name punctuation; up to 100 characters." value={values.lastName} error={errorFor('lastName')} onChange={update} onBlur={blur} maxLength={100} autoComplete="family-name" disabled={isSubmitting} />
      </div>
      <RegistrationField name="email" label="Email" guidance="Use a valid address with a dotted domain; up to 254 characters." value={values.email} error={errorFor('email')} onChange={update} onBlur={blur} type="email" maxLength={254} autoComplete="email" disabled={isSubmitting}>{availabilityNote}</RegistrationField>
      <div className="form-columns">
        <RegistrationField name="phone" label="Phone" guidance="Up to 15 characters; include a digit and use only +, parentheses, spaces, periods, or hyphens." value={values.phone} error={errorFor('phone')} onChange={update} onBlur={blur} type="tel" optional maxLength={15} autoComplete="tel" disabled={isSubmitting} />
        <RegistrationField name="address" label="Address" guidance="Optional; up to 65,535 UTF-8 bytes." value={values.address} error={errorFor('address')} onChange={update} onBlur={blur} optional maxLength={65535} autoComplete="street-address" disabled={isSubmitting} />
      </div>
      <RegistrationField name="password" label="Password" guidance="8–72 characters without spaces, including uppercase, lowercase, and a digit; maximum 72 UTF-8 bytes." value={values.password} error={errorFor('password')} onChange={update} onBlur={blur} type="password" maxLength={72} autoComplete="new-password" disabled={isSubmitting} />
      <RegistrationField name="confirmPassword" label="Confirm password" guidance="Repeat the password exactly; up to 72 characters." value={values.confirmPassword} error={errorFor('confirmPassword')} onChange={update} onBlur={blur} type="password" maxLength={72} autoComplete="new-password" disabled={isSubmitting} />
      <button className="button button-primary form-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Creating account...' : 'Create citizen account'}</button>
    </form><p className="auth-switch">Already registered? <Link to="/login">Sign in</Link></p><Link className="back-link" to="/">← Back to homepage</Link>
  </div></section>
}

export default Register
