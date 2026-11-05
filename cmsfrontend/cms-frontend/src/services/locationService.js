const configuredAccuracy = Number(import.meta.env.VITE_LOCATION_MAX_ACCURACY_METERS || 50)
export const MAX_LOCATION_ACCURACY_METERS = Number.isFinite(configuredAccuracy) && configuredAccuracy >= 0 ? configuredAccuracy : 50

function locationError(message) {
  return new Error(message)
}

function validateLocation(position) {
  const { latitude, longitude, accuracy } = position.coords
  if (!Number.isFinite(latitude) || latitude < -90 || latitude > 90 || !Number.isFinite(longitude) || longitude < -180 || longitude > 180) {
    throw locationError('Your device returned an invalid location. Please try again.')
  }
  if (!Number.isFinite(accuracy) || accuracy < 0) {
    throw locationError('Your device could not determine location accuracy. Please try again.')
  }
  if (accuracy > MAX_LOCATION_ACCURACY_METERS) {
    throw locationError(`Your location accuracy is ${Math.round(accuracy)} m. Move to a clearer area and retry; CMS requires ${MAX_LOCATION_ACCURACY_METERS} m or better.`)
  }
  return { latitude, longitude, accuracy }
}

export function getCurrentLocation() {
  if (!navigator.geolocation) return Promise.reject(locationError('Location is not supported by this browser.'))
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        try { resolve(validateLocation(position)) } catch (error) { reject(error) }
      },
      (error) => {
        const messages = {
          1: 'Location permission was denied. Allow location access and retry.',
          2: 'Your location is currently unavailable. Check your device signal and retry.',
          3: 'Location request timed out. Please retry in an area with a clearer signal.',
        }
        reject(locationError(messages[error.code] || 'Unable to get your current location. Please retry.'))
      },
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 },
    )
  })
}
