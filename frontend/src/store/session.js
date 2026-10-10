import { ref } from 'vue'

// 当前登录用户（轻量身份：与后端 X-User-Id 对应）。
// 系统无独立鉴权中心，前端通过「切换用户」模拟登录，选择持久化在 localStorage。
const STORAGE_KEY = 'daily-quotes:current-user'

const stored = (() => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
})()

export const currentUser = ref(stored)

export function useSession() {
  function setCurrentUser(user) {
    currentUser.value = user
    try {
      if (user) localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
      else localStorage.removeItem(STORAGE_KEY)
    } catch {
      /* localStorage 不可用时忽略 */
    }
  }

  return { currentUser, setCurrentUser }
}
