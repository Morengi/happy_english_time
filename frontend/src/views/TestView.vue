<template>
  <div>
    <div class="page-header">
      <h1>Проверка знаний</h1>
    </div>

    <!-- Config screen -->
    <div v-if="phase === 'config'" class="card" style="max-width:560px;margin:0 auto">
      <h3 style="font-size:20px;font-weight:600;margin-bottom:24px">Настройки теста</h3>

      <div class="form-group">
        <label>Количество слов</label>
        <div class="count-toggle">
          <button class="count-toggle__btn" :class="{ active: config.countMode === 'all' }"
            @click="config.countMode = 'all'">
            Все слова
          </button>
          <button class="count-toggle__btn" :class="{ active: config.countMode === 'custom' }"
            @click="config.countMode = 'custom'">
            Указать число
          </button>
        </div>
        <div v-if="config.countMode === 'custom'" class="count-input-row">
          <input v-model.number="config.customCount" type="number" min="1" class="form-control count-input"
            placeholder="Сколько слов?" @input="clampCount" />
          <span class="count-input-label">слов</span>
        </div>
      </div>

      <div class="form-group">
        <label>Направление перевода</label>
        <select v-model="config.direction" class="form-control">
          <option value="EN_TO_RU">Английский → Русский</option>
          <option value="RU_TO_EN">Русский → Английский</option>
        </select>
      </div>

      <div class="form-group">
        <label>Тип слов</label>
        <select v-model="config.filterType" class="form-control">
          <option value="ALL">Все слова</option>
          <option value="BY_LESSON">По занятиям</option>
          <option value="BY_GROUP">По группам</option>
        </select>
      </div>

      <div v-if="config.filterType === 'BY_LESSON'" class="form-group">
        <label>Занятие (необязательно)</label>
        <select v-model="config.filterLessonId" class="form-control">
          <option :value="null">Все занятия</option>
          <option v-for="l in lessons" :key="l.id" :value="l.id">{{ l.title }}</option>
        </select>
      </div>

      <div v-if="config.filterType === 'BY_GROUP'" class="form-group">
        <label>Группа (необязательно)</label>
        <div v-if="groups.length === 0" class="filter-hint">
          Вы не состоите ни в одной группе
        </div>
        <select v-else v-model="config.filterGroupId" class="form-control">
          <option :value="null">Все группы</option>
          <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
        </select>
      </div>

      <button class="btn btn--primary btn--full btn--lg" @click="startTest" :disabled="loading">
        {{ loading ? 'Загружаю слова...' : 'Начать тест' }}
      </button>
    </div>

    <!-- Test screen -->
    <div v-if="phase === 'test'">
      <div class="test-progress">
        <div class="test-progress__bar">
          <div class="test-progress__fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <span class="text-sm text-muted">{{ currentIndex + 1 }} / {{ testWords.length }}</span>
      </div>

      <div class="test-word-card card" style="max-width:600px;margin:0 auto">
        <div class="test-word-card__prompt">Переведите слово:</div>
        <div class="test-word-card__word">{{ currentQuestion }}</div>
        <div class="form-group" style="margin-top:24px">
          <input v-model="answers[currentIndex]" type="text" class="form-control"
            :placeholder="config.direction === 'EN_TO_RU' ? 'Введите перевод на русском...' : 'Enter English translation...'"
            @keydown.enter="nextWord" autofocus ref="inputRef" />
        </div>
        <div class="test-nav">
          <button class="btn btn--secondary" @click="prevWord" :disabled="currentIndex === 0">← Назад</button>
          <button v-if="currentIndex < testWords.length - 1" class="btn btn--primary" @click="nextWord">Далее →</button>
          <button v-else class="btn btn--primary" @click="finish">Завершить тест</button>
        </div>
      </div>

      <div class="test-all-words">
        <div v-for="(word, i) in testWords" :key="word.id" class="test-word-dot"
          :class="{ active: i === currentIndex, answered: answers[i] }"
          @click="currentIndex = i" :title="word.englishWord">
          {{ i + 1 }}
        </div>
      </div>
    </div>

    <!-- Results screen -->
    <div v-if="phase === 'results' && results">
      <button class="btn btn--secondary btn--sm" style="margin-bottom:20px" @click="resetTest">
        ← Новый тест
      </button>

      <div class="results-header card" style="text-align:center;margin-bottom:20px;padding:32px">
        <div class="results-score" :class="scoreClass">{{ results.scorePercent }}%</div>
        <div style="font-size:18px;font-weight:600;margin-top:8px">
          {{ results.correctCount }} из {{ results.totalCount }} правильно
        </div>
        <div style="color:#6b7280;margin-top:6px">{{ scoreMessage }}</div>
      </div>

      <div class="results-list">
        <div v-for="r in results.results" :key="r.id" class="result-item"
          :class="r.correct ? 'result-item--correct' : 'result-item--wrong'">
          <div class="result-item__icon">{{ r.correct ? '✅' : '❌' }}</div>
          <div class="result-item__content">
            <div class="result-item__question">
              <strong>{{ config.direction === 'EN_TO_RU' ? r.englishWord : r.russianTranslation }}</strong>
            </div>
            <div class="result-item__answer">
              Ваш ответ: <span :class="r.correct ? 'correct' : 'wrong'">{{ r.userAnswer || '—' }}</span>
            </div>
            <div v-if="!r.correct" class="result-item__correct">
              Правильно: <span class="correct">{{ config.direction === 'EN_TO_RU' ? r.russianTranslation : r.englishWord }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import { testApi, lessonApi, groupApi } from '@/api'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
const phase = ref('config')
const loading = ref(false)
const testWords = ref([])
const answers = ref([])
const currentIndex = ref(0)
const results = ref(null)
const lessons = ref([])
const groups = ref([])
const inputRef = ref(null)

const config = ref({
  countMode: 'all',    // 'all' | 'custom'
  customCount: 10,
  direction: 'EN_TO_RU',
  filterType: 'ALL',
  filterLessonId: null,
  filterGroupId: null
})

function clampCount() {
  if (config.value.customCount < 1) config.value.customCount = 1
}

const currentQuestion = computed(() => {
  const word = testWords.value[currentIndex.value]
  if (!word) return ''
  return config.value.direction === 'EN_TO_RU' ? word.englishWord : word.russianTranslation
})

const progressPercent = computed(() =>
  testWords.value.length ? Math.round((currentIndex.value / testWords.value.length) * 100) : 0
)

const scoreClass = computed(() => {
  if (!results.value) return ''
  const s = parseFloat(results.value.scorePercent)
  if (s >= 80) return 'score--good'
  if (s >= 50) return 'score--ok'
  return 'score--bad'
})

const scoreMessage = computed(() => {
  if (!results.value) return ''
  const s = parseFloat(results.value.scorePercent)
  if (s === 100) return '🏆 Отлично! Все слова знаете!'
  if (s >= 80) return '👍 Хорошая работа!'
  if (s >= 50) return '📚 Есть над чем поработать'
  return '💪 Нужно больше практики'
})

onMounted(() => {
  // Load independently so one failure doesn't prevent the other from loading
  lessonApi.getAll().then(r => { lessons.value = r.data }).catch(() => {})
  groupApi.getAll().then(r => { groups.value = r.data }).catch(() => {})
})

async function startTest() {
  // Validate custom count
  if (config.value.countMode === 'custom') {
    const n = parseInt(config.value.customCount)
    if (!n || n < 1) {
      toast.error('Введите количество слов (минимум 1)')
      return
    }
    config.value.customCount = n
  }

  loading.value = true
  try {
    const params = {
      // 0 → backend returns all available words
      count: config.value.countMode === 'all' ? 0 : config.value.customCount,
      direction: config.value.direction,
      filterType: config.value.filterType
    }
    if (config.value.filterLessonId) params.filterLessonId = config.value.filterLessonId
    if (config.value.filterGroupId) params.filterGroupId = config.value.filterGroupId

    const { data } = await testApi.getWords(params)
    if (data.length === 0) {
      toast.error('Нет слов для этого типа фильтра')
      return
    }
    testWords.value = data
    answers.value = new Array(data.length).fill('')
    currentIndex.value = 0
    phase.value = 'test'
    await nextTick()
    inputRef.value?.focus()
  } catch {
    toast.error('Ошибка загрузки слов')
  } finally {
    loading.value = false
  }
}

function nextWord() {
  if (currentIndex.value < testWords.value.length - 1) {
    currentIndex.value++
    nextTick(() => inputRef.value?.focus())
  }
}

function prevWord() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    nextTick(() => inputRef.value?.focus())
  }
}

