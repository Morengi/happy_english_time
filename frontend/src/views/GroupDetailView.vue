<template>
  <div v-if="group">
    <div class="page-header">
      <div>
        <h1>{{ group.name }}</h1>
        <p class="text-muted text-sm">Преподаватель: {{ group.teacher?.fullName }}</p>
      </div>
      <div class="flex gap-8">
        <button v-if="canManage" class="btn btn--secondary btn--sm" @click="showAddWord = true">+ Слово для группы</button>
        <button v-if="canManage" class="btn btn--secondary btn--sm" @click="showAddMember = true">+ Добавить участника</button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button v-for="tab in tabs" :key="tab.id" class="tab" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
        {{ tab.label }}
      </button>
    </div>

    <!-- Members tab -->
    <div v-if="activeTab === 'members'" class="card" style="margin-top:20px">

      <!-- Teacher block -->
      <div v-if="group.teacher" class="teacher-block">
        <UserAvatar :user="group.teacher" :size="40" />
        <div class="member-item__info">
          <div class="member-item__name">{{ group.teacher.fullName }}</div>
          <div class="member-item__nick text-muted text-sm">@{{ group.teacher.nickname }}</div>
        </div>
        <span class="role-badge">Преподаватель</span>
      </div>

      <div class="members-divider">
        <span>Студенты ({{ group.members?.length || 0 }})</span>
      </div>

      <div class="members-list">
        <div v-for="member in group.members" :key="member.id" class="member-item">
          <UserAvatar :user="member" :size="36" />
          <div class="member-item__info">
            <div class="member-item__name">{{ member.fullName }}</div>
            <div class="member-item__nick text-muted text-sm">@{{ member.nickname }}</div>
          </div>
          <button v-if="canManage" class="btn btn--ghost btn--sm" @click="removeMember(member.id)" title="Удалить">🗑️</button>
        </div>
        <div v-if="!group.members?.length" class="text-muted text-sm" style="padding:12px 10px">
          Участников пока нет
        </div>
      </div>
    </div>

    <!-- Chat tab -->
    <div v-if="activeTab === 'chat'" class="card chat-card" style="margin-top:20px">
      <div class="chat-messages" ref="chatRef">
        <div v-if="messages.length === 0" class="empty-state" style="padding:30px 0">
          <div class="empty-state__desc">Пока нет сообщений</div>
        </div>
        <div v-for="msg in messages" :key="msg.id" class="chat-msg"
          :class="{ 'chat-msg--mine': msg.sender.id === auth.user?.id }">
          <div class="chat-msg__author">{{ msg.sender.fullName }}</div>
          <div class="chat-msg__bubble">{{ msg.content }}</div>
          <div class="chat-msg__time">{{ formatTime(msg.createdAt) }}</div>
        </div>
      </div>
      <div class="chat-input">
        <input v-model="newMessage" type="text" class="form-control"
          placeholder="Введите сообщение..." @keydown.enter="sendMessage" />
        <button class="btn btn--primary" @click="sendMessage">Отправить</button>
      </div>
    </div>

    <!-- Ranking tab -->
    <div v-if="activeTab === 'ranking'" class="card" style="margin-top:20px">
      <h3 style="margin-bottom:16px">Рейтинг группы</h3>
      <div v-if="ranking.length === 0" class="empty-state" style="padding:20px 0">
        <div class="empty-state__desc">Рейтинг формируется после прохождения тестов</div>
      </div>
      <div v-else class="ranking-list">
        <div v-for="r in ranking" :key="r.userId" class="ranking-item">
          <div class="ranking-item__place" :class="`place-${r.rank}`">{{ r.rank }}</div>
          <div class="ranking-item__info">
            <div class="ranking-item__name">{{ r.fullName }}</div>
            <div class="text-sm text-muted">@{{ r.nickname }}</div>
          </div>
          <div class="ranking-item__score">
            <div class="ranking-bar">
              <div class="ranking-bar__fill" :style="{ width: r.avgScore + '%' }"></div>
            </div>
            <span>{{ r.avgScore }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Add member modal -->
    <div v-if="showAddMember" class="modal-overlay" @click.self="showAddMember = false">
      <div class="modal">
        <div class="modal__header">
          <h3>Добавить участника</h3>
          <button class="btn btn--ghost btn--icon" @click="showAddMember = false">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>Поиск по ФИО или никнейму</label>
            <input v-model="memberSearch" type="text" class="form-control" placeholder="Введите имя или @ник"
              @input="searchUsers" />
          </div>
          <div class="search-results">
            <div v-for="u in searchResults" :key="u.id" class="search-result-item" @click="addMember(u.nickname)">
              <UserAvatar :user="u" :size="32" />
              <div>
                <div>{{ u.fullName }}</div>
                <div class="text-sm text-muted">@{{ u.nickname }}</div>
              </div>
            </div>
          </div>
          <div class="form-group" style="margin-top:16px">
            <label>Или введите никнейм напрямую</label>
            <div class="flex gap-8">
              <input v-model="directNickname" type="text" class="form-control" placeholder="@nickname" />
              <button class="btn btn--primary" @click="addMember(directNickname)">Добавить</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add word modal -->
    <div v-if="showAddWord" class="modal-overlay" @click.self="showAddWord = false">
      <div class="modal">
        <div class="modal__header">
          <h3>Добавить слово для группы</h3>
          <button class="btn btn--ghost btn--icon" @click="showAddWord = false">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>Английское слово</label>
            <input v-model="wordForm.englishWord" type="text" class="form-control" />
          </div>
          <div class="form-group">
            <label>Перевод</label>
            <input v-model="wordForm.russianTranslation" type="text" class="form-control" />
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="showAddWord = false">Отмена</button>
          <button class="btn btn--primary" @click="addWord">Добавить</button>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="loading-center"><div class="spinner"></div></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useChatStore } from '@/stores/chat'
