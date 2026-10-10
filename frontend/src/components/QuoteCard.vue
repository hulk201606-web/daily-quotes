<script setup>
import { computed, ref, watch } from 'vue'
import { BookOpen, Globe2, Heart, Languages, Pencil, Sparkles, Tag, Trash2 } from 'lucide-vue-next'

const props = defineProps({
  quote: { type: Object, required: true },
  rank: { type: Number, required: true },
  currentUser: { type: Object, default: null },
})

const emit = defineEmits(['edit', 'delete', 'toggle-favorite', 'manage-tags'])

const isAdmin = computed(() => props.currentUser?.role === 'ADMIN')
const favorited = computed(() => Boolean(props.quote.favorited))
// 外文名言经采集后翻译为中文，originalContent 保留原文；可在此切换查看原文
const hasOriginal = computed(() => Boolean(props.quote.originalContent))
const showOriginal = ref(false)
const displayedContent = computed(() =>
  showOriginal.value && hasOriginal.value ? props.quote.originalContent : props.quote.content,
)
// 切换名言时重置「查看原文」状态
watch(() => props.quote.id, () => {
  showOriginal.value = false
})
const originLabel = computed(
  () =>
    ({
      INTERNET: '每日采集',
      MANUAL: '人工维护',
      SEED: '内置精选',
      CN_SEED: '中文精选',
      CN_INTERNET: '中文在线',
    }[props.quote.origin] || '精选内容'),
)
</script>

<template>
  <article class="quote-card" :class="{ 'quote-card--featured': quote.featured }">
    <header class="quote-card__header">
      <div class="rank-mark" :aria-label="`知名度排序第 ${rank} 位`">
        <span>NO.</span>
        <strong>{{ String(rank).padStart(2, '0') }}</strong>
      </div>
      <span class="origin-tag" :class="`origin-tag--${quote.origin?.toLowerCase()}`">
        <Globe2 v-if="quote.origin === 'INTERNET'" :size="14" aria-hidden="true" />
        <Sparkles v-else :size="14" aria-hidden="true" />
        {{ originLabel }}
      </span>
    </header>

    <blockquote>
      <span class="quote-mark" aria-hidden="true">“</span>
      <p>{{ displayedContent }}</p>
    </blockquote>

    <button
      v-if="hasOriginal"
      class="original-toggle"
      type="button"
      :aria-pressed="showOriginal"
      @click="showOriginal = !showOriginal"
    >
      <Languages :size="14" aria-hidden="true" />
      {{ showOriginal ? '查看译文' : '查看原文' }}
    </button>

    <div class="quote-card__identity">
      <strong>{{ quote.author }}</strong>
      <span class="source-line">
        <BookOpen :size="15" aria-hidden="true" />
        {{ quote.source }}
      </span>
    </div>

    <div v-if="quote.myTags && quote.myTags.length" class="quote-card__tags">
      <span v-for="tag in quote.myTags" :key="tag" class="tag-chip">
        <Tag :size="11" aria-hidden="true" />{{ tag }}
      </span>
    </div>

    <footer class="quote-card__footer">
      <div class="popularity" :aria-label="`知名度 ${quote.popularity} 分`">
        <div class="popularity__label">
          <span>知名度</span>
          <strong>{{ quote.popularity }}</strong>
        </div>
        <div class="popularity__track" aria-hidden="true">
          <span :style="{ width: `${quote.popularity}%` }"></span>
        </div>
      </div>
      <div class="card-actions">
        <button
          class="icon-button"
          type="button"
          :class="{ 'icon-button--active': favorited }"
          :aria-pressed="favorited"
          :aria-label="favorited ? '取消收藏' : '加入个人收藏'"
          @click="emit('toggle-favorite', quote)"
        >
          <Heart :size="17" :fill="favorited ? 'currentColor' : 'none'" aria-hidden="true" />
        </button>
        <button
          class="icon-button"
          type="button"
          aria-label="管理个人标签"
          @click="emit('manage-tags', quote)"
        >
          <Tag :size="17" aria-hidden="true" />
        </button>
        <template v-if="isAdmin">
          <button
            class="icon-button"
            type="button"
            :aria-label="`编辑 ${quote.author} 的名言`"
            @click="emit('edit', quote)"
          >
            <Pencil :size="17" aria-hidden="true" />
          </button>
          <button
            class="icon-button icon-button--danger"
            type="button"
            :aria-label="`删除 ${quote.author} 的名言`"
            @click="emit('delete', quote)"
          >
            <Trash2 :size="17" aria-hidden="true" />
          </button>
        </template>
      </div>
    </footer>
  </article>
</template>
