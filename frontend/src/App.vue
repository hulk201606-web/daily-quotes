<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ArrowDownAZ,
  ArrowDownUp,
  BookOpen,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  CircleAlert,
  Database,
  Heart,
  Languages,
  Plus,
  RefreshCw,
  Search,
  Sparkles,
  Tag,
  X,
} from 'lucide-vue-next'
import QuoteCard from './components/QuoteCard.vue'
import QuoteModal from './components/QuoteModal.vue'
import UserSwitcher from './components/UserSwitcher.vue'
import TagModal from './components/TagModal.vue'
import { useSession } from './store/session'
import {
  createQuote,
  deleteQuote,
  fetchDailyQuotes,
  getStats,
  listQuotes,
  updateQuote,
} from './api/quotes'
import { listUsers } from './api/users'
import {
  addFavorite,
  listFavorites,
  listMyTags,
  listQuotesByTag,
  removeFavorite,
} from './api/personal'

const { currentUser, setCurrentUser } = useSession()

const quotes = ref([])
const stats = ref({ total: 0, internetTotal: 0, manualTotal: 0, chineseSeedTotal: 0, fetchedToday: 0, dailyLimit: 30, sourceCount: 0, sources: [] })
const users = ref([])
const myTags = ref([])
const loading = ref(true)
const loadError = ref('')
const syncing = ref(false)
const page = ref(0)
const pageSize = 20
const totalPages = ref(0)
const totalElements = ref(0)
const modalOpen = ref(false)
const editingQuote = ref(null)
const saving = ref(false)
const formError = ref('')
const confirmQuote = ref(null)
const deleting = ref(false)
const tagModalQuote = ref(null)
// 视图模式：'all' 全部名言 | 'favorites' 我的收藏 | 其余为标签 id（按个人标签浏览收录）
const view = ref('all')
const toast = reactive({ visible: false, message: '', type: 'success' })
const filters = reactive({ keyword: '', source: '', lang: '', sortBy: 'fetched', direction: 'desc' })
let searchTimer
let loadSequence = 0
let toastTimer

const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long',
}).format(new Date())
const featuredQuote = computed(() => quotes.value.find((item) => item.featured) || quotes.value[0] || null)
const progress = computed(() => {
  const limit = stats.value.dailyLimit || 30
  return Math.min(100, Math.round((stats.value.fetchedToday / limit) * 100))
})
const pageNumbers = computed(() => {
  const count = totalPages.value
  if (count <= 5) return Array.from({ length: count }, (_, index) => index)
  const start = Math.min(Math.max(page.value - 2, 0), count - 5)
  return Array.from({ length: 5 }, (_, index) => start + index)
})
const viewTitle = computed(() => {
  if (view.value === 'all') return '名言典藏'
  if (view.value === 'favorites') return '我的收藏'
  const tag = myTags.value.find((item) => String(item.id) === String(view.value))
  return tag ? `标签 · ${tag.name}` : '名言典藏'
})
const viewDescription = computed(() => {
  if (view.value === 'favorites') return '你个人收录（喜好）的名言，仅自己可见。'
  if (view.value === 'all') return '按知名度、出处或时间发现值得留存的表达。'
  return '按个人标签归类收录的名言。'
})

async function loadUsers() {
  try {
    users.value = await listUsers()
    // 若本地未选择用户或已不在用户列表中，默认取第一位（种子数据中为管理员）
    if (!currentUser.value || !users.value.some((item) => item.id === currentUser.value.id)) {
      setCurrentUser(users.value[0] || null)
    }
  } catch {
    /* 用户列表加载失败不影响名言浏览 */
  }
}

async function loadMyTags() {
  try {
    myTags.value = await listMyTags()
  } catch {
    myTags.value = []
  }
}

async function loadQuotes() {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = ''
  try {
    let result
    if (view.value === 'favorites') {
      result = await listFavorites({ page: page.value, size: pageSize })
    } else if (view.value === 'all') {
      result = await listQuotes({ ...filters, page: page.value, size: pageSize })
    } else {
      result = await listQuotesByTag(view.value, { page: page.value, size: pageSize })
    }
    if (sequence !== loadSequence) return
    quotes.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } catch (error) {
    if (sequence !== loadSequence) return
    loadError.value = error.message || '加载名言失败'
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await getStats()
  } catch {
    showToast('统计信息暂时无法加载', 'error')
  }
}

function resetAndLoad() {
  page.value = 0
  loadQuotes()
}

async function selectUser(user) {
  setCurrentUser(user)
  view.value = 'all'
  page.value = 0
  await Promise.all([loadQuotes(), loadMyTags()])
}

