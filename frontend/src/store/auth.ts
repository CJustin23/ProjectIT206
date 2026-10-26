import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import { authApi, familyApi } from '../lib/endpoints'
import { onSessionExpired, toApiError } from '../lib/api'
import { clearTokens, getAccessToken, getRefreshToken, setTokenPair } from '../lib/tokens'
import type { Family, LoginPayload, Role, User } from '../types/api'

/** key phụ để lib/endpoints resolve familyId mà không import store (tránh vòng quan hệ) */
export const FAMILY_ID_KEY = 'ffms_family_id'

function writeFamilyId(family: Family | null): void {
  try {
    if (family && family.id > 0) window.localStorage.setItem(FAMILY_ID_KEY, String(family.id))
    else window.localStorage.removeItem(FAMILY_ID_KEY)
  } catch {
    /* storage bị chặn: bỏ qua, state trong memory vẫn dùng được */
  }
}

function readFamilyId(): number {
  try {
    return Number(window.localStorage.getItem(FAMILY_ID_KEY) ?? 0)
  } catch {
    return 0
  }
}

export interface AuthState {
  user: User | null
  family: Family | null
  families: Family[]
  accessToken: string | null
  refreshToken: string | null
  /** bootstrap()/me() đã chạy xong — ProtectedRoute dựa vào cờ này để hiện loading */
  ready: boolean
  busy: boolean
  error: string | null

  login: (payload: LoginPayload) => Promise<boolean>
  register: (payload: LoginPayload & { fullName: string }) => Promise<boolean>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
  bootstrap: () => Promise<void>
  switchFamily: (familyId: number) => Promise<void>
  createFamily: (name: string) => Promise<Family | null>
  clearError: () => void
}

type PersistedAuth = Pick<AuthState, 'user' | 'family' | 'families' | 'accessToken' | 'refreshToken'>

export const useAuthStore = create<AuthState>()(
  persist<AuthState, [], [], PersistedAuth>(
    (set, get) => ({
      user: null,
      family: null,
      families: [],
      accessToken: getAccessToken(),
      refreshToken: getRefreshToken(),
      ready: false,
      busy: false,
      error: null,

      login: async (payload) => {
        set({ busy: true, error: null })
        try {
          const res = await authApi.login(payload)
          applySession(res.accessToken, res.refreshToken, res.user, res.families, set, get)
          set({ busy: false, ready: true, error: null })
          return true
        } catch (err) {
          set({ busy: false, error: toApiError(err).message })
          return false
        }
      },

      register: async (payload) => {
        set({ busy: true, error: null })
        try {
          const res = await authApi.register(payload)
          if (res.accessToken) {
            applySession(res.accessToken, res.refreshToken, res.user, res.families, set, get)
            set({ busy: false, ready: true })
            return true
          }
          set({ busy: false, error: null })
          return false
        } catch (err) {
          set({ busy: false, error: toApiError(err).message })
          return false
        }
      },

      logout: async () => {
        await authApi.logout()
        clearTokens()
        writeFamilyId(null)
        set({ user: null, family: null, families: [], accessToken: null, refreshToken: null, ready: true, busy: false, error: null })
      },

      refreshUser: async () => {
        try {
          const me = await authApi.me()
          const families = me.families.length > 0 ? me.families : get().families
          const family = me.family ?? families.find((f) => f.id === readFamilyId()) ?? families[0] ?? null
          writeFamilyId(family)
          set({
            user: me.user.id > 0 ? { ...me.user, role: me.user.role ?? family?.myRole ?? null } : get().user,
            family,
            families,
          })
        } catch (err) {
          const apiError = toApiError(err)
          if (apiError.status === 401) {
            clearTokens()
            set({ accessToken: null, refreshToken: null })
          }
        }
      },

      bootstrap: async () => {
        const token = getAccessToken()
        if (!token) {
          set({ ready: true })
          return
        }
        set({ accessToken: token, refreshToken: getRefreshToken(), ready: false })
        await get().refreshUser()
        set({ ready: true })
      },

      switchFamily: async (familyId) => {
        const target = get().families.find((f) => f.id === familyId) ?? null
        if (!target) return
        writeFamilyId(target)
        set({ family: target, user: get().user ? { ...get().user!, role: target.myRole } : null })
        try {
          await authApi.updateProfile({ familyId })
          await get().refreshUser()
        } catch {
          /* backend chưa hỗ trợ đổi hộ (PATCH /users/me) — vẫn giữ selection cục bộ */
        }
      },

      createFamily: async (name) => {
        set({ busy: true, error: null })
        try {
          const family = await familyApi.create(name)
          writeFamilyId(family)
          set({ family, families: family.id > 0 ? [...get().families.filter((f) => f.id !== family.id), family] : get().families })
          await get().refreshUser()
          set({ busy: false })
          return family
        } catch (err) {
          set({ busy: false, error: toApiError(err).message })
          return null
        }
      },

      clearError: () => set({ error: null }),
    }),
    {
      name: 'ffms_auth',
      storage: createJSONStorage(() => window.localStorage),
      partialize: (state) => ({
        user: state.user,
        family: state.family,
        families: state.families,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
      onRehydrateStorage: () => (state) => {
        // đồng bộ token đã persist sang key localStorage mà axios interceptor đọc
        if (state?.accessToken || state?.refreshToken) {
          setTokenPair(state.accessToken ?? null, state.refreshToken ?? null)
        }
        if (state?.family) writeFamilyId(state.family)
      },
    },
  ),
)

function applySession(
  accessToken: string,
  refreshToken: string,
  user: User,
  families: Family[],
  set: (partial: Partial<AuthState>) => void,
  get: () => AuthState,
): void {
  setTokenPair(accessToken || null, refreshToken || null)
  const storedFamilyId = readFamilyId()
  const family = families.find((f) => f.id === storedFamilyId) ?? families[0] ?? null
  writeFamilyId(family)
  const active = family ? { ...family, myRole: family.myRole ?? user.role ?? null } : null
  set({
    accessToken: accessToken || null,
    refreshToken: refreshToken || null,
    user: user.id > 0 ? { ...user, role: user.role ?? active?.myRole ?? null } : get().user,
    family: active,
    families,
  })
}

/** refresh token hết hạn vĩnh viễn → interceptor báo về đây để xóa phiên */
onSessionExpired(() => {
  useAuthStore.setState({
    user: null,
    family: null,
    families: [],
    accessToken: null,
    refreshToken: null,
    ready: true,
    busy: false,
  })
})

/* ---------- selectors / helpers dùng trong component ---------- */

export const selectUser = (state: AuthState): User | null => state.user
export const selectFamily = (state: AuthState): Family | null => state.family
export const selectIsAuthenticated = (state: AuthState): boolean => Boolean(state.accessToken || state.refreshToken)
export const selectIsReady = (state: AuthState): boolean => state.ready

/** HeadOfFamily (OWNER) hoặc ADMIN mới được sửa ví / danh mục / ngân sách / thành viên (SRS §2.2) */
export function canManageFamily(role: Role | null | undefined, isAdminAccount = false): boolean {
  return role === 'OWNER' || role === 'ADMIN' || isAdminAccount
}

/** quyền xóa/xửa thành viên & danh mục hệ thống: chỉ chủ hộ */
export function isHeadOfFamily(role: Role | null | undefined): boolean {
  return role === 'OWNER'
}

export function isAdminUser(user: User | null): boolean {
  return Boolean(user?.isAdmin)
}
