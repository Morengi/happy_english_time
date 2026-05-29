<template>
  <div class="vr-room">
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
      <button v-if="auth.isTeacher && store.currentRoom" class="vr-btn vr-btn--danger-sm" @click="confirmEnd = true">
        Закрыть комнату
      </button>
    </div>

    <!-- Video grid -->
    <div class="vr-room__grid">
      <!-- Local tile -->
      <ParticipantTile
        :participant="selfParticipant"
        :stream="webrtc.localStream"
        :screen-stream="webrtc.screenStream"
        :is-local="true"
        :mic-enabled="webrtc.micEnabled"
        :cam-enabled="webrtc.cameraEnabled"
        :screen-sharing="webrtc.screenSharing"
      />

      <!-- Remote tiles -->
      <ParticipantTile
        v-for="p in store.participants"
        :key="p.userId"
        :participant="p"
        :stream="webrtc.remoteStreams[p.nickname]?.stream ?? null"
        :is-local="false"
        :show-teacher-controls="auth.isTeacher && p.nickname !== auth.user?.nickname"
        @mute-mic="webrtc.sendMuteSignal(p.nickname, 'MUTE_MIC')"
        @mute-cam="webrtc.sendMuteSignal(p.nickname, 'MUTE_CAM')"
        @mute-audio="webrtc.sendMuteSignal(p.nickname, 'MUTE_AUDIO')"
      />
    </div>

    <!-- Controls bar -->
    <MediaBar
      :mic-enabled="webrtc.micEnabled"
      :cam-enabled="webrtc.cameraEnabled"
      :headphones-enabled="webrtc.headphonesEnabled"
      :screen-sharing="webrtc.screenSharing"
      @toggle-mic="webrtc.toggleMic"
      @toggle-cam="webrtc.toggleCamera"
      @toggle-headphones="webrtc.toggleHeadphones"
      @toggle-screen="handleScreenToggle"
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
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

const roomId         = Number(route.params.id)
const showScreenDialog = ref(false)
const confirmEnd       = ref(false)
let   leftCleanly      = false

const selfParticipant = computed(() => ({
  userId:    auth.user?.id,
  nickname:  auth.user?.nickname,
  fullName:  auth.user?.fullName,
  avatarUrl: auth.user?.avatarUrl,
  role:      auth.user?.role
}))

onMounted(async () => {
  webrtc.connectStomp()
  // Brief delay to let STOMP handshake complete
  await new Promise(r => setTimeout(r, 700))
  const participants = await store.joinRoom(roomId)
  const others = (participants || []).filter(p => p.nickname !== auth.user?.nickname)
  await webrtc.joinRoom(roomId, others)
})

onBeforeUnmount(async () => {
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
  display: flex; flex-direction: column; height: calc(100vh - 64px);
  background: #0f0f1a; overflow: hidden;

  &__header {
    display: flex; align-items: center; gap: 14px; flex-shrink: 0;
    padding: 14px 20px; border-bottom: 1px solid rgba(255,255,255,0.08);
    background: #1a1a2e;
  }

  &__title {
    display: flex; align-items: center; gap: 10px; flex: 1; font-size: 22px;
    h2 { font-size: 16px; font-weight: 600; margin: 0; color: #e2e8f0; }
  }
  &__count { font-size: 12px; color: #94a3b8; }

  &__grid {
    flex: 1; overflow-y: auto; padding: 16px;
    display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 12px; align-content: start;
  }
}

.vr-back {
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px; padding: 7px 14px; color: #94a3b8; cursor: pointer;
  font-size: 13px; white-space: nowrap;
  &:hover { background: rgba(255,255,255,0.1); color: #e2e8f0; }
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
  &--danger-sm { background: #ef4444; color: #fff; padding: 7px 14px; font-size: 13px;
                 border-radius: 8px; border: none; cursor: pointer;
                 &:hover { background: #dc2626; } }
  &--ghost     { background: rgba(255,255,255,0.06); color: #94a3b8;
                 border: 1px solid rgba(255,255,255,0.1); &:hover { color: #e2e8f0; } }
}
</style>
