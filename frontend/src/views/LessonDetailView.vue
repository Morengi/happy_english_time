<template>
  <div v-if="lesson">
    <div class="page-header">
      <div>
        <h1>{{ lesson.title }}</h1>
        <p class="text-muted text-sm">{{ lesson.teacher?.fullName }} · {{ formatDate(lesson.updatedAt) }}</p>
      </div>
      <div v-if="canManage" class="flex gap-8">
        <button class="btn btn--secondary btn--sm" @click="showEdit = true">✏️ Редактировать</button>
        <button class="btn btn--secondary btn--sm" @click="showAccess = true">🔑 Доступ</button>
        <button class="btn btn--secondary btn--sm" @click="showAddWord = true">+ Слово</button>
        <button class="btn btn--danger btn--sm" @click="deleteLesson">🗑️ Удалить</button>
      </div>
    </div>

    <!-- Access groups -->
    <div v-if="lesson.accessGroups?.length > 0" style="margin-bottom:16px">
      <span v-for="g in lesson.accessGroups" :key="g.id" class="badge badge--success" style="margin-right:6px">
        👥 {{ g.name }}
      </span>
    </div>

    <!-- Content -->
    <div class="lesson-content card" v-if="lesson.content">
      <div class="lesson-text" v-html="formatContent(lesson.content)"></div>
    </div>

    <!-- Media & Files -->
    <div v-if="lesson.files?.length > 0" class="lesson-files">
      <h3 class="section-title" style="margin-top:24px">Материалы</h3>

      <!-- Images -->
      <div v-if="images.length" class="lesson-images">
        <img v-for="f in images" :key="f.id" :src="f.url" :alt="f.originalName" class="lesson-image" />
      </div>

      <!-- Videos -->
      <div v-for="f in videos" :key="f.id" class="lesson-video-wrap">
        <video :src="f.url" controls class="lesson-video"></video>
        <p class="text-sm text-muted">{{ f.originalName }}</p>
      </div>

      <!-- Documents -->
      <div v-if="docs.length" class="lesson-docs">
        <a v-for="f in docs" :key="f.id" :href="f.url" :download="f.originalName" class="doc-item">
          <span class="doc-item__icon">{{ docIcon(f.fileType) }}</span>
          <span class="doc-item__name">{{ f.originalName }}</span>
          <span class="doc-item__size">{{ formatSize(f.fileSize) }}</span>
          <span class="doc-item__dl">⬇</span>
        </a>
      </div>
    </div>

    <!-- Upload (teacher only) -->
    <div v-if="canManage" class="card" style="margin-top:20px">
      <h3 style="margin-bottom:12px">Загрузить файл</h3>
      <div class="upload-area" @dragover.prevent @drop.prevent="onDrop">
        <input type="file" ref="fileInputRef" @change="onFileChange" style="display:none" multiple />
        <div class="upload-area__content" @click="fileInputRef.click()">
          <div class="upload-area__icon">📂</div>
          <div>Перетащите файлы или <span style="color:var(--primary);cursor:pointer">нажмите для выбора</span></div>
          <div class="text-sm text-muted">PDF, DOCX, TXT, изображения, видео (до 500МБ)</div>
        </div>
      </div>
      <div v-if="uploading" style="margin-top:10px">
        <div class="spinner" style="width:20px;height:20px;border-width:2px"></div>
        <span class="text-sm text-muted" style="margin-left:8px">Загружаю...</span>
      </div>
    </div>

    <!-- Edit modal -->
    <div v-if="showEdit" class="modal-overlay" @click.self="showEdit = false">
      <div class="modal modal--wide">
        <div class="modal__header">
          <h3>Редактировать занятие</h3>
          <button class="btn btn--ghost btn--icon" @click="showEdit = false">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>Название</label>
            <input v-model="editForm.title" type="text" class="form-control" />
          </div>
          <div class="form-group">
            <label>Содержание (HTML)</label>
            <textarea v-model="editForm.content" class="form-control" rows="10"></textarea>
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="showEdit = false">Отмена</button>
          <button class="btn btn--primary" @click="saveEdit">Сохранить</button>
        </div>
      </div>
    </div>

    <!-- Access modal -->
    <div v-if="showAccess" class="modal-overlay" @click.self="showAccess = false">
      <div class="modal">
        <div class="modal__header">
          <h3>Управление доступом</h3>
          <button class="btn btn--ghost btn--icon" @click="showAccess = false">✕</button>
        </div>
        <div class="modal__body">
          <p class="text-sm text-muted" style="margin-bottom:16px">Группы с доступом к занятию:</p>
          <div v-for="group in myGroups" :key="group.id" class="access-group-item">
            <span>{{ group.name }}</span>
            <button v-if="hasAccess(group.id)" class="btn btn--danger btn--sm" @click="revokeAccess(group.id)">Убрать доступ</button>
            <button v-else class="btn btn--primary btn--sm" @click="grantAccess(group.id)">Дать доступ</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add word modal -->
    <div v-if="showAddWord" class="modal-overlay" @click.self="showAddWord = false">
      <div class="modal">
        <div class="modal__header">
          <h3>Добавить слово для занятия</h3>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { lessonApi, groupApi } from '@/api'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()

