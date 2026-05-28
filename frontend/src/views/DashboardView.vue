<template>
  <div>
    <div class="page-header">
      <h1>Добро пожаловать, {{ auth.user?.fullName?.split(' ')[0] }}! 👋</h1>
    </div>

    <div class="grid grid--3 mb-16" style="margin-bottom:24px">
      <div class="card stat-card">
        <div class="stat-card__icon">📖</div>
        <div class="stat-card__value">{{ stats.words }}</div>
        <div class="stat-card__label">Слов в словаре</div>
      </div>
      <div class="card stat-card">
        <div class="stat-card__icon">✅</div>
        <div class="stat-card__value">{{ stats.tests }}</div>
        <div class="stat-card__label">Тестов пройдено</div>
      </div>
      <div class="card stat-card">
        <div class="stat-card__icon">🎯</div>
        <div class="stat-card__value">{{ stats.avgScore }}%</div>
        <div class="stat-card__label">Средний результат</div>
      </div>
    </div>

    <div class="grid grid--2">
      <div class="card">
        <h3 class="section-title">Последние тесты</h3>
        <div v-if="recentTests.length === 0" class="empty-state" style="padding:30px 0">
          <div class="empty-state__icon">📊</div>
          <div class="empty-state__desc">Тестов ещё нет</div>
          <RouterLink to="/test" class="btn btn--primary btn--sm">Пройти тест</RouterLink>
        </div>
        <div v-else class="test-list">
          <div v-for="test in recentTests" :key="test.id" class="test-item">
            <div class="test-item__top">
              <span class="test-item__score" :class="scoreClass(test.scorePercent)">
                {{ test.scorePercent }}%
              </span>
              <span class="text-sm text-muted">{{ formatDate(test.completedAt) }}</span>
            </div>
            <div class="test-item__chips">
              <span class="ti-chip ti-chip--words">
                📝 {{ test.correctCount }}/{{ test.totalCount }}
              </span>
              <span class="ti-chip ti-chip--dir">
                {{ test.direction === 'EN_TO_RU' ? 'EN→RU' : 'RU→EN' }}
              </span>
              <span class="ti-chip" :class="filterChipClass(test.wordFilterType)">
                {{ filterLabel(test) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <h3 class="section-title">Быстрые действия</h3>
        <div class="quick-actions">
          <RouterLink to="/vocabulary" class="quick-action">
            <span class="quick-action__icon">📖</span>
            <span>Словарный запас</span>
          </RouterLink>
          <RouterLink to="/test" class="quick-action">
            <span class="quick-action__icon">✏️</span>
            <span>Проверить знания</span>
          </RouterLink>
          <RouterLink to="/lessons" class="quick-action">
            <span class="quick-action__icon">📝</span>
            <span>Занятия</span>
          </RouterLink>
          <RouterLink to="/groups" class="quick-action">
            <span class="quick-action__icon">👥</span>
            <span>Группы</span>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { testApi, vocabularyApi } from '@/api'

const auth = useAuthStore()
const recentTests = ref([])
const stats = ref({ words: 0, tests: 0, avgScore: 0 })

onMounted(async () => {
  try {
    const [testsRes, vocabRes] = await Promise.all([
      testApi.getHistory(),
      vocabularyApi.getAll()
    ])
    const tests = testsRes.data.slice(0, 5)
    recentTests.value = tests
    stats.value.words = vocabRes.data.length
    stats.value.tests = testsRes.data.length
    if (testsRes.data.length > 0) {
      stats.value.avgScore = Math.round(
        testsRes.data.reduce((s, t) => s + parseFloat(t.scorePercent), 0) / testsRes.data.length
      )
    }
  } catch {}
})

function formatDate(dt) {
  return new Date(dt).toLocaleDateString('ru-RU', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
}

function scoreClass(score) {
  if (score >= 80) return 'score--good'
  if (score >= 50) return 'score--ok'
  return 'score--bad'
}

function filterLabel(test) {
  if (test.wordFilterType === 'BY_LESSON') {
    return test.filterLessonTitle ? `📖 ${test.filterLessonTitle}` : '📖 Занятия'
  }
  if (test.wordFilterType === 'BY_GROUP') {
    return test.filterGroupName ? `👥 ${test.filterGroupName}` : '👥 Группы'
  }
  return '📚 Все слова'
}

function filterChipClass(type) {
  if (type === 'BY_LESSON') return 'ti-chip--lesson'
  if (type === 'BY_GROUP')  return 'ti-chip--group'
  return 'ti-chip--all'
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;

  &__icon { font-size: 32px; }
  &__value { font-size: 32px; font-weight: 700; color: $primary; }
  &__label { color: $text-muted; font-size: $font-size-sm; }
}

.section-title {
  font-size: $font-size-lg;
  font-weight: 600;
  margin-bottom: 16px;
}

.test-list { display: flex; flex-direction: column; gap: 8px; }

.test-item {
  padding: 10px 12px;
  border-radius: $border-radius-sm;
  background: $bg;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
  }

  &__score {
    font-size: $font-size-lg;
    font-weight: 700;
    line-height: 1;

    &.score--good { color: $success; }
    &.score--ok   { color: $warning; }
    &.score--bad  { color: $danger; }
  }

  &__chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }
}

.ti-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;

  &--words  { background: #f0f7ff; color: #2563eb; }
  &--dir    { background: #f5f0ff; color: #7c3aed; }
  &--all    { background: #f0fdf4; color: #16a34a; }
  &--lesson { background: #fff7ed; color: #c2410c; }
  &--group  { background: #fdf4ff; color: #9333ea; }
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: $border-radius-sm;
  background: $bg;
  color: $text;
  text-decoration: none;
  font-size: $font-size-sm;
  font-weight: 500;
  transition: $transition;
  text-align: center;

  &:hover { background: $primary-light; color: $primary; text-decoration: none; }

  &__icon { font-size: 24px; }
}
</style>
