<template>
  <div class="page news-detail-page">
    <div class="page-header news-detail-toolbar">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <div class="toolbar-actions">
        <el-button :icon="Pointer" :type="news?.liked ? 'primary' : 'default'" @click="toggleLike">点赞 {{ news?.likeCount || 0 }}</el-button>
        <el-button :icon="Star" :type="news?.favorited ? 'primary' : 'default'" @click="toggleFavorite">收藏 {{ news?.favoriteCount || 0 }}</el-button>
      </div>
    </div>

    <article class="article-shell">
      <el-skeleton v-if="loading" :rows="10" animated />
      <template v-else-if="news">
        <div class="cover-hero" :class="{ 'cover-hero--empty': !news.coverImage }">
          <img v-if="news.coverImage" :src="news.coverImage" :alt="news.title" />
          <div v-else class="cover-placeholder">
            <span>{{ dictStore.getLabel('news_category', news.category) || '新闻' }}</span>
          </div>
          <div class="cover-shade"></div>
          <div class="hero-copy">
            <div class="article-meta">
              <el-tag effect="light">{{ dictStore.getLabel('news_category', news.category) }}</el-tag>
              <span>{{ news.publisherName || '系统发布' }}</span>
              <span>{{ formatDateTime(news.publishedAt) }}</span>
            </div>
            <h1>{{ news.title }}</h1>
            <p v-if="news.summary" class="summary">{{ news.summary }}</p>
          </div>
        </div>

        <div class="article-grid">
          <aside class="article-side">
            <div class="side-stat">
              <strong>{{ news.viewCount || 0 }}</strong>
              <span>阅读</span>
            </div>
            <div class="side-stat">
              <strong>{{ news.likeCount || 0 }}</strong>
              <span>点赞</span>
            </div>
            <div class="side-stat">
              <strong>{{ news.commentCount || comments.length || 0 }}</strong>
              <span>评论</span>
            </div>
          </aside>

          <div class="article-body">
            <div class="content">
              <p v-for="paragraph in contentParagraphs" :key="paragraph">{{ paragraph }}</p>
            </div>
          </div>
        </div>
      </template>
    </article>

    <section class="comment-panel">
      <div class="section-head">
        <div>
          <h2>评论</h2>
          <p>围绕本条新闻进行反馈和补充。</p>
        </div>
        <span class="muted">{{ comments.length }} 条</span>
      </div>
      <el-input v-model="commentText" type="textarea" :rows="3" placeholder="写下你的评论" />
      <div class="comment-actions">
        <el-button type="primary" :loading="commenting" @click="addComment">发表评论</el-button>
      </div>
      <div class="comment-list">
        <div v-for="item in comments" :key="item.id" class="comment-item">
          <el-avatar :size="32">{{ (item.userName || 'U').slice(0, 1) }}</el-avatar>
          <div>
            <strong>{{ item.userName || item.userId }}</strong>
            <p>{{ item.content }}</p>
            <small>{{ formatDateTime(item.createdAt) }}</small>
          </div>
          <el-button v-if="userStore.isAdmin" text type="danger" @click="removeComment(item.id)">删除</el-button>
        </div>
        <el-empty v-if="comments.length === 0" description="暂无评论" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, Pointer, Star } from '@element-plus/icons-vue';
import { newsApi } from '@/api/news';
import type { NewsComment, NewsRecord } from '@/api/types';
import { useDictStore } from '@/stores/dict';
import { useUserStore } from '@/stores/user';
import { formatDateTime } from '@/utils/format';

const route = useRoute();
const router = useRouter();
const dictStore = useDictStore();
const userStore = useUserStore();
const id = Number(route.params.id);
const loading = ref(false);
const commenting = ref(false);
const news = ref<NewsRecord | null>(null);
const comments = ref<NewsComment[]>([]);
const commentText = ref('');

const contentParagraphs = computed(() => {
  return (news.value?.content || '')
    .split(/\n+/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean);
});

async function load() {
  loading.value = true;
  try {
    const [detail, commentList] = await Promise.all([newsApi.detail(id), newsApi.comments(id), newsApi.view(id)]);
    news.value = detail;
    comments.value = commentList;
  } finally {
    loading.value = false;
  }
}

async function toggleLike() {
  if (!news.value) return;
  if (news.value.liked) {
    await newsApi.unlike(news.value.id);
    news.value.likeCount = Math.max(0, (news.value.likeCount || 1) - 1);
  } else {
    await newsApi.like(news.value.id);
    news.value.likeCount = (news.value.likeCount || 0) + 1;
  }
  news.value.liked = !news.value.liked;
}

