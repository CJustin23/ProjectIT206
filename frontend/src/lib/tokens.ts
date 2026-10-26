/**
 * Khóa localStorage dùng chung FE (docs/api.md không đặt tên → thống nhất ở đây).
 * Module này cố tình không import axios/zustand để tránh vòng quan hệ.
 */
export const ACCESS_TOKEN_KEY = 'ffms_access_token'
export const REFRESH_TOKEN_KEY = 'ffms_refresh_token'

function storage(): Storage | null {
  try {
    return window.localStorage
  } catch {
    return null
  }
}

export function getAccessToken(): string | null {
  return storage()?.getItem(ACCESS_TOKEN_KEY) ?? null
}

export function getRefreshToken(): string | null {
  return storage()?.getItem(REFRESH_TOKEN_KEY) ?? null
}

export function setTokenPair(accessToken: string | null, refreshToken: string | null): void {
  const store = storage()
  if (!store) return
  if (accessToken) store.setItem(ACCESS_TOKEN_KEY, accessToken)
  else store.removeItem(ACCESS_TOKEN_KEY)
  if (refreshToken) store.setItem(REFRESH_TOKEN_KEY, refreshToken)
  else store.removeItem(REFRESH_TOKEN_KEY)
}

export function clearTokens(): void {
  setTokenPair(null, null)
}

export function hasSession(): boolean {
  return Boolean(getAccessToken() || getRefreshToken())
}
