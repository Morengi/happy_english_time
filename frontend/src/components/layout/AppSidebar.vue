<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': collapsed }">
    <div class="sidebar__logo">
      <span class="sidebar__logo-icon">📚</span>
      <span v-if="!collapsed" class="sidebar__logo-text">EnglishPro</span>
    </div>

    <nav class="sidebar__nav">
      <RouterLink v-for="item in navItems" :key="item.path" :to="item.path"
        class="sidebar__link" :title="collapsed ? item.label : ''">
        <span class="sidebar__link-icon">{{ item.icon }}</span>
        <span v-if="!collapsed" class="sidebar__link-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar__footer">
      <div class="sidebar__user" v-if="!collapsed">
        <div class="sidebar__avatar">{{ initials }}</div>
        <div class="sidebar__user-info">
          <div class="sidebar__user-name">{{ user?.fullName }}</div>
          <div class="sidebar__user-role">{{ roleLabel }}</div>
        </div>
      </div>
      <button class="sidebar__logout btn btn--ghost btn--sm" @click="handleLogout" :title="'Выйти'">
        <span>🚪</span>
        <span v-if="!collapsed">Выйти</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({ collapsed: Boolean })
const emit = defineEmits(['toggle'])
const auth = useAuthStore()
const router = useRouter()
const user = computed(() => auth.user)

const initials = computed(() => {
  if (!user.value?.fullName) return '?'
  return user.value.fullName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
})

const roleLabel = computed(() => {
  const map = { ADMIN: 'Администратор', TEACHER: 'Преподаватель', STUDENT: 'Студент' }
  return map[user.value?.role] || ''
})

const allItems = [
  { path: '/dashboard', icon: '🏠', label: 'Главная', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/vocabulary', icon: '📖', label: 'Словарный запас', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/test', icon: '✏️', label: 'Проверка знаний', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/lessons', icon: '📝', label: 'Занятия', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/groups', icon: '👥', label: 'Группы', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/messages', icon: '💬', label: 'Сообщения', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/profile', icon: '👤', label: 'Профиль', roles: ['ADMIN', 'TEACHER', 'STUDENT'] },
  { path: '/admin', icon: '⚙️', label: 'Администрирование', roles: ['ADMIN'] },
]

const navItems = computed(() =>
  allItems.filter(item => item.roles.includes(user.value?.role))
)

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: $sidebar-width;
  background: $sidebar-bg;
  display: flex;
  flex-direction: column;
  z-index: 100;
  transition: width $transition-slow;
  overflow: hidden;

  &--collapsed {
    width: $sidebar-collapsed;

    @media (max-width: $tablet) { width: 0; }
  }

  @media (max-width: $tablet) {
    width: $sidebar-width;
    transform: translateX(-100%);
    transition: transform $transition-slow;

    &:not(&--collapsed) { transform: translateX(0); }
  }

  &__logo {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 20px 16px;
    border-bottom: 1px solid rgba(255,255,255,0.08);
    min-height: 64px;
  }

  &__logo-icon { font-size: 24px; flex-shrink: 0; }

  &__logo-text {
    font-size: $font-size-lg;
    font-weight: 700;
    color: white;
    white-space: nowrap;
  }

  &__nav {
    flex: 1;
    padding: 12px 8px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  &__link {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: $border-radius-sm;
    color: $sidebar-text;
    text-decoration: none;
    transition: $transition;
    margin-bottom: 2px;
    white-space: nowrap;

    &:hover { background: $sidebar-hover; color: white; text-decoration: none; }

    &.router-link-active {
      background: rgba($primary, 0.2);
      color: white;
    }
  }

  &__link-icon { font-size: 18px; flex-shrink: 0; width: 22px; text-align: center; }
  &__link-label { font-size: $font-size-base; font-weight: 500; }

  &__footer {
    padding: 12px 8px;
    border-top: 1px solid rgba(255,255,255,0.08);
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
  }

  &__avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: $primary;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: $font-size-sm;
    flex-shrink: 0;
  }

  &__user-name {
    font-weight: 600;
    color: white;
    font-size: $font-size-sm;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 140px;
  }

  &__user-role { font-size: 11px; color: $sidebar-text; }

  &__logout {
    color: $sidebar-text !important;
    justify-content: flex-start;
    gap: 10px;
    padding: 8px 12px !important;
    border-radius: $border-radius-sm;

    &:hover { background: $sidebar-hover !important; color: white !important; }
  }
}
</style>
