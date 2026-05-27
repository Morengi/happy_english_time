<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-card__header">
        <div class="login-card__logo">📚</div>
        <h1 class="login-card__title">EnglishPro</h1>
        <p class="login-card__subtitle">Платформа для изучения английского</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-card__form">
        <div class="form-group">
          <label>Никнейм</label>
          <input v-model="form.nickname" type="text" class="form-control"
            placeholder="Введите ваш никнейм" required autocomplete="username" />
        </div>

        <div class="form-group">
          <label>Пароль</label>
          <div class="input-with-icon">
            <input v-model="form.password" :type="showPassword ? 'text' : 'password'"
              class="form-control" placeholder="Введите пароль"
              required autocomplete="current-password" />
            <button type="button" class="input-icon-btn" @click="showPassword = !showPassword">
              {{ showPassword ? '🙈' : '👁️' }}
            </button>
          </div>
        </div>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button type="submit" class="btn btn--primary btn--full btn--lg" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:18px;height:18px;border-width:2px"></span>
          <span v-else>Войти</span>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = ref({ nickname: '', password: '' })
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(form.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Неверный никнейм или пароль'
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e2a4a 0%, #2d4080 100%);
  padding: 20px;
}

.login-card {
  background: white;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-lg;
  width: 100%;
  max-width: 420px;
  padding: 40px;

  &__header { text-align: center; margin-bottom: 32px; }

  &__logo {
    font-size: 52px;
    margin-bottom: 12px;
    animation: float 3s ease-in-out infinite;
  }

  &__title {
    font-size: 28px;
    font-weight: 700;
    color: $text;
    margin-bottom: 6px;
  }

  &__subtitle { color: $text-muted; font-size: $font-size-base; }

  &__form { display: flex; flex-direction: column; gap: 4px; }
}

.login-error {
  background: $danger-light;
  color: darken($danger, 10%);
  padding: 10px 14px;
  border-radius: $border-radius-sm;
  font-size: $font-size-sm;
}

.input-with-icon {
  position: relative;

  .form-control { padding-right: 44px; }
}

.input-icon-btn {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
</style>
