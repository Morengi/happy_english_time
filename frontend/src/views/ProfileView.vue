<template>
  <div>
    <div class="page-header">
      <h1>Профиль</h1>
    </div>

    <div class="card" style="max-width:600px">
      <div class="profile-avatar">
        <!-- Avatar circle: photo or initials -->
        <div class="profile-avatar__circle" @click="avatarInputRef.click()" title="Нажмите, чтобы изменить фото">
          <img v-if="avatarUrl" :src="avatarUrl" class="profile-avatar__img" alt="Аватар" />
          <span v-else>{{ initials }}</span>
          <div class="profile-avatar__overlay">
            <span>{{ avatarUploading ? '⏳' : '📷' }}</span>
          </div>
        </div>
        <input type="file" ref="avatarInputRef" accept="image/*" style="display:none" @change="onAvatarChange" />

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
const avatarUploading = ref(false)
const avatarInputRef = ref(null)
const avatarUrl = ref(null)

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
  avatarUrl.value = data.avatarUrl || null
})

async function onAvatarChange(e) {
  const file = e.target.files[0]
  e.target.value = ''
  if (!file) return
  avatarUploading.value = true
  try {
    const { data } = await userApi.uploadAvatar(file)
    avatarUrl.value = data.avatarUrl
    auth.updateUser({ avatarUrl: data.avatarUrl })
    toast.success('Фото профиля обновлено')
  } catch {
    toast.error('Ошибка загрузки фото')
  } finally {
    avatarUploading.value = false
  }
}

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
    position: relative;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: $primary;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    font-weight: 700;
    flex-shrink: 0;
    cursor: pointer;
    overflow: hidden;

    &:hover .profile-avatar__overlay { opacity: 1; }
  }

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 50%;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    background: rgba(0,0,0,.45);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 22px;
    opacity: 0;
    transition: opacity .2s;
  }

  &__name {
    font-size: $font-size-xl;
    font-weight: 600;
    margin-bottom: 6px;
  }
}

.profile-form { display: flex; flex-direction: column; gap: 4px; }
</style>
