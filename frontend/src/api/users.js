import { request } from './client'

export function listUsers() {
  return request('/users')
}

export function getCurrentUser() {
  return request('/users/me')
}
