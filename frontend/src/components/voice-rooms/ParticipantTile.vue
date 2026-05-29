<template>
  <div
    class="vr-tile"
    :class="{
      'vr-tile--local':    isLocal,
      'vr-tile--speaking': isLocal ? speaking : remoteSpeaking,
      'vr-tile--pinned':   pinned
    }"
    @click.self="$emit('click')"
    @contextmenu.prevent="!isLocal && openMenu($event)"
  >
    <div class="vr-tile__video-wrap" @click.self="$emit('click')">
      <!-- Screen share (local) -->
      <video v-if="isLocal && screenSharing && screenStream"
        ref="screenEl" class="vr-tile__video" autoplay muted playsinline
        @click="$emit('click')" />
      <!-- Camera -->
      <video v-else-if="stream && camEnabled"
        ref="videoEl" class="vr-tile__video" autoplay :muted="isLocal" playsinline
        @click="$emit('click')" />
      <!-- Avatar fallback -->
      <div v-else class="vr-tile__avatar" @click="$emit('click')">
        <img v-if="participant.avatarUrl" :src="participant.avatarUrl" alt="" />
        <span v-else>{{ initials }}</span>
      </div>
      <!-- Remote audio -->
      <audio v-if="!isLocal" ref="audioEl" :data-remote="participant.nickname" autoplay playsinline />
    </div>

    <!-- Bottom bar -->
    <div class="vr-tile__bar">
      <span class="vr-tile__name" @click="$emit('click')">
        {{ participant.fullName || participant.nickname }}
        <span v-if="isLocal" class="vr-tile__you">(вы)</span>
      </span>
      <span :class="['vr-badge', roleBadge]">{{ roleLabel }}</span>
      <!-- Status icons -->
      <div class="vr-tile__icons">
        <span v-if="!micEnabled" title="Микрофон выключен">🔇</span>
        <span v-if="isLocal && screenSharing" title="Трансляция">🖥️</span>
        <!-- Volume indicator for remote -->
        <span v-if="!isLocal && volume < 0.05" title="Заглушен локально">🔕</span>
      </div>
    </div>
  </div>

  <!-- Dropdown menu — teleported to body to avoid overflow clipping -->
  <Teleport to="body">
    <div
      v-if="showMenu && !isLocal"
      class="vr-tile__menu"
      :style="{ position: 'fixed', top: menuPos.y + 'px', left: menuPos.x + 'px', zIndex: 9999 }"
      @click.stop
    >
      <!-- Volume slider -->
      <div class="vr-menu-row">
        <span class="vr-menu-label">🔊 Громкость</span>
        <span class="vr-menu-val">{{ Math.round(volume * 100) }}%</span>
      </div>
      <input
        type="range"
        class="vr-slider"
        min="0" max="2" step="0.05"
        :value="volume"
        @input="onVolumeInput"
      />

      <!-- Teacher controls -->
      <template v-if="showTeacherControls">
        <div class="vr-menu-divider" />
        <button class="vr-menu-action" @click="$emit('mute-mic'); showMenu = false">
          🔇 Выключить микрофон
        </button>
        <button class="vr-menu-action" @click="$emit('mute-cam'); showMenu = false">
          📷 Выключить камеру
        </button>
        <button class="vr-menu-action" @click="$emit('mute-audio'); showMenu = false">
          🔕 Заглушить звук
        </button>
      </template>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  participant:         { type: Object,  required: true },
  stream:              { type: Object,  default: null },
  screenStream:        { type: Object,  default: null },
  isLocal:             { type: Boolean, default: false },
  micEnabled:          { type: Boolean, default: false },
  camEnabled:          { type: Boolean, default: false },
  headphonesEnabled:   { type: Boolean, default: true },
  screenSharing:       { type: Boolean, default: false },
  showTeacherControls: { type: Boolean, default: false },
  remoteSpeaking:      { type: Boolean, default: false },
  pinned:              { type: Boolean, default: false },
  volume:              { type: Number,  default: 1 }     // 0–2 per-user volume
})

const emit = defineEmits(['mute-mic', 'mute-cam', 'mute-audio', 'click', 'volume-change'])

