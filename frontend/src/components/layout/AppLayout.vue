<template>
  <div class="app-layout">
    <AppSidebar :collapsed="sidebarCollapsed" @toggle="sidebarCollapsed = !sidebarCollapsed" />
    <div class="main-content" :style="{ marginLeft: sidebarCollapsed ? '64px' : '260px' }">
      <AppTopbar @toggle-sidebar="sidebarCollapsed = !sidebarCollapsed" />
      <div class="page-content">
        <RouterView />
      </div>
    </div>
    <!-- Mobile overlay -->
    <div v-if="!sidebarCollapsed && isMobile" class="sidebar-overlay" @click="sidebarCollapsed = true" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'

const sidebarCollapsed = ref(false)
const isMobile = ref(window.innerWidth < 1024)

const chat = useChatStore()
const auth = useAuthStore()

function onResize() {
  isMobile.value = window.innerWidth < 1024
  if (isMobile.value) sidebarCollapsed.value = true
}

onMounted(() => {
  onResize()
  window.addEventListener('resize', onResize)

  // Connect WebSocket and load initial unread count
  if (auth.token) {
    chat.connect(auth.token)
    chat.fetchUnread()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chat.disconnect()
})
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 99;

  @media (min-width: $tablet) { display: none; }
}

.main-content {
  transition: margin-left 0.3s ease;

  @media (max-width: $tablet) { margin-left: 0 !important; }
}
</style>
