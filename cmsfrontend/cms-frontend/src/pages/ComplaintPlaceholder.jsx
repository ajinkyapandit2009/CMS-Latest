import {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react'
import { Link } from 'react-router-dom'

import CameraCapture from '../components/CameraCapture'
import LocationQrModal from '../components/LocationQrModal'
import { useAuth } from '../context/useAuth'
import { createComplaint } from '../services/complaintService'
import { getCurrentLocation } from '../services/locationService'
import {
  cancelLocationSession,
  createLocationSession,
  getLocationSession,
} from '../services/locationSharingService'
import { uploadComplaintPhoto } from '../services/photoService'
import { createEvidenceOperations } from '../services/evidenceOperations'

const categories = [
  'Gas Leakage',
  'Water Pipeline Burst',
  'Power Cut',
  'Overflowing garbage',
  'Potholes and damaged roads',
  'Waterlogging',
  'Damaged footpaths',
  'Clogged drains',
  'Damaged traffic signals',
  'Other civic issue',
]

const emptyForm = {
  title: '',
  description: '',
  category: '',
  address: '',
}

function locationMode() {
  if (
    navigator.userAgentData?.mobile ||
    /Android|iPhone|iPad|iPod|Mobile/i.test(
      navigator.userAgent || ''
    )
  ) {
    return 'mobile'
  }

  if (
    navigator.maxTouchPoints > 0 &&
    navigator.geolocation
  ) {
    return 'ambiguous'
  }

  return 'computer'
}

function ComplaintPlaceholder() {
  const { token } = useAuth()

  const [values, setValues] = useState(emptyForm)

  const [capturedPhoto, setCapturedPhoto] =
    useState(null)

  const [uploadedPhoto, setUploadedPhoto] =
    useState(null)

  const [receivedLocation, setReceivedLocation] =
    useState(null)

  const [locationSession, setLocationSession] =
    useState(null)

  const [cameraOpen, setCameraOpen] =
    useState(false)

  const [error, setError] = useState('')
  const [photoMessage, setPhotoMessage] =
    useState('')

  const [createdComplaint, setCreatedComplaint] =
    useState(null)

  const [isSubmitting, setIsSubmitting] =
    useState(false)

  const [isUploadingPhoto, setIsUploadingPhoto] =
    useState(false)

  const [isCreatingSession, setIsCreatingSession] =
    useState(false)

  const [isResolvingAddress, setIsResolvingAddress] =
    useState(false)

  const [isGettingLocation, setIsGettingLocation] =
    useState(false)

  const [deviceMode] = useState(locationMode)

  const operations = useRef(createEvidenceOperations())
  const photoRef = useRef(null)
  const uploadedPhotoRef = useRef(null)
  const receivedLocationTicket = useRef(null)
  const uploadedPhotoTicket = useRef(null)
  const activeSession = useRef(null)
  const uploadController = useRef(null)
  const addressController = useRef(null)
  const photoUploadInFlight = useRef(false)
  const addressInFlight = useRef(false)
  const gpsInFlight = useRef(false)
  const sessionCreationInFlight = useRef(false)
  const submissionInFlight = useRef(false)
  const completedSubmission = useRef(false)

  const update = (event) => {
    const { name, value } = event.target

    setValues((current) => ({
      ...current,
      [name]: value,
    }))
  }

  useEffect(() => {
    return () => {
      if (capturedPhoto?.previewUrl) {
        URL.revokeObjectURL(
          capturedPhoto.previewUrl
        )
      }
    }
  }, [capturedPhoto])

  useEffect(() => () => {
    operations.current.newPhoto()
    uploadController.current?.abort()
    addressController.current?.abort()
    const sessionId = activeSession.current?.sessionId
    if (sessionId) void cancelLocationSession(sessionId, token).catch(() => undefined)
  }, [token])

  const resetPhotoEvidence = () => {
    operations.current.newPhoto()
    uploadController.current?.abort()
    addressController.current?.abort()
    const sessionId = activeSession.current?.sessionId
    if (sessionId) void cancelLocationSession(sessionId, token).catch(() => undefined)
    activeSession.current = null
    photoRef.current = null
    uploadedPhotoRef.current = null
    receivedLocationTicket.current = null
    uploadedPhotoTicket.current = null
    photoUploadInFlight.current = false
    addressInFlight.current = false
    gpsInFlight.current = false
    sessionCreationInFlight.current = false
    setCapturedPhoto(null)
    setUploadedPhoto(null)
    setReceivedLocation(null)
    setLocationSession(null)
    setIsUploadingPhoto(false)
    setIsResolvingAddress(false)
    setIsGettingLocation(false)
    setIsCreatingSession(false)
    setValues((current) => ({ ...current, address: '' }))
  }

  const beginLocationSelection = () => {
    const ticket = operations.current.newLocation()
    uploadController.current?.abort()
    addressController.current?.abort()
    const sessionId = activeSession.current?.sessionId
    if (sessionId) void cancelLocationSession(sessionId, token).catch(() => undefined)
    activeSession.current = null
    uploadedPhotoRef.current = null
    uploadedPhotoTicket.current = null
    receivedLocationTicket.current = null
    photoUploadInFlight.current = false
    addressInFlight.current = false
    setUploadedPhoto(null)
    setReceivedLocation(null)
    setLocationSession(null)
    setIsUploadingPhoto(false)
    setIsResolvingAddress(false)
    setValues((current) => ({ ...current, address: '' }))
    return ticket
  }

  /**
   * Converts verified GPS latitude/longitude into
   * a readable address.
   *
   * The address is not editable by the citizen.
   */
  const resolveAddress = useCallback(
    async (latitude, longitude, uploadTicket) => {
      if (!operations.current.isUpload(uploadTicket) || addressInFlight.current) return null
      const ticket = operations.current.newAddress()
      const controller = new AbortController()
      addressController.current?.abort()
      addressController.current = controller
      addressInFlight.current = true
      setIsResolvingAddress(true)
      setError('')

      try {
        const url =
          'https://nominatim.openstreetmap.org/reverse' +
          `?format=jsonv2` +
          `&lat=${encodeURIComponent(latitude)}` +
          `&lon=${encodeURIComponent(longitude)}` +
          '&zoom=18' +
          '&addressdetails=1'

        const response = await fetch(url, {
          method: 'GET',
          signal: controller.signal,
          headers: {
            Accept: 'application/json',
            'Accept-Language': 'en',
          },
        })

        if (!response.ok) {
          throw new Error(
            'Unable to determine the address from the verified location.'
          )
        }

        const result = await response.json()

        if (!operations.current.isAddress(ticket)) return null

        if (!result?.display_name) {
          throw new Error(
            'No readable address was found for the verified coordinates.'
          )
        }

        setValues((current) => ({
          ...current,
          address: result.display_name,
        }))

        return result.display_name
      } catch (requestError) {
        if (!operations.current.isAddress(ticket) || controller.signal.aborted) return null
        setValues((current) => ({
          ...current,
          address: '',
        }))

        setError(
          requestError.message ||
            'The address could not be determined from your verified location.'
        )

        return null
      } finally {
        if (operations.current.isAddress(ticket)) {
          addressInFlight.current = false
          addressController.current = null
          setIsResolvingAddress(false)
        }
      }
    },
    []
  )

  const uploadWithLocation = useCallback(
    async (location, locationTicket) => {
      if (
        !photoRef.current ||
        !operations.current.isLocation(locationTicket) ||
        photoUploadInFlight.current ||
        uploadedPhotoRef.current
      ) {
        return
      }

      const photo = photoRef.current
      const ticket = operations.current.newUpload()
      const controller = new AbortController()
      uploadController.current = controller
      photoUploadInFlight.current = true

      setError('')
      setIsUploadingPhoto(true)

      setPhotoMessage(
        'Uploading photo with verified location...'
      )

      try {
        const response =
          await uploadComplaintPhoto(
            photo.blob,
            location,
            token,
            controller.signal
          )

        if (!operations.current.isUpload(ticket)) return

        const verifiedLocation = {
          latitude: response.latitude,
          longitude: response.longitude,
          accuracy: response.accuracy,
        }

        uploadedPhotoRef.current = response
        uploadedPhotoTicket.current = ticket
        setUploadedPhoto(response)
        setReceivedLocation(verifiedLocation)

        setPhotoMessage(
          `Photo uploaded. Location confirmed to ${Math.round(
            Number(response.accuracy)
          )} m accuracy. Determining address...`
        )

        const resolvedAddress =
          await resolveAddress(
            response.latitude,
            response.longitude,
            ticket
          )

        if (!operations.current.isUpload(ticket)) return

        if (resolvedAddress) {
          setPhotoMessage(
            `Photo uploaded successfully. Verified location confirmed to ${Math.round(
              Number(response.accuracy)
            )} m accuracy and address detected.`
          )
        } else {
          setPhotoMessage(
            'Photo and coordinates were verified, but the address could not be determined. Retry the address lookup.'
          )
        }
      } catch (requestError) {
        if (!operations.current.isUpload(ticket) || controller.signal.aborted) return
        setPhotoMessage(
          'Location was received, but the photo upload failed. Retry the upload without scanning another QR code.'
        )

        setError(
          requestError.message ||
            'The photo could not be uploaded. Please retry.'
        )
      } finally {
        if (operations.current.isUpload(ticket)) {
          photoUploadInFlight.current = false
          uploadController.current = null
          setIsUploadingPhoto(false)
        }
      }
    },
    [
      token,
      resolveAddress,
    ]
  )

  useEffect(() => {
    if (!locationSession || !token) {
      return undefined
    }

    const session = activeSession.current
    if (!session || session.sessionId !== locationSession.sessionId) return undefined

    let stopped = false
    let timerId
    const controller = new AbortController()
    const isCurrent = () => !stopped && activeSession.current === session
      && operations.current.isSession(session.sessionTicket)
      && operations.current.isLocation(session.locationTicket)

    const poll = async () => {
      if (!isCurrent()) {
        return
      }

      try {
        const current =
          await getLocationSession(
            locationSession.sessionId,
            token,
            controller.signal
          )

        if (!isCurrent()) {
          return
        }

        if (current.status === 'COMPLETED') {
          const location = {
            latitude: current.latitude,
            longitude: current.longitude,
            accuracy: current.accuracy,
          }

          stopped = true

          operations.current.invalidateSession()
          activeSession.current = null
          receivedLocationTicket.current = session.locationTicket

          setReceivedLocation(location)
          setLocationSession(null)

          setPhotoMessage(
            'Location received successfully. Uploading the captured photo...'
          )

          void uploadWithLocation(location, session.locationTicket)

          return
        }

        if (
          current.status === 'EXPIRED' ||
          current.status === 'CANCELLED'
        ) {
          stopped = true

          operations.current.newLocation()
          activeSession.current = null

          setLocationSession(null)

          setError(
            current.status === 'EXPIRED'
              ? 'The QR code expired. Create a new location-sharing session and try again.'
              : 'Location sharing was cancelled.'
          )

          return
        }
      } catch (requestError) {
        if (isCurrent()) {
          setError(
            requestError.message ||
              'Unable to check the phone location. CMS will retry automatically.'
          )
        }
      }

      if (isCurrent()) {
        timerId = window.setTimeout(
          poll,
          2000
        )
      }
    }

    timerId = window.setTimeout(
      poll,
      2000
    )

    return () => {
      stopped = true
      window.clearTimeout(timerId)
      controller.abort()
    }
  }, [
    locationSession,
    token,
    uploadWithLocation,
  ])

  const confirmCapture = (photo) => {
    resetPhotoEvidence()
    const nextPhoto = {
      blob: photo,
      previewUrl:
        URL.createObjectURL(photo),
    }
    photoRef.current = nextPhoto
    setCapturedPhoto(nextPhoto)

    setPhotoMessage(
      'Photo captured. A verified current location is required before submitting.'
    )

    setCameraOpen(false)
  }

  const removeCapturedPhoto = () => {
    resetPhotoEvidence()
    setPhotoMessage('')
  }

  const getLocationFromThisDevice =
    async () => {
      if (!photoRef.current || gpsInFlight.current || photoUploadInFlight.current || sessionCreationInFlight.current || submissionInFlight.current) return
      const ticket = beginLocationSelection()
      gpsInFlight.current = true
      setIsGettingLocation(true)
      setError('')

      setPhotoMessage(
        'Getting your current location...'
      )

      try {
        const location =
          await getCurrentLocation()

        if (!operations.current.isLocation(ticket)) return

        receivedLocationTicket.current = ticket
        setReceivedLocation(location)
        void uploadWithLocation(location, ticket)
      } catch (requestError) {
        if (!operations.current.isLocation(ticket)) return
        setPhotoMessage('')

        setError(
          requestError.message ||
            'The current location could not be obtained. Please retry.'
        )
      } finally {
        if (operations.current.isLocation(ticket)) {
          gpsInFlight.current = false
          setIsGettingLocation(false)
        }
      }
    }

  const startPhoneLocation = async () => {
    if (!photoRef.current || sessionCreationInFlight.current || gpsInFlight.current || photoUploadInFlight.current || submissionInFlight.current) return
    const locationTicket = beginLocationSelection()
    const sessionTicket = operations.current.newSession()
    sessionCreationInFlight.current = true
    setError('')
    setIsCreatingSession(true)

    try {
      const session =
        await createLocationSession(token)

      if (!operations.current.isSession(sessionTicket) || !photoRef.current) {
        if (session?.sessionId) void cancelLocationSession(session.sessionId, token).catch(() => undefined)
        return
      }

      activeSession.current = { sessionId: session.sessionId, sessionTicket, locationTicket }
      setLocationSession(session)

      setPhotoMessage(
        'Scan the QR code with your phone to share its current verified location.'
      )
    } catch (requestError) {
      if (!operations.current.isSession(sessionTicket)) return
      setError(
        requestError.message ||
          'A location-sharing session could not be created. Please retry.'
      )
    } finally {
      if (operations.current.isSession(sessionTicket)) {
        sessionCreationInFlight.current = false
        setIsCreatingSession(false)
      }
    }
  }

  const cancelPhoneLocation =
    useCallback(async (sessionId) => {
      if (activeSession.current?.sessionId !== sessionId) return
      const ticket = operations.current.newLocation()
      activeSession.current = null

      setLocationSession(null)

      setPhotoMessage(
        'Location sharing cancelled.'
      )

      try {
        await cancelLocationSession(
          sessionId,
          token
        )
      } catch (requestError) {
        if (!operations.current.isLocation(ticket)) return
        setError(
          requestError.message ||
            'The location-sharing session could not be cancelled cleanly; it will expire automatically.'
        )
      }
    }, [token])

  const expirePhoneLocation =
    useCallback((sessionId) => {
      if (activeSession.current?.sessionId !== sessionId) return
      operations.current.newLocation()
      activeSession.current = null
      setLocationSession(null)

      setError(
        'The QR code expired. Create a new location-sharing session and try again.'
      )
    }, [])

  const retryAddressLookup = async () => {
    if (
      uploadedPhotoRef.current?.latitude == null ||
      uploadedPhotoRef.current?.longitude == null ||
      !operations.current.isUpload(uploadedPhotoTicket.current) ||
      addressInFlight.current ||
      submissionInFlight.current
    ) {
      setError(
        'Verified coordinates are required before determining the address.'
      )

      return
    }

    setError('')

    setPhotoMessage(
      'Determining address from verified coordinates...'
    )

    const resolvedAddress =
      await resolveAddress(
        uploadedPhoto.latitude,
        uploadedPhoto.longitude,
        uploadedPhotoTicket.current
      )

    if (resolvedAddress && operations.current.isUpload(uploadedPhotoTicket.current)) {
      setPhotoMessage(
        'Verified location and address are ready for complaint submission.'
      )
    }
  }

  const submit = async (event) => {
    event.preventDefault()
    if (submissionInFlight.current || completedSubmission.current) return
    setError('')

    if (!values.title.trim()) {
      setError(
        'Complaint title is required.'
      )
      return
    }

    if (!values.description.trim()) {
      setError(
        'Complaint description is required.'
      )
      return
    }

    if (!values.category) {
      setError(
        'Issue category is required.'
      )
      return
    }

    if (!capturedPhoto) {
      setError(
        'A camera photo is required for every complaint.'
      )
      return
    }

    if (!uploadedPhoto) {
      setError(
        'The photo must be uploaded with a verified current location before submitting.'
      )
      return
    }

    if (!operations.current.isUpload(uploadedPhotoTicket.current)) {
      setError('The selected photo or location changed. Upload the current photo before submitting.')
      return
    }

    if (
      uploadedPhoto.latitude == null ||
      uploadedPhoto.longitude == null
    ) {
      setError(
        'Verified latitude and longitude are required.'
      )
      return
    }

    if (!values.address.trim()) {
      setError(
        'A verified address must be obtained from the GPS coordinates before submitting.'
      )
      return
    }

    if (isResolvingAddress || addressInFlight.current || photoUploadInFlight.current || gpsInFlight.current || sessionCreationInFlight.current || activeSession.current) {
      setError(
        'Please wait for the current photo, location, and address checks to finish.'
      )
      return
    }

    if (
      values.title.trim().length > 200 ||
      values.category.length > 100
    ) {
      setError(
        'One or more fields exceed the allowed length.'
      )
      return
    }

    const submissionTicket = operations.current.beginSubmission()
    if (!submissionTicket) return
    submissionInFlight.current = true
    setIsSubmitting(true)

    try {
      const result =
        await createComplaint(
          {
            title:
              values.title.trim(),

            description:
              values.description.trim(),

            category:
              values.category,

            address:
              values.address.trim(),

            latitude:
              uploadedPhoto.latitude,

            longitude:
              uploadedPhoto.longitude,

            imageUrl:
              uploadedPhoto.imageUrl,
          },
          token
        )

      if (!operations.current.isSubmission(submissionTicket)) return

      completedSubmission.current = true
      setCreatedComplaint(result)
      setValues(emptyForm)

      removeCapturedPhoto()
    } catch (requestError) {
      if (!operations.current.isSubmission(submissionTicket)) return
      setError(
        requestError.message ||
          'Your complaint could not be submitted. Please try again.'
      )
    } finally {
      operations.current.endSubmission(submissionTicket)
      submissionInFlight.current = false
      setIsSubmitting(false)
    }
  }

  if (createdComplaint) {
    return (
      <section className="submission-result container">
        <p className="eyebrow">
          Complaint submitted
        </p>

        <h1>
          Thank you for reporting this issue.
        </h1>

        <p>
          Your complaint
          {createdComplaint.complaintId
            ? ` #${createdComplaint.complaintId}`
            : ''}{' '}
          has been recorded. You can review its
          current status from your dashboard.
        </p>

        <div className="button-row">
          <Link
            className="button button-primary"
            to="/citizen/dashboard"
          >
            View my dashboard
          </Link>

          <button
            className="button button-secondary"
            type="button"
            onClick={() =>
              { completedSubmission.current = false; setCreatedComplaint(null) }
            }
          >
            Raise another complaint
          </button>
        </div>
      </section>
    )
  }

  const directLocationAvailable =
    deviceMode === 'mobile' ||
    deviceMode === 'ambiguous'

  const phoneLocationAvailable =
    deviceMode === 'computer' ||
    deviceMode === 'ambiguous'

  const complaintReady =
    values.title.trim() &&
    values.description.trim() &&
    values.category &&
    capturedPhoto &&
    uploadedPhoto &&
    uploadedPhoto.latitude != null &&
    uploadedPhoto.longitude != null &&
    values.address.trim() &&
    !isGettingLocation &&
    !isCreatingSession &&
    !locationSession &&
    !isUploadingPhoto &&
    !isResolvingAddress

  return (
    <section className="complaint-form-page container">
      <div className="form-page-intro">
        <p className="eyebrow">
          Citizen reporting
        </p>

        <h1>Raise a Complaint</h1>

        <p>
          All fields are mandatory. Capture photographic
          evidence and provide a verified current GPS
          location. The complaint address is determined
          automatically from the verified coordinates.
        </p>
      </div>

      {error && (
        <p
          className="form-message form-error"
          role="alert"
        >
          {error}
        </p>
      )}

      <form
        className="complaint-form"
        onSubmit={submit}
        noValidate
      >
        <label>
          Complaint title

          <input
            name="title"
            value={values.title}
            onChange={update}
            maxLength="200"
            required
            disabled={isSubmitting}
          />
        </label>

        <label>
          What happened?

          <textarea
            name="description"
            value={values.description}
            onChange={update}
            rows="5"
            required
            disabled={isSubmitting}
          />
        </label>

        <div className="form-columns">
          <label>
            Issue category

            <select
              name="category"
              value={values.category}
              onChange={update}
              required
              disabled={isSubmitting}
            >
              <option value="">
                Select a category
              </option>

              {categories.map((category) => (
                <option
                  key={category}
                  value={category}
                >
                  {category}
                </option>
              ))}
            </select>
          </label>

          <label>
            Verified address

            <input
              name="address"
              value={
                isResolvingAddress
                  ? 'Determining address from GPS...'
                  : values.address
              }
              readOnly
              required
              disabled={isSubmitting}
              placeholder="Address will be filled automatically from verified GPS"
            />
          </label>
        </div>

        {uploadedPhoto && (
          <div className="form-columns">
            <label>
              Latitude

              <input
                value={
                  uploadedPhoto.latitude ?? ''
                }
                readOnly
              />
            </label>

            <label>
              Longitude

              <input
                value={
                  uploadedPhoto.longitude ?? ''
                }
                readOnly
              />
            </label>
          </div>
        )}

        <section
          className="photo-workflow"
          aria-labelledby="photo-workflow-heading"
        >
          <div>
            <h2 id="photo-workflow-heading">
              Photographic evidence
            </h2>

            <p>
              A live camera photo and verified GPS
              location are required for every complaint.
              Gallery files, manually entered coordinates,
              and manually entered addresses are not
              accepted.
            </p>
          </div>

          {!cameraOpen &&
            !capturedPhoto && (
              <button
                className="button button-secondary"
                type="button"
                onClick={() =>
                  setCameraOpen(true)
                }
                disabled={isSubmitting}
              >
                Take required photo
              </button>
            )}

          {cameraOpen && (
            <CameraCapture
              onConfirm={confirmCapture}
              onCancel={() =>
                setCameraOpen(false)
              }
            />
          )}

          {capturedPhoto &&
            !cameraOpen && (
              <div className="captured-photo-summary">
                <img
                  src={
                    capturedPhoto.previewUrl
                  }
                  alt="Captured complaint evidence"
                />

                <div>
                  <strong>
                    Required photo captured
                  </strong>

                  <p>
                    {uploadedPhoto
                      ? 'The photo and verified location are ready for complaint submission.'
                      : 'A verified current location is required. Coordinates cannot be edited.'}
                  </p>

                  <div className="camera-actions">
                    <button
                      className="button button-secondary"
                      type="button"
                      onClick={() => {
                        removeCapturedPhoto()
                        setCameraOpen(true)
                      }}
                      disabled={isSubmitting}
                    >
                      Retake
                    </button>

                    {directLocationAvailable &&
                      !uploadedPhoto && (
                        <button
                          className="button button-primary"
                          type="button"
                          onClick={() =>
                            void getLocationFromThisDevice()
                          }
                          disabled={
                            isUploadingPhoto ||
                            isGettingLocation ||
                            isCreatingSession ||
                            Boolean(locationSession) ||
                            isSubmitting
                          }
                        >
                          {isGettingLocation ? 'Getting location...' : 'Use This Device\'s Location'}
                        </button>
                      )}

                    {phoneLocationAvailable &&
                      !uploadedPhoto &&
                      !receivedLocation && (
                        <button
                          className="button button-primary"
                          type="button"
                          onClick={() =>
                            void startPhoneLocation()
                          }
                          disabled={
                            isCreatingSession ||
                            isGettingLocation ||
                            isUploadingPhoto ||
                            isSubmitting
                          }
                        >
                          {isCreatingSession
                            ? 'Creating secure QR...'
                            : 'Get Location Using Phone'}
                        </button>
                      )}

                    {receivedLocation &&
                      !uploadedPhoto && (
                        <button
                          className="button button-primary"
                          type="button"
                          onClick={() =>
                            void uploadWithLocation(
                              receivedLocation,
                              receivedLocationTicket.current
                            )
                          }
                          disabled={
                            isUploadingPhoto ||
                            isGettingLocation ||
                            isCreatingSession ||
                            isSubmitting
                          }
                        >
                          {isUploadingPhoto
                            ? 'Uploading...'
                            : 'Retry Upload'}
                        </button>
                      )}

                    {uploadedPhoto &&
                      !values.address &&
                      !isResolvingAddress && (
                        <button
                          className="button button-primary"
                          type="button"
                          onClick={() =>
                            void retryAddressLookup()
                          }
                          disabled={isSubmitting}
                        >
                          Retry Address Lookup
                        </button>
                      )}
                  </div>
                </div>
              </div>
            )}

          {receivedLocation && (
            <div
              className="location-confirmation"
              role="status"
            >
              <strong>
                Verified location received
              </strong>

              <span>
                Latitude:{' '}
                {receivedLocation.latitude}
              </span>

              <span>
                Longitude:{' '}
                {receivedLocation.longitude}
              </span>

              <span>
                Accuracy:{' '}
                {Math.round(
                  Number(
                    receivedLocation.accuracy
                  )
                )}{' '}
                m
              </span>
            </div>
          )}

          {values.address && (
            <div
              className="location-confirmation"
              role="status"
            >
              <strong>
                Verified address
              </strong>

              <span>
                {values.address}
              </span>
            </div>
          )}

          {photoMessage && (
            <p
              className={
                uploadedPhoto &&
                values.address
                  ? 'form-message form-success'
                  : 'photo-status'
              }
              role="status"
            >
              {photoMessage}
            </p>
          )}
        </section>

        <div className="form-actions">
          <Link
            className="button button-secondary"
            to="/citizen/dashboard"
            aria-disabled={isSubmitting}
            onClick={(event) => { if (isSubmitting) event.preventDefault() }}
          >
            Cancel
          </Link>

          <button
            className="button button-primary"
            type="submit"
            disabled={
              isSubmitting ||
              isGettingLocation ||
              isCreatingSession ||
              Boolean(locationSession) ||
              isUploadingPhoto ||
              isResolvingAddress ||
              !complaintReady
            }
          >
            {isSubmitting
              ? 'Submitting...'
              : isResolvingAddress
                ? 'Determining address...'
                : 'Submit complaint'}
          </button>
        </div>
      </form>

      {locationSession && (
        <LocationQrModal
          session={locationSession}
          onCancel={() =>
            void cancelPhoneLocation(locationSession.sessionId)
          }
          onExpired={() => expirePhoneLocation(locationSession.sessionId)}
        />
      )}
    </section>
  )
}

export default ComplaintPlaceholder
