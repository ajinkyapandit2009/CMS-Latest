import { useLayoutEffect } from 'react'
import { useLocation } from 'react-router-dom'

function ScrollToTop() {
  const { pathname, hash } = useLocation()

  useLayoutEffect(() => {
    const previousRestoration = window.history.scrollRestoration
    window.history.scrollRestoration = 'manual'
    return () => { window.history.scrollRestoration = previousRestoration }
  }, [])

  useLayoutEffect(() => {
    // Hash links still reach their section. Filters and local state do not reset scrolling.
    if (hash) {
      const frame = window.requestAnimationFrame(() => {
        let id
        try { id = decodeURIComponent(hash.slice(1)) } catch { return }
        document.getElementById(id)?.scrollIntoView({ behavior: 'instant', block: 'start' })
      })
      return () => window.cancelAnimationFrame(frame)
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' })
  }, [pathname, hash])

  return null
}

export default ScrollToTop