function resetTest() {
  phase.value = 'config'
  testWords.value = []
  answers.value = []
  currentIndex.value = 0
  results.value = null
}

async function finish() {
  const payload = {
    direction: config.value.direction,
    filterType: config.value.filterType,
    filterLessonId: config.value.filterLessonId,
    filterGroupId: config.value.filterGroupId,
    groupId: config.value.filterGroupId,
    answers: testWords.value.map((word, i) => ({
      wordId: word.id,
      englishWord: word.englishWord,
      russianTranslation: word.russianTranslation,
      userAnswer: answers.value[i] || ''
    }))
  }
  try {
    const { data } = await testApi.submit(payload)
    results.value = data
    phase.value = 'results'
  } catch {
    toast.error('Ошибка отправки результатов')
  }
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.test-progress {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  &__bar {
    flex: 1;
    height: 8px;
    background: $border;
    border-radius: 4px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: $primary;
    border-radius: 4px;
    transition: width 0.3s ease;
  }
}

.test-word-card {
  &__prompt { color: $text-muted; font-size: $font-size-sm; text-transform: uppercase; letter-spacing: 0.04em; margin-bottom: 12px; }
  &__word { font-size: 36px; font-weight: 700; color: $text; }
}

.test-nav {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}

.test-all-words {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 24px;
}

.test-word-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $bg;
  border: 2px solid $border;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: $font-size-sm;
  font-weight: 600;
  cursor: pointer;
  transition: $transition;

  &.active { border-color: $primary; color: $primary; }
  &.answered { background: $primary-light; border-color: $primary; }

  &:hover { border-color: $primary; }
}

