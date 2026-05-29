<template>
  <div ref="roomEl" class="vr-room" :class="{ 'vr-room--fullscreen': isFullscreen }">

    <!-- Header -->
    <div class="vr-room__header">
      <button class="vr-back" @click="leave">← Назад</button>
      <div class="vr-room__title">
        <span>🔊</span>
        <div>
          <h2>{{ store.currentRoom?.name || 'Комната' }}</h2>
          <span class="vr-room__count">{{ store.participants.length }} участников</span>
        </div>
      </div>
      <div class="vr-room__header-actions">
        <button v-if="pinnedNick" class="vr-icon-header-btn" title="Вернуть сетку" @click="pinnedNick = null">⊞</button>
        <button class="vr-icon-header-btn"
                :title="isFullscreen ? 'Выйти из полноэкранного' : 'На весь экран'"
                @click="toggleFullscreen">
          {{ isFullscreen ? '⊡' : '⛶' }}
        </button>
        <button v-if="auth.isTeacher && store.currentRoom"
                class="vr-btn vr-btn--danger-sm" @click="confirmEnd = true">
          Закрыть комнату
        </button>
      </div>
    </div>

    <!-- ─────────────────────────────────────────────────────────────────────
         Stage: ONE container, tiles always alive (v-show).
         In spotlight mode the pinned tile becomes position:absolute over the grid.
         This avoids component re-creation → no audio/video srcObject reset.
    ──────────────────────────────────────────────────────────────────────── -->
    <div class="vr-stage" :class="{ 'vr-stage--spotlight': !!pinnedNick }">

      <!-- ── Local tile ─────────────────────────────────────────────────── -->
      <div class="tile-wrap"
           :class="pinnedNick === selfParticipant.nickname ? 'tile-wrap--pinned' : (pinnedNick ? 'tile-wrap--side' : '')">
        <ParticipantTile
          :participant="selfParticipant"
          :stream="webrtc.localStream"
          :screen-stream="webrtc.screenStream"
          :is-local="true"
          :mic-enabled="webrtc.micEnabled"
          :cam-enabled="webrtc.cameraEnabled"
          :screen-sharing="webrtc.screenSharing"
          :pinned="pinnedNick === selfParticipant.nickname"
          @click="togglePin(selfParticipant.nickname)"
        />
      </div>

      <!-- ── Remote tiles ───────────────────────────────────────────────── -->
      <div
        v-for="p in remoteParticipants"
        :key="p.userId"
        class="tile-wrap"
        :class="pinnedNick === p.nickname ? 'tile-wrap--pinned' : (pinnedNick ? 'tile-wrap--side' : '')">
        <ParticipantTile
          :participant="p"
          :stream="webrtc.remoteStreams[p.nickname]?.stream ?? null"
          :cam-enabled="webrtc.remoteStreams[p.nickname]?.hasVideo ?? false"
          :remote-speaking="webrtc.remoteSpeaking[p.nickname] ?? false"
          :is-local="false"
          :show-teacher-controls="auth.isTeacher"
          :pinned="pinnedNick === p.nickname"
          :volume="webrtc.remoteVolumes[p.nickname] ?? 1"
          @click="togglePin(p.nickname)"
          @volume-change="v => webrtc.setUserVolume(p.nickname, v)"
          @mute-mic="webrtc.sendMuteSignal(p.nickname, 'MUTE_MIC')"
          @mute-cam="webrtc.sendMuteSignal(p.nickname, 'MUTE_CAM')"
          @mute-audio="webrtc.sendMuteSignal(p.nickname, 'MUTE_AUDIO')"
        />
      </div>

      <!-- Backdrop (click to unpin) -->
      <div v-if="pinnedNick" class="vr-stage__backdrop" @click="pinnedNick = null" />
    </div>

    <!-- Controls bar -->
    <MediaBar
      :mic-enabled="webrtc.micEnabled"
      :cam-enabled="webrtc.cameraEnabled"
      :headphones-enabled="webrtc.headphonesEnabled"
      :screen-sharing="webrtc.screenSharing"
      :audio-input-devices="webrtc.audioInputDevices"
      :audio-output-devices="webrtc.audioOutputDevices"
      :selected-mic-id="webrtc.selectedMicId"
      :selected-speaker-id="webrtc.selectedSpeakerId"
      :mic-gain-value="webrtc.micGainValue"
      :headphones-volume="webrtc.headphonesVolume"
      @toggle-mic="webrtc.toggleMic"
      @toggle-cam="webrtc.toggleCamera"
      @toggle-headphones="webrtc.toggleHeadphones"
      @toggle-screen="handleScreenToggle"
      @set-mic-device="webrtc.setMicDevice"
      @set-speaker-device="webrtc.setSpeakerDevice"
      @set-mic-gain="webrtc.setMicGain"
      @set-headphones-volume="webrtc.setHeadphonesVolume"
      @leave="leave"
    />

    <!-- Screen share quality dialog -->
    <ScreenShareDialog
      v-if="showScreenDialog"
      @confirm="onScreenConfirm"
      @cancel="showScreenDialog = false"
    />

    <!-- End room confirm -->
    <div v-if="confirmEnd" class="vr-overlay" @click.self="confirmEnd = false">
      <div class="vr-confirm">
        <h3>Закрыть комнату?</h3>
        <p>Все участники будут отключены.</p>
        <div class="vr-confirm__actions">
          <button class="vr-btn vr-btn--ghost" @click="confirmEnd = false">Отмена</button>
          <button class="vr-btn vr-btn--danger" @click="doEnd">Закрыть</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore }       from '@/stores/auth'
