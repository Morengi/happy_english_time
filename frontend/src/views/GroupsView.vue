<template>
  <div>
    <div class="page-header">
      <h1>Группы</h1>
      <button v-if="auth.isTeacher" class="btn btn--primary" @click="showCreate = true">+ Создать группу</button>
    </div>

    <div v-if="loading" class="loading-center"><div class="spinner"></div></div>

    <div v-else-if="groups.length === 0" class="empty-state">
      <div class="empty-state__icon">👥</div>
      <div class="empty-state__title">Групп нет</div>
      <div class="empty-state__desc">
        {{ auth.isTeacher ? 'Создайте первую группу' : 'Вы пока не состоите ни в одной группе' }}
      </div>
    </div>

    <div v-else class="grid grid--3">
      <RouterLink v-for="group in groups" :key="group.id"
        :to="`/groups/${group.id}`" class="card card--hover group-card" style="text-decoration:none">
        <div class="group-card__icon">👥</div>
        <div class="group-card__name">{{ group.name }}</div>
        <div class="group-card__meta">
          <span class="text-sm text-muted">Преподаватель: {{ group.teacher.fullName }}</span>
          <span class="badge badge--secondary">{{ group.memberCount }} уч.</span>
        </div>
      </RouterLink>
    </div>

    <!-- Create modal -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal__header">
          <h3>Создать группу</h3>
          <button class="btn btn--ghost btn--icon" @click="showCreate = false">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>Название группы</label>
            <input v-model="groupName" type="text" class="form-control" placeholder="Группа A1" />
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="showCreate = false">Отмена</button>
          <button class="btn btn--primary" @click="createGroup" :disabled="!groupName.trim()">Создать</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { groupApi } from '@/api'

const auth = useAuthStore()
const toast = useToastStore()
const groups = ref([])
const loading = ref(true)
const showCreate = ref(false)
const groupName = ref('')

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { data } = await groupApi.getAll()
    groups.value = data
  } finally {
    loading.value = false
  }
}

async function createGroup() {
  try {
    const { data } = await groupApi.create({ name: groupName.value.trim() })
    groups.value.unshift(data)
    showCreate.value = false
    groupName.value = ''
    toast.success('Группа создана')
  } catch (e) {
    toast.error(e.response?.data?.message || 'Ошибка создания')
  }
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.group-card {
  color: inherit;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__icon { font-size: 36px; }
  &__name { font-size: $font-size-lg; font-weight: 600; }
  &__meta { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }
}
</style>
