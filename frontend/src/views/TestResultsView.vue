<template>
  <div>
    <div class="page-header">
      <h1>История тестов</h1>
      <RouterLink to="/test" class="btn btn--primary">Новый тест</RouterLink>
    </div>

    <div v-if="loading" class="loading-center"><div class="spinner"></div></div>

    <div v-else-if="history.length === 0" class="empty-state">
      <div class="empty-state__icon">📊</div>
      <div class="empty-state__title">Тестов пока нет</div>
      <div class="empty-state__desc">Пройдите первый тест, чтобы увидеть статистику</div>
      <RouterLink to="/test" class="btn btn--primary">Пройти первый тест</RouterLink>
    </div>

    <div v-else>
      <!-- Summary bar -->
      <div class="stats-summary card" style="margin-bottom:24px">
        <div class="stats-summary__item">
          <div class="stats-summary__value">{{ history.length }}</div>
          <div class="stats-summary__label">Тестов пройдено</div>
        </div>
        <div class="stats-summary__divider"></div>
        <div class="stats-summary__item">
          <div class="stats-summary__value" :class="avgScoreClass">{{ avgScore }}%</div>
          <div class="stats-summary__label">Средний результат</div>
        </div>
        <div class="stats-summary__divider"></div>
        <div class="stats-summary__item">
          <div class="stats-summary__value">{{ totalWords }}</div>
          <div class="stats-summary__label">Слов проверено</div>
        </div>
        <div class="stats-summary__divider"></div>
        <div class="stats-summary__item">
          <div class="stats-summary__value">{{ bestScore }}%</div>
          <div class="stats-summary__label">Лучший результат</div>
        </div>
      </div>

      <!-- History list -->
      <div class="history-list">
        <div v-for="session in history" :key="session.id"
          class="history-card card">

          <!-- Top row: score + date -->
          <div class="history-card__top">
            <div class="history-score" :class="scoreClass(session.scorePercent)">
              {{ session.scorePercent }}%
            </div>
            <div class="history-card__date">{{ formatDate(session.completedAt) }}</div>
          </div>

          <!-- Info chips row -->
          <div class="history-card__chips">
            <!-- Word count -->
            <span class="hchip hchip--words">
              📝 {{ session.correctCount }}/{{ session.totalCount }} слов
            </span>

            <!-- Direction -->
            <span class="hchip hchip--dir">
              {{ session.direction === 'EN_TO_RU' ? '🇬🇧→🇷🇺' : '🇷🇺→🇬🇧' }}
              {{ session.direction === 'EN_TO_RU' ? 'EN → RU' : 'RU → EN' }}
            </span>

            <!-- Filter type + specific name -->
            <span class="hchip" :class="filterChipClass(session.wordFilterType)">
              {{ filterLabel(session) }}
            </span>
          </div>

          <!-- Expandable word-by-word results -->
          <details class="history-details">
            <summary class="history-details__toggle">
              Подробнее по словам
            </summary>
            <div class="history-details__list">
              <div v-for="r in session.results" :key="r.id"
                class="hword" :class="r.correct ? 'hword--ok' : 'hword--err'">
                <span class="hword__icon">{{ r.correct ? '✓' : '✗' }}</span>
                <span class="hword__en">{{ r.englishWord }}</span>
                <span class="hword__sep">→</span>
                <span class="hword__ru">{{ r.russianTranslation }}</span>
                <span v-if="!r.correct" class="hword__answer">
                  (вы: {{ r.userAnswer || '—' }})
                </span>
              </div>
            </div>
          </details>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { testApi } from '@/api'

const history = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const { data } = await testApi.getHistory()
    history.value = data
  } finally {
    loading.value = false
  }
})

// ─── Summary stats ────────────────────────────────────────────────────────────
const avgScore = computed(() => {
  if (!history.value.length) return 0
  const sum = history.value.reduce((acc, s) => acc + parseFloat(s.scorePercent), 0)
  return Math.round(sum / history.value.length)
})

const avgScoreClass = computed(() => {
  if (avgScore.value >= 80) return 'val--good'
  if (avgScore.value >= 50) return 'val--ok'
  return 'val--bad'
})