const lesson = ref(null)
const myGroups = ref([])
const showEdit = ref(false)
const showAccess = ref(false)
const showAddWord = ref(false)
const uploading = ref(false)
const fileInputRef = ref(null)
const editForm = ref({ title: '', content: '' })
const wordForm = ref({ englishWord: '', russianTranslation: '' })

const canManage = computed(() => auth.isAdmin ||
  (auth.isTeacher && lesson.value?.teacher?.id === auth.user?.id))

const images = computed(() => lesson.value?.files?.filter(f => f.image) || [])
const videos = computed(() => lesson.value?.files?.filter(f => f.video) || [])
const docs = computed(() => lesson.value?.files?.filter(f => !f.image && !f.video) || [])

onMounted(async () => {
  const [lr, gr] = await Promise.all([
    lessonApi.getById(route.params.id),
    groupApi.getAll()
  ])
  lesson.value = lr.data
  myGroups.value = gr.data.filter(g => auth.isAdmin || g.teacher?.id === auth.user?.id)
  editForm.value = { title: lesson.value.title, content: lesson.value.content || '' }
})

function hasAccess(groupId) {
  return lesson.value?.accessGroups?.some(g => g.id === groupId)
}

async function grantAccess(groupId) {
  const { data } = await lessonApi.grantAccess(route.params.id, groupId)
  lesson.value = data
  toast.success('Доступ выдан')
}

async function revokeAccess(groupId) {
  const { data } = await lessonApi.revokeAccess(route.params.id, groupId)
  lesson.value = data
  toast.success('Доступ убран')
}

async function saveEdit() {
  const { data } = await lessonApi.update(route.params.id, editForm.value)
  lesson.value = data
  showEdit.value = false
  toast.success('Занятие обновлено')
}

async function deleteLesson() {
  if (!confirm('Удалить занятие?')) return
  await lessonApi.delete(route.params.id)
  router.push('/lessons')
  toast.success('Занятие удалено')
}

async function onFileChange(e) {
  for (const file of e.target.files) await uploadFile(file)
  e.target.value = ''
}

async function onDrop(e) {
  for (const file of e.dataTransfer.files) await uploadFile(file)
}

async function uploadFile(file) {
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', file)
    const { data } = await lessonApi.uploadFile(route.params.id, form)
    lesson.value = data
    toast.success(`Файл "${file.name}" загружен`)
  } catch { toast.error('Ошибка загрузки файла') } finally { uploading.value = false }
}

async function addWord() {
  if (!wordForm.value.englishWord || !wordForm.value.russianTranslation) return
  try {
    await lessonApi.addWord(route.params.id, wordForm.value)
    showAddWord.value = false
    wordForm.value = { englishWord: '', russianTranslation: '' }
    toast.success('Слово добавлено для занятия')
  } catch (e) { toast.error(e.response?.data?.message || 'Ошибка') }
}

function formatDate(dt) { return new Date(dt).toLocaleDateString('ru-RU') }
function formatContent(html) { return html }
function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' Б'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' КБ'
  return (bytes / 1024 / 1024).toFixed(1) + ' МБ'
}

function docIcon(type) {
  if (type?.includes('pdf')) return '📄'
  if (type?.includes('word') || type?.includes('docx')) return '📘'
  if (type?.includes('text')) return '📃'
  return '📎'
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.lesson-content { margin-bottom: 20px; }
.lesson-text { line-height: 1.8; }

.lesson-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.lesson-image {
  border-radius: $border-radius-sm;
  width: 100%;
  object-fit: cover;
  max-height: 300px;
}

.lesson-video-wrap { margin-bottom: 16px; }
.lesson-video { width: 100%; border-radius: $border-radius-sm; max-height: 480px; }

.lesson-docs { display: flex; flex-direction: column; gap: 8px; }

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: $border-radius-sm;
  background: $bg;
  border: 1px solid $border;
  text-decoration: none;
  color: $text;
  transition: $transition;

  &:hover { background: $primary-light; border-color: $primary; text-decoration: none; }

  &__icon { font-size: 20px; }
  &__name { flex: 1; font-weight: 500; }
  &__size { color: $text-muted; font-size: $font-size-sm; }
  &__dl { color: $primary; }
}

.upload-area {
  border: 2px dashed $border;
  border-radius: $border-radius;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: $transition;

  &:hover { border-color: $primary; background: $primary-light; }

  &__icon { font-size: 40px; margin-bottom: 12px; }
}

.access-group-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid $border;
  font-weight: 500;
}
</style>
