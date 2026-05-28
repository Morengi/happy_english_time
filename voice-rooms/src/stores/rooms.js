import { defineStore } from 'pinia'
import { ref } from 'vue'
import { roomsApi } from '@/api'

export const useRoomsStore = defineStore('rooms', () => {
  const rooms       = ref([])
  const currentRoom = ref(null)
  const participants = ref([])
  const loading     = ref(false)
  const error       = ref(null)

  async function fetchRooms() {
    loading.value = true
    error.value   = null
    try {
      const { data } = await roomsApi.list()
      rooms.value = data
    } catch (e) {
      error.value = e?.response?.data?.message || 'Не удалось загрузить комнаты'
    } finally {
      loading.value = false
    }
  }

  async function createRoom(name, maxParticipants) {
    const payload = { name }
    if (maxParticipants != null) payload.maxParticipants = maxParticipants
    const { data } = await roomsApi.create(payload)
    rooms.value.push(data)
    return data
  }

  async function joinRoom(roomId) {
    const { data } = await roomsApi.join(roomId)
    participants.value = data
    const room = rooms.value.find(r => r.id === roomId)
    if (room) currentRoom.value = room
    return data
  }

  async function leaveRoom(roomId) {
    await roomsApi.leave(roomId)
    currentRoom.value  = null
    participants.value = []
  }

  async function endRoom(roomId) {
    await roomsApi.end(roomId)
    rooms.value = rooms.value.filter(r => r.id !== roomId)
    if (currentRoom.value?.id === roomId) {
      currentRoom.value  = null
      participants.value = []
    }
  }

  /** Called via STOMP when a remote JOIN/LEAVE event arrives */
  function handleParticipantEvent(event) {
    if (event.type === 'JOIN') {
      const exists = participants.value.find(p => p.userId === event.userId)
      if (!exists) {
        participants.value.push({
          userId:      event.userId,
          nickname:    event.userNickname,
          fullName:    event.userFullName,
          avatarUrl:   event.userAvatarUrl,
          joinedAt:    new Date().toISOString()
        })
      }
      // Update room participant count in list
      const room = rooms.value.find(r => r.id === event.roomId)
      if (room) room.participantCount++
    } else if (event.type === 'LEAVE') {
      participants.value = participants.value.filter(p => p.userId !== event.userId)
      const room = rooms.value.find(r => r.id === event.roomId)
      if (room && room.participantCount > 0) room.participantCount--
    } else if (event.type === 'ROOM_ENDED') {
      rooms.value = rooms.value.filter(r => r.id !== event.roomId)
      if (currentRoom.value?.id === event.roomId) {
        currentRoom.value  = null
        participants.value = []
      }
    }
  }

  /** Called when a new room is broadcast via /topic/voice-rooms/list */
  function handleNewRoom(room) {
    const exists = rooms.value.find(r => r.id === room.id)
    if (!exists) rooms.value.push(room)
  }

  return {
    rooms, currentRoom, participants, loading, error,
    fetchRooms, createRoom, joinRoom, leaveRoom, endRoom,
    handleParticipantEvent, handleNewRoom
  }
})