const bestScore = computed(() => {
  if (!history.value.length) return 0
  return Math.max(...history.value.map(s => parseFloat(s.scorePercent)))
})

const totalWords = computed(() =>
  history.value.reduce((acc, s) => acc + s.totalCount, 0)
)

// ─── Per-card helpers ─────────────────────────────────────────────────────────
function scoreClass(score) {
  const v = parseFloat(score)
  if (v >= 80) return 'history-score--good'
  if (v >= 50) return 'history-score--ok'
  return 'history-score--bad'
}

function filterLabel(session) {
  if (session.wordFilterType === 'ALL') return '📚 Все слова'
  if (session.wordFilterType === 'BY_LESSON') {
    return session.filterLessonTitle
      ? `📖 Занятие: ${session.filterLessonTitle}`
      : '📖 Все занятия'
  }
  if (session.wordFilterType === 'BY_GROUP') {
    return session.filterGroupName
      ? `👥 Группа: ${session.filterGroupName}`
      : '👥 Все группы'
  }
  return '—'
}

function filterChipClass(type) {
  if (type === 'BY_LESSON') return 'hchip--lesson'
  if (type === 'BY_GROUP')  return 'hchip--group'
  return 'hchip--all'
}

function formatDate(dt) {
  return new Date(dt).toLocaleString('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

// ─── Summary bar ──────────────────────────────────────────────────────────────
.stats-summary {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px 24px;
  flex-wrap: wrap;
  gap: 16px;

  &__item { text-align: center; }

  &__value {
    font-size: $font-size-2xl;
    font-weight: 700;
    color: $text;
    line-height: 1;

    &.val--good { color: $success; }
    &.val--ok   { color: $warning; }
    &.val--bad  { color: $danger; }
  }

  &__label {
    font-size: $font-size-sm;
    color: $text-muted;
    margin-top: 4px;
  }

  &__divider {
    width: 1px;
    height: 40px;
    background: $border;

    @media (max-width: $mobile) { display: none; }
  }
}

// ─── History list ─────────────────────────────────────────────────────────────
.history-list { display: flex; flex-direction: column; gap: 12px; }

.history-card {
  padding: 16px 20px;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__date {
    font-size: $font-size-sm;
    color: $text-muted;
  }

  &__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
  }
}

.history-score {
  font-size: $font-size-2xl;
  font-weight: 700;
  line-height: 1;

  &--good { color: $success; }
  &--ok   { color: $warning; }
  &--bad  { color: $danger; }
}

// ─── Chips ────────────────────────────────────────────────────────────────────
.hchip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: $font-size-sm;
  font-weight: 500;
  background: $bg;
  color: $text-muted;

  &--words  { background: #f0f7ff; color: #2563eb; }
  &--dir    { background: #f5f0ff; color: #7c3aed; }
  &--all    { background: #f0fdf4; color: #16a34a; }
  &--lesson { background: #fff7ed; color: #c2410c; }
  &--group  { background: #fdf4ff; color: #9333ea; }
}

// ─── Expandable details ───────────────────────────────────────────────────────
.history-details {
  border-top: 1px solid $border;
  padding-top: 12px;

  &__toggle {
    font-size: $font-size-sm;
    color: $primary;
    cursor: pointer;
    user-select: none;
    list-style: none;
    display: flex;
    align-items: center;
    gap: 6px;

    &::before { content: '▶'; font-size: 9px; transition: transform 0.2s; }

    &::-webkit-details-marker { display: none; }
  }

  &__list {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

details[open] .history-details__toggle::before { transform: rotate(90deg); }

.hword {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border-radius: $border-radius-sm;
  font-size: $font-size-sm;

  &--ok  { background: $success-light; }
  &--err { background: $danger-light; }

  &__icon { font-size: 13px; font-weight: 700; flex-shrink: 0; }
  &__en   { font-weight: 600; }
  &__sep  { color: $text-muted; }
  &__ru   { color: $text-muted; }
  &__answer { color: $danger; font-style: italic; margin-left: auto; }
}
</style>
