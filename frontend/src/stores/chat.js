import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { messageApi } from '@/api'

export const useChatStore = defineStore('chat', () => {
  const unreadCount = ref(0)
  const unreadBySender = reactive({})   // senderId (number) → unread count
  const connected = ref(false)

  let stompClient = null
  const messageHandlers = []        // handlers for personal /user/queue/messages
  const pendingGroupSubs = []       // { topic, handler } waiting for connection

  // ─── Unread count ────────────────────────────────────────────────────────────

  async function fetchUnread() {
    try {
      const { data } = await messageApi.getUnread()
      unreadCount.value = data.count
    } catch {}
  }

  async function fetchUnreadBySender() {
    try {
      const { data } = await messageApi.getUnreadBySender()
      // Clear and repopulate (keys are strings from JSON, convert to numbers)
      Object.keys(unreadBySender).forEach(k => delete unreadBySender[k])
      Object.entries(data).forEach(([k, v]) => { unreadBySender[Number(k)] = v })
    } catch {}
  }

  function markContactRead(contactId) {
    delete unreadBySender[contactId]
  }

  // ─── WebSocket connection ─────────────────────────────────────────────────────

  function connect(token) {
    if (stompClient?.active) return

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,

      onConnect: () => {
        connected.value = true

        // Personal messages queue
        stompClient.subscribe('/user/queue/messages', (frame) => {
          const msg = JSON.parse(frame.body)
          messageHandlers.forEach(h => h(msg))
          fetchUnread()
          fetchUnreadBySender()
        })

        // Flush any group subscriptions requested before connection was ready
        pendingGroupSubs.forEach(({ topic, resolve }) => {
          const sub = stompClient.subscribe(topic, (frame) => {
            resolve.handler(JSON.parse(frame.body))
          })
          resolve.setSub(sub)
        })
        pendingGroupSubs.length = 0
      },

      onDisconnect: () => { connected.value = false },
      onStompError: (frame) => {
        console.error('WebSocket STOMP error:', frame.headers?.message)
      }
    })

    stompClient.activate()
  }

  function disconnect() {
    if (stompClient?.active) stompClient.deactivate()
    stompClient = null
    connected.value = false
    messageHandlers.length = 0
    pendingGroupSubs.length = 0
    unreadCount.value = 0
  }

  // ─── Personal message handlers ───────────────────────────────────────────────

  function onMessage(handler) {
    messageHandlers.push(handler)
    return () => {
      const idx = messageHandlers.indexOf(handler)
      if (idx > -1) messageHandlers.splice(idx, 1)
    }
  }

  // ─── Group topic subscription ─────────────────────────────────────────────

  /**
   * Subscribe to /topic/group/{groupId}.
   * Works whether called before or after WS connects.
   * Returns an unsubscribe function — call it in onUnmounted.
   */
  function subscribeToGroup(groupId, handler) {
    const topic = `/topic/group/${groupId}`
    let stompSub = null

    if (connected.value && stompClient?.active) {
      // Already connected — subscribe right away
      stompSub = stompClient.subscribe(topic, (frame) => {
        handler(JSON.parse(frame.body))
      })
    } else {
      // Queue until onConnect fires
      pendingGroupSubs.push({
        topic,
        resolve: {
          handler,
          setSub: (sub) => { stompSub = sub }
        }
      })
    }

    return () => { if (stompSub) stompSub.unsubscribe() }
  }

  return {
    unreadCount, unreadBySender, connected,
    connect, disconnect, onMessage, subscribeToGroup,
    fetchUnread, fetchUnreadBySender, markContactRead
  }
})
