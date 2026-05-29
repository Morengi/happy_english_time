<template>
  <div class="vr-page">
    <div class="vr-page__header">
      <div class="vr-page__title">
        <span class="vr-page__icon">🎤</span>
        <div>
          <h1>Голосовые комнаты</h1>
          <p class="vr-page__sub">Войдите в комнату, чтобы общаться голосом и видео</p>
        </div>
      </div>
      <button v-if="auth.isTeacher" class="vr-btn vr-btn--primary" @click="showCreate = true">
        + Создать комнату
      </button>
    </div>

    <div v-if="store.loading" class="vr-state">Загрузка...</div>
    <div v-else-if="store.error" class="vr-state vr-state--error">{{ store.error }}</div>

    <div v-else-if="store.rooms.length === 0" class="vr-empty">
      <div class="vr-empty__icon">🔇</div>
      <p>Нет активных комнат</p>
      <p v-if="auth.isTeacher" class="vr-empty__hint">Создайте первую комнату, чтобы начать</p>
      <p v-else class="vr-empty__hint">Преподаватель ещё не создал ни одной комнаты</p>
    </div>

    <div v-else class="vr-grid">
      <div v-for="room in store.rooms" :key="room.id" class="vr-card" @click="enter(room)">
        <div class="vr-card__top">
          <span class="vr-card__wave">🔊</span>
          <div class="vr-card__info">
            <h3>{{ room.name }}</h3>
            <span class="vr-card__creator">{{ room.creatorName }}</span>
          </div>
          <button v-if="canEnd(room)" class="vr-card__end" title="Закрыть комнату"
            @click.stop="confirmEnd(room)">✕</button>
        </div>
        <div class="vr-card__foot">
          <span class="vr-card__count">
            👥 {{ room.participantCount }}
            <template v-if="room.maxParticipants"> / {{ room.maxParticipants }}</template>
          </span>
          <span class="vr-card__enter">Войти →</span>
        </div>
      </div>
    </div>

    <CreateRoomModal v-if="showCreate" @close="showCreate = false" @created="showCreate = false" />

    <div v-if="roomToEnd" class="vr-overlay" @click.self="roomToEnd = null">
      <div class="vr-confirm">
        <h3>Закрыть комнату?</h3>
        <p>«{{ roomToEnd.name }}» будет закрыта для всех участников.</p>
        <div class="vr-confirm__actions">
          <button class="vr-btn vr-btn--ghost" @click="roomToEnd = null">Отмена</button>
          <button class="vr-btn vr-btn--danger" @click="doEnd">Закрыть</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useVoiceRoomsStore } from '@/stores/voiceRooms'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import CreateRoomModal from '@/components/voice-rooms/CreateRoomModal.vue'

const router = useRouter()
const auth   = useAuthStore()
const store  = useVoiceRoomsStore()

const showCreate = ref(false)
const roomToEnd  = ref(null)
let   listStomp  = null

onMounted(async () => {
  await store.fetchRooms()
  subscribeRoomList()
})

onBeforeUnmount(() => listStomp?.deactivate())

function subscribeRoomList() {
  listStomp = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: { Authorization: `Bearer ${auth.token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      listStomp.subscribe('/topic/voice-rooms/list', msg => {
        store.handleNewRoom(JSON.parse(msg.body))
      })
    }
  })
  listStomp.activate()
}

function enter(room) {
  router.push(`/voice-rooms/room/${room.id}`)
}

function canEnd(room) {
  return room.creatorId === auth.user?.id || auth.user?.role === 'ADMIN'
}

function confirmEnd(room) { roomToEnd.value = room }

async function doEnd() {
  await store.endRoom(roomToEnd.value.id)
  roomToEnd.value = null
}
</script>

<style lang="scss" scoped>
// Dark-themed section within the light main app
.vr-page {
  min-height: calc(100vh - 64px);
  background: #0f0f1a;
  padding: 28px 24px;
  color: #e2e8f0;

  &__header {
    display: flex; align-items: center; justify-content: space-between;
    margin-bottom: 28px; flex-wrap: wrap; gap: 12px;
  }

  &__title { display: flex; align-items: center; gap: 14px; }
  &__icon  { font-size: 34px; }

  h1 { font-size: 22px; font-weight: 700; margin: 0; color: #e2e8f0; }
  &__sub { font-size: 13px; color: #94a3b8; margin: 2px 0 0; }
}

.vr-state {
  text-align: center; padding: 48px 0; color: #94a3b8;
  &--error { color: #ef4444; }
}

.vr-empty {
  text-align: center; padding: 64px 0;
  &__icon { font-size: 48px; margin-bottom: 12px; }
  p { color: #94a3b8; margin-bottom: 4px; font-size: 15px; }
  &__hint { font-size: 13px; }
}

.vr-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.vr-card {
  background: #1a1a2e; border: 1px solid rgba(255,255,255,0.08);
  border-radius: 12px; padding: 18px; cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: rgba(79,70,229,0.5);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0,0,0,0.4);
  }

  &__top { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 14px; }
  &__wave { font-size: 22px; }
  &__info { flex: 1;
    h3 { font-size: 15px; font-weight: 600; margin: 0 0 3px; color: #e2e8f0; }
  }
  &__creator { font-size: 12px; color: #94a3b8; }

  &__end {
    opacity: 0; background: none; border: none; color: #ef4444; cursor: pointer;
    font-size: 16px; padding: 2px 6px; border-radius: 4px; transition: 0.15s;
    &:hover { background: rgba(239,68,68,0.15); }
  }
  &:hover &__end { opacity: 1; }

  &__foot { display: flex; align-items: center; justify-content: space-between; }
  &__count { color: #94a3b8; font-size: 13px; }
  &__enter { color: rgba(165,180,252,0.9); font-size: 13px; font-weight: 500; }
}

// Modal overlay
.vr-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 300;
  backdrop-filter: blur(4px);
}
.vr-confirm {
  background: #1a1a2e; border: 1px solid rgba(255,255,255,0.1);
  border-radius: 14px; padding: 26px; max-width: 380px; width: 100%; color: #e2e8f0;
  h3 { font-size: 17px; margin-bottom: 8px; }
  p  { font-size: 14px; color: #94a3b8; }
  &__actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
}

// Buttons
.vr-btn {
  padding: 9px 18px; border-radius: 8px; border: none;
  cursor: pointer; font-size: 14px; font-weight: 500; transition: all 0.2s;
  &--primary { background: #4f46e5; color: #fff; &:hover { background: #3730a3; } }
  &--danger  { background: #ef4444; color: #fff; &:hover { background: #dc2626; } }
  &--ghost   { background: rgba(255,255,255,0.06); color: #94a3b8;
               border: 1px solid rgba(255,255,255,0.1); &:hover { color: #e2e8f0; } }
}
</style>
