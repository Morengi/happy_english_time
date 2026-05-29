import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const user  = ref(null)
  const token = ref(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const isTeacherOrAdmin = computed(() =>
    user.value?.role === 'TEACHER' || user.value?.role === 'ADMIN'
  )

  /**
   * Bootstrap: read token from ?token= URL param first, then localStorage.
   * Then fetch current user from /api/users/profile.
   */
  async function init() {
    // 1. Extract token from URL param (when opened from main platform iframe)
    const params = new URLSearchParams(window.location.search)
    const urlToken = params.get('token')
    if (urlToken) {
      localStorage.setItem('vr_token', urlToken)
      // Clean token from URL without reloading the page
      params.delete('token')
      const newUrl = window.location.pathname + (params.toString() ? '?' + params.toString() : '')
      window.history.replaceState({}, '', newUrl)
    }

    // 2. Use stored token
    const stored = localStorage.getItem('vr_token')
    if (!stored) return

    token.value = stored

    // 3. Fetch user info
    try {
      const { data } = await authApi.me()
      user.value = {
        id:        data.id,
        nickname:  data.nickname,
        fullName:  data.fullName,
        role:      data.role,
        avatarUrl: data.avatarUrl || null
      }
    } catch {
      // Token is invalid — clear it
      logout()
    }
  }

  async function login(nickname, password) {
    const { data } = await authApi.login({ nickname, password })
    token.value = data.token
    localStorage.setItem('vr_token', data.token)
    user.value = {
      id:        data.userId,
      nickname:  data.nickname,
      fullName:  data.fullName,
      role:      data.role,
      avatarUrl: data.avatarUrl || null
    }
  }

  function logout() {
    token.value = null
    user.value  = null
    localStorage.removeItem('vr_token')
  }

  return { user, token, isAuthenticated, isTeacherOrAdmin, init, login, logout }
})
