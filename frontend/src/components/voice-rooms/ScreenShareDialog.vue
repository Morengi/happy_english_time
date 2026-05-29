<template>
  <div class="modal-overlay" @click.self="$emit('cancel')">
    <div class="modal screen-dialog">
      <h3>🖥️ Настройки трансляции экрана</h3>

      <div class="screen-dialog__group">
        <label>Частота кадров</label>
        <div class="screen-dialog__options">
          <button
            v-for="f in fpsOptions"
            :key="f"
            class="opt-btn"
            :class="{ 'opt-btn--active': selectedFps === f }"
            @click="selectedFps = f"
          >
            {{ f }} fps
          </button>
        </div>
      </div>

      <div class="screen-dialog__group">
        <label>Разрешение</label>
        <div class="screen-dialog__options">
          <button
            v-for="r in resolutions"
            :key="r.label"
            class="opt-btn"
            :class="{ 'opt-btn--active': selectedResolution.label === r.label }"
            @click="selectedResolution = r"
          >
            {{ r.label }}
          </button>
        </div>
      </div>

      <p class="screen-dialog__hint">
        Рекомендуется: 30 fps / 720p для баланса качества и нагрузки на сеть.
      </p>

      <div class="screen-dialog__actions">
        <button class="btn btn--ghost" @click="$emit('cancel')">Отмена</button>
        <button class="btn btn--primary" @click="confirm">Начать трансляцию</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['confirm', 'cancel'])

const fpsOptions = [15, 30, 60]
const resolutions = [
  { label: '360p',  height: 360 },
  { label: '480p',  height: 480 },
  { label: '720p',  height: 720 },
  { label: '1080p', height: 1080 },
  { label: '1440p', height: 1440 }
]

const selectedFps        = ref(30)
const selectedResolution = ref(resolutions[2]) // 720p default

function confirm() {
  emit('confirm', { fps: selectedFps.value, height: selectedResolution.value.height })
}
</script>

<style lang="scss" scoped>
$primary: #4f46e5;
$border: rgba(255,255,255,0.08);
$text-muted: #94a3b8;

.screen-dialog {
  &__group {
    margin-bottom: 20px;

    label {
      display: block;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: $text-muted;
      margin-bottom: 10px;
    }
  }

  &__options {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__hint {
    font-size: 12px;
    color: $text-muted;
    margin-bottom: 20px;
    line-height: 1.5;
  }

  &__actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
  }
}

.opt-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid $border;
  background: rgba(255,255,255,0.04);
  color: #e2e8f0;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s ease;

  &:hover { background: rgba(255,255,255,0.09); }

  &--active {
    background: rgba($primary, 0.25);
    border-color: rgba($primary, 0.6);
    color: lighten($primary, 25%);
    font-weight: 600;
  }
}
</style>
