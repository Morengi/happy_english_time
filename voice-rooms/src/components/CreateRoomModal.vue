<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <h3>➕ Создать комнату</h3>

      <form @submit.prevent="submit">
        <div class="field">
          <label>Название комнаты</label>
          <input
            v-model="form.name"
            class="input"
            placeholder="Например: Урок грамматики"
            maxlength="100"
            required
          />
        </div>

        <div class="field">
          <label>Максимум участников</label>
          <div class="limit-row">
            <input
              v-model="limitEnabled"
              type="checkbox"
              id="limit-cb"
            />
            <label for="limit-cb" class="limit-label">Ограничить количество</label>
          </div>
          <input
            v-if="limitEnabled"
            v-model.number="form.maxParticipants"
            class="input"
            type="number"
            min="2"
            max="999"
            placeholder="Минимум 2"
          />
          <p v-else class="hint">Без ограничений</p>
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="modal-actions">
          <button type="button" class="btn btn--ghost" @click="$emit('close')">Отмена</button>
          <button type="submit" class="btn btn--primary" :disabled="loading">
            {{ loading ? 'Создаём...' : 'Создать' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoomsStore } from '@/stores/rooms'

const emit = defineEmits(['close', 'created'])
const rooms = useRoomsStore()

const form = ref({ name: '', maxParticipants: 10 })
const limitEnabled = ref(false)
const loading = ref(false)
const error   = ref('')

async function submit() {
  error.value   = ''
  loading.value = true
  try {
    const max = limitEnabled.value ? form.value.maxParticipants : null
    if (limitEnabled.value && max < 2) {
      error.value = 'Минимум 2 участника'
      return
    }
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
$text-muted: #94a3b8;
$danger: #ef4444;

.field {
  margin-bottom: 16px;

  & > label:first-child {
    display: block;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: $text-muted;
    margin-bottom: 8px;
  }
}

.limit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.limit-label {
  font-size: 14px;
  cursor: pointer;
  text-transform: none;
  letter-spacing: 0;
  font-weight: 400;
}

.hint  { font-size: 13px; color: $text-muted; }
.error { font-size: 13px; color: $danger; margin-top: 4px; }

.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
