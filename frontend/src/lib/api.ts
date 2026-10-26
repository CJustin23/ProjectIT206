import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import type { ApiErrorBody, AuthTokens, Page } from '../types/api'
import { asList, asObject } from './normalize'
import { clearTokens, getAccessToken, getRefreshToken, setTokenPair } from './tokens'

const rawBase = import.meta.env.VITE_API_BASE_URL ?? '/api'
/** docs/api.md: base path /api/v1 — vite dev proxy chuyển tiếp /api → backend:8090 */
export const BASE_PATH = `${String(rawBase).replace(/\/+$/, '')}/v1`

/** Cấu trúc lỗi chuẩn (api.md §1) — FE switch theo `code`, không parse `message`. */
export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string
  readonly details: ApiErrorBody['details']
  readonly serverMessage: string

  constructor(status: number, code: string, serverMessage: string, details: ApiErrorBody['details']) {
    super(ApiRequestError.describe(status, code, serverMessage))
    this.name = 'ApiRequestError'
    this.status = status
    this.code = code
    this.details = details ?? []
    this.serverMessage = serverMessage
  }

  /** message tiếng Việt theo nghiệp vụ, giữ nguyên `code` để lập trình viên đối chiếu docs */
  private static describe(status: number, code: string, serverMessage: string): string {
    switch (code) {
      case 'VALIDATION_FAILED':
        return 'Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại các trường được đánh dấu.'
      case 'AUTH_INVALID_CREDENTIALS':
        return 'Email hoặc mật khẩu không đúng.'
      case 'TOKEN_EXPIRED':
        return 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.'
      case 'FORBIDDEN_ROLE':
        return 'Bạn không có quyền thực hiện thao tác này (cần vai trò chủ hộ).'
      case 'NOT_FOUND':
        return 'Không tìm thấy dữ liệu. Có thể API chưa được backend triển khai.'
      case 'BUDGET_CONFLICT':
        return 'Ngân sách vừa bị thay đổi bởi người khác. Hãy tải lại rồi thử lại.'
      case 'ACCOUNT_LOCKED':
        return 'Tài khoản bị khóa tạm thời vì đăng nhập sai 5 lần. Thử lại sau 15 phút.'
      case 'RATE_LIMITED':
        return 'Bạn thao tác quá nhanh (giới hạn 100 request/phút). Đợi chút rồi thử lại.'
      default:
        if (status === 0) return 'Không kết nối được máy chủ API (backend chưa chạy?).'
        if (status >= 500) return 'Lỗi từ máy chủ (backend đang phát triển). Vui lòng thử lại sau.'
        return serverMessage || `Lỗi không xác định (${status}).`
    }
  }

  /** lỗi field để hiển thị inline validation theo docs/api.md `details[]` */
  fieldIssues(): Record<string, string> {
    const out: Record<string, string> = {}
    const issues: NonNullable<ApiErrorBody['details']> = this.details ?? []
    issues.forEach((issue) => {
      if (issue && issue.field) out[issue.field] = issue.reason || 'Không hợp lệ'
    })
    return out
  }
}

export function toApiError(error: unknown): ApiRequestError {
  if (error instanceof ApiRequestError) return error
  if (axios.isAxiosError(error)) {
    const status = error.response?.status ?? 0
    const body = (error.response?.data ?? {}) as ApiErrorBody
    const code = body.code ?? (status === 401 ? 'TOKEN_EXPIRED' : status === 404 ? 'NOT_FOUND' : 'UNKNOWN_ERROR')
    return new ApiRequestError(status, code, body.message ?? error.message, body.details)
  }
  return new ApiRequestError(0, 'UNKNOWN_ERROR', error instanceof Error ? error.message : 'Lỗi không xác định', [])
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

const MUTATION_METHODS = ['post', 'put', 'patch', 'delete']

/** Thông báo toàn cục (ToastHost đăng ký qua main.tsx) — tránh vòng import store/api */
let networkErrorSink: ((message: string) => void) | null = null
export function setNetworkErrorHandler(handler: ((message: string) => void) | null): void {
  networkErrorSink = handler
}

/** Được gọi khi refresh token cũng hỏng → store/auth đăng ký để xóa state */
let unauthorizedListeners: Array<() => void> = []
export function onSessionExpired(listener: () => void): () => void {
  unauthorizedListeners.push(listener)
  return () => {
    unauthorizedListeners = unauthorizedListeners.filter((fn) => fn !== listener)
  }
}

function handleSessionExpired(): void {
  clearTokens()
  unauthorizedListeners.forEach((fn) => fn())
  if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
    window.location.assign('/login')
  }
}

export const http: AxiosInstance = axios.create({
  baseURL: BASE_PATH,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  const headers = AxiosHeaders.from(config.headers)
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)
  return config
})

