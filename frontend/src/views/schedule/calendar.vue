<template>
  <div class="page schedule-calendar">
    <div class="page-header">
      <div>
        <h1 class="page-title">日历视图</h1>
        <p class="page-subtitle">按月、周、日查看日程，点击日期快速新增。</p>
      </div>
      <div class="toolbar-actions">
        <el-segmented v-model="viewMode" :options="viewOptions" />
        <el-button :icon="Plus" type="primary" @click="openCreate(formatDate(new Date()))">新增日程</el-button>
      </div>
    </div>

    <section class="panel panel-body">
      <el-calendar v-if="viewMode === 'month'" v-model="selectedDate">
        <template #date-cell="{ data }">
          <button class="calendar-cell" type="button" @click="openCreate(data.day)">
            <span>{{ data.day.split('-').slice(1).join('-') }}</span>
            <small v-for="item in schedulesByDate[data.day]" :key="item.id" @click.stop="openDetail(item)">
              {{ item.title }}
            </small>
          </button>
        </template>
      </el-calendar>

      <div v-else class="agenda-view">
        <div v-for="day in agendaDays" :key="day" class="agenda-day">
          <h3>{{ day }}</h3>
          <button v-for="item in schedulesByDate[day]" :key="item.id" type="button" class="agenda-item" @click="openDetail(item)">
            <strong>{{ item.title }}</strong>
            <span>{{ formatDateTime(item.startTime) }} - {{ formatDateTime(item.endTime) }}</span>
            <small>{{ item.location || '未设置地点' }}</small>
          </button>
          <el-empty v-if="!schedulesByDate[day]?.length" description="暂无日程" />
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" title="新增日程" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="标题" prop="title">
          <el-input v-model.trim="form.title" placeholder="请输入日程标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <DictSelect v-model="form.type" type-code="schedule_type" />
        </el-form-item>
        <el-form-item label="时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model.trim="form.location" placeholder="请输入地点" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="日程详情" width="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatDateTime(current.startTime) }} - {{ formatDateTime(current.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ current.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ current.content || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { schedulesApi } from '@/api/schedules';
import type { ScheduleRecord } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import { formatDate, formatDateTime, getDateTimeRange } from '@/utils/format';

const selectedDate = ref(new Date());
const viewMode = ref<'month' | 'week' | 'day'>('month');
const viewOptions = [
  { label: '月', value: 'month' },
  { label: '周', value: 'week' },
  { label: '日', value: 'day' }
];
const schedules = ref<ScheduleRecord[]>([]);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const submitting = ref(false);
const current = ref<ScheduleRecord | null>(null);
const formRef = ref<FormInstance>();

const form = reactive({
  title: '',
  type: 'PERSONAL',
  timeRange: [] as string[],
  location: '',
  content: ''
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
  timeRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }]
};

const schedulesByDate = computed(() => {
  return schedules.value.reduce<Record<string, ScheduleRecord[]>>((acc, item) => {
    const day = formatDate(item.startTime);
    acc[day] ||= [];
    acc[day].push(item);
    return acc;
  }, {});
});

const agendaDays = computed(() => {
  const base = selectedDate.value;
  if (viewMode.value === 'day') return [formatDate(base)];
  const start = new Date(base);
  const day = start.getDay() || 7;
  start.setDate(start.getDate() - day + 1);
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return formatDate(date);
  });
});

async function load() {
  const start = new Date(selectedDate.value);
  start.setDate(1);
  const end = new Date(start);
  end.setMonth(end.getMonth() + 1);
  schedules.value = await schedulesApi.calendar({ start: formatDate(start), end: formatDate(end) });
}

function openCreate(day: string) {
  Object.assign(form, {
    title: '',
    type: 'PERSONAL',
    timeRange: [`${day} 09:00:00`, `${day} 10:00:00`],
    location: '',
    content: ''
  });
  dialogVisible.value = true;
}

function openDetail(item: ScheduleRecord) {
  current.value = item;
  detailVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const time = getDateTimeRange(form.timeRange);
    await schedulesApi.create({ ...form, startTime: time.startTime, endTime: time.endTime });
    ElMessage.success('日程已创建');
    dialogVisible.value = false;
    load();
  } finally {
    submitting.value = false;
  }
}

watch(selectedDate, load);
onMounted(load);
</script>

<style scoped>
.calendar-cell {
  display: flex;
  width: 100%;
  min-height: 86px;
  flex-direction: column;
  gap: 5px;
  padding: 4px;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.calendar-cell span {
  color: var(--oa-muted);
  font-size: 12px;
}

.calendar-cell small,
.agenda-item {
  overflow: hidden;
  border-radius: 6px;
  background: var(--oa-primary-soft);
  color: var(--oa-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.calendar-cell small {
  padding: 3px 6px;
}

.agenda-view {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.agenda-day {
  min-height: 220px;
  padding: 12px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
}

.agenda-day h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.agenda-item {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 8px;
  padding: 10px;
  border: 0;
  text-align: left;
  cursor: pointer;
}

.agenda-item span,
.agenda-item small {
  color: #475569;
}
</style>
