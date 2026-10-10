<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { Plus, Tag as TagIcon, X } from 'lucide-vue-next'
import {
  addQuoteTag,
  createTag,
  listQuoteTags,
  removeQuoteTag,
} from '../api/personal'

const props = defineProps({
  quote: { type: Object, required: true },
  // 当前用户的标签字典：[{ id, name }]
  tags: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'saved'])

const dialog = ref(null)
const previousFocus = document.activeElement
const currentTags = ref([])
const selectedTagId = ref(null)
const newTagName = ref('')
const pending = ref(false)
const error = ref('')

const availableTags = () => {
  const applied = new Set(currentTags.value.map((tag) => tag.id))
  return props.tags.filter((tag) => !applied.has(tag.id))
}

async function loadCurrentTags() {
  try {
    currentTags.value = await listQuoteTags(props.quote.id)
  } catch (exception) {
    error.value = exception.message || '读取标签失败'
  }
}

async function addExisting() {
  if (!selectedTagId.value) return
  pending.value = true
  error.value = ''
  try {
    await addQuoteTag(props.quote.id, selectedTagId.value)
    selectedTagId.value = null
    await loadCurrentTags()
    emit('saved', { created: null })
  } catch (exception) {
    error.value = exception.message || '添加标签失败'
  } finally {
    pending.value = false
  }
}

async function addNew() {
  const name = newTagName.value.trim()
  if (!name) return
  pending.value = true
  error.value = ''
  try {
    const created = await createTag(name)
    await addQuoteTag(props.quote.id, created.id)
    newTagName.value = ''
    await loadCurrentTags()
    emit('saved', { created })
  } catch (exception) {
    error.value = exception.message || '创建标签失败'
  } finally {
    pending.value = false
  }
}

async function remove(tagId) {
  pending.value = true
  error.value = ''
  try {
    await removeQuoteTag(props.quote.id, tagId)
    await loadCurrentTags()
    emit('saved', { created: null })
  } catch (exception) {
    error.value = exception.message || '移除标签失败'
  } finally {
    pending.value = false
  }
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(async () => {
  document.body.classList.add('modal-open')
  document.addEventListener('keydown', handleKeydown)
  await nextTick()
  await loadCurrentTags()
  dialog.value?.querySelector('input, select, button')?.focus?.()
})

onBeforeUnmount(() => {
  document.body.classList.remove('modal-open')
  document.removeEventListener('keydown', handleKeydown)
  previousFocus?.focus?.()
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @mousedown.self="emit('close')">
      <section ref="dialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="tag-modal-title">
        <header class="modal__header">
          <div>
            <span class="eyebrow">PERSONAL TAGS</span>
            <h2 id="tag-modal-title">管理个人标签</h2>
          </div>
          <button class="icon-button" type="button" aria-label="关闭弹窗" @click="emit('close')">
            <X :size="20" aria-hidden="true" />
          </button>
        </header>

        <div class="tag-modal">
          <p class="tag-modal__quote">“{{ quote.content }}”</p>

          <div v-if="error" class="form-alert" role="alert">{{ error }}</div>

          <section class="tag-section">
            <h3>已打标签</h3>
            <div v-if="currentTags.length" class="tag-list">
              <span v-for="tag in currentTags" :key="tag.id" class="tag-pill">
                {{ tag.name }}
                <button
                  class="tag-pill__remove"
                  type="button"
                  :disabled="pending"
                  :aria-label="`移除标签 ${tag.name}`"
                  @click="remove(tag.id)"
                >
                  <X :size="13" aria-hidden="true" />
                </button>
              </span>
            </div>
            <p v-else class="tag-empty">还没有为这条名言打标签。</p>
          </section>

          <section class="tag-section">
            <h3>添加已有标签</h3>
            <div class="tag-add">
              <select v-model="selectedTagId" :disabled="pending">
                <option :value="null">选择标签…</option>
                <option v-for="tag in availableTags()" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
              </select>
              <button
                class="button button--secondary button--compact"
                type="button"
                :disabled="pending || !selectedTagId"
                @click="addExisting"
              >
                <Plus :size="16" aria-hidden="true" />添加
              </button>
            </div>
          </section>

          <section class="tag-section">
            <h3>或新建标签</h3>
            <div class="tag-add">
              <input
                v-model="newTagName"
                maxlength="64"
                placeholder="如：治愈、励志、职场"
                :disabled="pending"
                @keyup.enter="addNew"
              />
              <button
                class="button button--primary button--compact"
                type="button"
                :disabled="pending || !newTagName.trim()"
                @click="addNew"
              >
                <TagIcon :size="16" aria-hidden="true" />新建并添加
              </button>
            </div>
          </section>
        </div>
      </section>
    </div>
  </Teleport>
</template>
