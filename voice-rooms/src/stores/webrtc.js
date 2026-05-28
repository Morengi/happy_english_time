import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { useAuthStore } from './auth'
import { useRoomsStore } from './rooms'

// ICE servers — use public STUN + optional TURN
const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' }
]

export const useWebRTCStore = defineStore('webrtc', () => {
  const auth  = useAuthStore()
  const rooms = useRoomsStore()

  // ── STOMP ────────────────────────────────────────────────────────────────
  let stompClient = null
  const connected = ref(false)

  // ── Local media ──────────────────────────────────────────────────────────
  const localStream      = ref(null)   // camera + mic
  const screenStream     = ref(null)   // screen share
  const micEnabled       = ref(true)
  const cameraEnabled    = ref(false)
  const headphonesEnabled = ref(true)  // controls remote audio playback
  const screenSharing    = ref(false)

  // ── Remote streams ───────────────────────────────────────────────────────
  // Map: nickname → { stream: MediaStream, micMuted, camMuted, audioMuted }
  const remoteStreams = reactive({})

  // ── Peer connections ─────────────────────────────────────────────────────
  // Map: nickname → RTCPeerConnection
  const peerConnections = {}

  // ── Current room ─────────────────────────────────────────────────────────
  const currentRoomId = ref(null)

  // ─────────────────────────────────────────────────────────────────────────
  // STOMP connection
  // ─────────────────────────────────────────────────────────────────────────
  function connectStomp() {
    if (stompClient?.connected) return

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      reconnectDelay: 3000,
      onConnect: () => { connected.value = true },
      onDisconnect: () => { connected.value = false }
    })
    stompClient.activate()
  }

  function disconnectStomp() {
    stompClient?.deactivate()
    connected.value = false
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Room lifecycle
  // ─────────────────────────────────────────────────────────────────────────
  async function joinRoom(roomId, existingParticipants) {
    currentRoomId.value = roomId

    // 1. Get local media (mic only at first — camera is opt-in)
    await startLocalMedia()

    // 2. Subscribe to STOMP topics for this room
    subscribeToRoom(roomId)

    // 3. For each existing participant, initiate a peer connection as caller
    for (const p of existingParticipants) {
      if (p.nickname !== auth.user.nickname) {
        await createOffer(p.nickname)
      }
    }
  }

  async function leaveRoom() {
    const roomId = currentRoomId.value

    // Close all peer connections
    for (const nick of Object.keys(peerConnections)) {
      peerConnections[nick].close()
      delete peerConnections[nick]
    }

    // Stop local streams
    stopLocalMedia()

    // Unsubscribe from STOMP room topics
    if (stompClient?.connected && roomId) {
      stompClient.unsubscribe(`/topic/voice/${roomId}/events`)
      stompClient.unsubscribe(`/user/queue/voice-signal`)
    }

    // Clear remote streams
    for (const k of Object.keys(remoteStreams)) delete remoteStreams[k]

    currentRoomId.value = null
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Local media helpers
  // ─────────────────────────────────────────────────────────────────────────
  async function startLocalMedia() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: cameraEnabled.value ? { facingMode: 'user' } : false
      })
      localStream.value = stream
      applyMicState(stream)
    } catch (e) {
      console.warn('Could not get local media:', e)
    }
  }

  function stopLocalMedia() {
    localStream.value?.getTracks().forEach(t => t.stop())
    localStream.value = null
    screenStream.value?.getTracks().forEach(t => t.stop())
    screenStream.value = null
    screenSharing.value = false
  }

  function applyMicState(stream = localStream.value) {
    stream?.getAudioTracks().forEach(t => { t.enabled = micEnabled.value })
  }

  function applyCameraState(stream = localStream.value) {
    stream?.getVideoTracks().forEach(t => { t.enabled = cameraEnabled.value })
  }

  // ── Self media toggles ────────────────────────────────────────────────────
  async function toggleMic() {
    micEnabled.value = !micEnabled.value
    applyMicState()
  }

  async function toggleCamera() {
    cameraEnabled.value = !cameraEnabled.value
    if (!localStream.value) return

    const videoTrack = localStream.value.getVideoTracks()[0]
    if (!videoTrack) {
      // Add video track for the first time
      if (cameraEnabled.value) {
        try {
          const camStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
          const newTrack = camStream.getVideoTracks()[0]
          localStream.value.addTrack(newTrack)
          // Replace track in all peer connections
          for (const pc of Object.values(peerConnections)) {
            const sender = pc.getSenders().find(s => s.track?.kind === 'video')
            if (sender) sender.replaceTrack(newTrack)
            else pc.addTrack(newTrack, localStream.value)
          }
        } catch (e) { console.warn('Camera access denied:', e); cameraEnabled.value = false }
      }
    } else {
      videoTrack.enabled = cameraEnabled.value
    }
  }

  function toggleHeadphones() {
    headphonesEnabled.value = !headphonesEnabled.value
    // Mute all remote audio elements
    document.querySelectorAll('audio[data-remote]').forEach(el => {
      el.muted = !headphonesEnabled.value
    })
  }

  // ── Screen sharing ────────────────────────────────────────────────────────
  async function startScreenShare(fps, height) {
    try {
      const stream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          frameRate: { ideal: fps },
          height:    { ideal: height }
        },
        audio: true
      })
      screenStream.value = stream
      screenSharing.value = true

      const screenTrack = stream.getVideoTracks()[0]
      screenTrack.addEventListener('ended', () => stopScreenShare())

      // Replace video track in all peer connections
      for (const pc of Object.values(peerConnections)) {
        const sender = pc.getSenders().find(s => s.track?.kind === 'video')
        if (sender) sender.replaceTrack(screenTrack)
      }
    } catch (e) {
      console.warn('Screen share cancelled or denied:', e)
    }
  }

  async function stopScreenShare() {
    screenStream.value?.getTracks().forEach(t => t.stop())
    screenStream.value = null
    screenSharing.value = false

    // Restore camera track (or null if camera is off)
    const camTrack = cameraEnabled.value
      ? localStream.value?.getVideoTracks()[0]
      : null

    for (const pc of Object.values(peerConnections)) {
      const sender = pc.getSenders().find(s => s.track?.kind === 'video')
      if (sender) sender.replaceTrack(camTrack || null)
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Teacher controls (mute remote peers)
  // ─────────────────────────────────────────────────────────────────────────
  function sendMuteSignal(targetNickname, type) {
    // type: MUTE_MIC | MUTE_CAM | MUTE_AUDIO
    sendSignal({
      type,
      toUserNickname: targetNickname,
      fromUserId:     auth.user.id,
      fromNickname:   auth.user.nickname,
      roomId:         currentRoomId.value,
      payload:        null
    })
  }

  /** Called when THIS user receives a mute command from teacher */
  function handleRemoteMuteCommand(signal) {
    if (signal.type === 'MUTE_MIC') {
      micEnabled.value = false
      applyMicState()
    } else if (signal.type === 'MUTE_CAM') {
      cameraEnabled.value = false
      applyCameraState()
    } else if (signal.type === 'MUTE_AUDIO') {
      headphonesEnabled.value = false
      document.querySelectorAll('audio[data-remote]').forEach(el => { el.muted = true })
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // STOMP subscriptions
  // ─────────────────────────────────────────────────────────────────────────
  function subscribeToRoom(roomId) {
    if (!stompClient?.connected) return

    // Room-wide events (JOIN / LEAVE / ROOM_ENDED)
    stompClient.subscribe(`/topic/voice/${roomId}/events`, msg => {
      const event = JSON.parse(msg.body)
      rooms.handleParticipantEvent(event)
      if (event.type === 'JOIN' && event.userNickname !== auth.user.nickname) {
        // New peer joined — they will send us an offer; nothing to do here
      } else if (event.type === 'LEAVE') {
        closePeerConnection(event.userNickname)
      }
    })

    // Personal signal queue (OFFER / ANSWER / ICE / MUTE_*)
    stompClient.subscribe(`/user/queue/voice-signal`, async msg => {
      const signal = JSON.parse(msg.body)
      await handleIncomingSignal(signal)
    })
  }

  // ─────────────────────────────────────────────────────────────────────────
  // WebRTC helpers
  // ─────────────────────────────────────────────────────────────────────────
  function createPeerConnection(peerNickname) {
    if (peerConnections[peerNickname]) return peerConnections[peerNickname]

    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
    peerConnections[peerNickname] = pc

    // Add local tracks
    if (localStream.value) {
      localStream.value.getTracks().forEach(t => pc.addTrack(t, localStream.value))
    }

    // Receive remote stream
    pc.ontrack = (evt) => {
      if (!remoteStreams[peerNickname]) {
        remoteStreams[peerNickname] = { stream: new MediaStream(), micMuted: false, camMuted: false, audioMuted: false }
      }
      remoteStreams[peerNickname].stream.addTrack(evt.track)
    }

    // ICE candidate → send via STOMP
    pc.onicecandidate = (evt) => {
      if (evt.candidate) {
        sendSignal({
          type:           'ICE_CANDIDATE',
          toUserNickname: peerNickname,
          fromUserId:     auth.user.id,
          fromNickname:   auth.user.nickname,
          roomId:         currentRoomId.value,
          payload:        JSON.stringify(evt.candidate)
        })
      }
    }

    pc.onconnectionstatechange = () => {
      if (['disconnected', 'failed', 'closed'].includes(pc.connectionState)) {
        closePeerConnection(peerNickname)
      }
    }

    return pc
  }

  async function createOffer(peerNickname) {
    const pc = createPeerConnection(peerNickname)
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    sendSignal({
      type:           'OFFER',
      toUserNickname: peerNickname,
      fromUserId:     auth.user.id,
      fromNickname:   auth.user.nickname,
      roomId:         currentRoomId.value,
      payload:        JSON.stringify(offer)
    })
  }

  async function handleIncomingSignal(signal) {
    const { type, fromNickname, payload } = signal

    if (type === 'OFFER') {
      const pc = createPeerConnection(fromNickname)
      await pc.setRemoteDescription(JSON.parse(payload))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      sendSignal({
        type:           'ANSWER',
        toUserNickname: fromNickname,
        fromUserId:     auth.user.id,
        fromNickname:   auth.user.nickname,
        roomId:         currentRoomId.value,
        payload:        JSON.stringify(answer)
      })
    } else if (type === 'ANSWER') {
      const pc = peerConnections[fromNickname]
      if (pc) await pc.setRemoteDescription(JSON.parse(payload))
    } else if (type === 'ICE_CANDIDATE') {
      const pc = peerConnections[fromNickname]
      if (pc && payload) await pc.addIceCandidate(JSON.parse(payload))
    } else if (['MUTE_MIC', 'MUTE_CAM', 'MUTE_AUDIO'].includes(type)) {
      handleRemoteMuteCommand(signal)
    }
  }

  function closePeerConnection(peerNickname) {
    peerConnections[peerNickname]?.close()
    delete peerConnections[peerNickname]
    delete remoteStreams[peerNickname]
  }

  // ── STOMP send helper ─────────────────────────────────────────────────────
  function sendSignal(signal) {
    if (!stompClient?.connected) return
    stompClient.publish({
      destination: `/app/voice/${signal.roomId}/signal`,
      body:        JSON.stringify(signal)
    })
  }

  return {
    connected, localStream, screenStream,
    micEnabled, cameraEnabled, headphonesEnabled, screenSharing,
    remoteStreams, currentRoomId,
    connectStomp, disconnectStomp,
    joinRoom, leaveRoom,
    toggleMic, toggleCamera, toggleHeadphones,
    startScreenShare, stopScreenShare,
    sendMuteSignal
  }
})
