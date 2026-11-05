import { ApiError, apiBlobRequest, apiRequest } from './api'

export const isManagedPhotoReference = (reference) => /^\/api\/complaint-photos\/[0-9a-fA-F-]{36}\/content$/.test(reference || '')

export function uploadComplaintPhoto(photo, location, token, signal) {
  const formData = new FormData()
  formData.append('photo', photo, 'complaint-photo.jpg')
  formData.append('latitude', String(location.latitude))
  formData.append('longitude', String(location.longitude))
  formData.append('accuracy', String(location.accuracy))
  return apiRequest('/api/complaint-photos', { method: 'POST', token, body: formData, signal })
}

export function getComplaintPhoto(reference, token) {
  if (!isManagedPhotoReference(reference)) return Promise.reject(new ApiError('Invalid managed photo reference.', 400))
  return apiBlobRequest(reference, { token })
}
