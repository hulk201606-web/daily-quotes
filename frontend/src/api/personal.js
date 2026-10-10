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

// 个人标签字典
export function listMyTags() {
  return request('/personal/tags')
}

export function createTag(name) {
  return request('/personal/tags', { method: 'POST', body: JSON.stringify({ name }) })
}

export function deleteTag(tagId) {
  return request(`/personal/tags/${tagId}`, { method: 'DELETE' })
}

// 个人收藏（喜好收录）
export function listFavorites(params) {
  return request(`/personal/favorites?${toQuery(params)}`)
}

export function addFavorite(quoteId) {
  return request(`/personal/quotes/${quoteId}/favorite`, { method: 'POST' })
}

export function removeFavorite(quoteId) {
  return request(`/personal/quotes/${quoteId}/favorite`, { method: 'DELETE' })
}

// 名言打个人标签
export function listQuoteTags(quoteId) {
  return request(`/personal/quotes/${quoteId}/tags`)
}

export function addQuoteTag(quoteId, tagId) {
  return request(`/personal/quotes/${quoteId}/tags`, { method: 'POST', body: JSON.stringify({ tagId }) })
}

export function removeQuoteTag(quoteId, tagId) {
  return request(`/personal/quotes/${quoteId}/tags/${tagId}`, { method: 'DELETE' })
}

// 按个人标签浏览收录
export function listQuotesByTag(tagId, params) {
  const query = toQuery({ tagId, ...params })
  return request(`/personal/quotes?${query}`)
}
