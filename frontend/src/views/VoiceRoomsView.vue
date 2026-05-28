<template>
  <div class="voice-rooms-wrapper">
    <iframe
      v-if="token"
      :src="iframeSrc"
      class="voice-rooms-frame"
      allow="camera; microphone; display-capture; autoplay"
      allowfullscreen
    />
    <div v-else class="voice-rooms-fallback">
      <span>🎤</span>
      <p>Не удалось загрузить голосовые комнаты — токен авторизации недоступен.</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const token = computed(() => auth.token)

const iframeSrc = computed(() =>
  token.value ? `/voice-rooms/?token=${encodeURIComponent(token.value)}` : null
)
</script>

<style scoped>
.voice-rooms-wrapper {
  width: 100%;
  height: calc(100vh - 64px); /* subtract topbar height */
  display: flex;
}

.voice-rooms-frame {
  width: 100%;
  height: 100%;
  border: none;
  flex: 1;
}

.voice-rooms-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  color: #94a3b8;
  font-size: 14px;

  span { font-size: 48px; }
}
</style>
