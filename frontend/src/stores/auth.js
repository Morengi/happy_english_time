import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('token') || null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isTeacher = computed(() => user.value?.role === 'TEACHER' || user.value?.role === 'ADMIN')
  const isStudent = computed(() => user.value?.role === 'STUDENT')

  async function login(credentials) {
    const { data } = await authApi.login(credentials)
    token.value = data.token
    user.value = {
      id: data.userId,
      nickname: data.nickname,
      fullName: data.fullName,
      role: data.role,
      avatarUrl: data.avatarUrl || null
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return data
  }

  async function fetchMe() {
    try {
      const { data } = await authApi.me()
      user.value = data
      localStorage.setItem('user', JSON.stringify(data))
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function updateUser(data) {
    user.value = { ...user.value, ...data }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  return { user, token, isAuthenticated, isAdmin, isTeacher, isStudent, login, logout, fetchMe, updateUser }
})