import { groupApi, userApi, messageApi } from '@/api'
import UserAvatar from '@/components/common/UserAvatar.vue'

const route = useRoute()
const auth = useAuthStore()
const toast = useToastStore()
const chat = useChatStore()

const group = ref(null)
const messages = ref([])
const ranking = ref([])
const activeTab = ref('members')
const newMessage = ref('')
const chatRef = ref(null)
const showAddMember = ref(false)
const showAddWord = ref(false)
const memberSearch = ref('')
const directNickname = ref('')
const searchResults = ref([])
const wordForm = ref({ englishWord: '', russianTranslation: '' })

const tabs = [
  { id: 'members', label: '👥 Участники' },
  { id: 'chat', label: '💬 Чат группы' },
  { id: 'ranking', label: '🏆 Рейтинг' }
]

const canManage = ref(false)

let unsubscribeGroup = null

onMounted(async () => {
  await loadGroup()
  await Promise.all([loadMessages(), loadRanking()])

  // Subscribe to real-time group chat messages via WebSocket
  unsubscribeGroup = chat.subscribeToGroup(route.params.id, (msg) => {
    if (!messages.value.find(m => m.id === msg.id)) {
      messages.value.push(msg)
      nextTick().then(scrollChat)
    }
  })
})

onUnmounted(() => {
  if (unsubscribeGroup) unsubscribeGroup()
})

async function loadGroup() {
  const { data } = await groupApi.getById(route.params.id)
  group.value = data
  canManage.value = auth.isAdmin ||
    (auth.isTeacher && data.teacher?.id === auth.user?.id)
}

async function loadMessages() {
  try {
    const { data } = await groupApi.getMessages(route.params.id)
    messages.value = data
    await nextTick()
    scrollChat()
  } catch {}
}

async function loadRanking() {
  try {
    const { data } = await groupApi.getRanking(route.params.id)
    ranking.value = data
  } catch {}
}

function scrollChat() {
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}

async function sendMessage() {
  if (!newMessage.value.trim()) return
  try {
    await messageApi.send({ groupId: parseInt(route.params.id), content: newMessage.value })
    // Don't push locally — message arrives back via WebSocket to avoid duplicates
    newMessage.value = ''
  } catch { toast.error('Ошибка отправки') }
}

async function removeMember(userId) {
  if (!confirm('Удалить участника?')) return
  try {
    const { data } = await groupApi.removeMember(route.params.id, userId)
    group.value = data
    toast.success('Участник удалён')
  } catch { toast.error('Ошибка') }
}

