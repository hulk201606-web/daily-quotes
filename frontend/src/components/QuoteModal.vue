<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Save, Star, X } from 'lucide-vue-next'

const props = defineProps({
  quote: { type: Object, default: null },
  sources: { type: Array, default: () => [] },
  saving: { type: Boolean, default: false },
  serverError: { type: String, default: '' },
})

const emit = defineEmits(['close', 'save'])
const dialog = ref(null)
const contentInput = ref(null)
const previousFocus = document.activeElement
const errors = reactive({})
const form = reactive({
  content: props.quote?.content || '',
  author: props.quote?.author || '',
  source: props.quote?.source || '',
  popularity: props.quote?.popularity ?? 80,
  featured: props.quote?.featured || false,
})

function validate() {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!form.content.trim()) errors.content = '请输入名言内容'
  if (!form.author.trim()) errors.author = '请输入作者'
  if (!form.source.trim()) errors.source = '请输入出处'
  if (form.popularity < 1 || form.popularity > 100) errors.popularity = '知名度应为 1—100'
  return Object.keys(errors).length === 0
}

function submit() {
  if (!validate()) return
  emit('save', {
    content: form.content.trim(),
    author: form.author.trim(),
    source: form.source.trim(),
    popularity: Number(form.popularity),
    featured: form.featured,
  })
}

function handleKeydown(event) {
  if (event.key === 'Escape' && !props.saving) {
    emit('close')
    return
  }
  if (event.key !== 'Tab' || !dialog.value) return
  const focusable = [...dialog.value.querySelectorAll('button:not([disabled]), input:not([disabled]), textarea:not([disabled])')]
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(async () => {
  document.body.classList.add('modal-open')
  document.addEventListener('keydown', handleKeydown)
  await nextTick()
  contentInput.value?.focus()
})

onBeforeUnmount(() => {
  document.body.classList.remove('modal-open')
  document.removeEventListener('keydown', handleKeydown)
  previousFocus?.focus?.()
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @mousedown.self="!saving && emit('close')">
      <section ref="dialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="quote-modal-title">
        <header class="modal__header">
          <div>
            <span class="eyebrow">CONTENT STUDIO</span>
            <h2 id="quote-modal-title">{{ quote ? '编辑名言' : '收录新名言' }}</h2>
          </div>
          <button class="icon-button" type="button" aria-label="关闭弹窗" :disabled="saving" @click="emit('close')">
            <X :size="20" aria-hidden="true" />
          </button>
        </header>

        <form class="quote-form" novalidate @submit.prevent="submit">
          <div v-if="serverError" class="form-alert" role="alert">{{ serverError }}</div>

          <label class="field field--full">
            <span>名言内容</span>
            <textarea
              ref="contentInput"
              v-model="form.content"
              rows="4"
              maxlength="2000"
              placeholder="输入值得被记住的一句话"
              :aria-invalid="Boolean(errors.content)"
              :aria-describedby="errors.content ? 'content-error' : undefined"
            ></textarea>
            <small v-if="errors.content" id="content-error" class="field-error">{{ errors.content }}</small>
            <small v-else class="field-hint">{{ form.content.length }} / 2000</small>
          </label>

          <div class="form-grid">
            <label class="field">
              <span>作者</span>
              <input v-model="form.author" maxlength="120" placeholder="如：孔子" :aria-invalid="Boolean(errors.author)" />
              <small v-if="errors.author" class="field-error">{{ errors.author }}</small>
            </label>
            <label class="field">
              <span>出处</span>
              <input v-model="form.source" maxlength="180" list="quote-source-list" placeholder="如：《论语·雍也》" :aria-invalid="Boolean(errors.source)" />
              <datalist id="quote-source-list">
                <option v-for="source in sources" :key="source" :value="source" />
              </datalist>
              <small v-if="errors.source" class="field-error">{{ errors.source }}</small>
            </label>
          </div>

          <div class="popularity-field">
            <div class="popularity-field__heading">
              <label for="popularity-range">知名度</label>
              <output for="popularity-range">{{ form.popularity }}</output>
            </div>
            <input id="popularity-range" v-model.number="form.popularity" type="range" min="1" max="100" />
            <div class="range-labels"><span>小众发现</span><span>广为人知</span></div>
            <small v-if="errors.popularity" class="field-error">{{ errors.popularity }}</small>
          </div>

          <label class="featured-option">
            <input v-model="form.featured" type="checkbox" />
            <span class="featured-option__check"><Star :size="17" aria-hidden="true" /></span>
            <span><strong>设为精选</strong><small>优先出现在今日焦点区域</small></span>
          </label>

          <footer class="modal__footer">
            <button class="button button--ghost" type="button" :disabled="saving" @click="emit('close')">取消</button>
            <button class="button button--primary" type="submit" :disabled="saving">
              <span v-if="saving" class="spinner" aria-hidden="true"></span>
              <Save v-else :size="18" aria-hidden="true" />
              {{ saving ? '保存中…' : '保存内容' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>