import { useVoiceRoomsStore } from '@/stores/voiceRooms'
import { useWebRTCStore }     from '@/stores/webrtc'
import ParticipantTile   from '@/components/voice-rooms/ParticipantTile.vue'
import MediaBar          from '@/components/voice-rooms/MediaBar.vue'
import ScreenShareDialog from '@/components/voice-rooms/ScreenShareDialog.vue'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()
const store  = useVoiceRoomsStore()
const webrtc = useWebRTCStore()

const roomId           = Number(route.params.id)
const showScreenDialog = ref(false)
const confirmEnd       = ref(false)
const roomEl           = ref(null)
const isFullscreen     = ref(false)
const pinnedNick       = ref(null)   // null = grid; nick = spotlight
let   leftCleanly      = false

const selfParticipant = computed(() => ({
  userId:    auth.user?.id,
  nickname:  auth.user?.nickname,
  fullName:  auth.user?.fullName,
  avatarUrl: auth.user?.avatarUrl,
  role:      auth.user?.role
}))

const remoteParticipants = computed(() =>
  store.participants.filter(p => Number(p.userId) !== Number(auth.user?.id))
)

// Toggle pin: same nick = unpin, new nick = pin
function togglePin(nick) {
  pinnedNick.value = pinnedNick.value === nick ? null : nick
}

// ── Auto-spotlight: one remote starts screen share → pin them ────────────
watch(() => ({ ...webrtc.remoteScreenSharing }), (curr) => {
  const active = Object.entries(curr).filter(([, v]) => v).map(([k]) => k)
  if (active.length === 1) {
    pinnedNick.value = active[0]
  } else if (active.length === 0) {
    // Only auto-unpin if pinned user was the screen-sharer
    if (pinnedNick.value && !webrtc.screenSharing) {
      pinnedNick.value = null
    }
  } else {
    pinnedNick.value = null  // multiple sharers → grid
  }
}, { deep: true })

// ── Fullscreen ─────────────────────────────────────────────────────────────
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    roomEl.value?.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

const onFullscreenChange = () => { isFullscreen.value = !!document.fullscreenElement }

// ── Room join / leave ──────────────────────────────────────────────────────
onMounted(async () => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  try {
    webrtc.connectStomp()
    await new Promise(r => setTimeout(r, 700))
    webrtc.currentRoomId = roomId
    await webrtc.subscribeToRoom(roomId)
    const participants = await store.joinRoom(roomId)
    const others = (participants || []).filter(p => p.nickname !== auth.user?.nickname)
    console.log('[VoiceRoom] joined room, others:', others.map(p => p.nickname))
    await webrtc.joinRoom(roomId, others)
  } catch (e) {
    console.error('[VoiceRoom] onMounted error:', e)
  }
})

onBeforeUnmount(async () => {
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (!leftCleanly) await doLeave()
})

async function leave() {
  leftCleanly = true
  await doLeave()
  router.push('/voice-rooms')
}
async function doLeave() {
  await webrtc.leaveRoom()
  await store.leaveRoom(roomId)
}

function handleScreenToggle() {
  if (webrtc.screenSharing) webrtc.stopScreenShare()
  else showScreenDialog.value = true
}
async function onScreenConfirm({ fps, height }) {
  showScreenDialog.value = false
  await webrtc.startScreenShare(fps, height)
  // Auto-pin own screen share
  pinnedNick.value = selfParticipant.value.nickname
}
async function doEnd() {
  confirmEnd.value = false
  leftCleanly = true
  await store.endRoom(roomId)
  await webrtc.leaveRoom()
  router.push('/voice-rooms')
}
</script>

