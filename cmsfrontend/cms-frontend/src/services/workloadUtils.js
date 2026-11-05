const positiveInteger = (value, fallback) => {
  const parsed = Number(value)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : fallback
}

const yellowFrom = positiveInteger(import.meta.env.VITE_CMS_WORKLOAD_YELLOW_FROM, 4)
const configuredRedFrom = positiveInteger(import.meta.env.VITE_CMS_WORKLOAD_RED_FROM, 8)

export const workloadThresholds = {
  yellowFrom,
  redFrom: configuredRedFrom > yellowFrom ? configuredRedFrom : yellowFrom + 1,
}

export function workloadLevel(totalActive) {
  if (totalActive >= workloadThresholds.redFrom) return { key: 'high', label: 'High' }
  if (totalActive >= workloadThresholds.yellowFrom) return { key: 'moderate', label: 'Moderate' }
  return { key: 'low', label: 'Low' }
}
