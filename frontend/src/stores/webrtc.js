import { defineStore } from 'pinia'
import { ref, reactive, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from './auth'
import { useVoiceRoomsStore } from './voiceRooms'

const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' }
]

const LOG = (...a) => console.log('[WebRTC]', ...a)

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
  const micEnabled        = ref(false)
  const cameraEnabled     = ref(false)
  const headphonesEnabled = ref(true)
  const screenSharing     = ref(false)

  // ── Volume / gain controls ────────────────────────────────────────────────
  const micGainValue      = ref(1.0)    // 0–2
  const headphonesVolume  = ref(1.0)    // 0–1
  const remoteVolumes     = reactive({}) // nick → 0–2

  // ── Mic GainNode (WebAudio) ───────────────────────────────────────────────
  let micGainNode = null
  let micAudioCtx = null
  let rawMicStream = null  // original getUserMedia stream (kept for replacement)

  // ── Device lists ──────────────────────────────────────────────────────────
  const audioInputDevices  = ref([])
  const audioOutputDevices = ref([])
  const selectedMicId      = ref('')
  const selectedSpeakerId  = ref('')

  // ── Remote streams / state ─────────────────────────────────────────────────
  const remoteStreams      = reactive({})
  const remoteSpeaking     = reactive({})
  const remoteScreenSharing = reactive({})

  // ── Peer connections / ICE buffer ────────────────────────────────────────
  const peerConnections  = {}
  const pendingCandidates = {}
  const speechAnalysers  = {}
  const videoSenders     = {}   // nick → RTCRtpSender for the video track (screen or camera)
  const currentRoomId    = ref(null)

  // ── When localStream becomes available, add its tracks to all existing PCs ─
  watch(localStream, (stream) => {
    if (!stream) return
    Object.entries(peerConnections).forEach(([, pc]) => {
      stream.getTracks().forEach(track => {
        const existing = pc.getSenders().find(s => s.track?.kind === track.kind)
        if (!existing) pc.addTrack(track, stream)
      })
    })
  })

  // ── STOMP connection ──────────────────────────────────────────────────────
  function connectStomp() {
    if (stompClient?.active) return
    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${auth.token}` },
      reconnectDelay: 3000,
      onConnect:    () => { connected.value = true;  LOG('STOMP connected') },
      onDisconnect: () => { connected.value = false; LOG('STOMP disconnected') }
    })
    stompClient.activate()
  }

  function disconnectStomp() {
    signalSub?.unsubscribe(); eventSub?.unsubscribe()
    signalSub = null; eventSub = null
    stompClient?.deactivate()
    connected.value = false
  }

  // ── Subscribe to room events ──────────────────────────────────────────────
  function subscribeToRoom(roomId) {
    return new Promise(resolve => {
      const trySubscribe = (attempts = 0) => {
        if (stompClient?.connected) {
          eventSub?.unsubscribe(); signalSub?.unsubscribe()
          eventSub = stompClient.subscribe(`/topic/voice/${roomId}/events`, msg => {
            const event = JSON.parse(msg.body)
            LOG('room event:', event.type, event.userNickname || '')
            rooms.handleParticipantEvent(event)
            if (event.type === 'LEAVE') closePeerConnection(event.userNickname)
          })
          signalSub = stompClient.subscribe('/user/queue/voice-signal', msg => {
            handleIncomingSignal(JSON.parse(msg.body))
          })
          LOG('subscribed to room', roomId)
          resolve()
        } else if (attempts < 20) {
          setTimeout(() => trySubscribe(attempts + 1), 200)
        } else { LOG('WARN: gave up waiting for STOMP'); resolve() }
      }
      trySubscribe()
    })
  }

  // ── Device enumeration ────────────────────────────────────────────────────
  async function enumerateDevices() {
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      audioInputDevices.value = devices
        .filter(d => d.kind === 'audioinput')
        .map(d => ({ deviceId: d.deviceId, label: d.label || `Микрофон ${d.deviceId.slice(0,6)}` }))
      audioOutputDevices.value = devices
        .filter(d => d.kind === 'audiooutput')
        .map(d => ({ deviceId: d.deviceId, label: d.label || `Динамик ${d.deviceId.slice(0,6)}` }))
    } catch (e) { LOG('enumerateDevices error:', e) }
  }

  // ── Join / Leave room ─────────────────────────────────────────────────────
  async function joinRoom(roomId, existingParticipants) {
    currentRoomId.value = roomId
    await startLocalMedia()
    await enumerateDevices()
    LOG('joining room', roomId, 'existing peers:', existingParticipants.map(p => p.nickname))
    for (const p of existingParticipants) {
      if (p.nickname !== auth.user?.nickname) {
        LOG('creating PC for existing peer:', p.nickname)
        createPeerConnection(p.nickname)
      }
    }
  }

  async function leaveRoom() {
    Object.keys(peerConnections).forEach(nick => {
      peerConnections[nick].close(); delete peerConnections[nick]
    })
    Object.values(speechAnalysers).forEach(({ ctx, interval }) => {
      clearInterval(interval); ctx?.close()
    })
    Object.keys(speechAnalysers).forEach(k => delete speechAnalysers[k])
    Object.keys(remoteSpeaking).forEach(k => delete remoteSpeaking[k])
    Object.keys(remoteScreenSharing).forEach(k => delete remoteScreenSharing[k])
    Object.keys(pendingCandidates).forEach(k => delete pendingCandidates[k])
    Object.keys(videoSenders).forEach(k => delete videoSenders[k])
    stopLocalMedia()
    signalSub?.unsubscribe(); eventSub?.unsubscribe()
    signalSub = null; eventSub = null
    Object.keys(remoteStreams).forEach(k => delete remoteStreams[k])
    currentRoomId.value = null
  }

  // ── Local media ───────────────────────────────────────────────────────────
  async function startLocalMedia() {
    try {
      const constraints = {
        audio: selectedMicId.value ? { deviceId: { exact: selectedMicId.value } } : true,
        video: false
      }
      rawMicStream = await navigator.mediaDevices.getUserMedia(constraints)

      // Pipe through GainNode for volume control
      const AudioCtx = window.AudioContext || window.webkitAudioContext
      micAudioCtx?.close()
      micAudioCtx = new AudioCtx()
      micAudioCtx.resume()
      const source = micAudioCtx.createMediaStreamSource(rawMicStream)
      micGainNode  = micAudioCtx.createGain()
      micGainNode.gain.value = micGainValue.value
      const dest = micAudioCtx.createMediaStreamDestination()
      source.connect(micGainNode)
      micGainNode.connect(dest)

      localStream.value = dest.stream
      applyMicState()
      LOG('local media started (with gain node)')
    } catch (e) {
      console.warn('[WebRTC] Microphone access denied:', e)
    }
  }

  function stopLocalMedia() {
    rawMicStream?.getTracks().forEach(t => t.stop())
    rawMicStream = null
    micAudioCtx?.close(); micAudioCtx = null; micGainNode = null
    localStream.value?.getTracks().forEach(t => t.stop())
    localStream.value = null
    screenStream.value?.getTracks().forEach(t => t.stop())
    screenStream.value = null
    screenSharing.value = false
  }

  function applyMicState(stream = localStream.value) {
    stream?.getAudioTracks().forEach(t => { t.enabled = micEnabled.value })
  }

  // ── Volume / gain setters ─────────────────────────────────────────────────
  function setMicGain(value) {
    micGainValue.value = Math.max(0, Math.min(2, value))
    if (micGainNode) micGainNode.gain.value = micGainValue.value
  }

  function setHeadphonesVolume(value) {
    headphonesVolume.value = Math.max(0, Math.min(1, value))
    document.querySelectorAll('audio[data-remote]').forEach(el => {
      el.volume = headphonesVolume.value
    })
  }

  function setUserVolume(nick, value) {
    remoteVolumes[nick] = Math.max(0, Math.min(2, value))
    const el = document.querySelector(`audio[data-remote="${nick}"]`)
    if (el) el.volume = Math.min(1, remoteVolumes[nick]) * headphonesVolume.value
  }

  // ── Switch microphone device ──────────────────────────────────────────────
  async function setMicDevice(deviceId) {
    selectedMicId.value = deviceId
    if (!localStream.value) return
    try {
      const newRaw = await navigator.mediaDevices.getUserMedia({
        audio: { deviceId: { exact: deviceId } }, video: false
      })
      rawMicStream?.getTracks().forEach(t => t.stop())
      rawMicStream = newRaw
      const source = micAudioCtx.createMediaStreamSource(newRaw)
      source.connect(micGainNode)
      const newTrack = localStream.value.getAudioTracks()[0]
      if (newTrack) {
        for (const pc of Object.values(peerConnections)) {
          const sender = pc.getSenders().find(s => s.track?.kind === 'audio')
          if (sender) await sender.replaceTrack(newTrack)
        }
      }
    } catch (e) { LOG('setMicDevice error:', e) }
  }

  // ── Switch audio output device ────────────────────────────────────────────
  async function setSpeakerDevice(deviceId) {
    selectedSpeakerId.value = deviceId
    document.querySelectorAll('audio[data-remote]').forEach(async el => {
      if (typeof el.setSinkId === 'function') {
        try { await el.setSinkId(deviceId) } catch {}
      }
    })
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
    if (screenSharing.value) {
      // During screen share, only update local track state; don't touch the video sender
      if (existing) existing.enabled = cameraEnabled.value
      return
    }
    if (!existing) {
      if (cameraEnabled.value) {
        try {
          const s = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' } })
          const track = s.getVideoTracks()[0]
          localStream.value.addTrack(track)
          for (const [nick, pc] of Object.entries(peerConnections)) {
            if (videoSenders[nick]) {
              await videoSenders[nick].replaceTrack(track)
            } else {
              const sender = pc.getSenders().find(s => s.track?.kind === 'video')
              if (sender) {
                await sender.replaceTrack(track)
                videoSenders[nick] = sender
              } else {
                videoSenders[nick] = pc.addTrack(track, localStream.value)
              }
            }
          }
        } catch { cameraEnabled.value = false }
      }
    } else {
      existing.enabled = cameraEnabled.value
      for (const [nick, pc] of Object.entries(peerConnections)) {
        if (videoSenders[nick]) {
          await videoSenders[nick].replaceTrack(cameraEnabled.value ? existing : null)
        } else {
          const sender = pc.getSenders().find(s => s.track?.kind === 'video')
          if (sender) {
            await sender.replaceTrack(cameraEnabled.value ? existing : null)
            videoSenders[nick] = sender
          }
        }
      }
    }
    broadcastVideoState(cameraEnabled.value)
  }

  function toggleHeadphones() {
    headphonesEnabled.value = !headphonesEnabled.value
    document.querySelectorAll('audio[data-remote]').forEach(el => {
      el.muted = !headphonesEnabled.value
      if (!el.muted) el.play().catch(() => {})
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
      for (const [nick, pc] of Object.entries(peerConnections)) {
        if (videoSenders[nick]) {
          await videoSenders[nick].replaceTrack(track)
        } else {
          videoSenders[nick] = pc.addTrack(track, screenStream.value)
        }
      }
      broadcastScreenState(true)
    } catch (e) { console.warn('[WebRTC] Screen share cancelled:', e) }
  }

  async function stopScreenShare() {
    screenStream.value?.getTracks().forEach(t => t.stop())
    screenStream.value  = null
    screenSharing.value = false
    const camTrack = cameraEnabled.value ? localStream.value?.getVideoTracks()[0] : null
    for (const [nick, pc] of Object.entries(peerConnections)) {
      if (videoSenders[nick]) {
        await videoSenders[nick].replaceTrack(camTrack || null)
      } else {
        const sender = pc.getSenders().find(s => s.track?.kind === 'video')
        if (sender) {
          await sender.replaceTrack(camTrack || null)
          videoSenders[nick] = sender
        }
      }
    }
    broadcastScreenState(false)
    if (cameraEnabled.value) broadcastVideoState(true)
  }

  // ── Broadcast helpers ─────────────────────────────────────────────────────
  function broadcastVideoState(on) {
    Object.keys(peerConnections).forEach(nick => {
      sendSignal({ type: 'VIDEO_STATE', toUserNickname: nick,
        fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
        roomId: currentRoomId.value, payload: on ? 'on' : 'off' })
    })
  }

  function broadcastScreenState(on) {
    Object.keys(peerConnections).forEach(nick => {
      sendSignal({ type: 'SCREEN_STATE', toUserNickname: nick,
        fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
        roomId: currentRoomId.value, payload: on ? 'on' : 'off' })
    })
  }

  // ── Teacher mute controls ─────────────────────────────────────────────────
  function sendMuteSignal(targetNickname, type) {
    sendSignal({ type, toUserNickname: targetNickname,
      fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
      roomId: currentRoomId.value, payload: null })
  }

  function handleRemoteMuteCommand(signal) {
    if (signal.type === 'MUTE_MIC') {
      micEnabled.value = false; applyMicState()
    } else if (signal.type === 'MUTE_CAM') {
      cameraEnabled.value = false
      localStream.value?.getVideoTracks().forEach(t => { t.enabled = false })
    } else if (signal.type === 'MUTE_AUDIO') {
      headphonesEnabled.value = false
      document.querySelectorAll('audio[data-remote]').forEach(el => { el.muted = true })
    }
  }

  // ── Remote speech detection ───────────────────────────────────────────────
  function startRemoteSpeechDetection(nick, stream) {
    if (speechAnalysers[nick]) {
      clearInterval(speechAnalysers[nick].interval)
      speechAnalysers[nick].ctx?.close()
    }
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext
      const ctx      = new AudioCtx()
      ctx.resume()
      const source   = ctx.createMediaStreamSource(stream)
      const analyser = ctx.createAnalyser()
      analyser.fftSize = 256
      source.connect(analyser)
      const data = new Uint8Array(analyser.frequencyBinCount)
      const interval = setInterval(() => {
        if (!remoteStreams[nick]) { clearInterval(interval); return }
        analyser.getByteFrequencyData(data)
        remoteSpeaking[nick] = (data.reduce((a, b) => a + b, 0) / data.length) > 10
      }, 150)
      speechAnalysers[nick] = { ctx, interval }
    } catch {}
  }

  // ── WebRTC peer connections ───────────────────────────────────────────────
  function createPeerConnection(peerNickname) {
    if (peerConnections[peerNickname]) return peerConnections[peerNickname]
    LOG('createPeerConnection:', peerNickname)

    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
    peerConnections[peerNickname] = pc

    // Add audio tracks from the (gain-processed) local stream
    localStream.value?.getTracks().forEach(t => {
      LOG('adding local', t.kind, 'track to new PC for', peerNickname)
      pc.addTrack(t, localStream.value)
    })

    // If screen share or camera is active, add the video track so late joiners get it
    if (screenSharing.value && screenStream.value) {
      screenStream.value.getVideoTracks().forEach(t => {
        LOG('adding active screen track to new PC for', peerNickname)
        videoSenders[peerNickname] = pc.addTrack(t, screenStream.value)
      })
    } else if (cameraEnabled.value) {
      localStream.value?.getVideoTracks().forEach(t => {
        const already = pc.getSenders().find(s => s.track?.kind === 'video')
        if (!already) videoSenders[peerNickname] = pc.addTrack(t, localStream.value)
      })
    }

    // Incoming remote tracks
    pc.ontrack = evt => {
      LOG('ontrack from', peerNickname, ':', evt.track.kind)
      if (!remoteStreams[peerNickname]) {
        remoteStreams[peerNickname] = { stream: new MediaStream(), hasVideo: false }
      }
      if (evt.track.kind === 'video') {
        // Replace any existing video track to avoid duplicates from renegotiation
        remoteStreams[peerNickname].stream.getVideoTracks().forEach(t => {
          remoteStreams[peerNickname].stream.removeTrack(t)
        })
        remoteStreams[peerNickname].stream.addTrack(evt.track)
        remoteStreams[peerNickname].hasVideo = true
        evt.track.onmute   = () => { if (remoteStreams[peerNickname]) remoteStreams[peerNickname].hasVideo = false }
        evt.track.onunmute = () => { if (remoteStreams[peerNickname]) remoteStreams[peerNickname].hasVideo = true }
        evt.track.onended  = () => { if (remoteStreams[peerNickname]) remoteStreams[peerNickname].hasVideo = false }
      } else {
        remoteStreams[peerNickname].stream.addTrack(evt.track)
      }
      if (evt.track.kind === 'audio') {
        startRemoteSpeechDetection(peerNickname, remoteStreams[peerNickname].stream)
      }
    }

    pc.onicecandidate = evt => {
      if (evt.candidate) {
        sendSignal({ type: 'ICE_CANDIDATE', toUserNickname: peerNickname,
          fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
          roomId: currentRoomId.value, payload: JSON.stringify(evt.candidate) })
      }
    }

    pc.onicegatheringstatechange  = () => LOG('ICE gathering:', pc.iceGatheringState, '←', peerNickname)
    pc.oniceconnectionstatechange = () => LOG('ICE connection:', pc.iceConnectionState, '←', peerNickname)

    pc.onnegotiationneeded = async () => {
      LOG('onnegotiationneeded for', peerNickname, 'state:', pc.signalingState)
      try {
        if (pc.signalingState !== 'stable') return
        const offer = await pc.createOffer()
        if (pc.signalingState !== 'stable') return
        await pc.setLocalDescription(offer)
        LOG('sending OFFER to', peerNickname)
        sendSignal({ type: 'OFFER', toUserNickname: peerNickname,
          fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
          roomId: currentRoomId.value, payload: JSON.stringify(offer) })
      } catch (e) { console.warn('[WebRTC] onnegotiationneeded error:', e) }
    }

    pc.onconnectionstatechange = () => {
      LOG('connection state:', pc.connectionState, '←', peerNickname)
      if (pc.connectionState === 'connected') {
        if (currentRoomId.value) rooms.refreshParticipants(currentRoomId.value)
        // Inform newly connected peer of our active media state
        if (screenSharing.value) {
          sendSignal({ type: 'SCREEN_STATE', toUserNickname: peerNickname,
            fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
            roomId: currentRoomId.value, payload: 'on' })
        }
        if (cameraEnabled.value) {
          sendSignal({ type: 'VIDEO_STATE', toUserNickname: peerNickname,
            fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
            roomId: currentRoomId.value, payload: 'on' })
        }
      }
      if (['disconnected', 'failed', 'closed'].includes(pc.connectionState)) {
        closePeerConnection(peerNickname)
      }
    }

    return pc
  }

  async function handleIncomingSignal(signal) {
    const { type, fromNickname, payload } = signal
    LOG('incoming signal:', type, 'from', fromNickname)

    if (type === 'OFFER') {
      const pc = createPeerConnection(fromNickname)
      await pc.setRemoteDescription(JSON.parse(payload))
      LOG('setRemoteDescription(offer) done for', fromNickname)
      if (pendingCandidates[fromNickname]?.length) {
        for (const c of pendingCandidates[fromNickname]) {
          try { await pc.addIceCandidate(c) } catch {}
        }
        pendingCandidates[fromNickname] = []
      }
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      LOG('sending ANSWER to', fromNickname)
      sendSignal({ type: 'ANSWER', toUserNickname: fromNickname,
        fromUserId: auth.user?.id, fromNickname: auth.user?.nickname,
        roomId: currentRoomId.value, payload: JSON.stringify(answer) })

    } else if (type === 'ANSWER') {
      const pc = peerConnections[fromNickname]
      if (pc && pc.signalingState === 'have-local-offer') {
        await pc.setRemoteDescription(JSON.parse(payload))
        LOG('setRemoteDescription(answer) done for', fromNickname)
        if (pendingCandidates[fromNickname]?.length) {
          for (const c of pendingCandidates[fromNickname]) {
            try { await pc.addIceCandidate(c) } catch {}
          }
          pendingCandidates[fromNickname] = []
        }
      } else {
        LOG('WARN: ignoring ANSWER from', fromNickname, '— state:', pc?.signalingState)
      }

    } else if (type === 'ICE_CANDIDATE') {
      const pc = peerConnections[fromNickname]
      if (!pc || !pc.remoteDescription) {
        if (!pendingCandidates[fromNickname]) pendingCandidates[fromNickname] = []
        pendingCandidates[fromNickname].push(JSON.parse(payload))
      } else {
        try { await pc.addIceCandidate(JSON.parse(payload)) } catch (e) {
          LOG('WARN: addIceCandidate failed:', e.message)
        }
      }

    } else if (type === 'VIDEO_STATE') {
      if (remoteStreams[fromNickname]) {
        remoteStreams[fromNickname].hasVideo = (payload === 'on')
      }

    } else if (type === 'SCREEN_STATE') {
      remoteScreenSharing[fromNickname] = (payload === 'on')
      if (remoteStreams[fromNickname]) {
        if (payload === 'on')  remoteStreams[fromNickname].hasVideo = true
        else                   remoteStreams[fromNickname].hasVideo = false
      }

    } else if (['MUTE_MIC', 'MUTE_CAM', 'MUTE_AUDIO'].includes(type)) {
      handleRemoteMuteCommand(signal)
    }
  }

  function closePeerConnection(nick) {
    peerConnections[nick]?.close()
    delete peerConnections[nick]
    delete remoteStreams[nick]
    delete pendingCandidates[nick]
    delete videoSenders[nick]
    if (speechAnalysers[nick]) {
      clearInterval(speechAnalysers[nick].interval)
      speechAnalysers[nick].ctx?.close()
      delete speechAnalysers[nick]
    }
    delete remoteSpeaking[nick]
    delete remoteScreenSharing[nick]
  }

  function sendSignal(signal) {
    if (!stompClient?.connected) { LOG('WARN: STOMP not connected'); return }
    stompClient.publish({
      destination: `/app/voice/${signal.roomId}/signal`,
      body: JSON.stringify(signal)
    })
  }

  return {
    connected, localStream, screenStream,
    micEnabled, cameraEnabled, headphonesEnabled, screenSharing,
    micGainValue, headphonesVolume, remoteVolumes,
    remoteStreams, remoteSpeaking, remoteScreenSharing, currentRoomId,
    audioInputDevices, audioOutputDevices, selectedMicId, selectedSpeakerId,
    connectStomp, disconnectStomp, subscribeToRoom,
    joinRoom, leaveRoom,
    toggleMic, toggleCamera, toggleHeadphones,
    startScreenShare, stopScreenShare,
    sendMuteSignal,
    setMicGain, setHeadphonesVolume, setUserVolume,
    setMicDevice, setSpeakerDevice, enumerateDevices
  }
})
