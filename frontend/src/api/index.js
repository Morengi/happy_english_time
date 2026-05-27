import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me')
}

export const userApi = {
  getProfile: () => api.get('/users/profile'),
  updateProfile: (data) => api.put('/users/profile', data),
  searchStudents: (q) => api.get('/users/search', { params: { q } }),
  getById: (id) => api.get(`/users/${id}`)
}

export const adminApi = {
  getUsers: (params) => api.get('/admin/users', { params }),
  createUser: (data) => api.post('/admin/users', data),
  updateUser: (id, data) => api.put(`/admin/users/${id}`, data),
  deleteUser: (id) => api.delete(`/admin/users/${id}`)
}

export const groupApi = {
  getAll: () => api.get('/groups'),
  getById: (id) => api.get(`/groups/${id}`),
  create: (data) => api.post('/groups', data),
  update: (id, data) => api.put(`/groups/${id}`, data),
  delete: (id) => api.delete(`/groups/${id}`),
  addMember: (id, nickname) => api.post(`/groups/${id}/members`, { nickname }),
  removeMember: (id, userId) => api.delete(`/groups/${id}/members/${userId}`),
  getMessages: (id) => api.get(`/groups/${id}/messages`),
  getRanking: (id) => api.get(`/groups/${id}/ranking`),
  addWord: (id, data) => api.post(`/groups/${id}/words`, data)
}

export const lessonApi = {
  getAll: () => api.get('/lessons'),
  getById: (id) => api.get(`/lessons/${id}`),
  create: (data) => api.post('/lessons', data),
  update: (id, data) => api.put(`/lessons/${id}`, data),
  delete: (id) => api.delete(`/lessons/${id}`),
  grantAccess: (id, groupId) => api.post(`/lessons/${id}/access`, { groupId }),
  revokeAccess: (id, groupId) => api.delete(`/lessons/${id}/access/${groupId}`),
  uploadFile: (id, formData) => api.post(`/lessons/${id}/files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  deleteFile: (id, fileId) => api.delete(`/lessons/${id}/files/${fileId}`),
  addWord: (id, data) => api.post(`/lessons/${id}/words`, data)
}

export const vocabularyApi = {
  getAll: (source) => api.get('/vocabulary', { params: source ? { source } : {} }),
  add: (data) => api.post('/vocabulary', data),
  update: (id, data) => api.put(`/vocabulary/${id}`, data),
  delete: (id) => api.delete(`/vocabulary/${id}`)
}

export const testApi = {
  getWords: (params) => api.get('/test/words', { params }),
  submit: (data) => api.post('/test/submit', data),
  getHistory: () => api.get('/test/history')
}

export const messageApi = {
  send: (data) => api.post('/messages', data),
  getPrivate: (otherId) => api.get(`/messages/private/${otherId}`),
  getContacts: () => api.get('/messages/contacts'),
  getUnread: () => api.get('/messages/unread')
}

export default api
