<template>
  <div class="media-bar">
    <div class="media-bar__controls">
      <!-- Microphone -->
      <button class="media-btn" :class="{ 'media-btn--off': !micEnabled }" @click="$emit('toggle-mic')">
        <span class="media-btn__icon">{{ micEnabled ? '🎤' : '🔇' }}</span>
        <span class="media-btn__label">{{ micEnabled ? 'Микрофон' : 'Без звука' }}</span>
      </button>

      <!-- Camera -->
      <button class="media-btn" :class="{ 'media-btn--off': !camEnabled }" @click="$emit('toggle-cam')">
        <span class="media-btn__icon">📷</span>
        <span class="media-btn__label">{{ camEnabled ? 'Камера' : 'Камера выкл.' }}</span>
      </button>

      <!-- Headphones -->
      <button class="media-btn" :class="{ 'media-btn--off': !headphonesEnabled }" @click="$emit('toggle-headphones')">
        <span class="media-btn__icon">{{ headphonesEnabled ? '🎧' : '🔕' }}</span>
        <span class="media-btn__label">{{ headphonesEnabled ? 'Звук вкл.' : 'Звук выкл.' }}</span>
      </button>

      <!-- Screen share -->
      <button class="media-btn" :class="{ 'media-btn--active': screenSharing }" @click="$emit('toggle-screen')">
        <span class="media-btn__icon">🖥️</span>
        <span class="media-btn__label">{{ screenSharing ? 'Остановить' : 'Экран' }}</span>
      </button>

      <!-- Device settings -->
      <button class="media-btn media-btn--settings" @click="showDevices = !showDevices">
        <span class="media-btn__icon">⚙️</span>
        <span class="media-btn__label">Устройства</span>
      </button>
    </div>

    <!-- Leave button -->
    <button class="media-bar__leave" @click="$emit('leave')">
      <span>📴</span><span class="leave-label"> Покинуть</span>
    </button>

    <!-- Device picker popup -->
    <div v-if="showDevices" class="device-popup">
      <div class="device-popup__header">
        <span>Настройки устройств</span>
        <button class="device-popup__close" @click="showDevices = false">✕</button>
      </div>

      <!-- Microphone device + gain -->
      <label class="device-label">🎤 Микрофон</label>
      <select class="device-select" :value="selectedMicId" @change="onMicChange">
        <option value="">Устройство по умолчанию</option>
        <option v-for="d in audioInputDevices" :key="d.deviceId" :value="d.deviceId">{{ d.label }}</option>
      </select>
      <div class="device-slider-row">
        <span class="device-slider-label">Усиление</span>
        <span class="device-slider-val">{{ Math.round(micGainValue * 100) }}%</span>
      </div>
      <input type="range" class="device-slider" min="0" max="2" step="0.05"
             :value="micGainValue" @input="e => $emit('set-mic-gain', parseFloat(e.target.value))" />

      <!-- Speaker device + master volume -->
      <label class="device-label" style="margin-top:6px">🔊 Динамик / наушники</label>
      <select class="device-select" :value="selectedSpeakerId" @change="onSpeakerChange">
        <option value="">Устройство по умолчанию</option>
        <option v-for="d in audioOutputDevices" :key="d.deviceId" :value="d.deviceId">{{ d.label }}</option>
      </select>
      <p v-if="!audioOutputDevices.length" class="device-note">Выбор динамика не поддерживается в Safari</p>
      <div class="device-slider-row">
        <span class="device-slider-label">Громкость</span>
        <span class="device-slider-val">{{ Math.round(headphonesVolume * 100) }}%</span>
      </div>
      <input type="range" class="device-slider" min="0" max="1" step="0.02"
             :value="headphonesVolume" @input="e => $emit('set-headphones-volume', parseFloat(e.target.value))" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  micEnabled:          { type: Boolean, default: false },
  camEnabled:          { type: Boolean, default: false },
  headphonesEnabled:   { type: Boolean, default: true },
  screenSharing:       { type: Boolean, default: false },
  audioInputDevices:   { type: Array,   default: () => [] },
  audioOutputDevices:  { type: Array,   default: () => [] },
  selectedMicId:       { type: String,  default: '' },
  selectedSpeakerId:   { type: String,  default: '' },
  micGainValue:        { type: Number,  default: 1 },
  headphonesVolume:    { type: Number,  default: 1 }
})

