<template>
  <!-- messages-layout--in-chat switches panels on mobile -->
  <div class="messages-layout" :class="{ 'messages-layout--in-chat': selectedContact }">

    <!-- ═══ CONTACTS SIDEBAR ═══════════════════════════════════════════════════ -->
    <div class="msg-sidebar">
      <div class="msg-sidebar__header">
        <h3>Сообщения</h3>
      </div>

      <div class="msg-sidebar__search">
        <input v-model="newContactSearch" type="text" class="form-control"
          placeholder="🔍  Найти пользователя..." @input="searchContacts" />
      </div>

      <!-- Search results (replaces contacts list while typing) -->
      <div v-if="newContactSearch.length >= 2" class="msg-contacts">
        <div v-if="searchedUsers.length === 0" class="msg-contacts__empty">
          Никого не найдено
        </div>
        <div v-for="u in searchedUsers" :key="u.id"
          class="msg-contact" @click="selectContact(u)">
          <UserAvatar :user="u" :size="44" class="msg-contact__avatar" />
          <div class="msg-contact__info">
            <div class="msg-contact__name">{{ u.fullName }}</div>
            <div class="msg-contact__sub">@{{ u.nickname }}</div>
          </div>
        </div>
      </div>

      <!-- Regular contacts list -->
      <div v-else class="msg-contacts">
        <div v-if="contacts.length === 0" class="msg-contacts__empty">
          Начните диалог — найдите пользователя выше
        </div>
        <div v-for="contact in contacts" :key="contact.id"
          class="msg-contact" :class="{ active: selectedContact?.id === contact.id }"
          @click="selectContact(contact)">
          <UserAvatar :user="contact" :size="44" class="msg-contact__avatar" />
          <div class="msg-contact__info">
            <div class="msg-contact__name">{{ contact.fullName }}</div>
            <div class="msg-contact__sub">@{{ contact.nickname }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ CHAT PANEL ════════════════════════════════════════════════════════ -->
    <div class="msg-chat">

      <!-- Empty state (desktop only — on mobile sidebar is shown instead) -->
      <div v-if="!selectedContact" class="msg-chat__empty">
        <div class="msg-chat__empty-icon">💬</div>
        <p>Выберите контакт для общения</p>
      </div>

      <template v-else>
        <!-- Chat header -->
        <div class="msg-chat__header">
          <button class="msg-chat__back" @click="closeChat" aria-label="Назад">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6"/>
            </svg>
          </button>
          <UserAvatar :user="selectedContact" :size="40" />
          <div class="msg-chat__header-info">
            <div class="msg-chat__header-name">{{ selectedContact.fullName }}</div>
            <div class="msg-chat__header-nick">@{{ selectedContact.nickname }}</div>
          </div>
        </div>

        <!-- Messages -->
        <div class="msg-chat__messages" ref="chatRef">
          <div v-if="conversation.length === 0" class="msg-chat__no-messages">
            Напишите первое сообщение 👋
          </div>
          <div v-for="msg in conversation" :key="msg.id"
            class="chat-msg" :class="{ 'chat-msg--mine': msg.sender?.id === auth.user?.id }">
            <div class="chat-msg__bubble">{{ msg.content }}</div>
            <div class="chat-msg__time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>

        <!-- Input bar -->
        <div class="msg-chat__input">
          <input v-model="newMsg" type="text" class="form-control"
            placeholder="Введите сообщение..." @keydown.enter="sendMessage" />
          <button class="msg-send-btn" @click="sendMessage" :disabled="!newMsg.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useChatStore } from '@/stores/chat'
import { messageApi, userApi } from '@/api'
import UserAvatar from '@/components/common/UserAvatar.vue'

const auth = useAuthStore()
const toast = useToastStore()
const chat = useChatStore()

const contacts = ref([])
const selectedContact = ref(null)
const conversation = ref([])
const newMsg = ref('')
const chatRef = ref(null)
const newContactSearch = ref('')
const searchedUsers = ref([])

// ─── WebSocket: receive incoming messages in real-time ───────────────────────
const unsubscribe = chat.onMessage((msg) => {
  const myId = auth.user?.id
  const partnerId = selectedContact.value?.id

  // Add to open conversation if it belongs there (avoid duplicates)
  if (partnerId) {
    const fromPartner = msg.sender?.id === partnerId
    const toPartner   = msg.receiverId === partnerId && msg.sender?.id === myId
    if (fromPartner || toPartner) {
      if (!conversation.value.find(m => m.id === msg.id)) {
        conversation.value.push(msg)
        nextTick().then(scrollChat)
      }
      return
    }
  }

  // Message from someone not in current view → add/move sender to top of contacts
  const senderId = msg.sender?.id
  if (senderId && senderId !== myId) {
    const existing = contacts.value.find(c => c.id === senderId)
    if (existing) {
      contacts.value = [existing, ...contacts.value.filter(c => c.id !== senderId)]
    } else if (msg.sender) {
      contacts.value.unshift(msg.sender)
    }
  }
})

onUnmounted(unsubscribe)

// ─── Load contacts on mount ───────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { data } = await messageApi.getContacts()
    contacts.value = data
  } catch {}
})

