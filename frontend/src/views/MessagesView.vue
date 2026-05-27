<template>
  <div class="messages-layout">
    <!-- Contacts sidebar -->
    <div class="msg-sidebar">
      <div class="msg-sidebar__header">
        <h3>Сообщения</h3>
      </div>

      <div class="msg-contacts">
        <div v-for="contact in contacts" :key="contact.id"
          class="msg-contact" :class="{ active: selectedContact?.id === contact.id }"
          @click="selectContact(contact)">
          <div class="msg-contact__avatar">{{ initials(contact.fullName) }}</div>
          <div class="msg-contact__info">
            <div class="msg-contact__name">{{ contact.fullName }}</div>
            <div class="msg-contact__nick text-sm text-muted">@{{ contact.nickname }}</div>
          </div>
        </div>

        <div v-if="contacts.length === 0" class="text-muted text-sm" style="padding:16px">
          Нет контактов
        </div>
      </div>

      <div class="msg-sidebar__footer">
        <div class="form-group" style="margin:0">
          <input v-model="newContactSearch" type="text" class="form-control"
            placeholder="Найти пользователя..." @input="searchContacts" />
        </div>
        <div v-if="searchedUsers.length" class="search-dropdown">
          <div v-for="u in searchedUsers" :key="u.id" class="search-dropdown__item"
            @click="selectContact(u)">
            {{ u.fullName }} (@{{ u.nickname }})
          </div>
        </div>
      </div>
    </div>

    <!-- Chat area -->
    <div class="msg-chat">
      <div v-if="!selectedContact" class="msg-chat__empty">
        <div style="font-size:48px">💬</div>
        <p>Выберите контакт для общения</p>
      </div>

      <template v-else>
        <div class="msg-chat__header">
          <div class="member-item__avatar">{{ initials(selectedContact.fullName) }}</div>
          <div>
            <div class="font-semibold">{{ selectedContact.fullName }}</div>
            <div class="text-sm text-muted">@{{ selectedContact.nickname }}</div>
          </div>
        </div>

        <div class="msg-chat__messages" ref="chatRef">
          <div v-for="msg in conversation" :key="msg.id" class="chat-msg"
            :class="{ 'chat-msg--mine': msg.sender?.id === auth.user?.id }">
            <div class="chat-msg__bubble">{{ msg.content }}</div>
            <div class="chat-msg__time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>

        <div class="msg-chat__input">
          <input v-model="newMsg" type="text" class="form-control"
            placeholder="Введите сообщение..." @keydown.enter="sendMessage" />
          <button class="btn btn--primary" @click="sendMessage">➤</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { messageApi, userApi } from '@/api'

const auth = useAuthStore()
const toast = useToastStore()
const contacts = ref([])
const selectedContact = ref(null)
const conversation = ref([])
const newMsg = ref('')
const chatRef = ref(null)
const newContactSearch = ref('')
const searchedUsers = ref([])

onMounted(async () => {
  try {
    const { data } = await messageApi.getContacts()
    contacts.value = data
  } catch {}
})

async function selectContact(contact) {
  selectedContact.value = contact
  newContactSearch.value = ''
  searchedUsers.value = []
  if (!contacts.value.find(c => c.id === contact.id)) {
    contacts.value.unshift(contact)
  }
  await loadConversation(contact.id)
}

async function loadConversation(userId) {
  try {
    const { data } = await messageApi.getPrivate(userId)
    conversation.value = data
    await nextTick()
    scrollChat()
  } catch {}
}

async function sendMessage() {
  if (!newMsg.value.trim() || !selectedContact.value) return
  try {
    const { data } = await messageApi.send({ receiverId: selectedContact.value.id, content: newMsg.value })
    conversation.value.push(data)
    newMsg.value = ''
    await nextTick()
    scrollChat()
  } catch { toast.error('Ошибка отправки') }
}

async function searchContacts() {
  if (newContactSearch.value.length < 2) { searchedUsers.value = []; return }
  try {
    const { data } = await userApi.searchStudents(newContactSearch.value)
    searchedUsers.value = data.filter(u => u.id !== auth.user?.id).slice(0, 5)
  } catch {}
}

function scrollChat() {
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}

function initials(name) {
  return name?.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'
}

function formatTime(dt) {
  return new Date(dt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.messages-layout {
  display: flex;
  height: calc(100vh - 128px);
  background: white;
  border-radius: $border-radius;
  border: 1px solid $border;
  overflow: hidden;

  @media (max-width: $mobile) {
    flex-direction: column;
    height: auto;
  }
}

.msg-sidebar {
  width: 300px;
  border-right: 1px solid $border;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  @media (max-width: $mobile) {
    width: 100%;
    max-height: 200px;
  }

  &__header {
    padding: 16px 20px;
    border-bottom: 1px solid $border;
    font-size: $font-size-lg;
    font-weight: 600;
  }

  &__footer {
    padding: 12px 16px;
    border-top: 1px solid $border;
    position: relative;
  }
}

.msg-contacts {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.msg-contact {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: $transition;

  &:hover { background: $bg; }
  &.active { background: $primary-light; }

  &__avatar {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    background: $primary;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: $font-size-sm;
    font-weight: 600;
    flex-shrink: 0;
  }

  &__info { flex: 1; min-width: 0; }
  &__name { font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
}

.search-dropdown {
  position: absolute;
  bottom: 100%;
  left: 16px;
  right: 16px;
  background: white;
  border: 1px solid $border;
  border-radius: $border-radius-sm;
  box-shadow: $shadow;
  z-index: 10;

  &__item {
    padding: 10px 14px;
    cursor: pointer;
    font-size: $font-size-sm;
    transition: $transition;

    &:hover { background: $bg; }
  }
}

.msg-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;

  &__empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: $text-muted;
    gap: 12px;
  }

  &__header {
    padding: 14px 20px;
    border-bottom: 1px solid $border;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  &__input {
    padding: 14px 20px;
    border-top: 1px solid $border;
    display: flex;
    gap: 10px;
  }
}
</style>
