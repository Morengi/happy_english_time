import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

// Attach JWT from localStorage on every request
api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('vr_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// ── Voice Rooms ───────────────────────────────────────────────────────────────
export const roomsApi = {
  list:         ()       => api.get('/voice-rooms'),
  create:       (data)   => api.post('/voice-rooms', data),
  end:          (id)     => api.delete(`/voice-rooms/${id}`),
  participants: (id)     => api.get(`/voice-rooms/${id}/participants`),
  join:         (id)     => api.post(`/voice-rooms/${id}/join`),
  leave:        (id)     => api.post(`/voice-rooms/${id}/leave`)
}

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authApi = {
  me:    ()      => api.get('/users/profile'),
  login: (data)  => api.post('/auth/login', data)
}

export default api
