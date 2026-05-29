<template>
  <div class="vr-tile" :class="{ 'vr-tile--local': isLocal, 'vr-tile--speaking': speaking }">
    <div class="vr-tile__video-wrap">
      <!-- Screen share (local) -->
      <video v-if="isLocal && screenSharing && screenStream"
        ref="screenEl" class="vr-tile__video" autoplay muted playsinline />
      <!-- Camera -->
      <video v-else-if="stream && camEnabled"
        ref="videoEl" class="vr-tile__video" autoplay :muted="isLocal" playsinline />
      <!-- Avatar fallback -->
      <div v-else class="vr-tile__avatar">
        <img v-if="participant.avatarUrl" :src="participant.avatarUrl" alt="" />
        <span v-else>{{ initials }}</span>
      </div>
      <!-- Remote audio -->
      <audio v-if="!isLocal && stream" ref="audioEl" :data-remote="participant.nickname" autoplay playsinline />
    </div>

    <div class="vr-tile__bar">
      <span class="vr-tile__name">
        {{ participant.fullName || participant.nickname }}
        <span v-if="isLocal" class="vr-tile__you">(вы)</span>
      </span>
      <span :class="['vr-badge', roleBadge]">{{ roleLabel }}</span>
      <div class="vr-tile__icons">
        <span v-if="!micEnabled" title="Микрофон выключен">🔇</span>
        <span v-if="!camEnabled" title="Камера выключена">📷</span>
        <span v-if="isLocal && screenSharing" title="Трансляция">🖥️</span>
      </div>
    </div>

    <!-- Teacher controls (hover) -->
    <div v-if="showTeacherControls" class="vr-tile__controls">
      <button class="vr-icon-btn" title="Выкл. микрофон" @click="$emit('mute-mic')">🔇</button>
      <button class="vr-icon-btn" title="Выкл. камеру"   @click="$emit('mute-cam')">🚫📷</button>
      <button class="vr-icon-btn" title="Выкл. звук"     @click="$emit('mute-audio')">🔕</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'

const props = defineProps({
  participant:         { type: Object,  required: true },
  stream:              { type: Object,  default: null },
  screenStream:        { type: Object,  default: null },
  isLocal:             { type: Boolean, default: false },
  micEnabled:          { type: Boolean, default: true },
  camEnabled:          { type: Boolean, default: false },
  headphonesEnabled:   { type: Boolean, default: true },
  screenSharing:       { type: Boolean, default: false },
  showTeacherControls: { type: Boolean, default: false }
})
defineEmits(['mute-mic', 'mute-cam', 'mute-audio'])

const videoEl  = ref(null)
const screenEl = ref(null)
const audioEl  = ref(null)
const speaking = ref(false)

const initials = computed(() => {
  const n = props.participant.fullName || props.participant.nickname || '?'
  return n.split(' ').map(w => w[0]).filter(Boolean).join('').slice(0, 2).toUpperCase()
})

const roleLabelMap = { ADMIN: 'Админ', TEACHER: 'Преп.', STUDENT: 'Студент' }
const roleBadgeMap = { ADMIN: 'vr-badge--admin', TEACHER: 'vr-badge--teacher', STUDENT: 'vr-badge--student' }
const roleLabel = computed(() => roleLabelMap[props.participant.role] || '')
const roleBadge = computed(() => roleBadgeMap[props.participant.role] || '')

watch(() => props.stream, stream => {
  if (videoEl.value && stream) videoEl.value.srcObject = stream
  if (audioEl.value && stream) audioEl.value.srcObject = stream
}, { immediate: true })

watch(() => props.screenStream, stream => {
  if (screenEl.value && stream) screenEl.value.srcObject = stream
}, { immediate: true })

onMounted(() => {
  if (videoEl.value && props.stream) videoEl.value.srcObject = props.stream
  if (audioEl.value && props.stream) audioEl.value.srcObject = props.stream
  if (screenEl.value && props.screenStream) screenEl.value.srcObject = props.screenStream
})

let analyserInterval = null
watch(() => props.stream, stream => {
  if (!props.isLocal || !stream) return
  try {
    clearInterval(analyserInterval)
    const ctx      = new AudioContext()
    const source   = ctx.createMediaStreamSource(stream)
    const analyser = ctx.createAnalyser()
    analyser.fftSize = 256
    source.connect(analyser)
    const data = new Uint8Array(analyser.frequencyBinCount)
    analyserInterval = setInterval(() => {
      analyser.getByteFrequencyData(data)
      speaking.value = (data.reduce((a, b) => a + b, 0) / data.length) > 10
    }, 200)
  } catch {}
}, { immediate: true })
</script>

<style lang="scss" scoped>
.vr-tile {
  background: #1a1a2e; border: 2px solid rgba(255,255,255,0.08);
  border-radius: 12px; overflow: hidden; position: relative;
  aspect-ratio: 16/9; display: flex; flex-direction: column;
  transition: border-color 0.2s ease;

  &--local   { border-color: rgba(79,70,229,0.4); }
  &--speaking { border-color: #22c55e !important; }
  &:hover .vr-tile__controls { opacity: 1; }

  &__video-wrap { flex: 1; position: relative; overflow: hidden; background: #0f0f1a; }

  &__video {
    width: 100%; height: 100%; object-fit: cover; display: block;
  }

  &__avatar {
    width: 100%; height: 100%;
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(135deg, #2d2d5a, #1a1a3e);
    font-size: 30px; font-weight: 700; color: rgba(255,255,255,0.8);
    img { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; }
  }

  &__bar {
    display: flex; align-items: center; gap: 6px;
    padding: 6px 10px; background: rgba(0,0,0,0.6);
    backdrop-filter: blur(4px); flex-shrink: 0;
  }

  &__name {
    flex: 1; font-size: 12px; font-weight: 500; color: #e2e8f0;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  }
  &__you   { font-size: 10px; color: #94a3b8; }
  &__icons { display: flex; gap: 2px; font-size: 12px; }

  &__controls {
    position: absolute; top: 6px; right: 6px;
    display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s;
    background: rgba(0,0,0,0.65); border-radius: 8px; padding: 4px;
  }
}

.vr-icon-btn {
  background: rgba(255,255,255,0.08); border: none; border-radius: 50%;
  width: 30px; height: 30px; cursor: pointer; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  &:hover { background: rgba(255,255,255,0.18); }
}

.vr-badge {
  display: inline-block; padding: 1px 7px; border-radius: 999px;
  font-size: 10px; font-weight: 600; text-transform: uppercase; white-space: nowrap;
  &--teacher { background: rgba(79,70,229,0.25); color: #a5b4fc; }
  &--admin   { background: rgba(245,158,11,0.25); color: #fcd34d; }
  &--student { background: rgba(34,197,94,0.2);  color: #86efac; }
}
</style>
