<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">日程列表</h1>
        <p class="page-subtitle">管理个人日程、会议邀请和提醒时间。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="MagicStick" @click="openAiParse">AI 解析</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增日程</el-button>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题/地点" />
      </el-form-item>
      <el-form-item label="类型">
        <DictSelect v-model="query.type" type-code="schedule_type" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="请选择">
          <el-option label="正常" value="NORMAL" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已完成" value="FINISHED" />
        </el-select>
      </el-form-item>
    </SearchPanel>

    <PaginationTable
      v-model:page="query.pageNum"
      v-model:page-size="query.pageSize"
      :data="records"
      :total="total"
      :loading="loading"
      @refresh="load"
    >
      <el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip />
      <el-table-column label="类型" width="95">
        <template #default="{ row }">{{ dictStore.getLabel('schedule_type', row.type) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="130" show-overflow-tooltip />
      <el-table-column prop="startTime" label="开始时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column prop="endTime" label="结束时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="95">
        <template #default="{ row }"><StatusTag :status="row.status || 'NORMAL'" mode="schedule" /></template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="190" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 4">
            <el-button class="action-col-1" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.type === 'MEETING'" class="action-col-2" text @click="accept(row)">接受</el-button>
            <el-button v-if="row.type === 'MEETING'" class="action-col-3" text @click="reject(row)">拒绝</el-button>
            <el-button class="action-col-4" text type="danger" @click="remove(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑日程' : '新增日程'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <div class="form-grid">
          <el-form-item label="日程标题" prop="title" class="full">
            <el-input v-model.trim="form.title" placeholder="请输入日程标题" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <DictSelect v-model="form.type" type-code="schedule_type" />
          </el-form-item>
          <el-form-item label="提醒">
            <el-input-number v-model="form.reminderMinutes" :min="0" :max="1440" />
            <span class="muted form-inline-text">分钟前</span>
          </el-form-item>
          <el-form-item label="时间范围" prop="timeRange" class="full">
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
          <el-form-item v-if="form.type === 'MEETING'" label="参与人">
            <el-select v-model="form.participantIds" multiple filterable placeholder="请选择参与人">
              <el-option v-for="item in users" :key="item.id" :label="item.realName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="内容" class="full">
            <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入日程内容" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiVisible" title="AI 解析日程" width="620px">
      <el-input v-model="aiText" type="textarea" :rows="5" placeholder="输入自然语言日程，例如：明天下午三点开项目周会" />
      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="parseSchedule">解析并填入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { MagicStick, Plus } from '@element-plus/icons-vue';
import { schedulesApi } from '@/api/schedules';
import { usersApi } from '@/api/users';
import type { PageQuery, ScheduleRecord, UserRecord } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { formatDateTime, getDateTimeRange } from '@/utils/format';

type ScheduleForm = Partial<ScheduleRecord> & { timeRange?: string[]; participantIds?: number[] };

const dictStore = useDictStore();
const loading = ref(false);
const submitting = ref(false);
const records = ref<ScheduleRecord[]>([]);
const total = ref(0);
const users = ref<UserRecord[]>([]);
const dialogVisible = ref(false);
const aiVisible = ref(false);
const aiLoading = ref(false);
const aiText = ref('');
const isEdit = ref(false);
const formRef = ref<FormInstance>();

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  type: '',
  status: ''
});

const emptyForm = (): ScheduleForm => ({
  title: '',
  type: 'PERSONAL',
  timeRange: [],
  location: '',
  reminderMinutes: 15,
  content: '',
  participantIds: []
});

const form = reactive<ScheduleForm>(emptyForm());
const rules: FormRules = {
  title: [{ required: true, message: '请输入日程标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择日程类型', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择时间范围', trigger: 'change' }]
};

async function load() {
  loading.value = true;
  try {
    const page = await schedulesApi.page(query);
    records.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadUsers() {
  users.value = await usersApi.options();
}

function search() {
  if (query.pageNum === 1) load();
  else query.pageNum = 1;
}

function reset() {
  const shouldLoad = query.pageNum === 1 && query.pageSize === 10;
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', type: '', status: '' });
  if (shouldLoad) load();
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
}

function openEdit(row: ScheduleRecord) {
  isEdit.value = true;
  Object.assign(form, row, { timeRange: [row.startTime, row.endTime], participantIds: [] });
  dialogVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const time = getDateTimeRange(form.timeRange);
    const payload = { ...form, startTime: time.startTime, endTime: time.endTime };
    delete payload.timeRange;
    if (isEdit.value && form.id) {
      await schedulesApi.update(form.id, payload);
    } else {
      await schedulesApi.create(payload);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    load();
  } finally {
    submitting.value = false;
  }
}

async function remove(row: ScheduleRecord) {
  await ElMessageBox.confirm(`确认删除日程「${row.title}」吗？`, '删除日程', { type: 'warning' });
  await schedulesApi.remove(row.id);
  ElMessage.success('删除成功');
  load();
}

async function accept(row: ScheduleRecord) {
  await schedulesApi.accept(row.id);
  ElMessage.success('已接受会议邀请');
  load();
}

async function reject(row: ScheduleRecord) {
  await ElMessageBox.confirm(`确认拒绝会议「${row.title}」吗？`, '拒绝会议', { type: 'warning' });
  await schedulesApi.reject(row.id);
  ElMessage.success('已拒绝会议邀请');
  load();
}

function openAiParse() {
  aiText.value = '';
  aiVisible.value = true;
}

async function parseSchedule() {
  if (!aiText.value.trim()) {
    ElMessage.warning('请输入自然语言日程');
    return;
  }
  aiLoading.value = true;
  try {
    const result = await schedulesApi.aiParse(aiText.value);
    Object.assign(form, {
      title: result.title || form.title,
      content: result.content || form.content,
      type: result.type || 'PERSONAL',
      location: result.location || '',
      timeRange: [result.startTime, result.endTime].filter(Boolean),
      aiOriginText: aiText.value
    });
    isEdit.value = false;
    aiVisible.value = false;
    dialogVisible.value = true;
  } finally {
    aiLoading.value = false;
  }
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  dictStore.fetchDict('schedule_type');
  load();
  loadUsers();
});
</script>

<style scoped>
.form-inline-text {
  margin-left: 8px;
}
</style>