const videoEl  = ref(null)
const screenEl = ref(null)
const audioEl  = ref(null)
const speaking = ref(false)
const showMenu = ref(false)
const menuPos  = ref({ x: 0, y: 0 })

function openMenu(event) {
  const MENU_W = 210, MENU_H = 200
  let x = event.clientX
  let y = event.clientY
  if (x + MENU_W > window.innerWidth)  x = event.clientX - MENU_W
  if (y + MENU_H > window.innerHeight) y = event.clientY - MENU_H
  menuPos.value = { x, y }
  showMenu.value = true
}

// Close menu when clicking outside
function onDocClick() { showMenu.value = false }
watch(showMenu, open => {
  if (open) setTimeout(() => document.addEventListener('click', onDocClick, { once: true }), 0)
})

const initials = computed(() => {
  const n = props.participant.fullName || props.participant.nickname || '?'
  return n.split(' ').map(w => w[0]).filter(Boolean).join('').slice(0, 2).toUpperCase()
})

const roleLabelMap = { ADMIN: 'Админ', TEACHER: 'Преп.', STUDENT: 'Студент' }
const roleBadgeMap = { ADMIN: 'vr-badge--admin', TEACHER: 'vr-badge--teacher', STUDENT: 'vr-badge--student' }
const roleLabel = computed(() => roleLabelMap[props.participant.role] || '')
const roleBadge = computed(() => roleBadgeMap[props.participant.role] || '')

function onVolumeInput(e) {
  const val = parseFloat(e.target.value)
  emit('volume-change', val)
  if (audioEl.value) audioEl.value.volume = Math.min(1, val)
}

// ── Stream / srcObject watcher ─────────────────────────────────────────────
watch(
  [() => props.stream, () => props.camEnabled, () => props.screenStream, () => props.screenSharing],
  () => {
    if (videoEl.value  && props.stream)       videoEl.value.srcObject  = props.stream
    if (screenEl.value && props.screenStream) screenEl.value.srcObject = props.screenStream
    if (audioEl.value  && props.stream) {
      if (audioEl.value.srcObject !== props.stream) {
        audioEl.value.srcObject = props.stream
      }
      audioEl.value.play().catch(() => {})
    }
  },
  { immediate: true, flush: 'post' }
)

// Apply volume prop changes to audio element
watch(() => props.volume, val => {
  if (audioEl.value) audioEl.value.volume = Math.min(1, val)
})

// ── Local speaking analyser ────────────────────────────────────────────────
let analyserInterval = null
let audioCtx = null

function startAnalyser(stream) {
  clearInterval(analyserInterval)
  try { audioCtx?.close() } catch {}
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext
    audioCtx = new AudioCtx()
    const tryResume = () => audioCtx.state === 'suspended' && audioCtx.resume()
    tryResume()
    document.addEventListener('click',     tryResume, { once: true })
    document.addEventListener('touchend',  tryResume, { once: true })
    const source   = audioCtx.createMediaStreamSource(stream)
    const analyser = audioCtx.createAnalyser()
    analyser.fftSize = 256
    source.connect(analyser)
    const data = new Uint8Array(analyser.frequencyBinCount)
    analyserInterval = setInterval(() => {
      if (audioCtx.state === 'suspended') { audioCtx.resume(); return }
      analyser.getByteFrequencyData(data)
      speaking.value = (data.reduce((a, b) => a + b, 0) / data.length) > 10
    }, 150)
  } catch (e) { console.warn('[Analyser]', e) }
}

watch(() => props.stream, stream => {
  if (!props.isLocal || !stream) return
  startAnalyser(stream)
}, { immediate: true })
</script>

