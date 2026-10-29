import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth'
import { dashboardForRole } from '../services/authService'

export function PageScaffold({ eyebrow, title, description, action }) { return <section className="page-scaffold container"><div className="scaffold-card"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="lead">{description}</p><div className="button-row">{action}</div></div></section> }

function Login() {
  const [values, setValues] = useState({ email: '', password: '' }); const [error, setError] = useState(''); const [showPassword, setShowPassword] = useState(false); const [isSubmitting, setIsSubmitting] = useState(false)
  const { user, isInitializing, login } = useAuth(); const location = useLocation(); const navigate = useNavigate()
  if (!isInitializing && user) return <Navigate to={dashboardForRole(user.role)} replace />
  const update = (event) => setValues((current) => ({ ...current, [event.target.name]: event.target.value }))
  const submit = async (event) => { event.preventDefault(); setError(''); if (!values.email.trim() || !values.password) { setError('Enter your email and password.'); return }; setIsSubmitting(true); try { const authenticatedUser = await login({ email: values.email.trim(), password: values.password }); const requestedPath = location.state?.from; const destination = requestedPath?.startsWith('/') && requestedPath !== '/login' ? requestedPath : dashboardForRole(authenticatedUser.role); navigate(destination, { replace: true }) } catch (requestError) { setError(requestError.message || 'Unable to sign in. Please try again.') } finally { setIsSubmitting(false) } }
  return <section className="auth-page"><div className="auth-card"><Link className="auth-brand" to="/">CMS</Link><p className="eyebrow">Welcome back</p><h1>Sign in to CMS</h1><p className="auth-intro">Use the same sign-in for citizen, case officer, and administrator accounts.</p>{error && <p className="form-message form-error" role="alert">{error}</p>}<form className="auth-form" onSubmit={submit} noValidate><label>Email<input name="email" type="email" autoComplete="email" value={values.email} onChange={update} required /></label><label>Password<span className="password-field"><input name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={values.password} onChange={update} required /><button type="button" onClick={() => setShowPassword((show) => !show)}>{showPassword ? 'Hide' : 'Show'}</button></span></label><button className="button button-primary form-submit" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Signing in…' : 'Sign in'}</button></form><p className="auth-switch">New to CMS? <Link to="/register">Register as a citizen</Link></p><Link className="back-link" to="/">← Back to homepage</Link></div></section>
}

export default Login