// ─── Select contact and open conversation ─────────────────────────────────────
async function selectContact(contact) {
  selectedContact.value = contact
  newContactSearch.value = ''
  searchedUsers.value = []
  if (!contacts.value.find(c => c.id === contact.id)) {
    contacts.value.unshift(contact)
  }
  await loadConversation(contact.id)
}

// Close chat → return to contacts (mobile)
function closeChat() {
  selectedContact.value = null
  conversation.value = []
}

async function loadConversation(userId) {
  try {
    const { data } = await messageApi.getPrivate(userId)
    conversation.value = data
    await nextTick()
    scrollChat()
    chat.fetchUnread()
  } catch {}
}

// ─── Send message ─────────────────────────────────────────────────────────────
async function sendMessage() {
  if (!newMsg.value.trim() || !selectedContact.value) return
  try {
    await messageApi.send({ receiverId: selectedContact.value.id, content: newMsg.value })
    newMsg.value = ''
  } catch { toast.error('Ошибка отправки') }
}

// ─── Search users ─────────────────────────────────────────────────────────────
async function searchContacts() {
  if (newContactSearch.value.length < 2) { searchedUsers.value = []; return }
  try {
    const { data } = await userApi.searchAll(newContactSearch.value)
    searchedUsers.value = data.filter(u => u.id !== auth.user?.id).slice(0, 8)
  } catch {}
}

function scrollChat() {
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}