function goToView(target) {
  view.value = target
  page.value = 0
  loadQuotes()
  if (target !== 'all') {
    document.querySelector('#quote-library')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function showToast(message, type = 'success') {
  clearTimeout(toastTimer)
  toast.message = message
  toast.type = type
  toast.visible = true
  toastTimer = setTimeout(() => {
    toast.visible = false
  }, 3200)
}

function openCreate() {
  editingQuote.value = null
  formError.value = ''
  modalOpen.value = true
}

function openEdit(quote) {
  editingQuote.value = quote
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  if (saving.value) return
  modalOpen.value = false
}

async function saveQuote(payload) {
  saving.value = true
  formError.value = ''
  try {
    if (editingQuote.value) {
      await updateQuote(editingQuote.value.id, payload)
      showToast('名言内容已更新')
    } else {
      await createQuote(payload)
      showToast('新名言已收录')
    }
    modalOpen.value = false
    await Promise.all([loadQuotes(), loadStats()])
  } catch (error) {
    formError.value = error.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!confirmQuote.value) return
  deleting.value = true
  try {
    await deleteQuote(confirmQuote.value.id)
    confirmQuote.value = null
    if (quotes.value.length === 1 && page.value > 0) page.value--
    await Promise.all([loadQuotes(), loadStats()])
    showToast('名言已删除')
  } catch (error) {
    showToast(error.message || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

async function toggleFavorite(quote) {
  try {
    if (quote.favorited) {
      await removeFavorite(quote.id)
      quote.favorited = false
      showToast('已取消收藏')
    } else {
      await addFavorite(quote.id)
      quote.favorited = true
      showToast('已加入个人收藏')
    }
    // 在收藏视图下取消收藏后，需要从列表中移除
    if (view.value === 'favorites' && !quote.favorited) {
      loadQuotes()
    }
  } catch (error) {
    showToast(error.message || '操作失败', 'error')
  }
}

function openTags(quote) {
  tagModalQuote.value = quote
}

function closeTags() {
  tagModalQuote.value = null
}

async function onTagsSaved() {
  // 标签可能新增，刷新字典；并重新拉取当前视图以更新卡片上的个人标签
  await Promise.all([loadMyTags(), loadQuotes()])
  closeTags()
}

async function syncQuotes() {
  if (!isAdmin.value) return
  syncing.value = true
  try {
    await fetchDailyQuotes()
    await Promise.all([loadQuotes(), loadStats()])
    showToast('已从互联网收录新的名言')
  } catch (error) {
    showToast(error.message || '同步失败，请稍后重试', 'error')
  } finally {
    syncing.value = false
  }
}

function goToPage(target) {
  if (target < 0 || target >= totalPages.value || target === page.value) return
  page.value = target
  loadQuotes()
  document.querySelector('#quote-library')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function toggleDirection() {
  filters.direction = filters.direction === 'desc' ? 'asc' : 'desc'
}

watch(() => filters.keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(resetAndLoad, 350)
})
watch([() => filters.source, () => filters.lang, () => filters.sortBy, () => filters.direction], resetAndLoad)

onMounted(async () => {
  await loadUsers()
  await Promise.all([loadQuotes(), loadStats(), loadMyTags()])
})
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <a class="brand" href="#top" aria-label="言序首页">
        <span class="brand__mark">言</span>
        <span><strong>言序</strong><small>DAILY QUOTES</small></span>
      </a>
      <div class="header-actions">
        <span class="date-chip"><CalendarDays :size="16" aria-hidden="true" />{{ todayLabel }}</span>
        <UserSwitcher :users="users" :current-user="currentUser" @select="selectUser" />
        <button
          v-if="isAdmin"
          class="button button--primary button--compact"
          type="button"
          @click="openCreate"
        >
          <Plus :size="18" aria-hidden="true" />
          收录名言
        </button>
      </div>
    </header>

    <main id="top">
      <section class="hero" aria-labelledby="page-title">
        <div class="hero__main">
          <div class="hero__heading">
            <span class="eyebrow eyebrow--light">TODAY'S WORDS · 每日更新</span>
            <h1 id="page-title">让一句好话，<br /><em>开启今天。</em></h1>
          </div>

          <div v-if="featuredQuote" class="featured-quote">
            <span class="featured-quote__mark" aria-hidden="true">“</span>
            <blockquote>{{ featuredQuote.content }}</blockquote>
            <div class="featured-quote__source">
              <span></span>
              <strong>{{ featuredQuote.author }}</strong>
              <small>{{ featuredQuote.source }}</small>
            </div>
          </div>
          <div v-else class="featured-quote featured-quote--empty">
            <blockquote>今天的第一句话，等你来收录。</blockquote>
          </div>
        </div>

        <aside class="hero__aside" aria-label="内容概览">
          <article class="stat-card stat-card--accent">
            <div class="stat-card__icon"><Database :size="22" aria-hidden="true" /></div>
            <span>名言总藏</span>
            <strong>{{ stats.total }}</strong>
            <small>来自 {{ stats.sourceCount }} 个出处</small>
          </article>

          <article class="stat-card stat-card--daily">
            <div class="stat-card__top">
              <div class="stat-card__icon"><RefreshCw :size="22" aria-hidden="true" /></div>
              <span>今日采集</span>
            </div>
            <strong>{{ stats.fetchedToday }}<small>/ {{ stats.dailyLimit || 30 }}</small></strong>
            <div class="daily-progress" aria-hidden="true"><span :style="{ width: `${progress}%` }"></span></div>
            <button
              v-if="isAdmin"
              class="text-button"
              type="button"
              :disabled="syncing"
              @click="syncQuotes"
            >
              <RefreshCw :size="15" :class="{ spinning: syncing }" aria-hidden="true" />
              {{ syncing ? '正在采集…' : '立即采集' }}
            </button>
            <small v-else class="text-button text-button--hint">仅管理员可采集</small>
          </article>
        </aside>
      </section>

      <section id="quote-library" class="library" aria-labelledby="library-title">
        <div class="section-heading">
          <div>
            <span class="eyebrow">QUOTE LIBRARY</span>
            <h2 id="library-title">{{ viewTitle }}</h2>
            <p>{{ viewDescription }}</p>
          </div>
          <button
            v-if="view !== 'all'"
            class="button button--ghost button--compact"
            type="button"
            @click="goToView('all')"
          >
            <X :size="16" aria-hidden="true" />返回全部
          </button>
        </div>

        <nav class="view-tabs" aria-label="视图切换">
          <button
            class="view-tab"
            type="button"
            :class="{ active: view === 'all' }"
            :aria-current="view === 'all' ? 'page' : undefined"
            @click="goToView('all')"
          >
            <BookOpen :size="15" aria-hidden="true" />全部名言
          </button>
          <button
            class="view-tab"
            type="button"
            :class="{ active: view === 'favorites' }"
            :aria-current="view === 'favorites' ? 'page' : undefined"
            @click="goToView('favorites')"
          >
            <Heart :size="15" aria-hidden="true" />我的收藏
          </button>
          <span v-if="myTags.length" class="view-tabs__divider" aria-hidden="true"></span>
          <button
            v-for="tag in myTags"
            :key="tag.id"
            class="view-tab view-tab--tag"
            type="button"
            :class="{ active: String(view) === String(tag.id) }"
            :aria-current="String(view) === String(tag.id) ? 'page' : undefined"
            @click="goToView(String(tag.id))"
          >
            <Tag :size="14" aria-hidden="true" />{{ tag.name }}
          </button>
        </nav>

        <div v-if="view === 'all'" class="toolbar">
          <label class="search-field">
            <Search :size="19" aria-hidden="true" />
            <span class="sr-only">搜索名言</span>
            <input v-model="filters.keyword" type="search" placeholder="搜索内容、作者或出处" />
            <button v-if="filters.keyword" type="button" aria-label="清空搜索" @click="filters.keyword = ''">
              <X :size="16" aria-hidden="true" />
            </button>
          </label>

          <div class="filter-group">
            <label class="select-field">
              <BookOpen :size="17" aria-hidden="true" />
              <span class="sr-only">筛选出处</span>
              <select v-model="filters.source">
                <option value="">全部出处</option>
                <option v-for="source in stats.sources" :key="source" :value="source">{{ source }}</option>
              </select>
            </label>
            <label class="select-field">
              <Languages :size="17" aria-hidden="true" />
              <span class="sr-only">筛选语种</span>
              <select v-model="filters.lang">
                <option value="">全部语种</option>
                <option value="zh">中文</option>
                <option value="en">外文（已翻译）</option>
              </select>
            </label>
            <label class="select-field">
              <ArrowDownAZ :size="17" aria-hidden="true" />
              <span class="sr-only">排序方式</span>
              <select v-model="filters.sortBy">
                <option value="fetched">按采集时间</option>
                <option value="popularity">按知名度</option>
                <option value="source">按出处</option>
                <option value="author">按作者</option>
                <option value="latest">按收录时间</option>
              </select>
            </label>
            <button
              class="direction-button"
              type="button"
              :aria-label="filters.direction === 'desc' ? '当前降序，点击切换升序' : '当前升序，点击切换降序'"
              @click="toggleDirection"
            >
              <ArrowDownUp :size="17" aria-hidden="true" />
              {{ filters.direction === 'desc' ? '降序' : '升序' }}
            </button>
          </div>
        </div>

        <div class="results-meta" aria-live="polite">
          <span>共找到 <strong>{{ totalElements }}</strong> 条名言</span>
          <span class="results-meta__line"></span>
        </div>

        <div v-if="loading" class="quote-grid" aria-label="正在加载名言">
          <div v-for="index in 6" :key="index" class="quote-card quote-card--skeleton">
            <span></span><span></span><span></span><span></span>
          </div>
        </div>

        <div v-else-if="loadError" class="state-panel" role="alert">
          <CircleAlert :size="30" aria-hidden="true" />
          <h3>暂时无法读取名言</h3>
          <p>{{ loadError }}</p>
          <button class="button button--secondary" type="button" @click="loadQuotes">重新加载</button>
        </div>

        <div v-else-if="quotes.length === 0" class="state-panel">
          <Sparkles :size="30" aria-hidden="true" />
          <h3 v-if="view === 'favorites'">你还没有收藏任何名言</h3>
          <h3 v-else>没有找到匹配内容</h3>
          <p v-if="view === 'favorites'">浏览名言时，点击卡片上的爱心即可收录到个人收藏。</p>
          <p v-else>换个关键词或清除筛选条件再试试。</p>
          <button
            v-if="view === 'favorites'"
            class="button button--secondary"
            type="button"
            @click="goToView('all')"
          >
            去浏览名言
          </button>
          <button
            v-else
            class="button button--secondary"
            type="button"
            @click="filters.keyword = ''; filters.source = ''"
          >
            清除筛选
          </button>
        </div>

        <div v-else class="quote-grid">
          <QuoteCard
            v-for="(quote, index) in quotes"
            :key="quote.id"
            :quote="quote"
            :rank="page * pageSize + index + 1"
            :current-user="currentUser"
            @edit="openEdit"
            @delete="confirmQuote = $event"
            @toggle-favorite="toggleFavorite"
            @manage-tags="openTags"
          />
        </div>

        <nav v-if="totalPages > 1" class="pagination" aria-label="名言分页">
          <button type="button" aria-label="上一页" :disabled="page === 0" @click="goToPage(page - 1)">
            <ChevronLeft :size="18" aria-hidden="true" />
          </button>
          <button
            v-for="number in pageNumbers"
            :key="number"
            type="button"
            :class="{ active: number === page }"
            :aria-current="number === page ? 'page' : undefined"
            @click="goToPage(number)"
          >{{ number + 1 }}</button>
          <button type="button" aria-label="下一页" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
            <ChevronRight :size="18" aria-hidden="true" />
          </button>
        </nav>
      </section>
    </main>

    <footer class="site-footer">
      <span class="brand brand--footer"><span class="brand__mark">言</span><strong>言序</strong></span>
      <p>每天 06:30 自动采集 100 条互联网名言，愿好文字常伴左右。</p>
      <span>Java × Vue 3</span>
    </footer>

    <QuoteModal
      v-if="modalOpen"
      :key="editingQuote?.id || 'new'"
      :quote="editingQuote"
      :sources="stats.sources"
      :saving="saving"
      :server-error="formError"
      @close="closeModal"
      @save="saveQuote"
    />

    <TagModal
      v-if="tagModalQuote"
      :quote="tagModalQuote"
      :tags="myTags"
      @close="closeTags"
      @saved="onTagsSaved"
    />

    <Teleport to="body">
      <div
        v-if="confirmQuote"
        class="modal-backdrop modal-backdrop--confirm"
        @mousedown.self="!deleting && (confirmQuote = null)"
      >
        <section class="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-description">
          <div class="confirm-dialog__icon"><CircleAlert :size="24" aria-hidden="true" /></div>
          <h2 id="confirm-title">确认删除这条名言？</h2>
          <p id="confirm-description">“{{ confirmQuote.content }}”</p>
          <small>删除后无法恢复，请谨慎操作。</small>
          <div class="confirm-dialog__actions">
            <button class="button button--ghost" type="button" :disabled="deleting" @click="confirmQuote = null">保留</button>
            <button class="button button--danger" type="button" :disabled="deleting" @click="confirmDelete">
              {{ deleting ? '删除中…' : '确认删除' }}
            </button>
          </div>
        </section>
      </div>
    </Teleport>

    <Transition name="toast">
      <div v-if="toast.visible" class="toast" :class="`toast--${toast.type}`" role="status">
        <span></span>{{ toast.message }}
      </div>
    </Transition>
  </div>
</template>
