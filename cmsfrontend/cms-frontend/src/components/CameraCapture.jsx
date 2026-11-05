import { useEffect, useRef, useState } from 'react'

const cameraErrorMessage = (error) => {
  if (error?.name === 'NotAllowedError' || error?.name === 'SecurityError') return 'Camera permission was denied. Allow camera access and try again.'
  if (error?.name === 'NotFoundError') return 'No camera is available on this device.'
  if (error?.name === 'NotReadableError') return 'The camera is already in use or unavailable. Close other camera apps and try again.'
  return 'The camera could not be started. Please try again.'
}

function CameraCapture({ onConfirm, onCancel }) {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const previewUrlRef = useRef(null)
  const requestIdRef = useRef(0)
  const captureIdRef = useRef(0)
  const [devices, setDevices] = useState([])
  const [selectedDeviceId, setSelectedDeviceId] = useState('')
  const [capturedPhoto, setCapturedPhoto] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [error, setError] = useState('')
  const [isStarting, setIsStarting] = useState(false)
  const [cameraReady, setCameraReady] = useState(false)

  const stopCamera = () => {
    requestIdRef.current += 1
    captureIdRef.current += 1
    streamRef.current?.getTracks().forEach((track) => track.stop())
    streamRef.current = null
    setCameraReady(false)
    if (videoRef.current) videoRef.current.srcObject = null
  }

  const clearCapturedPhoto = () => {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current)
    previewUrlRef.current = null
    setPreviewUrl('')
    setCapturedPhoto(null)
  }

  const loadDevices = async () => {
    const availableDevices = await navigator.mediaDevices.enumerateDevices()
    return availableDevices.filter((device) => device.kind === 'videoinput')
  }

  const startCamera = async (deviceId = selectedDeviceId) => {
    if (!navigator.mediaDevices?.getUserMedia) {
      setError('Live camera capture is not supported by this browser.')
      return
    }
    stopCamera()
    const requestId = requestIdRef.current
    setError('')
    setCameraReady(false)
    setIsStarting(true)
    try {
      let stream
      try {
        stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: deviceId ? { deviceId: { exact: deviceId } } : { facingMode: { ideal: 'environment' } } })
      } catch (initialError) {
        if (requestId !== requestIdRef.current) return
        if (deviceId) throw initialError
        stream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true })
      }
      if (requestId !== requestIdRef.current) {
        stream.getTracks().forEach((track) => track.stop())
        return
      }
      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play().catch(() => undefined)
      }
      const devices = await loadDevices()
      if (requestId !== requestIdRef.current) return
      setDevices(devices)
      setCameraReady(true)
      setIsStarting(false)
    } catch (cameraError) {
      if (requestId !== requestIdRef.current) return
      stopCamera()
      setError(cameraErrorMessage(cameraError))
      setIsStarting(false)
    }
  }

  const capturePhoto = () => {
    const captureId = captureIdRef.current
    const video = videoRef.current
    if (!video || !video.videoWidth || !video.videoHeight) {
      setError('The camera is not ready yet. Please wait a moment and try again.')
      return
    }
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    canvas.getContext('2d')?.drawImage(video, 0, 0, canvas.width, canvas.height)
    canvas.toBlob((blob) => {
      if (captureId !== captureIdRef.current) return
      if (!blob) {
        setError('The photo could not be captured. Please try again.')
        return
      }
      clearCapturedPhoto()
      const nextPreviewUrl = URL.createObjectURL(blob)
      previewUrlRef.current = nextPreviewUrl
      setCapturedPhoto(blob)
      setPreviewUrl(nextPreviewUrl)
      stopCamera()
    }, 'image/jpeg', 0.9)
  }

  const retake = () => {
    clearCapturedPhoto()
    void startCamera()
  }

  const cancel = () => {
    stopCamera()
    clearCapturedPhoto()
    onCancel()
  }

  useEffect(() => () => {
    stopCamera()
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current)
  }, [])

  return <section className="camera-capture" aria-labelledby="camera-heading">
    <div className="camera-capture-header"><div><p className="eyebrow">Camera capture</p><h2 id="camera-heading">Photograph the issue</h2></div><button className="button button-quiet" type="button" onClick={cancel}>Close camera</button></div>
    {error && <p className="form-message form-error" role="alert">{error}</p>}
    {!capturedPhoto && <>
      {devices.length > 1 && <label className="camera-device-select">Camera<select value={selectedDeviceId} onChange={(event) => { setSelectedDeviceId(event.target.value); void startCamera(event.target.value) }}><option value="">Default camera</option>{devices.map((device, index) => <option key={device.deviceId} value={device.deviceId}>{device.label || `Camera ${index + 1}`}</option>)}</select></label>}
      <div className="camera-preview"><video ref={videoRef} autoPlay muted playsInline aria-label="Live camera preview" /><p className="camera-preview-placeholder">{isStarting ? 'Starting camera…' : 'Open your live camera preview to take a photograph.'}</p></div>
      <div className="camera-actions"><button className="button button-secondary" type="button" onClick={() => void startCamera()} disabled={isStarting}>{isStarting ? 'Starting…' : 'Open camera'}</button><button className="button button-primary" type="button" onClick={capturePhoto} disabled={!cameraReady}>Capture photo</button></div>
    </>}
    {capturedPhoto && <><div className="captured-photo"><img src={previewUrl} alt="Captured complaint evidence" /></div><div className="camera-actions"><button className="button button-secondary" type="button" onClick={retake}>Retake</button><button className="button button-primary" type="button" onClick={() => onConfirm(capturedPhoto)}>Confirm photo</button></div></>}
  </section>
}

export default CameraCapture
