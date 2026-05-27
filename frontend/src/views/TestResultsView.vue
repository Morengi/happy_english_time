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
      <RouterLink to="/test" class="btn btn--primary">Пройти первый тест</RouterLink>
    </div>

    <div v-else class="history-list">
      <div v-for="session in history" :key="session.id" class="card card--compact history-item">
        <div class="history-item__header">
          <span class="score-badge" :class="scoreClass(session.scorePercent)">{{ session.scorePercent }}%</span>
          <span class="text-sm text-muted">{{ formatDate(session.completedAt) }}</span>
        </div>
        <div class="history-item__stats">
          <span>{{ session.correctCount }} / {{ session.totalCount }} правильно</span>
          <span class="badge badge--secondary">{{ directionLabel(session.direction) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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

function scoreClass(score) {
  if (score >= 80) return 'score-badge--good'
  if (score >= 50) return 'score-badge--ok'
  return 'score-badge--bad'
}

function directionLabel(d) {
  return d === 'EN_TO_RU' ? 'EN → RU' : 'RU → EN'
}

function formatDate(dt) {
  return new Date(dt).toLocaleString('ru-RU')
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.history-list { display: flex; flex-direction: column; gap: 12px; }

.history-item {
  &__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
  &__stats { display: flex; align-items: center; gap: 12px; font-size: $font-size-sm; color: $text-muted; }
}

.score-badge {
  font-size: $font-size-xl;
  font-weight: 700;

  &--good { color: $success; }
  &--ok { color: $warning; }
  &--bad { color: $danger; }
}
</style>