<style lang="scss" scoped>
.vr-room {
  display: flex; flex-direction: column;
  height: calc(100vh - 64px);
  background: #0f0f1a; overflow: hidden;
  &--fullscreen { height: 100vh; }

  &__header {
    display: flex; align-items: center; gap: 12px; flex-shrink: 0;
    padding: 10px 16px; border-bottom: 1px solid rgba(255,255,255,0.08);
    background: #1a1a2e;
  }
  &__title {
    display: flex; align-items: center; gap: 10px; flex: 1; font-size: 20px;
    h2 { font-size: 15px; font-weight: 600; margin: 0; color: #e2e8f0; }
  }
  &__count { font-size: 11px; color: #94a3b8; }
  &__header-actions { display: flex; align-items: center; gap: 8px; }
}

// ── Stage: single container, tiles always alive ──────────────────────────
.vr-stage {
  flex: 1;
  position: relative;  // anchor for absolutely-positioned pinned tile
  overflow-y: auto;
  overflow-x: hidden;
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
  align-content: start;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }

  // In spotlight mode, lock scroll
  &--spotlight { overflow: hidden; }

  // Backdrop: dims the grid behind the pinned tile
  &__backdrop {
    position: absolute; inset: 0; z-index: 9;
    background: rgba(0,0,0,0.55); cursor: pointer;
  }
}

// ── Tile wrapper ─────────────────────────────────────────────────────────
.tile-wrap {
  cursor: pointer;

  // Grid mode: tile fills the cell naturally (aspect-ratio: 16/9 from component)
  :deep(.vr-tile) { cursor: pointer; }

  // ── PINNED: absolute overlay, fills the stage ─────────────────────────
  &--pinned {
    position: absolute;
    inset: 8px;          // 8px gap from stage edges
    z-index: 10;
    cursor: pointer;

    // Override tile styles to fill the container
    :deep(.vr-tile) {
      width: 100%;
      height: 100%;
      aspect-ratio: unset !important;  // remove 16:9 constraint → fill space
      cursor: pointer;
      border-color: rgba(79,70,229,0.8) !important;
    }

    // Ensure video fills the large tile properly
    :deep(.vr-tile__video-wrap) { flex: 1; }
    :deep(.vr-tile__video) { width: 100%; height: 100%; object-fit: contain; }
    :deep(.vr-tile__avatar) {
      font-size: 60px;
      img { width: 120px; height: 120px; }
    }
  }

  // ── SIDE tiles (in spotlight mode, visible under backdrop) ───────────
  // They remain in the grid — no special sizing needed
  &--side {
    cursor: default;
    :deep(.vr-tile) { cursor: default; }
  }
}

// ── Shared button styles ─────────────────────────────────────────────────
.vr-back {
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px; padding: 6px 12px; color: #94a3b8; cursor: pointer;
  font-size: 13px; white-space: nowrap;
  &:hover { background: rgba(255,255,255,0.1); color: #e2e8f0; }
}
.vr-icon-header-btn {
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px; padding: 5px 10px; color: #94a3b8; cursor: pointer;
  font-size: 15px; line-height: 1;
  &:hover { background: rgba(255,255,255,0.12); color: #e2e8f0; }
}

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
.vr-btn {
  padding: 9px 18px; border-radius: 8px; border: none;
  cursor: pointer; font-size: 14px; font-weight: 500; transition: all 0.2s;
  &--danger    { background: #ef4444; color: #fff; &:hover { background: #dc2626; } }
  &--danger-sm { background: #ef4444; color: #fff; padding: 6px 12px; font-size: 12px;
                 border-radius: 8px; border: none; cursor: pointer;
                 &:hover { background: #dc2626; } }
  &--ghost     { background: rgba(255,255,255,0.06); color: #94a3b8;
                 border: 1px solid rgba(255,255,255,0.1); &:hover { color: #e2e8f0; } }
}

// ── Mobile ───────────────────────────────────────────────────────────────
@media (max-width: 600px) {
  .vr-room__header { padding: 8px 10px; gap: 8px; }
  .vr-room__title h2 { font-size: 13px; }
  .vr-stage { padding: 8px; gap: 8px;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); }
  .tile-wrap--pinned { inset: 4px; }
}
</style>
