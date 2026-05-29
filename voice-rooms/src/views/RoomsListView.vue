<template>
  <!-- Standalone login fallback (when not embedded) -->
  <div v-if="showLogin" class="login-wrap">
    <div class="login-card">
      <h2>🎤 Голосовые комнаты</h2>
      <p>Войдите, чтобы продолжить</p>
      <form @submit.prevent="doLogin">
        <input v-model="loginForm.nickname" class="input" placeholder="Логин" required />
        <input v-model="loginForm.password" class="input" type="password" placeholder="Пароль" required />
        <p v-if="loginError" class="error">{{ loginError }}</p>
        <button class="btn btn--primary" type="submit" :disabled="loginLoading">
          {{ loginLoading ? 'Входим...' : 'Войти' }}
        </button>
      </form>
    </div>
  </div>

  <!-- Main rooms list -->
  <div v-else class="rooms-page">
    <header class="rooms-page__header">
      <div class="rooms-page__title">
        <span class="rooms-page__icon">🎤</span>
        <div>
          <h1>Голосовые комнаты</h1>
          <p class="rooms-page__subtitle">Активные комнаты для общения</p>
        </div>
      </div>
      <button v-if="auth.isTeacherOrAdmin" class="btn btn--primary" @click="showCreate = true">
        + Создать комнату
      </button>
    </header>

    <div v-if="rooms.loading" class="rooms-page__state">Загрузка...</div>
    <div v-else-if="rooms.error" class="rooms-page__state rooms-page__state--error">{{ rooms.error }}</div>

    <div v-else-if="rooms.rooms.length === 0" class="rooms-page__empty">
      <div class="rooms-page__empty-icon">🔇</div>
      <p>Нет активных комнат</p>
      <p v-if="auth.isTeacherOrAdmin" class="rooms-page__empty-hint">Создайте первую комнату, чтобы начать</p>
    </div>

    <div v-else class="rooms-grid">
      <div
        v-for="room in rooms.rooms"
        :key="room.id"
        class="room-card"
        @click="enterRoom(room)"
      >
        <div class="room-card__header">
          <span class="room-card__icon">🔊</span>
          <div class="room-card__info">
            <h3>{{ room.name }}</h3>
            <span class="room-card__creator">{{ room.creatorName }}</span>
          </div>
          <button
            v-if="canEndRoom(room)"
            class="btn btn--ghost btn--icon room-card__end"
            title="Закрыть комнату"
            @click.stop="confirmEnd(room)"
          >✕</button>
        </div>
        <div class="room-card__footer">
          <span class="room-card__participants">
            👥 {{ room.participantCount }}
            <template v-if="room.maxParticipants"> / {{ room.maxParticipants }}</template>
          </span>
          <span class="room-card__join">Войти →</span>
        </div>
      </div>
    </div>

    <!-- Create room modal -->
    <CreateRoomModal v-if="showCreate" @close="showCreate = false" @created="onRoomCreated" />

    <!-- End confirmation -->
    <div v-if="roomToEnd" class="modal-overlay" @click.self="roomToEnd = null">
      <div class="modal">
        <h3>Закрыть комнату?</h3>
        <p>«{{ roomToEnd.name }}» будет закрыта для всех участников.</p>
        <div class="modal__actions">
          <button class="btn btn--ghost" @click="roomToEnd = null">Отмена</button>
          <button class="btn btn--danger" @click="doEndRoom">Закрыть</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import { useWebRTCStore } from '@/stores/webrtc'
import CreateRoomModal from '@/components/CreateRoomModal.vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs.min.js'

const router = useRouter()
const route  = useRoute()
const auth   = useAuthStore()
const rooms  = useRoomsStore()
const webrtc = useWebRTCStore()

const showCreate  = ref(false)
const roomToEnd   = ref(null)
const showLogin   = ref(false)
const loginForm   = ref({ nickname: '', password: '' })
const loginError  = ref('')
const loginLoading = ref(false)

// STOMP client for live room list updates
let listStomp = null

onMounted(async () => {
  // Check if we need to show a login form (standalone mode)
  if (route.query.login === '1' || !auth.isAuthenticated) {
    showLogin.value = true
    return
  }
  await rooms.fetchRooms()
  subscribeToRoomList()
})

onBeforeUnmount(() => { listStomp?.deactivate() })

function subscribeToRoomList() {
  listStomp = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${auth.token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      listStomp.subscribe('/topic/voice-rooms/list', msg => {
        rooms.handleNewRoom(JSON.parse(msg.body))
      })
    }
  })
  listStomp.activate()
}

async function doLogin() {
  loginError.value  = ''
  loginLoading.value = true
  try {
    await auth.login(loginForm.value.nickname, loginForm.value.password)
    showLogin.value = false
    await rooms.fetchRooms()
    subscribeToRoomList()
  } catch {
    loginError.value = 'Неверный логин или пароль'
  } finally {
    loginLoading.value = false
  }
}

async function enterRoom(room) {
  await router.push(`/voice-rooms/room/${room.id}`)
}

function canEndRoom(room) {
  return room.creatorId === auth.user?.id || auth.user?.role === 'ADMIN'
}

function confirmEnd(room) { roomToEnd.value = room }

async function doEndRoom() {
  await rooms.endRoom(roomToEnd.value.id)
  roomToEnd.value = null
}

function onRoomCreated() {
  showCreate.value = false
}
</script>

<style lang="scss" scoped>
$primary: #4f46e5;
$bg-card: #1a1a2e;
$border: rgba(255,255,255,0.08);
$text-muted: #94a3b8;
$danger: #ef4444;

.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  background: $bg-card;
  border: 1px solid $border;
  border-radius: 16px;
  padding: 32px;
  width: 100%;
  max-width: 360px;

  h2 { font-size: 22px; font-weight: 700; margin-bottom: 6px; }
  p  { color: $text-muted; margin-bottom: 20px; }

  form { display: flex; flex-direction: column; gap: 12px; }

  .error { color: $danger; font-size: 13px; }

  .btn { width: 100%; }
}

.rooms-page {
  min-height: 100vh;
  padding: 24px;
  max-width: 1100px;
  margin: 0 auto;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 28px;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 14px;

    h1 { font-size: 22px; font-weight: 700; }
  }

  &__icon  { font-size: 32px; }
  &__subtitle { color: $text-muted; font-size: 13px; }

  &__state {
    text-align: center;
    padding: 48px 0;
    color: $text-muted;
    &--error { color: $danger; }
  }

  &__empty {
    text-align: center;
    padding: 64px 0;
    &-icon { font-size: 48px; margin-bottom: 12px; }
    p      { color: $text-muted; margin-bottom: 4px; }
    &-hint { font-size: 13px; }
  }
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.room-card {
  background: $bg-card;
  border: 1px solid $border;
  border-radius: 12px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba($primary, 0.5);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0,0,0,0.3);
  }

  &__header {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 14px;
  }

  &__icon   { font-size: 22px; }
  &__info   { flex: 1; }
  &__info h3 { font-size: 15px; font-weight: 600; margin-bottom: 2px; }
  &__creator { font-size: 12px; color: $text-muted; }

  &__end { opacity: 0; margin-left: auto; }
  &:hover &__end { opacity: 1; }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__participants { color: $text-muted; font-size: 13px; }
  &__join { color: rgba($primary, 0.9); font-size: 13px; font-weight: 500; }
}

.modal__actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
