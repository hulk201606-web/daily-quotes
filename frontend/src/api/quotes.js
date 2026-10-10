import { request } from './client'

function toQuery(params) {
  const query = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      query.set(key, String(value))
    }
  })
  return query.toString()
}

export function listQuotes(params) {
  return request(`/quotes?${toQuery(params)}`)
}

export function getStats() {
  return request('/quotes/stats')
}

export function createQuote(payload) {
  return request('/quotes', { method: 'POST', body: JSON.stringify(payload) })
}

export function updateQuote(id, payload) {
  return request(`/quotes/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
}

export function deleteQuote(id) {
  return request(`/quotes/${id}`, { method: 'DELETE' })
}

export function fetchDailyQuotes() {
  return request('/quotes/fetch', { method: 'POST' })
}