function formatTime(dt) {
  return new Date(dt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

// ─── Layout container ─────────────────────────────────────────────────────────
.messages-layout {
  display: flex;
  height: calc(100vh - 128px);
  background: white;
  border-radius: $border-radius;
  border: 1px solid $border;
  overflow: hidden;

  @media (max-width: $mobile) {
    // Break out of page-content padding to use full width
    margin: -16px;
    border-radius: 0;
    border-left: none;
    border-right: none;
    border-bottom: none;
    // Full height: 100dvh minus topbar (64px) and top page padding (16px)
    height: calc(100dvh - 80px);
    position: relative;
  }
}

// ─── Sidebar ──────────────────────────────────────────────────────────────────
.msg-sidebar {
  width: 300px;
  border-right: 1px solid $border;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  background: white;

  @media (max-width: $mobile) {
    position: absolute;
    inset: 0;
    width: 100%;
    border-right: none;
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 1;
  }

  &__header {
    padding: 16px 20px;
    border-bottom: 1px solid $border;

    h3 {
      font-size: $font-size-lg;
      font-weight: 700;
      color: $text;
      margin: 0;
    }
  }

  &__search {
    padding: 12px 16px;
    border-bottom: 1px solid $border;

    .form-control {
      width: 100%;
      border-radius: 24px;
      background: $bg;
      border-color: transparent;
      font-size: $font-size-sm;

      &:focus {
        background: white;
        border-color: $primary;
      }
    }
  }
}

// Contacts list
.msg-contacts {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.msg-contacts__empty {
  padding: 24px 20px;
  font-size: $font-size-sm;
  color: $text-muted;
  text-align: center;
  line-height: 1.5;
}

.msg-contact {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: $bg; }
  &.active { background: $primary-light; }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-weight: 600;
    font-size: $font-size-base;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: $text;
  }

  &__sub {
    font-size: $font-size-sm;
    color: $text-muted;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// ─── Chat panel ───────────────────────────────────────────────────────────────
.msg-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: white;

  @media (max-width: $mobile) {
    position: absolute;
    inset: 0;
    transform: translateX(100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 2;
  }

  &__empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: $text-muted;
    gap: 12px;

    // Hidden on mobile (sidebar is shown instead)
    @media (max-width: $mobile) { display: none; }
  }

  &__empty-icon { font-size: 52px; }

  &__header {
    padding: 12px 16px;
    border-bottom: 1px solid $border;
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
    background: white;
  }

  &__header-info { flex: 1; min-width: 0; }
  &__header-name {
    font-weight: 600;
    font-size: $font-size-base;
    color: $text;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__header-nick {
    font-size: $font-size-sm;
    color: $text-muted;
  }

  &__messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    background: $bg;

    @media (max-width: $mobile) {
      padding: 12px;
    }
  }

  &__no-messages {
    text-align: center;
    color: $text-muted;
    font-size: $font-size-sm;
    margin: auto;
    padding: 40px 20px;
  }

  &__input {
    padding: 12px 16px;
    border-top: 1px solid $border;
    display: flex;
    gap: 8px;
    align-items: center;
    flex-shrink: 0;
    background: white;

    .form-control {
      flex: 1;
      border-radius: 24px;
      background: $bg;
      border-color: transparent;
      font-size: $font-size-base;

      &:focus {
        background: white;
        border-color: $primary;
      }
    }
  }

  // Back button — only visible on mobile
  &__back {
    display: none;
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    border: none;
    background: none;
    cursor: pointer;
    border-radius: 50%;
    color: $text;
    align-items: center;
    justify-content: center;
    transition: background 0.15s;
    padding: 0;

    &:hover { background: $bg; }

    @media (max-width: $mobile) {
      display: flex;
    }
  }
}

// ─── Mobile: active chat — slide panels ──────────────────────────────────────
.messages-layout--in-chat {
  @media (max-width: $mobile) {
    // Sidebar slides out to the left
    .msg-sidebar {
      transform: translateX(-100%);
    }
    // Chat slides in from the right
    .msg-chat {
      transform: translateX(0);
    }
  }
}

// ─── Send button ──────────────────────────────────────────────────────────────
.msg-send-btn {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  background: $primary;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, opacity 0.15s;

  &:hover:not(:disabled) { background: $primary-dark; }
  &:disabled { opacity: 0.4; cursor: default; }
}

// ─── Chat message bubbles ─────────────────────────────────────────────────────
.chat-msg {
  display: flex;
  flex-direction: column;
  max-width: 75%;

  &--mine {
    align-self: flex-end;
    align-items: flex-end;
  }
  &:not(&--mine) {
    align-self: flex-start;
    align-items: flex-start;
  }

  &__bubble {
    padding: 10px 14px;
    border-radius: 18px;
    font-size: $font-size-base;
    line-height: 1.45;
    background: white;
    border: 1px solid $border;
    word-break: break-word;

    .chat-msg--mine & {
      background: $primary;
      color: white;
      border-color: $primary;
      border-bottom-right-radius: 4px;
    }

    .chat-msg:not(.chat-msg--mine) & {
      border-bottom-left-radius: 4px;
    }
  }

  &__time {
    font-size: 11px;
    color: $text-muted;
    margin-top: 3px;
    padding: 0 4px;
  }
}
</style>
