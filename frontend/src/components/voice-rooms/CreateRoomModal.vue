<template>
  <div class="vr-overlay" @click.self="$emit('close')">
    <div class="vr-modal">
      <h3>➕ Создать комнату</h3>

      <form @submit.prevent="submit">
        <div class="vr-field">
          <label>Название комнаты</label>
          <input v-model="form.name" class="vr-input" placeholder="Например: Урок грамматики" maxlength="100" required />
        </div>

        <div class="vr-field">
          <label>Максимум участников</label>
          <div class="vr-check-row">
            <input v-model="limitEnabled" type="checkbox" id="vr-limit-cb" />
            <label for="vr-limit-cb" class="vr-check-label">Ограничить количество</label>
          </div>
          <input v-if="limitEnabled" v-model.number="form.maxParticipants"
            class="vr-input" type="number" min="2" max="999" placeholder="Минимум 2" />
          <p v-else class="vr-hint">Без ограничений</p>
        </div>

        <p v-if="error" class="vr-error">{{ error }}</p>

        <div class="vr-actions">
          <button type="button" class="vr-btn vr-btn--ghost" @click="$emit('close')">Отмена</button>
          <button type="submit" class="vr-btn vr-btn--primary" :disabled="loading">
            {{ loading ? 'Создаём...' : 'Создать' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useVoiceRoomsStore } from '@/stores/voiceRooms'

const emit  = defineEmits(['close', 'created'])
const rooms = useVoiceRoomsStore()

const form         = ref({ name: '', maxParticipants: 10 })
const limitEnabled = ref(false)
const loading      = ref(false)
const error        = ref('')

async function submit() {
  error.value   = ''
  loading.value = true
  try {
    const max = limitEnabled.value ? form.value.maxParticipants : null
    if (limitEnabled.value && max < 2) { error.value = 'Минимум 2 участника'; return }
    await rooms.createRoom(form.value.name, max)
    emit('created')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Ошибка при создании комнаты'
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.vr-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center;
  z-index: 300; backdrop-filter: blur(4px);
}
.vr-modal {
  background: #1a1a2e; border: 1px solid rgba(255,255,255,0.08);
  border-radius: 14px; padding: 26px; width: 100%; max-width: 420px; color: #e2e8f0;
  h3 { font-size: 17px; font-weight: 600; margin-bottom: 20px; }
}
.vr-field { margin-bottom: 16px;
  & > label:first-child { display: block; font-size: 11px; font-weight: 600;
    text-transform: uppercase; letter-spacing: 0.5px; color: #94a3b8; margin-bottom: 8px; }
}
.vr-check-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.vr-check-label { font-size: 14px; cursor: pointer; color: #e2e8f0; }
.vr-input {
  width: 100%; padding: 10px 14px; background: #16213e;
  border: 1px solid rgba(255,255,255,0.1); border-radius: 8px;
  color: #e2e8f0; font-size: 14px; outline: none;
  &::placeholder { color: #94a3b8; }
  &:focus { border-color: #4f46e5; }
}
.vr-hint  { font-size: 13px; color: #94a3b8; }
.vr-error { font-size: 13px; color: #ef4444; margin-top: 6px; }
.vr-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px; }
.vr-btn {
  padding: 9px 18px; border-radius: 8px; border: none;
  cursor: pointer; font-size: 14px; font-weight: 500; transition: all 0.2s;
  &--primary { background: #4f46e5; color: #fff; &:hover { background: #3730a3; } &:disabled { opacity: 0.5; cursor: not-allowed; } }
  &--ghost { background: rgba(255,255,255,0.06); color: #94a3b8; border: 1px solid rgba(255,255,255,0.1); &:hover { background: rgba(255,255,255,0.1); color: #e2e8f0; } }
}
</style>
