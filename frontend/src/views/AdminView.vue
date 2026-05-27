<template>
  <div>
    <div class="page-header">
      <h1>Администрирование</h1>
      <button class="btn btn--primary" @click="showCreate = true">+ Создать пользователя</button>
    </div>

    <!-- Filters -->
    <div class="card card--compact" style="margin-bottom:20px">
      <div class="admin-filters">
        <input v-model="search" type="text" class="form-control" placeholder="Поиск по ФИО или нику..."
          style="max-width:300px" @input="loadUsers" />
        <div class="vocab-tabs">
          <button v-for="r in roleFilters" :key="r.value" class="vocab-tab"
            :class="{ active: roleFilter === r.value }"
            @click="roleFilter = r.value; loadUsers()">
            {{ r.label }}
          </button>
        </div>
        <span class="text-muted text-sm">Всего: {{ totalUsers }}</span>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="loading-center" style="min-height:200px"><div class="spinner"></div></div>

      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ФИО</th>
              <th>Никнейм</th>
              <th>Email</th>
              <th>Роль</th>
              <th>Создан</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.fullName }}</td>
              <td>@{{ user.nickname }}</td>
              <td>{{ user.email || '—' }}</td>
              <td>
                <span class="badge" :class="roleClass(user.role)">{{ roleLabel(user.role) }}</span>
              </td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>
                <div class="flex gap-8">
                  <button class="btn btn--ghost btn--sm" @click="startEdit(user)" title="Редактировать">✏️</button>
                  <button class="btn btn--ghost btn--sm" @click="deleteUser(user.id)" title="Удалить"
                    :disabled="user.id === auth.user?.id">🗑️</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="users.length === 0" class="empty-state">
          <div class="empty-state__icon">👤</div>
          <div class="empty-state__title">Пользователей не найдено</div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="btn btn--ghost btn--sm" :disabled="page === 0" @click="page--; loadUsers()">← Назад</button>
        <span class="text-sm text-muted">{{ page + 1 }} / {{ totalPages }}</span>
        <button class="btn btn--ghost btn--sm" :disabled="page >= totalPages - 1" @click="page++; loadUsers()">Вперёд →</button>
      </div>
    </div>

    <!-- Create modal -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal__header">
          <h3>Создать пользователя</h3>
          <button class="btn btn--ghost btn--icon" @click="showCreate = false">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>ФИО</label>
            <input v-model="createForm.fullName" type="text" class="form-control" />
          </div>
          <div class="form-group">
            <label>Никнейм</label>
            <input v-model="createForm.nickname" type="text" class="form-control" />
          </div>
          <div class="form-group">
            <label>Email (необязательно)</label>
            <input v-model="createForm.email" type="email" class="form-control" />
          </div>
          <div class="form-group">
            <label>Пароль</label>
            <input v-model="createForm.password" type="password" class="form-control" />
          </div>
          <div class="form-group">
            <label>Роль</label>
            <select v-model="createForm.role" class="form-control">
              <option value="STUDENT">Студент</option>
              <option value="TEACHER">Преподаватель</option>
              <option value="ADMIN">Администратор</option>
            </select>
          </div>
          <p v-if="createError" style="color:red;font-size:13px">{{ createError }}</p>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="showCreate = false">Отмена</button>
          <button class="btn btn--primary" @click="createUser">Создать</button>
        </div>
      </div>
    </div>

    <!-- Edit modal -->
    <div v-if="editUser" class="modal-overlay" @click.self="editUser = null">
      <div class="modal">
        <div class="modal__header">
          <h3>Редактировать пользователя</h3>
          <button class="btn btn--ghost btn--icon" @click="editUser = null">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>ФИО</label>
            <input v-model="editForm.fullName" type="text" class="form-control" />
          </div>
          <div class="form-group">
            <label>Никнейм</label>
            <input v-model="editForm.nickname" type="text" class="form-control" />
          </div>
          <div class="form-group">
            <label>Email</label>
            <input v-model="editForm.email" type="email" class="form-control" />
          </div>
          <div class="form-group">
            <label>Новый пароль (оставьте пустым)</label>
            <input v-model="editForm.password" type="password" class="form-control" />
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="editUser = null">Отмена</button>
          <button class="btn btn--primary" @click="saveUser">Сохранить</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { adminApi } from '@/api'

const auth = useAuthStore()
const toast = useToastStore()
const users = ref([])
const loading = ref(true)
const search = ref('')
const roleFilter = ref('')
const page = ref(0)
const totalPages = ref(1)
const totalUsers = ref(0)
const showCreate = ref(false)
const createError = ref('')
const editUser = ref(null)

const createForm = ref({ fullName: '', nickname: '', email: '', password: '', role: 'STUDENT' })
const editForm = ref({ fullName: '', nickname: '', email: '', password: '' })

const roleFilters = [
  { value: '', label: 'Все' },
  { value: 'STUDENT', label: 'Студенты' },
  { value: 'TEACHER', label: 'Преподаватели' },
  { value: 'ADMIN', label: 'Администраторы' }
]

onMounted(loadUsers)

async function loadUsers() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (search.value) params.search = search.value
    if (roleFilter.value) params.role = roleFilter.value
    const { data } = await adminApi.getUsers(params)
    users.value = data.content
    totalPages.value = data.totalPages
    totalUsers.value = data.totalElements
  } finally {
    loading.value = false
  }
}

async function createUser() {
  createError.value = ''
  try {
    const { data } = await adminApi.createUser(createForm.value)
    users.value.unshift(data)
    showCreate.value = false
    createForm.value = { fullName: '', nickname: '', email: '', password: '', role: 'STUDENT' }
    toast.success('Пользователь создан')
    totalUsers.value++
  } catch (e) { createError.value = e.response?.data?.message || 'Ошибка' }
}

function startEdit(user) {
  editUser.value = user
  editForm.value = { fullName: user.fullName, nickname: user.nickname, email: user.email || '', password: '' }
}

async function saveUser() {
  try {
    const payload = { ...editForm.value }
    if (!payload.password) delete payload.password
    const { data } = await adminApi.updateUser(editUser.value.id, payload)
    const idx = users.value.findIndex(u => u.id === editUser.value.id)
    if (idx !== -1) users.value[idx] = data
    editUser.value = null
    toast.success('Пользователь обновлён')
  } catch (e) { toast.error(e.response?.data?.message || 'Ошибка') }
}

async function deleteUser(id) {
  if (!confirm('Удалить пользователя? Это действие необратимо.')) return
  try {
    await adminApi.deleteUser(id)
    users.value = users.value.filter(u => u.id !== id)
    toast.success('Пользователь удалён')
    totalUsers.value--
  } catch { toast.error('Ошибка удаления') }
}

function roleLabel(role) {
  return { ADMIN: 'Администратор', TEACHER: 'Преподаватель', STUDENT: 'Студент' }[role] || role
}

function roleClass(role) {
  return { ADMIN: 'badge--danger', TEACHER: 'badge--warning', STUDENT: 'badge--primary' }[role] || ''
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('ru-RU')
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.admin-filters {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid $border;
  margin-top: 16px;
}
</style>
