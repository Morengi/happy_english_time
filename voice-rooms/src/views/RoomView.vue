<template>
  <div class="room-view">
    <!-- Header -->
    <div class="room-view__header">
      <button class="btn btn--ghost btn--icon" @click="leave">←</button>
      <div class="room-view__title">
        <span>🔊</span>
        <div>
          <h2>{{ rooms.currentRoom?.name || 'Комната' }}</h2>
          <span class="room-view__count">{{ rooms.participants.length }} участников</span>
        </div>
      </div>
      <div class="room-view__header-actions">
        <button
          v-if="auth.isTeacherOrAdmin && rooms.currentRoom"
          class="btn btn--danger"
          style="font-size:13px; padding: 6px 14px;"
          @click="confirmEndRoom = true"
        >
          Закрыть комнату
        </button>
      </div>
    </div>

    <!-- Main: video grid -->
    <div class="room-view__body">
      <!-- Local tile -->
      <ParticipantTile
        :participant="localParticipant"
        :stream="webrtc.localStream"
        :is-local="true"
        :mic-enabled="webrtc.micEnabled"
        :cam-enabled="webrtc.cameraEnabled"
        :screen-sharing="webrtc.screenSharing"
        :screen-stream="webrtc.screenStream"
      />

      <!-- Remote tiles -->
      <ParticipantTile
        v-for="p in rooms.participants"
        :key="p.userId"
        :participant="p"
        :stream="webrtc.remoteStreams[p.nickname]?.stream"
        :is-local="false"
        :show-teacher-controls="auth.isTeacherOrAdmin && p.nickname !== auth.user.nickname"
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
      @confirm="startScreenShare"
      @cancel="showScreenDialog = false"
    />

    <!-- End room confirmation -->
    <div v-if="confirmEndRoom" class="modal-overlay" @click.self="confirmEndRoom = false">
      <div class="modal">
        <h3>Закрыть комнату?</h3>
        <p>Все участники будут отключены.</p>
        <div style="display:flex; gap:10px; margin-top:20px; justify-content:flex-end;">
          <button class="btn btn--ghost" @click="confirmEndRoom = false">Отмена</button>
          <button class="btn btn--danger" @click="doEndRoom">Закрыть</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore }  from '@/stores/auth'
import { useRoomsStore } from '@/stores/rooms'
import { useWebRTCStore } from '@/stores/webrtc'
import ParticipantTile from '@/components/ParticipantTile.vue'
import MediaBar        from '@/components/MediaBar.vue'
import ScreenShareDialog from '@/components/ScreenShareDialog.vue'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()
const rooms  = useRoomsStore()
const webrtc = useWebRTCStore()

const roomId         = Number(route.params.id)
const showScreenDialog = ref(false)
const confirmEndRoom   = ref(false)

const localParticipant = computed(() => ({
  userId:    auth.user?.id,
  nickname:  auth.user?.nickname,
  fullName:  auth.user?.fullName,
  avatarUrl: auth.user?.avatarUrl,
  role:      auth.user?.role
}))

onMounted(async () => {
  webrtc.connectStomp()
  // Small delay to let STOMP connect before joining
  await new Promise(r => setTimeout(r, 600))
  const participants = await rooms.joinRoom(roomId)
  // Pass existing participants (excluding self) so we can send offers
  const others = participants.filter(p => p.nickname !== auth.user?.nickname)
  await webrtc.joinRoom(roomId, others)
})

onBeforeUnmount(async () => {
  await doLeave()
})

async function leave() {
  await doLeave()
  router.push('/voice-rooms/')
}

async function doLeave() {
  await webrtc.leaveRoom()
  await rooms.leaveRoom(roomId)
}

function handleScreenToggle() {
  if (webrtc.screenSharing) {
    webrtc.stopScreenShare()
  } else {
    showScreenDialog.value = true
  }
}

async function startScreenShare({ fps, height }) {
  showScreenDialog.value = false
  await webrtc.startScreenShare(fps, height)
}

async function doEndRoom() {
  confirmEndRoom.value = false
  await rooms.endRoom(roomId)
  await webrtc.leaveRoom()
  router.push('/voice-rooms/')
}
</script>

<style lang="scss" scoped>
$bg: #0f0f1a;
$bg-card: #1a1a2e;
$border: rgba(255,255,255,0.08);
$text-muted: #94a3b8;

.room-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 20px;
    border-bottom: 1px solid $border;
    background: $bg-card;
    flex-shrink: 0;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: 1;
    font-size: 22px;

    h2 { font-size: 17px; font-weight: 600; margin: 0; }
  }

  &__count { font-size: 12px; color: $text-muted; }

  &__header-actions { display: flex; gap: 8px; }

  &__body {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 12px;
    padding: 16px;
    overflow-y: auto;
    align-content: start;
  }
}
</style>
