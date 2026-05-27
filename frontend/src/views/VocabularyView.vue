<template>
  <div>
    <div class="page-header">
      <h1>Словарный запас</h1>
      <button class="btn btn--primary" @click="showAddModal = true">+ Добавить слово</button>
    </div>

    <!-- Filters -->
    <div class="vocab-filters card card--compact mb-16" style="margin-bottom:20px">
      <div class="vocab-filters__row">
        <input v-model="search" type="text" class="form-control" placeholder="Поиск слов..." style="max-width:300px" />
        <div class="vocab-tabs">
          <button v-for="f in filters" :key="f.value" class="vocab-tab"
            :class="{ active: activeFilter === f.value }" @click="activeFilter = f.value">
            {{ f.label }}
          </button>
        </div>
        <div class="vocab-count">{{ filteredWords.length }} слов</div>
      </div>
    </div>

    <div v-if="loading" class="loading-center"><div class="spinner"></div></div>

    <div v-else-if="filteredWords.length === 0" class="empty-state">
      <div class="empty-state__icon">📖</div>
      <div class="empty-state__title">Слов не найдено</div>
      <div class="empty-state__desc">Добавьте первое слово в ваш словарь</div>
    </div>

    <div v-else class="word-grid">
      <div v-for="word in filteredWords" :key="word.id" class="word-card">
        <div class="word-card__main">
          <div class="word-card__en">{{ word.englishWord }}</div>
          <div class="word-card__arrow">→</div>
          <div class="word-card__ru">{{ word.russianTranslation }}</div>
        </div>
        <div class="word-card__footer">
          <span class="badge" :class="sourceClass(word.sourceType)">{{ sourceLabel(word) }}</span>
          <div class="word-card__actions">
            <button class="btn btn--ghost btn--sm btn--icon" @click="startEdit(word)" title="Редактировать">✏️</button>
            <button class="btn btn--ghost btn--sm btn--icon" @click="deleteWord(word.id)" title="Удалить">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showAddModal || editWord" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal__header">
          <h3>{{ editWord ? 'Редактировать слово' : 'Добавить слово' }}</h3>
          <button class="btn btn--ghost btn--icon" @click="closeModal">✕</button>
        </div>
        <div class="modal__body">
          <div class="form-group">
            <label>Английское слово</label>
            <input v-model="wordForm.englishWord" type="text" class="form-control" placeholder="apple" />
          </div>
          <div class="form-group">
            <label>Перевод (можно несколько через запятую)</label>
            <input v-model="wordForm.russianTranslation" type="text" class="form-control" placeholder="яблоко, яблок" />
          </div>
        </div>
        <div class="modal__footer">
          <button class="btn btn--secondary" @click="closeModal">Отмена</button>
          <button class="btn btn--primary" @click="saveWord" :disabled="saving">
            {{ saving ? 'Сохраняю...' : (editWord ? 'Сохранить' : 'Добавить') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { vocabularyApi } from '@/api'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const words = ref([])
const loading = ref(true)
const search = ref('')
const activeFilter = ref('ALL')
const showAddModal = ref(false)
const editWord = ref(null)
const saving = ref(false)
const wordForm = ref({ englishWord: '', russianTranslation: '' })

const filters = [
  { value: 'ALL', label: 'Все' },
  { value: 'PERSONAL', label: 'Мои' },
  { value: 'LESSON', label: 'По занятиям' },
  { value: 'GROUP', label: 'По группам' }
]

const filteredWords = computed(() => {
  let list = words.value
  if (activeFilter.value !== 'ALL') list = list.filter(w => w.sourceType === activeFilter.value)
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(w => w.englishWord.toLowerCase().includes(q) || w.russianTranslation.toLowerCase().includes(q))
  }
  return list
})

async function load() {
  loading.value = true
  try {
    const { data } = await vocabularyApi.getAll()
    words.value = data
  } finally {
    loading.value = false
  }
}

function startEdit(word) {
  editWord.value = word
  wordForm.value = { englishWord: word.englishWord, russianTranslation: word.russianTranslation }
}

function closeModal() {
  showAddModal.value = false
  editWord.value = null
  wordForm.value = { englishWord: '', russianTranslation: '' }
}

async function saveWord() {
  if (!wordForm.value.englishWord || !wordForm.value.russianTranslation) return
  saving.value = true
  try {
    if (editWord.value) {
      const { data } = await vocabularyApi.update(editWord.value.id, wordForm.value)
      const idx = words.value.findIndex(w => w.id === editWord.value.id)
      if (idx !== -1) words.value[idx] = data
      toast.success('Слово обновлено')
    } else {
      const { data } = await vocabularyApi.add({ ...wordForm.value, sourceType: 'PERSONAL' })
      words.value.unshift(data)
      toast.success('Слово добавлено')
    }
    closeModal()
  } catch (e) {
    toast.error(e.response?.data?.message || 'Ошибка')
  } finally {
    saving.value = false
  }
}

async function deleteWord(id) {
  if (!confirm('Удалить слово?')) return
  try {
    await vocabularyApi.delete(id)
    words.value = words.value.filter(w => w.id !== id)
    toast.success('Слово удалено')
  } catch { toast.error('Ошибка удаления') }
}

function sourceLabel(word) {
  if (word.sourceType === 'PERSONAL') return 'Моё'
  if (word.sourceType === 'LESSON') return word.lessonTitle || 'Занятие'
  if (word.sourceType === 'GROUP') return word.groupName || 'Группа'
  return ''
}

function sourceClass(type) {
  if (type === 'PERSONAL') return 'badge--primary'
  if (type === 'LESSON') return 'badge--success'
  return 'badge--warning'
}

onMounted(load)
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.vocab-filters {
  &__row {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
  }
}

.vocab-tabs {
  display: flex;
  gap: 4px;
  background: $bg;
  padding: 4px;
  border-radius: $border-radius-sm;
}

.vocab-tab {
  padding: 6px 14px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: $font-size-sm;
  font-weight: 500;
  color: $text-muted;
  transition: $transition;
  font-family: $font-family;

  &.active { background: white; color: $primary; box-shadow: $shadow-sm; }
}

.vocab-count { color: $text-muted; font-size: $font-size-sm; margin-left: auto; }

.word-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.word-card {
  background: white;
  border: 1px solid $border;
  border-radius: $border-radius;
  padding: 16px;
  transition: $transition;

  &:hover { box-shadow: $shadow; }

  &__main {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }

  &__en {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $primary;
  }

  &__arrow { color: $text-muted; font-size: 18px; }

  &__ru {
    font-size: $font-size-lg;
    color: $text;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__actions { display: flex; gap: 4px; }
}
</style>
