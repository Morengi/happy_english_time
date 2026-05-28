<template>
  <div class="user-avatar" :style="sizeStyle">
    <img v-if="avatarUrl && !imgError"
      :src="avatarUrl"
      class="user-avatar__img"
      :alt="displayName"
      @error="imgError = true"
    />
    <span v-else class="user-avatar__initials">{{ initials }}</span>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  user:   { type: Object, default: null },   // { fullName, avatarUrl }
  name:   { type: String,  default: '' },    // fallback if no user object
  url:    { type: String,  default: null },  // direct avatarUrl override
  size:   { type: Number,  default: 36 }
})

const imgError = ref(false)

const displayName = computed(() => props.user?.fullName || props.name || '?')
const avatarUrl   = computed(() => props.url ?? props.user?.avatarUrl ?? null)

const initials = computed(() => {
  const n = displayName.value
  return n.split(' ').map(w => w[0]).filter(Boolean).join('').slice(0, 2).toUpperCase() || '?'
})

// Reset error flag when URL changes (e.g. after new upload)
watch(avatarUrl, () => { imgError.value = false })

const sizeStyle = computed(() => ({
  width:    props.size + 'px',
  height:   props.size + 'px',
  fontSize: Math.round(props.size * 0.36) + 'px'
}))
</script>

<style lang="scss">
@use '@/assets/styles/variables' as *;

.user-avatar {
  border-radius: 50%;
  background: $primary;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &__initials { line-height: 1; }
}
</style>
