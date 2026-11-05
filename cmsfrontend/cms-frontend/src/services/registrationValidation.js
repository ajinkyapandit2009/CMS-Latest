const namePattern = /^[\p{L}\p{M}][\p{L}\p{M}\p{Zs}'\u2019.-]*$/u
const emailPattern = /^(?!.*\.\.)[A-Za-z0-9_+%'-]+(?:\.[A-Za-z0-9_+%'-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/
const passwordPattern = /^(?=\S*[A-Z])(?=\S*[a-z])(?=\S*[0-9])\S{8,72}$/
const phonePattern = /^(?=.*[0-9])[+0-9() .-]+$/
const byteLength = (value) => new TextEncoder().encode(value).length

export const normalizeRegistrationEmail = (value) => value.trim().toLowerCase()

export function prepareRegistration(values) {
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    email: normalizeRegistrationEmail(values.email),
    phone: values.phone.trim() || null,
    address: values.address.trim() || null,
    password: values.password,
    confirmPassword: values.confirmPassword,
  }
}

export function validateRegistrationField(field, values) {
  const prepared = prepareRegistration(values)
  const value = prepared[field]
  if (field === 'firstName' || field === 'lastName') {
    const label = field === 'firstName' ? 'First name' : 'Last name'
    if (!value) return `${label} is required`
    if (value.length > 100) return `${label} must be 100 characters or fewer`
    if (!namePattern.test(value)) return `${label} must contain letters and common name punctuation`
  }
  if (field === 'email') {
    if (!value) return 'Email is required'
    if (value.length > 254) return 'Email must be 254 characters or fewer'
    if (!emailPattern.test(value)) return 'Enter a valid email address'
  }
  if (field === 'password') {
    if (!value) return 'Password is required'
    if (!passwordPattern.test(value)) return 'Use 8 to 72 characters without spaces, including uppercase, lowercase, and a number'
    if (byteLength(value) > 72) return 'Password must be 72 UTF-8 bytes or fewer'
  }
  if (field === 'confirmPassword') {
    if (!value) return 'Confirm password is required'
    if (value.length > 72) return 'Confirm password must be 72 characters or fewer'
    if (value !== prepared.password) return 'Passwords do not match'
  }
  if (field === 'phone' && value) {
    if (value.length > 15) return 'Phone must be 15 characters or fewer'
    if (!phonePattern.test(value)) return 'Phone must contain a digit and use only phone characters'
  }
  if (field === 'address' && value) {
    if (value.length > 65535 || byteLength(value) > 65535) return 'Address is too long'
  }
  return ''
}

export const registrationFields = ['firstName', 'lastName', 'email', 'phone', 'address', 'password', 'confirmPassword']

export function validateRegistration(values) {
  return Object.fromEntries(registrationFields.map((field) => [field, validateRegistrationField(field, values)]).filter(([, error]) => error))
}