<style lang="scss" scoped>
.vr-tile {
  background: #1a1a2e; border: 2px solid rgba(255,255,255,0.08);
  border-radius: 12px; overflow: hidden; position: relative;
  aspect-ratio: 16/9; display: flex; flex-direction: column;
  transition: border-color 0.2s ease;

  &--local    { border-color: rgba(79,70,229,0.4); }
  &--speaking { border-color: #22c55e !important; }
  &--pinned   { border-color: rgba(79,70,229,0.7); }

  &__video-wrap {
    flex: 1; position: relative; overflow: hidden; background: #0f0f1a;
    cursor: pointer;
    .vr-tile--pinned & { cursor: pointer; }
  }

  &__video { width: 100%; height: 100%; object-fit: cover; display: block; }

  &__avatar {
    width: 100%; height: 100%; cursor: pointer;
    display: flex; align-items: center; justify-content: center;
    background: linear-gradient(135deg, #2d2d5a, #1a1a3e);
    font-size: 30px; font-weight: 700; color: rgba(255,255,255,0.8);
    img { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; }
    .vr-tile--pinned & { cursor: pointer;
      font-size: 60px;
      img { width: 120px; height: 120px; }
    }
  }

  &__bar {
    display: flex; align-items: center; gap: 5px;
    padding: 5px 8px; background: rgba(0,0,0,0.65);
    backdrop-filter: blur(4px); flex-shrink: 0;
  }

  &__name {
    flex: 1; font-size: 11px; font-weight: 500; color: #e2e8f0;
    white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    cursor: pointer;
    .vr-tile--pinned & { cursor: pointer; }
  }
  &__you   { font-size: 10px; color: #94a3b8; }
  &__icons { display: flex; gap: 2px; font-size: 11px; }

  // ── Dropdown menu (rendered via Teleport; position set inline) ────────────
  &__menu {
    background: #1e1e3a; border: 1px solid rgba(255,255,255,0.12);
    border-radius: 10px; padding: 10px; width: 200px;
    box-shadow: 0 6px 24px rgba(0,0,0,0.5);
    display: flex; flex-direction: column; gap: 6px;
  }
}

// ── Menu button (⋯) ────────────────────────────────────────────────────────
.vr-menu-btn {
  background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 6px; color: #94a3b8; cursor: pointer;
  font-size: 14px; line-height: 1; padding: 2px 7px; flex-shrink: 0;
  &:hover, &--active { background: rgba(255,255,255,0.14); color: #e2e8f0; }
}

// ── Menu content ───────────────────────────────────────────────────────────
.vr-menu-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 11px; color: #94a3b8;
}
.vr-menu-label { font-size: 11px; color: #94a3b8; }
.vr-menu-val   { font-size: 11px; color: #a5b4fc; font-weight: 600; }
.vr-menu-divider { height: 1px; background: rgba(255,255,255,0.1); margin: 2px 0; }
.vr-menu-action {
  background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 6px; color: #e2e8f0; cursor: pointer;
  font-size: 11px; padding: 5px 8px; text-align: left; width: 100%;
  &:hover { background: rgba(255,255,255,0.1); }
}

// ── Slider ─────────────────────────────────────────────────────────────────
.vr-slider {
  width: 100%; appearance: none; height: 4px;
  background: rgba(255,255,255,0.15); border-radius: 2px; outline: none;
  &::-webkit-slider-thumb {
    appearance: none; width: 14px; height: 14px; border-radius: 50%;
    background: #4f46e5; cursor: pointer;
  }
  &::-moz-range-thumb {
    width: 14px; height: 14px; border-radius: 50%;
    background: #4f46e5; cursor: pointer; border: none;
  }
}

// ── Badge ──────────────────────────────────────────────────────────────────
.vr-badge {
  display: inline-block; padding: 1px 6px; border-radius: 999px;
  font-size: 9px; font-weight: 600; text-transform: uppercase; white-space: nowrap;
  &--teacher { background: rgba(79,70,229,0.25); color: #a5b4fc; }
  &--admin   { background: rgba(245,158,11,0.25); color: #fcd34d; }
  &--student { background: rgba(34,197,94,0.2);  color: #86efac; }
}

// ── Icon btn (keep for compatibility) ─────────────────────────────────────
.vr-icon-btn {
  background: rgba(255,255,255,0.08); border: none; border-radius: 50%;
  width: 28px; height: 28px; cursor: pointer; font-size: 12px;
  display: flex; align-items: center; justify-content: center;
  &:hover { background: rgba(255,255,255,0.18); }
}
</style>