.results-score {
  font-size: 64px;
  font-weight: 700;

  &.score--good { color: $success; }
  &.score--ok { color: $warning; }
  &.score--bad { color: $danger; }
}

.results-list { display: flex; flex-direction: column; gap: 10px; }

.result-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-radius: $border-radius-sm;
  border: 1px solid;

  &--correct { background: $success-light; border-color: lighten($success, 30%); }
  &--wrong { background: $danger-light; border-color: lighten($danger, 20%); }

  &__icon { font-size: 18px; flex-shrink: 0; margin-top: 2px; }
  &__content { flex: 1; }
  &__question { font-size: $font-size-lg; margin-bottom: 4px; }
  &__answer, &__correct { font-size: $font-size-sm; color: $text-muted; margin-top: 2px; }

  .correct { color: darken($success, 10%); font-weight: 600; }
  .wrong { color: darken($danger, 10%); font-weight: 600; }
}

// ─── Count mode toggle ────────────────────────────────────────────────────────
.count-toggle {
  display: flex;
  border: 1px solid $border;
  border-radius: $border-radius-sm;
  overflow: hidden;
  margin-top: 6px;

  &__btn {
    flex: 1;
    padding: 9px 16px;
    border: none;
    background: white;
    font-size: $font-size-base;
    font-family: $font-family;
    color: $text-muted;
    cursor: pointer;
    transition: $transition;
    font-weight: 500;

    & + & { border-left: 1px solid $border; }

    &.active {
      background: $primary;
      color: white;
    }

    &:not(.active):hover {
      background: $bg;
      color: $text;
    }
  }
}

.count-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.count-input {
  width: 120px;
  text-align: center;
  font-size: $font-size-lg;
  font-weight: 600;
}

.count-input-label {
  font-size: $font-size-base;
  color: $text-muted;
}

.filter-hint {
  margin-top: 6px;
  padding: 10px 14px;
  border-radius: $border-radius-sm;
  background: $warning-light;
  font-size: $font-size-sm;
  color: darken($warning, 30%);
  border: 1px solid $warning;
}
</style>
