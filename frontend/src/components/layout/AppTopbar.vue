<template>
  <header class="topbar">
    <button class="topbar__menu-btn btn btn--ghost btn--icon" @click="emit('toggle-sidebar')">
      ☰
    </button>
    <div class="topbar__breadcrumb">
      <span>{{ pageTitle }}</span>
    </div>
    <div class="topbar__actions">
      <RouterLink to="/messages" class="topbar__action-btn">
        💬
        <span v-if="unreadCount > 0" class="topbar__badge">{{ unreadCount }}</span>
      </RouterLink>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const emit = defineEmits(['toggle-sidebar'])
const route = useRoute()
const chat = useChatStore()

const unreadCount = computed(() => chat.unreadCount)

const titleMap = {
  Dashboard: 'Главная',
  Profile: 'Профиль',
  Vocabulary: 'Словарный запас',
  Test: 'Проверка знаний',
  TestResults: 'Результаты теста',
  Groups: 'Группы',
  GroupDetail: 'Группа',
  Lessons: 'Занятия',
  LessonDetail: 'Занятие',
  Messages: 'Сообщения',
  VoiceRooms: 'Голосовые комнаты',
  Admin: 'Администрирование'
}

const pageTitle = computed(() => titleMap[route.name] || 'EnglishPro')
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.topbar {
  height: 64px;
  background: $bg-white;
  border-bottom: 1px solid $border;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;

  &__menu-btn {
    font-size: 20px;
    color: $text-muted;

    @media (min-width: $tablet) { display: none; }
  }

  &__breadcrumb {
    flex: 1;
    font-size: $font-size-lg;
    font-weight: 600;
    color: $text;
  }

  &__actions { display: flex; align-items: center; gap: 8px; }

  &__action-btn {
    position: relative;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    font-size: 18px;
    text-decoration: none;
    transition: $transition;
    color: $text;

    &:hover { background: $bg; text-decoration: none; }
  }

  &__badge {
    position: absolute;
    top: 2px;
    right: 2px;
    background: $danger;
    color: white;
    font-size: 10px;
    font-weight: 700;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
