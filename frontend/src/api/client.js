import { currentUser } from '../store/session'

const BASE = '/api'

// 统一请求封装：自动附带当前用户身份头 X-User-Id，并归一化错误为带状态码与字段错误的 Error。
export async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  if (currentUser.value && currentUser.value.id != null) {
    headers['X-User-Id'] = String(currentUser.value.id)
  }

  const response = await fetch(`${BASE}${path}`, { headers, ...options })

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    const error = new Error(payload?.message || `请求失败（${response.status}）`)
    error.status = response.status
    error.fieldErrors = payload?.fieldErrors || {}
    throw error
  }

  if (response.status === 204) return null
  return response.json()
}