async function searchUsers() {
  if (memberSearch.value.length < 2) { searchResults.value = []; return }
  try {
    const { data } = await userApi.searchStudents(memberSearch.value)
    searchResults.value = data.filter(u => !group.value.members.some(m => m.id === u.id))
  } catch {}
}

async function addMember(nickname) {
  if (!nickname?.trim()) return
  try {
    const { data } = await groupApi.addMember(route.params.id, nickname.trim().replace('@', ''))
    group.value = data
    showAddMember.value = false
    memberSearch.value = ''
    directNickname.value = ''
    searchResults.value = []
    toast.success('Участник добавлен')
  } catch (e) { toast.error(e.response?.data?.message || 'Ошибка') }
}

async function addWord() {
  if (!wordForm.value.englishWord || !wordForm.value.russianTranslation) return
  try {
    await groupApi.addWord(route.params.id, wordForm.value)
    showAddWord.value = false
    wordForm.value = { englishWord: '', russianTranslation: '' }
    toast.success('Слово добавлено для всей группы')
  } catch (e) { toast.error(e.response?.data?.message || 'Ошибка') }
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

.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 2px solid $border;
  padding-bottom: 0;
}

.tab {
  padding: 10px 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: $font-size-base;
  font-weight: 500;
  color: $text-muted;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: $transition;
  font-family: $font-family;

  &.active { color: $primary; border-bottom-color: $primary; }
  &:hover:not(.active) { color: $text; }
}

.teacher-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: $primary-light;
  border-radius: $border-radius-sm;
  margin-bottom: 4px;
}

.role-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  background: $primary;
  color: white;
  white-space: nowrap;
  flex-shrink: 0;
}

.members-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 10px 8px;
  font-size: $font-size-sm;
  font-weight: 600;
  color: $text-muted;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.members-list { display: flex; flex-direction: column; gap: 4px; }

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: $border-radius-sm;
  transition: $transition;

  &:hover { background: $bg; }

  &__avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: $primary;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: $font-size-sm;
    flex-shrink: 0;

    &.small { width: 32px; height: 32px; font-size: 11px; }
  }

  &__info { flex: 1; }
  &__name { font-weight: 500; }
}

.chat-card {
  display: flex;
  flex-direction: column;
  height: 500px;
  padding: 0;

  @media (max-width: $mobile) { height: 70vh; }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-msg {
  display: flex;
  flex-direction: column;
  max-width: 70%;

  &--mine { align-self: flex-end; align-items: flex-end; }
  &:not(&--mine) { align-self: flex-start; }

  &__author { font-size: $font-size-sm; color: $text-muted; margin-bottom: 4px; }
  &__bubble {
    padding: 10px 14px;
    border-radius: 16px;
    font-size: $font-size-base;
    background: $bg;
    border: 1px solid $border;

    .chat-msg--mine & { background: $primary; color: white; border-color: $primary; }
  }

  &__time { font-size: 11px; color: $text-muted; margin-top: 4px; }
}

.chat-input {
  padding: 16px;
  border-top: 1px solid $border;
  display: flex;
  gap: 10px;
}

.ranking-list { display: flex; flex-direction: column; gap: 10px; }

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: $border-radius-sm;
  background: $bg;

  &__place {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: $secondary;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: $font-size-sm;
    flex-shrink: 0;

    &.place-1 { background: #FFD700; }
    &.place-2 { background: #C0C0C0; }
    &.place-3 { background: #CD7F32; }
  }

  &__info { flex: 1; }
  &__name { font-weight: 500; }
  &__score {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 140px;
    font-weight: 600;
  }
}

.ranking-bar {
  flex: 1;
  height: 8px;
  background: $border;
  border-radius: 4px;
  overflow: hidden;

  &__fill {
    height: 100%;
    background: linear-gradient(90deg, $primary, $primary-dark);
    border-radius: 4px;
    transition: width 0.5s ease;
  }
}

.search-results { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; }

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: $border-radius-sm;
  cursor: pointer;
  transition: $transition;

  &:hover { background: $bg; }
}
</style>
