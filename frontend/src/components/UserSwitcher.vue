<script setup>
import { ref } from 'vue'
import { ChevronDown, ShieldCheck, UserRound } from 'lucide-vue-next'

const props = defineProps({
  users: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
})
const emit = defineEmits(['select'])

const open = ref(false)
const trigger = ref(null)

function toggle() {
  open.value = !open.value
}
function pick(user) {
  emit('select', user)
  open.value = false
}
function close() {
  open.value = false
}
</script>

<template>
  <div class="user-switcher" :class="{ open }">
    <button
      ref="trigger"
      class="user-switcher__trigger"
      type="button"
      :aria-expanded="open"
      aria-label="切换当前用户"
      @click="toggle"
    >
      <span class="user-switcher__avatar"><UserRound :size="16" aria-hidden="true" /></span>
      <span class="user-switcher__meta">
        <strong>{{ currentUser?.displayName || '未选择用户' }}</strong>
        <small :class="currentUser?.role === 'ADMIN' ? 'role--admin' : 'role--user'">
          <ShieldCheck v-if="currentUser?.role === 'ADMIN'" :size="11" aria-hidden="true" />
          {{ currentUser?.role === 'ADMIN' ? '管理员' : '普通用户' }}
        </small>
      </span>
      <ChevronDown :size="16" aria-hidden="true" />
    </button>

    <div v-if="open" class="user-switcher__overlay" @click="close"></div>
    <ul v-if="open" class="user-switcher__menu" role="menu">
      <li v-for="user in users" :key="user.id" role="none">
        <button
          role="menuitem"
          type="button"
          :class="{ active: user.id === currentUser?.id }"
          @click="pick(user)"
        >
          <span class="user-switcher__avatar user-switcher__avatar--sm">
            <UserRound :size="14" aria-hidden="true" />
          </span>
          <span class="user-switcher__meta">
            <strong>{{ user.displayName }}</strong>
            <small :class="user.role === 'ADMIN' ? 'role--admin' : 'role--user'">
              {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </small>
          </span>
        </button>
      </li>
    </ul>
  </div>
</template>