/** refresh token dùng một lần (BR-02) → chỉ 1 request refresh tại một thời điểm */
let refreshPromise: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken()
    refreshPromise = axios
      .post<Partial<AuthTokens> & { token?: string; access_token?: string }>(
        `${BASE_PATH}/auth/refresh`,
        { refreshToken },
        { headers: { 'Content-Type': 'application/json', Accept: 'application/json' } },
      )
      .then((res) => {
        const data = asObject(res.data)
        const accessToken = String(data.accessToken ?? data.token ?? data.access_token ?? '')
        const nextRefresh = String(data.refreshToken ?? data.refresh_token ?? '') || refreshToken
        if (!accessToken) throw new ApiRequestError(401, 'TOKEN_EXPIRED', 'Không nhận được access token mới', [])
        setTokenPair(accessToken, nextRefresh ?? null)
        return accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function isAuthEndpoint(url: string | undefined): boolean {
  if (!url) return false
  return /\/auth\/(login|register|refresh|forgot-password)/.test(url)
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    const status = error.response?.status ?? 0

    if (status === 401 && config && !config._retried && !isAuthEndpoint(config.url) && getRefreshToken()) {
      config._retried = true
      try {
        const token = await refreshAccessToken()
        const headers = AxiosHeaders.from(config.headers)
        headers.set('Authorization', `Bearer ${token}`)
        config.headers = headers
        return await http.request(config)
      } catch (refreshError) {
        handleSessionExpired()
        const normalized = toApiError(refreshError)
        networkErrorSink?.(normalized.message)
        throw normalized
      }
    }

    const normalized = toApiError(error)
    if (status === 401 && !isAuthEndpoint(config?.url)) handleSessionExpired()

    const method = (config?.method ?? 'get').toLowerCase()
    if (MUTATION_METHODS.includes(method) && status !== 401) {
      networkErrorSink?.(normalized.message)
    }
    throw normalized
  },
)

/* ---------- typed helpers ---------- */

export async function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.get<T>(url, { params, ...config })
  return res.data
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.post<T>(url, body ?? {}, config)
  return res.data
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.put<T>(url, body ?? {}, config)
  return res.data
}

export async function patch<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.patch<T>(url, body ?? {}, config)
  return res.data
}

export async function remove<T = void>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const res = await http.delete<T>(url, config)
  return res.data
}

/** tải file (CSV export) kèm Bearer + xử lý 401 như request thường */
export async function download(url: string, params?: Record<string, unknown>, filename = 'export.csv'): Promise<void> {
  const res = await http.get<Blob>(url, { params, responseType: 'blob' })
  const blobUrl = URL.createObjectURL(res.data)
  const link = window.document.createElement('a')
  link.href = blobUrl
  link.download = filename
  window.document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(blobUrl)
}

/** UUID cho header Idempotency-Key (api.md §2.4) */
export function idempotencyKey(): string {
  const cryptoRef = typeof crypto !== 'undefined' ? crypto : undefined
  if (cryptoRef && typeof cryptoRef.randomUUID === 'function') return cryptoRef.randomUUID()
  return `fe-${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`
}

export interface PageParams {
  page: number
  size: number
}

/**
 * docs/api.md cho phép `_page`/`_size`/`_total`, Spring mặc định trả `page{number,size,...}`,
 * ngoài ra backend mới có thể trả mảng phẳng. Hàm này quy về Page<T> duy nhất cho UI.
 */
export function toPage<T>(raw: unknown, params: PageParams): Page<T> {
  if (Array.isArray(raw)) {
    const size = raw.length || params.size
    return { content: raw as T[], page: params.page, size, totalElements: raw.length, totalPages: raw.length === 0 ? 0 : 1 }
  }

  const body = asObject(raw)
  let content = asList(body.content ?? body.items ?? body.records ?? body.data ?? body.rows)

  if (content.length === 0) {
    const embedded = asObject(body._embedded)
    const firstArray = Object.values(embedded).find((value) => Array.isArray(value))
    if (firstArray) content = asList(firstArray)
  }

  const nestedPage = asObject(body.page ?? body.pageable)
  const page = firstNumber([body._page, body.page, nestedPage.number, body.number], params.page)
  const size = firstNumber([body._size, body.size, nestedPage.size], params.size)
  const totalElements = firstNumber([body._total, body.totalElements, nestedPage.totalElements, body.total], content.length)
  const rawTotalPages = firstNumber([body.totalPages, nestedPage.totalPages], NaN)
  const totalPages = Number.isFinite(rawTotalPages) && rawTotalPages >= 0
    ? rawTotalPages
    : size > 0
      ? Math.max(0, Math.ceil(totalElements / size))
      : 0

  return {
    content: content as T[],
    page: Math.max(0, page),
    size: size > 0 ? size : params.size,
    totalElements: Math.max(0, totalElements),
    totalPages: Math.max(0, totalPages),
  }
}

function firstNumber(candidates: unknown[], fallback: number): number {
  for (const candidate of candidates) {
    if (typeof candidate === 'number' && Number.isFinite(candidate)) return candidate
    if (typeof candidate === 'string' && candidate.trim() !== '' && Number.isFinite(Number(candidate))) {
      return Number(candidate)
    }
  }
  return fallback
}
