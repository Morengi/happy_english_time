import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from './auth'
import { useVoiceRoomsStore } from './voiceRooms'

const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' }
]

export const useWebRTCStore = defineStore('webrtc', () => {
  const auth  = useAuthStore()
  const rooms = useVoiceRoomsStore()

  // ── STOMP ─────────────────────────────────────────────────────────────────
  let stompClient = null
  const connected = ref(false)
  let signalSub   = null
  let eventSub    = null

  // ── Local media ───────────────────────────────────────────────────────────
  const localStream       = ref(null)
  const screenStream      = ref(null)
  const micEnabled        = ref(true)
  const cameraEnabled     = ref(false)
  const headphonesEnabled = ref(true)
  const screenSharing     = ref(false)

  // ── Remote streams: nickname → { stream, micMuted, camMuted, audioMuted } ─
  const remoteStreams = reactive({})

  // ── Peer connections: nickname → RTCPeerConnection ────────────────────────
  const peerConnections = {}

  const currentRoomId = ref(null)

  // ── STOMP connection ──────────────────────────────────────────────────────
  function connectStomp() {
    if (stompClient?.active) return
    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      reconnectDelay: 3000,
      onConnect:    () => { connected.value = true },
      onDisconnect: () => { connected.value = false }
    })
    stompClient.activate()
  }

  function disconnectStomp() {
    signalSub?.unsubscribe()
    eventSub?.unsubscribe()
    signalSub = null
    eventSub  = null
    stompClient?.deactivate()
    connected.value = false
  }

  // ── Join / Leave room ─────────────────────────────────────────────────────
  async function joinRoom(roomId, existingParticipants) {
    currentRoomId.value = roomId
    await startLocalMedia()
    subscribeToRoom(roomId)
    // Initiate offers to everyone already in the room
    for (const p of existingParticipants) {
      if (p.nickname !== auth.user?.nickname) {
        await createOffer(p.nickname)
      }
    }
  }

  async function leaveRoom() {
    Object.keys(peerConnections).forEach(nick => {
      peerConnections[nick].close()
      delete peerConnections[nick]
    })
    stopLocalMedia()
    signalSub?.unsubscribe()
    eventSub?.unsubscribe()
    signalSub = null
    eventSub  = null
    Object.keys(remoteStreams).forEach(k => delete remoteStreams[k])
    currentRoomId.value = null
  }

  // ── Local media ───────────────────────────────────────────────────────────
  async function startLocalMedia() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: false
      })
      localStream.value = stream
      applyMicState(stream)
    } catch (e) {
      console.warn('Microphone access denied:', e)
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

  // ── Self controls ─────────────────────────────────────────────────────────
  function toggleMic() {
    micEnabled.value = !micEnabled.value
    applyMicState()
  }

  async function toggleCamera() {
    cameraEnabled.value = !cameraEnabled.value
    if (!localStream.value) return
    const existing = localStream.value.getVideoTracks()[0]
    if (!existing) {
      if (cameraEnabled.value) {
        try {
          const s = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
          const track = s.getVideoTracks()[0]
          localStream.value.addTrack(track)
          for (const pc of Object.values(peerConnections)) {
            const sender = pc.getSenders().find(s => s.track?.kind === 'video')
            if (sender) sender.replaceTrack(track)
            else pc.addTrack(track, localStream.value)
          }
        } catch { cameraEnabled.value = false }
      }
    } else {
      existing.enabled = cameraEnabled.value
    }
  }

  function toggleHeadphones() {
    headphonesEnabled.value = !headphonesEnabled.value
    document.querySelectorAll('audio[data-remote]').forEach(el => {
      el.muted = !headphonesEnabled.value
    })
  }

  // ── Screen share ──────────────────────────────────────────────────────────
  async function startScreenShare(fps, height) {
    try {
      const stream = await navigator.mediaDevices.getDisplayMedia({
        video: { frameRate: { ideal: fps }, height: { ideal: height } },
        audio: true
      })
      screenStream.value  = stream
      screenSharing.value = true
      const track = stream.getVideoTracks()[0]
      track.addEventListener('ended', () => stopScreenShare())
      for (const pc of Object.values(peerConnections)) {
        const sender = pc.getSenders().find(s => s.track?.kind === 'video')
        if (sender) sender.replaceTrack(track)
      }
    } catch (e) { console.warn('Screen share cancelled:', e) }
  }

  async function stopScreenShare() {
    screenStream.value?.getTracks().forEach(t => t.stop())
    screenStream.value  = null
    screenSharing.value = false
    const camTrack = cameraEnabled.value ? localStream.value?.getVideoTracks()[0] : null
    for (const pc of Object.values(peerConnections)) {
      const sender = pc.getSenders().find(s => s.track?.kind === 'video')
      if (sender) sender.replaceTrack(camTrack || null)
    }
  }

  // ── Teacher mute controls ─────────────────────────────────────────────────
  function sendMuteSignal(targetNickname, type) {
    sendSignal({ type, toUserNickname: targetNickname,
      fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
      roomId: currentRoomId.value, payload: null })
  }

  function handleRemoteMuteCommand(signal) {
    if (signal.type === 'MUTE_MIC') { micEnabled.value = false; applyMicState() }
    else if (signal.type === 'MUTE_CAM') { cameraEnabled.value = false; localStream.value?.getVideoTracks().forEach(t => { t.enabled = false }) }
    else if (signal.type === 'MUTE_AUDIO') { headphonesEnabled.value = false; document.querySelectorAll('audio[data-remote]').forEach(el => { el.muted = true }) }
  }

  // ── STOMP subscriptions ───────────────────────────────────────────────────
  function subscribeToRoom(roomId) {
    // Wait for STOMP connection (retry up to 3s)
    const trySubscribe = (attempts = 0) => {
      if (stompClient?.connected) {
        eventSub = stompClient.subscribe(`/topic/voice/${roomId}/events`, msg => {
          const event = JSON.parse(msg.body)
          rooms.handleParticipantEvent(event)
          if (event.type === 'LEAVE') closePeerConnection(event.userNickname)
        })
        signalSub = stompClient.subscribe('/user/queue/voice-signal', async msg => {
          await handleIncomingSignal(JSON.parse(msg.body))
        })
      } else if (attempts < 15) {
        setTimeout(() => trySubscribe(attempts + 1), 200)
      }
    }
    trySubscribe()
  }

  // ── WebRTC peer connections ───────────────────────────────────────────────
  function createPeerConnection(peerNickname) {
    if (peerConnections[peerNickname]) return peerConnections[peerNickname]
    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
    peerConnections[peerNickname] = pc

    localStream.value?.getTracks().forEach(t => pc.addTrack(t, localStream.value))

    pc.ontrack = evt => {
      if (!remoteStreams[peerNickname]) {
        remoteStreams[peerNickname] = { stream: new MediaStream(), micMuted: false, camMuted: false }
      }
      remoteStreams[peerNickname].stream.addTrack(evt.track)
    }

    pc.onicecandidate = evt => {
      if (evt.candidate) {
        sendSignal({ type: 'ICE_CANDIDATE', toUserNickname: peerNickname,
          fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
          roomId: currentRoomId.value, payload: JSON.stringify(evt.candidate) })
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
    const pc    = createPeerConnection(peerNickname)
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    sendSignal({ type: 'OFFER', toUserNickname: peerNickname,
      fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
      roomId: currentRoomId.value, payload: JSON.stringify(offer) })
  }

  async function handleIncomingSignal(signal) {
    const { type, fromNickname, payload } = signal
    if (type === 'OFFER') {
      const pc     = createPeerConnection(fromNickname)
      await pc.setRemoteDescription(JSON.parse(payload))
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      sendSignal({ type: 'ANSWER', toUserNickname: fromNickname,
        fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
        roomId: currentRoomId.value, payload: JSON.stringify(answer) })
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

  function closePeerConnection(nick) {
    peerConnections[nick]?.close()
    delete peerConnections[nick]
    delete remoteStreams[nick]
  }

  function sendSignal(signal) {
    if (!stompClient?.connected) return
    stompClient.publish({
      destination: `/app/voice/${signal.roomId}/signal`,
      body: JSON.stringify(signal)
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
