<template>
  <div class="page news-list-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">新闻中心</h1>
        <p class="page-subtitle">查看公司公告、制度流程和团队动态。</p>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题/摘要" />
      </el-form-item>
      <el-form-item label="分类">
        <DictSelect v-model="query.category" type-code="news_category" />
      </el-form-item>
    </SearchPanel>

    <section v-loading="loading" class="news-list panel">
      <button v-for="item in records" :key="item.id" type="button" class="news-item" @click="router.push(`/news/${item.id}`)">
        <div class="news-cover">
          <img
            v-if="item.coverImage && !brokenImageIds.has(item.id)"
            :src="item.coverImage"
            :alt="item.title"
            @error="markImageBroken(item.id)"
          />
          <span v-else>{{ dictStore.getLabel('news_category', item.category) }}</span>
        </div>
        <div class="news-copy">
          <div class="news-meta">
            <el-tag v-if="item.isTop" type="danger" effect="light">置顶</el-tag>
            <span>{{ dictStore.getLabel('news_category', item.category) }}</span>
            <span>{{ formatDateTime(item.publishedAt) }}</span>
          </div>
          <h2>{{ item.title }}</h2>
          <p>{{ item.summary || '暂无摘要' }}</p>
          <div class="news-stats">
            <span>阅读 {{ item.viewCount || 0 }}</span>
            <span>点赞 {{ item.likeCount || 0 }}</span>
            <span>评论 {{ item.commentCount || 0 }}</span>
          </div>
        </div>
      </button>
      <el-empty v-if="records.length === 0 && !loading" description="暂无新闻" />
      <div class="news-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          @update:current-page="(value: number) => (query.pageNum = value)"
          @update:page-size="(value: number) => (query.pageSize = value)"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { newsApi } from '@/api/news';
import type { NewsRecord, PageQuery } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import { useDictStore } from '@/stores/dict';
import { formatDateTime } from '@/utils/format';

const router = useRouter();
const dictStore = useDictStore();
const loading = ref(false);
const records = ref<NewsRecord[]>([]);
const total = ref(0);
const brokenImageIds = ref(new Set<number>());

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: '',
  status: 'PUBLISHED'
});

async function load() {
  loading.value = true;
  brokenImageIds.value = new Set();
  try {
    const page = await newsApi.page(query);
    records.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  if (query.pageNum === 1) load();
  else query.pageNum = 1;
}

function reset() {
  const shouldLoad = query.pageNum === 1 && query.pageSize === 10;
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', category: '', status: 'PUBLISHED' });
  if (shouldLoad) load();
}

function markImageBroken(id: number) {
  brokenImageIds.value = new Set([...brokenImageIds.value, id]);
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  dictStore.fetchDict('news_category');
  load();
});
</script>

<style scoped>
.news-list {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.news-item {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
  gap: 18px;
  width: 100%;
  padding: 16px;
  border: 0;
  border-bottom: 1px solid var(--oa-border);
  background: #ffffff;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.news-cover {
  display: grid;
  aspect-ratio: 16 / 9;
  place-items: center;
  overflow: hidden;
  border-radius: 8px;
  background: #eef2ff;
  color: #3730a3;
  font-weight: 650;
}

.news-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.news-copy {
  min-width: 0;
}

.news-meta,
.news-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: var(--oa-muted);
}

.news-copy h2 {
  margin: 10px 0 8px;
  font-size: 18px;
}

.news-copy p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0 0 16px;
  color: #475569;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.news-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

@media (max-width: 640px) {
  .news-item {
    grid-template-columns: 1fr;
  }

  .news-pagination {
    justify-content: flex-start;
  }

  .news-pagination :deep(.el-pagination) {
    flex-wrap: wrap;
  }
}
</style>
