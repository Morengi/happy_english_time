<template>
  <div>
    <div class="page-header">
      <h1>Занятия</h1>
      <button v-if="auth.isTeacher" class="btn btn--primary" @click="showCreate = true">+ Создать занятие</button>
    </div>

    <div v-if="loading" class="loading-center"><div class="spinner"></div></div>

    <div v-else-if="lessons.length === 0" class="empty-state">
      <div class="empty-state__icon">📝</div>
      <div class="empty-state__title">Занятий нет</div>
      <div class="empty-state__desc">
        {{ auth.isTeacher ? 'Создайте первое занятие' : 'Занятия ещё не добавлены' }}
      </div>
    </div>

    <div v-else class="grid grid--3">
      <RouterLink v-for="lesson in lessons" :key="lesson.id"
        :to="`/lessons/${lesson.id}`" class="card card--hover lesson-card" style="text-decoration:none;color:inherit">
        <div class="lesson-card__icon">📝</div>
        <div class="lesson-card__title">{{ lesson.title }}</div>
        <div class="lesson-card__meta">
          <span class="text-sm text-muted">{{ lesson.teacher?.fullName }}</span>
          <span class="text-sm text-muted">{{ formatDate(lesson.updatedAt) }}</span>
        </div>
      </RouterLink>
    </div>

    <!-- Create modal -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal modal--wide">
        <div class="modal__header">
          <h3>Создать занятие</h3>
          <button class="btn btn--ghost btn--icon" @click="showCreate = false">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>Название</label>
            <input v-model="createForm.title" type="text" class="form-control" placeholder="Тема занятия" />
          </div>
          <div class="form-group">
            <label>Теория (необязательно)</label>
            <RichEditor
              v-model="createForm.content"
              :upload-image-fn="uploadEditorImage"
              placeholder="Введите теорию занятия..."
              min-height="200px"
            />
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="showCreate = false">Отмена</button>
          <button class="btn btn--primary" @click="createLesson" :disabled="!createForm.title.trim()">Создать</button>
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
import { lessonApi, uploadApi } from '@/api'
import RichEditor from '@/components/common/RichEditor.vue'

const auth = useAuthStore()
const toast = useToastStore()
const lessons = ref([])
const loading = ref(true)
const showCreate = ref(false)
const createForm = ref({ title: '', content: '' })

onMounted(load)

async function load() {
  loading.value = true
  try {
    const { data } = await lessonApi.getAll()
    lessons.value = data
  } finally {
    loading.value = false
  }
}

async function uploadEditorImage(file) {
  const { data } = await uploadApi.image(file)
  return data.url
}

async function createLesson() {
  try {
    const { data } = await lessonApi.create(createForm.value)
    lessons.value.unshift(data)
    showCreate.value = false
    createForm.value = { title: '', content: '' }
    toast.success('Занятие создано')
  } catch (e) { toast.error(e.response?.data?.message || 'Ошибка') }
}

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('ru-RU')
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.lesson-card {
  display: flex;
  flex-direction: column;
  gap: 10px;

  &__icon { font-size: 32px; }
  &__title { font-size: $font-size-lg; font-weight: 600; }
  &__meta { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 4px; }
}
</style>
