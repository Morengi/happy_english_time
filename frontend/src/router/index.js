import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },

  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/ProfileView.vue') },
      { path: 'vocabulary', name: 'Vocabulary', component: () => import('@/views/VocabularyView.vue') },
      { path: 'test', name: 'Test', component: () => import('@/views/TestView.vue') },
      { path: 'test/results', name: 'TestResults', component: () => import('@/views/TestResultsView.vue') },
      { path: 'groups', name: 'Groups', component: () => import('@/views/GroupsView.vue') },
      { path: 'groups/:id', name: 'GroupDetail', component: () => import('@/views/GroupDetailView.vue') },
      { path: 'lessons', name: 'Lessons', component: () => import('@/views/LessonsView.vue') },
      { path: 'lessons/:id', name: 'LessonDetail', component: () => import('@/views/LessonDetailView.vue') },
      { path: 'messages', name: 'Messages', component: () => import('@/views/MessagesView.vue') },
      { path: 'voice-rooms', name: 'VoiceRooms', component: () => import('@/views/VoiceRoomsView.vue') },
      { path: 'voice-rooms/room/:id', name: 'VoiceRoom', component: () => import('@/views/VoiceRoomView.vue') },
      { path: 'admin', name: 'Admin', component: () => import('@/views/AdminView.vue'), meta: { role: 'ADMIN' } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.guest && auth.isAuthenticated) return next('/')
  if (to.meta.requiresAuth && !auth.isAuthenticated) return next('/login')
  if (to.meta.role && auth.user?.role !== to.meta.role) return next('/')

  next()
})

export default router
