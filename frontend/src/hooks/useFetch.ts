import { useCallback, useEffect, useRef, useState } from 'react'
import { toApiError } from '../lib/api'

export interface FetchResult<T> {
  data: T | null
  loading: boolean
  /** message tiếng Việt thân thiện (đã qua ApiRequestError.describe) */
  error: string | null
  /** code + chi tiết kỹ thuật để đối chiếu docs/api.md khi demo */
  errorCode: string | null
  reload: () => void
  setData: (next: T) => void
}

/**
 * Hook fetch chung cho mọi màn hình: không crash khi backend thiếu endpoint,
 * luôn có loading / error / empty state rõ ràng.
 */
export function useFetch<T>(loader: () => Promise<T>, deps: unknown[], initial: T | null = null): FetchResult<T> {
  const [data, setData] = useState<T | null>(initial)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [errorCode, setErrorCode] = useState<string | null>(null)
  const [nonce, setNonce] = useState(0)
  const loaderRef = useRef(loader)
  loaderRef.current = loader
  const requestSeq = useRef(0)

  useEffect(() => {
    const seq = ++requestSeq.current
    let cancelled = false
    setLoading(true)
    setError(null)
    setErrorCode(null)
    loaderRef
      .current()
      .then((result) => {
        if (cancelled || seq !== requestSeq.current) return
        setData(result)
        setLoading(false)
      })
      .catch((err: unknown) => {
        if (cancelled || seq !== requestSeq.current) return
        const apiError = toApiError(err)
        setError(apiError.message)
        setErrorCode(apiError.code === 'UNKNOWN_ERROR' ? String(apiError.status || '-') : apiError.code)
        setData(null)
        setLoading(false)
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, nonce])

  const reload = useCallback(() => setNonce((n) => n + 1), [])

  return { data, loading, error, errorCode, reload, setData }
}

export interface ActionState {
  busy: boolean
  error: string | null
  run: <T>(task: () => Promise<T>) => Promise<T | null>
  reset: () => void
}

/** cho form submit: chặn double-click, giữ message lỗi inline */
export function useAction(): ActionState {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const mounted = useRef(true)

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  const run = useCallback(async <T,>(task: () => Promise<T>): Promise<T | null> => {
    setBusy(true)
    setError(null)
    try {
      return await task()
    } catch (err) {
      if (mounted.current) setError(toApiError(err).message)
      return null
    } finally {
      if (mounted.current) setBusy(false)
    }
  }, [])

  const reset = useCallback(() => {
    setBusy(false)
    setError(null)
  }, [])

  return { busy, error, run, reset }
}
