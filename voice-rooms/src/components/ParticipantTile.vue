<template>
  <div class="tile" :class="{ 'tile--local': isLocal, 'tile--speaking': speaking }">
    <!-- Video / screen-share element -->
    <div class="tile__video-wrap">
      <!-- Screen sharing (local) takes priority -->
      <video
        v-if="isLocal && screenSharing && screenStream"
        ref="screenEl"
        class="tile__video"
        autoplay
        muted
        playsinline
      />
      <!-- Camera video -->
      <video
        v-else-if="stream && camEnabled"
        ref="videoEl"
        class="tile__video"
        :autoplay="true"
        :muted="isLocal"
        playsinline
      />
      <!-- Avatar fallback -->
      <div v-else class="tile__avatar">
        <img v-if="participant.avatarUrl" :src="participant.avatarUrl" alt="" />
        <span v-else>{{ initials }}</span>
      </div>

      <!-- Remote audio (hidden) -->
      <audio
        v-if="!isLocal && stream"
        ref="audioEl"
        :data-remote="participant.nickname"
        autoplay
        playsinline
      />
    </div>

    <!-- Name + status bar -->
    <div class="tile__bar">
      <span class="tile__name">
        {{ participant.fullName || participant.nickname }}
        <span v-if="isLocal" class="tile__you">(вы)</span>
      </span>
      <span :class="['badge', roleBadge]">{{ roleLabel }}</span>
      <div class="tile__indicators">
        <span v-if="!micEnabled" title="Микрофон выключен">🔇</span>
        <span v-if="!camEnabled" title="Камера выключена">📷</span>
        <span v-if="isLocal && screenSharing" title="Трансляция экрана">🖥️</span>
      </div>
    </div>

    <!-- Teacher controls (shown on hover for non-self participants) -->
    <div v-if="showTeacherControls" class="tile__teacher-controls">
      <button class="btn btn--ghost btn--icon" title="Выключить микрофон" @click="$emit('mute-mic')">🔇</button>
      <button class="btn btn--ghost btn--icon" title="Выключить камеру"   @click="$emit('mute-cam')">🚫📷</button>
      <button class="btn btn--ghost btn--icon" title="Выключить звук"     @click="$emit('mute-audio')">🔕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  participant:       { type: Object, required: true },
  stream:            { type: Object, default: null },   // MediaStream
  screenStream:      { type: Object, default: null },
  isLocal:           { type: Boolean, default: false },
  micEnabled:        { type: Boolean, default: true },
  camEnabled:        { type: Boolean, default: false },
  headphonesEnabled: { type: Boolean, default: true },
  screenSharing:     { type: Boolean, default: false },
  showTeacherControls: { type: Boolean, default: false }
})

const emit = defineEmits(['mute-mic', 'mute-cam', 'mute-audio'])

const videoEl  = ref(null)
const screenEl = ref(null)
const audioEl  = ref(null)
const speaking = ref(false)

const initials = computed(() => {
  const n = props.participant.fullName || props.participant.nickname || '?'
  return n.split(' ').map(w => w[0]).filter(Boolean).join('').slice(0, 2).toUpperCase()
})

const roleLabelMap = { ADMIN: 'Админ', TEACHER: 'Преподаватель', STUDENT: 'Студент' }
const roleBadgeMap = { ADMIN: 'badge--admin', TEACHER: 'badge--teacher', STUDENT: 'badge--student' }
const roleLabel = computed(() => roleLabelMap[props.participant.role] || '')
const roleBadge = computed(() => roleBadgeMap[props.participant.role] || '')

// Attach stream to video element whenever it changes
watch(() => props.stream, attachVideo, { immediate: true })
watch(() => props.screenStream, attachScreen, { immediate: true })

function attachVideo(stream) {
  if (videoEl.value && stream) videoEl.value.srcObject = stream
  if (audioEl.value && stream) audioEl.value.srcObject = stream
}

function attachScreen(stream) {
  if (screenEl.value && stream) screenEl.value.srcObject = stream
}

onMounted(() => {
  attachVideo(props.stream)
  attachScreen(props.screenStream)
})

// Audio level detection for "speaking" indicator (local only)
let analyserInterval = null
watch(() => props.stream, (stream) => {
  if (!props.isLocal || !stream) return
  try {
    const ctx     = new AudioContext()
    const source  = ctx.createMediaStreamSource(stream)
    const analyser = ctx.createAnalyser()
    analyser.fftSize = 256
    source.connect(analyser)
    const data = new Uint8Array(analyser.frequencyBinCount)
    clearInterval(analyserInterval)
    analyserInterval = setInterval(() => {
      analyser.getByteFrequencyData(data)
      const avg = data.reduce((a, b) => a + b, 0) / data.length
      speaking.value = avg > 10
    }, 200)
  } catch { /* AudioContext not available */ }
}, { immediate: true })
</script>

<style lang="scss" scoped>
$bg-card: #1a1a2e;
$border: rgba(255,255,255,0.08);
$primary: #4f46e5;
$success: #22c55e;
$text-muted: #94a3b8;

.tile {
  background: $bg-card;
  border: 2px solid $border;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  aspect-ratio: 16/9;
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s ease;

  &--local  { border-color: rgba($primary, 0.4); }
  &--speaking { border-color: $success !important; }

  &:hover .tile__teacher-controls { opacity: 1; }

  &__video-wrap {
    flex: 1;
    position: relative;
    overflow: hidden;
  }

  &__video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transform: scaleX(-1); // mirror local video
  }

  &__avatar {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #2d2d5a, #1a1a3e);
    font-size: 32px;
    font-weight: 700;
    color: rgba(255,255,255,0.8);

    img {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      object-fit: cover;
    }
  }

  &__bar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    background: rgba(0,0,0,0.6);
    backdrop-filter: blur(4px);
    flex-shrink: 0;
  }

  &__name {
    flex: 1;
    font-size: 13px;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__you { font-size: 11px; color: $text-muted; }

  &__indicators {
    display: flex;
    gap: 2px;
    font-size: 13px;
  }

  &__teacher-controls {
    position: absolute;
    top: 8px;
    right: 8px;
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s ease;
    background: rgba(0,0,0,0.6);
    border-radius: 8px;
    padding: 4px;
  }
}
</style>
