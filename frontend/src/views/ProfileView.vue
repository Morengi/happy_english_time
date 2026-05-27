<template>
  <div>
    <div class="page-header">
      <h1>Профиль</h1>
    </div>

    <div class="card" style="max-width:600px">
      <div class="profile-avatar">
        <div class="profile-avatar__circle">{{ initials }}</div>
        <div>
          <div class="profile-avatar__name">{{ form.fullName }}</div>
          <div class="badge badge--primary">{{ roleLabel }}</div>
        </div>
      </div>

      <form @submit.prevent="save" class="profile-form">
        <div class="form-group">
          <label>ФИО</label>
          <input v-model="form.fullName" type="text" class="form-control" required />
        </div>

        <div class="form-group">
          <label>Никнейм</label>
          <input v-model="form.nickname" type="text" class="form-control" required />
        </div>

        <div class="form-group">
          <label>Email</label>
          <input v-model="form.email" type="email" class="form-control" placeholder="Не указан" />
        </div>

        <div class="form-group">
          <label>Новый пароль</label>
          <input v-model="form.password" type="password" class="form-control"
            placeholder="Оставьте пустым, если не меняете" />
        </div>

        <p v-if="error" style="color:red;margin-bottom:12px;font-size:13px">{{ error }}</p>

        <button type="submit" class="btn btn--primary" :disabled="saving">
          {{ saving ? 'Сохраняю...' : 'Сохранить изменения' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { userApi } from '@/api'

const auth = useAuthStore()
const toast = useToastStore()
const saving = ref(false)
const error = ref('')

const form = ref({ fullName: '', nickname: '', email: '', password: '' })

const initials = computed(() => {
  return form.value.fullName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'
})

const roleLabel = computed(() => {
  const map = { ADMIN: 'Администратор', TEACHER: 'Преподаватель', STUDENT: 'Студент' }
  return map[auth.user?.role] || ''
})

onMounted(async () => {
  const { data } = await userApi.getProfile()
  form.value.fullName = data.fullName
  form.value.nickname = data.nickname
  form.value.email = data.email || ''
})

async function save() {
  error.value = ''
  saving.value = true
  try {
    const payload = { fullName: form.value.fullName, nickname: form.value.nickname, email: form.value.email }
    if (form.value.password) payload.password = form.value.password
    const { data } = await userApi.updateProfile(payload)
    auth.updateUser(data)
    form.value.password = ''
    toast.success('Профиль успешно обновлён')
  } catch (e) {
    error.value = e.response?.data?.message || 'Ошибка сохранения'
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.profile-avatar {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 32px;

  &__circle {
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: $primary;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: 700;
    flex-shrink: 0;
  }

  &__name {
    font-size: $font-size-xl;
    font-weight: 600;
    margin-bottom: 6px;
  }
}

.profile-form { display: flex; flex-direction: column; gap: 4px; }
</style>
