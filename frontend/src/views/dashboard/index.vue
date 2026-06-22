<template>
  <div class="page dashboard-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">首页仪表盘</h1>
        <p class="page-subtitle">集中查看审批、日程、通知和新闻状态。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="metric-grid">
      <button v-for="item in metrics" :key="item.label" type="button" class="metric panel" @click="router.push(item.path)">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </button>
    </section>

    <section class="dashboard-workspace">
      <div class="panel panel-body chart-panel">
        <div class="section-head">
          <h2>审批类型统计</h2>
          <span class="muted">按当前可见审批聚合</span>
        </div>
        <div v-show="hasChartData" ref="chartRef" class="chart" />
        <div v-if="!hasChartData" class="chart-empty">
          <el-empty description="暂无审批统计数据" />
        </div>
      </div>

      <div class="side-stack">
        <div class="panel panel-body quick-panel">
          <div class="section-head">
            <h2>快捷入口</h2>
          </div>
          <div class="quick-actions">
            <el-button :icon="EditPen" type="primary" @click="router.push('/approvals/create')">发起审批</el-button>
            <el-button :icon="Calendar" @click="router.push('/schedules/calendar')">查看日历</el-button>
            <el-button :icon="Reading" @click="router.push('/news')">新闻中心</el-button>
            <el-button :icon="MagicStick" @click="router.push('/ai')">AI 助手</el-button>
          </div>
        </div>

        <div class="panel panel-body brief-panel">
          <div class="section-head">
            <h2>今日概览</h2>
          </div>
          <div class="brief-list">
            <span>审批待办</span>
            <strong>{{ todoCount }}</strong>
            <span>日程安排</span>
            <strong>{{ todaySchedules.length }}</strong>
            <span>未读通知</span>
            <strong>{{ notificationStore.unreadCount }}</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="dashboard-columns">
      <div class="panel panel-body">
        <div class="section-head">
          <h2>近期审批</h2>
          <el-button text type="primary" @click="router.push('/approvals')">更多</el-button>
        </div>
        <el-table :data="visibleRecentApprovals" size="small" :loading="loading">
          <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
          <el-table-column label="类型" width="92">
            <template #default="{ row }">{{ dictStore.getLabel('approval_type', row.type) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><StatusTag :status="row.status" mode="approval" /></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel panel-body">
        <div class="section-head">
          <h2>今日日程</h2>
          <el-button text type="primary" @click="router.push('/schedules')">更多</el-button>
        </div>
        <el-table :data="visibleTodaySchedules" size="small" :loading="loading">
          <el-table-column prop="title" label="日程" min-width="150" show-overflow-tooltip />
          <el-table-column prop="startTime" label="开始时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel panel-body">
        <div class="section-head">
          <h2>最新新闻</h2>
          <el-button text type="primary" @click="router.push('/news')">更多</el-button>
        </div>
        <div class="news-list">
          <button v-for="item in visibleLatestNews" :key="item.id" type="button" @click="router.push(`/news/${item.id}`)">
            <strong>{{ item.title }}</strong>
            <small>{{ item.summary || formatDateTime(item.publishedAt || item.createdAt) }}</small>
          </button>
          <el-empty v-if="latestNews.length === 0 && !loading" description="暂无新闻" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import * as echarts from 'echarts';
import { Bell, Calendar, EditPen, MagicStick, Reading, Refresh, Tickets } from '@element-plus/icons-vue';
import { approvalsApi } from '@/api/approvals';
import { schedulesApi } from '@/api/schedules';
import { newsApi } from '@/api/news';
import type { ApprovalRecord, NewsRecord, ScheduleRecord } from '@/api/types';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { useNotificationStore } from '@/stores/notification';
import { useUserStore } from '@/stores/user';
import { formatDateTime } from '@/utils/format';

const router = useRouter();
const dictStore = useDictStore();
const userStore = useUserStore();
const notificationStore = useNotificationStore();

const loading = ref(false);
const todoCount = ref(0);
const todaySchedules = ref<ScheduleRecord[]>([]);
const latestNews = ref<NewsRecord[]>([]);
const recentApprovals = ref<ApprovalRecord[]>([]);
const chartRef = ref<HTMLDivElement>();
let chart: echarts.ECharts | null = null;
const DASHBOARD_CARD_LIMIT = 5;

const hasChartData = computed(() => recentApprovals.value.length > 0);
const visibleRecentApprovals = computed(() => recentApprovals.value.slice(0, DASHBOARD_CARD_LIMIT));
const visibleTodaySchedules = computed(() => todaySchedules.value.slice(0, DASHBOARD_CARD_LIMIT));
const visibleLatestNews = computed(() => latestNews.value.slice(0, DASHBOARD_CARD_LIMIT));

const metrics = computed(() => [
  { label: '审批待办', value: todoCount.value, path: '/approvals/todo', icon: Tickets },
  { label: '今日日程', value: todaySchedules.value.length, path: '/schedules', icon: Calendar },
  { label: '未读通知', value: notificationStore.unreadCount, path: '/notifications', icon: Bell },
  { label: '新闻数量', value: latestNews.value.length, path: '/news', icon: Reading }
]);

