import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/voice-rooms/',
    name: 'Rooms',
    component: () => import('@/views/RoomsListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/voice-rooms/room/:id',
    name: 'Room',
    component: () => import('@/views/RoomView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/voice-rooms/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  if (!to.meta.requiresAuth) return next()

  const auth = useAuthStore()

  // Bootstrap auth from ?token= URL param or localStorage on first load
  if (!auth.isAuthenticated) {
    await auth.init()
  }

  if (!auth.isAuthenticated) {
    // Show inline login if opened standalone (not embedded)
    return next({ path: '/voice-rooms/', query: { login: '1' } })
  }

  next()
})

export default router
