<template>
  <div class="media-bar">
    <div class="media-bar__controls">
      <!-- Microphone -->
      <button
        class="media-btn"
        :class="{ 'media-btn--off': !micEnabled }"
        title="Микрофон"
        @click="$emit('toggle-mic')"
      >
        <span class="media-btn__icon">{{ micEnabled ? '🎤' : '🔇' }}</span>
        <span class="media-btn__label">{{ micEnabled ? 'Микрофон' : 'Без звука' }}</span>
      </button>

      <!-- Camera -->
      <button
        class="media-btn"
        :class="{ 'media-btn--off': !camEnabled }"
        title="Камера"
        @click="$emit('toggle-cam')"
      >
        <span class="media-btn__icon">📷</span>
        <span class="media-btn__label">{{ camEnabled ? 'Камера' : 'Камера выкл.' }}</span>
      </button>

      <!-- Headphones (receive audio) -->
      <button
        class="media-btn"
        :class="{ 'media-btn--off': !headphonesEnabled }"
        title="Звук собеседников"
        @click="$emit('toggle-headphones')"
      >
        <span class="media-btn__icon">{{ headphonesEnabled ? '🎧' : '🔕' }}</span>
        <span class="media-btn__label">{{ headphonesEnabled ? 'Звук вкл.' : 'Звук выкл.' }}</span>
      </button>

      <!-- Screen share -->
      <button
        class="media-btn"
        :class="{ 'media-btn--active': screenSharing }"
        title="Трансляция экрана"
        @click="$emit('toggle-screen')"
      >
        <span class="media-btn__icon">🖥️</span>
        <span class="media-btn__label">{{ screenSharing ? 'Остановить' : 'Экран' }}</span>
      </button>
    </div>

    <!-- Leave button -->
    <button class="media-bar__leave" @click="$emit('leave')">
      <span>📴</span> Покинуть
    </button>
  </div>
</template>

<script setup>
defineProps({
  micEnabled:        { type: Boolean, default: true },
  camEnabled:        { type: Boolean, default: false },
  headphonesEnabled: { type: Boolean, default: true },
  screenSharing:     { type: Boolean, default: false }
})

defineEmits(['toggle-mic', 'toggle-cam', 'toggle-headphones', 'toggle-screen', 'leave'])
</script>

<style lang="scss" scoped>
$bg-card: #1a1a2e;
$border: rgba(255,255,255,0.08);
$danger: #ef4444;
$primary: #4f46e5;
$success: #22c55e;
$text-muted: #94a3b8;

.media-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 14px 24px;
  background: $bg-card;
  border-top: 1px solid $border;
  flex-shrink: 0;
  flex-wrap: wrap;

  &__controls {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__leave {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    background: $danger;
    color: #fff;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.2s ease;

    &:hover { background: darken($danger, 10%); }
  }
}

.media-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 16px;
  background: rgba(255,255,255,0.06);
  border: 1px solid $border;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #fff;
  min-width: 72px;

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

  &__icon  { font-size: 20px; }
  &__label { font-size: 11px; color: $text-muted; white-space: nowrap; }
}
</style>