const emit = defineEmits([
  'toggle-mic', 'toggle-cam', 'toggle-headphones', 'toggle-screen', 'leave',
  'set-mic-device', 'set-speaker-device',
  'set-mic-gain', 'set-headphones-volume'
])

const showDevices = ref(false)

function onMicChange(e)     { emit('set-mic-device',     e.target.value) }
function onSpeakerChange(e) { emit('set-speaker-device', e.target.value) }
</script>

<style lang="scss" scoped>
$bg-card: #1a1a2e;
$border: rgba(255,255,255,0.08);
$danger: #ef4444;
$primary: #4f46e5;
$text-muted: #94a3b8;

.media-bar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 10px 16px;
  background: $bg-card;
  border-top: 1px solid $border;
  flex-shrink: 0;
  flex-wrap: wrap;

  &__controls {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    justify-content: center;
  }

  &__leave {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 16px;
    background: $danger;
    color: #fff;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    transition: background 0.2s;
    &:hover { background: darken($danger, 10%); }
  }
}

.media-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 8px 12px;
  background: rgba(255,255,255,0.06);
  border: 1px solid $border;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  color: #fff;
  min-width: 60px;

  &:hover { background: rgba(255,255,255,0.1); }

  &--off {
    background: rgba($danger, 0.15);
    border-color: rgba($danger, 0.3);
    .media-btn__icon  { filter: grayscale(1); opacity: 0.6; }
    .media-btn__label { color: $danger; }
  }

  &--active {
    background: rgba($primary, 0.2);
    border-color: rgba($primary, 0.5);
    .media-btn__label { color: lighten($primary, 20%); }
  }

  &--settings {
    background: rgba(255,255,255,0.04);
  }

  &__icon  { font-size: 18px; line-height: 1; }
  &__label { font-size: 10px; color: $text-muted; white-space: nowrap; }
}

/* ── Device picker popup ──────────────────────────────────────────────── */
.device-popup {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  background: #1e1e3a;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 12px;
  padding: 16px;
  width: min(320px, 90vw);
  z-index: 500;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.5);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 13px;
    font-weight: 600;
    color: #e2e8f0;
    margin-bottom: 4px;
  }

  &__close {
    background: none;
    border: none;
    color: $text-muted;
    cursor: pointer;
    font-size: 14px;
    padding: 2px 6px;
    border-radius: 4px;
    &:hover { background: rgba(255,255,255,0.08); }
  }
}

.device-label {
  font-size: 11px;
  color: $text-muted;
  font-weight: 500;
}

.device-select {
  width: 100%;
  padding: 7px 10px;
  background: rgba(255,255,255,0.07);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  color: #e2e8f0;
  font-size: 12px;
  outline: none;
  cursor: pointer;
  option { background: #1e1e3a; }
  &:focus { border-color: rgba($primary, 0.5); }
}

.device-note {
  font-size: 10px;
  color: $text-muted;
  margin: 0;
}

.device-slider-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 4px;
}
.device-slider-label { font-size: 10px; color: $text-muted; }
.device-slider-val   { font-size: 10px; color: #a5b4fc; font-weight: 600; }

.device-slider {
  width: 100%; appearance: none; height: 4px;
  background: rgba(255,255,255,0.15); border-radius: 2px; outline: none;
  margin-bottom: 2px;
  &::-webkit-slider-thumb {
    appearance: none; width: 14px; height: 14px;
    border-radius: 50%; background: $primary; cursor: pointer;
  }
  &::-moz-range-thumb {
    width: 14px; height: 14px; border-radius: 50%;
    background: $primary; cursor: pointer; border: none;
  }
}

/* ── Mobile / small screen ────────────────────────────────────────────── */
@media (max-width: 600px) {
  .media-bar {
    padding: 8px 10px;
    gap: 8px;
  }

  .media-btn {
    padding: 6px 8px;
    min-width: 48px;
    border-radius: 8px;
    &__icon  { font-size: 16px; }
    &__label { display: none; }   /* hide labels on mobile — icons only */
  }

  .media-bar__leave {
    padding: 7px 12px;
    font-size: 12px;
  }

  .leave-label { display: none; }  /* show only icon on very small screens */
}
</style>