async function toggleFavorite() {
  if (!news.value) return;
  if (news.value.favorited) {
    await newsApi.unfavorite(news.value.id);
    news.value.favoriteCount = Math.max(0, (news.value.favoriteCount || 1) - 1);
  } else {
    await newsApi.favorite(news.value.id);
    news.value.favoriteCount = (news.value.favoriteCount || 0) + 1;
  }
  news.value.favorited = !news.value.favorited;
}

async function addComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论内容');
    return;
  }
  commenting.value = true;
  try {
    const item = await newsApi.addComment(id, commentText.value);
    comments.value.unshift(item);
    commentText.value = '';
    ElMessage.success('评论已发布');
  } finally {
    commenting.value = false;
  }
}

async function removeComment(commentId: number) {
  await ElMessageBox.confirm('确认删除该评论吗？', '删除评论', { type: 'warning' });
  await newsApi.removeComment(commentId);
  comments.value = comments.value.filter((item) => item.id !== commentId);
  ElMessage.success('评论已删除');
}

onMounted(() => {
  dictStore.fetchDict('news_category');
  load();
});
</script>

<style scoped>
.news-detail-page {
  --article-text: #172033;
  --article-muted: #64748b;
  max-width: 1280px;
}

.news-detail-toolbar {
  margin-bottom: 12px;
}

.article-shell,
.comment-panel {
  overflow: hidden;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 42px rgb(15 23 42 / 6%);
}

.cover-hero {
  position: relative;
  min-height: 360px;
  overflow: hidden;
  background: linear-gradient(135deg, #19345f 0%, #475569 100%);
}

.cover-hero img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-hero--empty {
  min-height: 300px;
}

.cover-placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgb(255 255 255 / 72%);
  font-size: 72px;
  font-weight: 800;
  letter-spacing: 0;
}

.cover-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgb(15 23 42 / 10%) 0%, rgb(15 23 42 / 72%) 100%),
    linear-gradient(90deg, rgb(15 23 42 / 65%) 0%, rgb(15 23 42 / 14%) 62%);
}

.hero-copy {
  position: absolute;
  left: clamp(24px, 5vw, 64px);
  right: clamp(24px, 5vw, 64px);
  bottom: clamp(26px, 5vw, 54px);
  max-width: 840px;
  color: #fff;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: rgb(255 255 255 / 82%);
}

.hero-copy h1 {
  margin: 0;
  color: #fff;
  font-size: clamp(28px, 3vw, 42px);
  line-height: 1.22;
  letter-spacing: 0;
}

.summary {
  max-width: 720px;
  margin: 16px 0 0;
  color: rgb(255 255 255 / 88%);
  font-size: 16px;
  line-height: 1.75;
}

.article-grid {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 34px;
  padding: 34px clamp(24px, 5vw, 58px) 46px;
}

.article-side {
  position: sticky;
  top: 92px;
  align-self: start;
  display: grid;
  gap: 10px;
}

.side-stat {
  display: grid;
  gap: 3px;
  padding: 14px 12px;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  background: #f8fbff;
  text-align: center;
}

.side-stat strong {
  color: var(--article-text);
  font-size: 22px;
  line-height: 1.1;
}

.side-stat span {
  color: var(--article-muted);
  font-size: 12px;
}

.article-body {
  min-width: 0;
  color: var(--article-text);
}

.content {
  max-width: 880px;
  font-size: 16px;
  line-height: 1.95;
}

.content :deep(p) {
  margin: 0 0 18px;
}

.content :deep(h1),
.content :deep(h2),
.content :deep(h3) {
  margin: 28px 0 12px;
  color: var(--article-text);
  line-height: 1.35;
  letter-spacing: 0;
}

.content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.comment-panel {
  margin-top: 18px;
  padding: 26px clamp(20px, 4vw, 38px) 30px;
}

.section-head,
.comment-actions {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  color: var(--article-text);
  font-size: 20px;
}

.section-head p {
  margin: 6px 0 0;
  color: var(--article-muted);
}

.comment-actions {
  justify-content: flex-end;
  margin-top: 10px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  gap: 10px;
  padding: 16px 0 0;
  border-top: 1px solid var(--oa-border);
}

.comment-item p {
  margin: 6px 0;
  line-height: 1.6;
}

.comment-item small {
  color: var(--oa-muted);
}

@media (max-width: 900px) {
  .cover-hero {
    min-height: 300px;
  }

  .article-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .article-side {
    position: static;
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .news-detail-toolbar {
    align-items: stretch;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .toolbar-actions .el-button {
    flex: 1;
  }

  .cover-hero {
    min-height: 260px;
  }

  .hero-copy {
    left: 18px;
    right: 18px;
    bottom: 22px;
  }

  .article-grid {
    padding: 22px 18px 28px;
  }

  .comment-item {
    grid-template-columns: 32px minmax(0, 1fr);
  }

  .comment-item .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