async function load() {
  loading.value = true;
  try {
    await Promise.all([dictStore.fetchDict('approval_type'), notificationStore.fetchUnreadCount()]);

    const [approvalPage, schedules, newsPage, todoPage] = await Promise.all([
      userStore.isAdmin ? approvalsApi.page({ pageNum: 1, pageSize: DASHBOARD_CARD_LIMIT }) : approvalsApi.my({ pageNum: 1, pageSize: DASHBOARD_CARD_LIMIT }),
      schedulesApi.today(),
      newsApi.page({ pageNum: 1, pageSize: DASHBOARD_CARD_LIMIT, status: 'PUBLISHED' }),
      userStore.isApprover ? approvalsApi.todo({ pageNum: 1, pageSize: 1 }) : Promise.resolve({ records: [], total: 0 })
    ]);

    recentApprovals.value = approvalPage.records;
    todaySchedules.value = schedules;
    latestNews.value = newsPage.records;
    todoCount.value = todoPage.total;
    renderChart();
  } finally {
    loading.value = false;
  }
}

function renderChart() {
  nextTick(() => {
    if (!hasChartData.value || !chartRef.value) {
      chart?.dispose();
      chart = null;
      return;
    }
    chart ||= echarts.init(chartRef.value);
    const counter = recentApprovals.value.reduce<Record<string, number>>((acc, item) => {
      const label = dictStore.getLabel('approval_type', item.type);
      acc[label] = (acc[label] || 0) + 1;
      return acc;
    }, {});

    chart.setOption({
      color: ['#2563eb', '#0f766e', '#f59e0b', '#dc2626'],
      tooltip: { trigger: 'item' },
      grid: { left: 24, right: 12, top: 24, bottom: 24 },
      xAxis: { type: 'category', data: Object.keys(counter), axisTick: { show: false } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e5e7eb' } } },
      series: [{ type: 'bar', data: Object.values(counter), barWidth: 26, itemStyle: { borderRadius: [5, 5, 0, 0] } }]
    });
  });
}

function resizeChart() {
  chart?.resize();
}

onMounted(() => {
  load();
  window.addEventListener('resize', resizeChart);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart);
  chart?.dispose();
});
</script>

<style scoped>
.dashboard-page {
  --dashboard-gap: 14px;
  --dashboard-side-width: 360px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) var(--dashboard-side-width);
  gap: var(--dashboard-gap);
}

.metric {
  display: grid;
  grid-template-areas:
    "icon label"
    "icon value";
  grid-template-columns: 42px 1fr;
  gap: 4px 12px;
  align-items: center;
  height: 82px;
  padding: 16px 18px;
  border: 1px solid var(--oa-border);
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.metric:hover {
  border-color: #c7d2fe;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.metric .el-icon {
  grid-area: icon;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  grid-row: 1 / 3;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: var(--oa-primary-soft);
  color: var(--oa-primary);
  font-size: 20px;
}

.metric span {
  grid-area: label;
  min-width: 0;
  overflow: hidden;
  color: var(--oa-muted);
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric strong {
  grid-area: value;
  min-width: 0;
  overflow: hidden;
  font-size: 24px;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--dashboard-side-width);
  gap: var(--dashboard-gap);
  align-items: start;
}

.dashboard-columns {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--dashboard-gap);
}

.dashboard-columns .panel {
  display: flex;
  flex-direction: column;
  height: 360px;
  min-height: 0;
  overflow: hidden;
}

.dashboard-columns .el-table {
  flex: 1;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.chart {
  height: 292px;
}

.chart-empty {
  display: grid;
  min-height: 292px;
  place-items: center;
  border: 1px dashed #d6deea;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff, #fbfdff);
}

.side-stack {
  display: grid;
  gap: 14px;
}

.quick-panel,
.brief-panel {
  min-height: 0;
}

.quick-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.quick-actions .el-button {
  justify-content: flex-start;
  height: 40px;
  margin: 0;
}

.brief-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px 16px;
  align-items: center;
  color: var(--oa-muted);
}

.brief-list strong {
  color: var(--oa-text);
  font-size: 18px;
}

.news-list {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.news-list button {
  display: flex;
  flex-direction: column;
  gap: 5px;
  width: 100%;
  min-height: 52px;
  padding: 9px 0;
  border: 0;
  border-bottom: 1px solid var(--oa-border);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.news-list strong {
  overflow: hidden;
  color: var(--oa-text);
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-list small {
  overflow: hidden;
  color: var(--oa-muted);
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1360px) {
  .metric-grid,
  .dashboard-columns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-workspace {
    grid-template-columns: minmax(0, 1fr);
  }

  .side-stack {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .metric-grid,
  .dashboard-columns,
  .side-stack {
    grid-template-columns: 1fr;
  }
}
</style>
