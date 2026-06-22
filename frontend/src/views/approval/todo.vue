<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">待我审批</h1>
        <p class="page-subtitle">管理员和审批人可处理分配到自己的审批单。</p>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题/编号/申请人" />
      </el-form-item>
      <el-form-item label="类型">
        <DictSelect v-model="query.type" type-code="approval_type" />
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
      <el-table-column prop="approvalNo" label="编号" min-width="140" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="applicantName" label="申请人" width="110" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ dictStore.getLabel('approval_type', row.type) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" mode="approval" /></template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="210" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 3">
            <el-button class="action-col-1" text type="primary" @click="router.push(`/approvals/${row.id}`)">详情</el-button>
            <el-button class="action-col-2" text type="success" @click="openAction(row, 'approve')">通过</el-button>
            <el-button class="action-col-3" text type="danger" @click="openAction(row, 'reject')">驳回</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>

    <el-dialog v-model="actionVisible" :title="actionType === 'approve' ? '审批通过' : '审批驳回'" width="520px">
      <el-input v-model="comment" type="textarea" :rows="5" placeholder="请输入审批意见" />
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button :type="actionType === 'approve' ? 'success' : 'danger'" :loading="submitting" @click="submitAction">
          确认{{ actionType === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { approvalsApi } from '@/api/approvals';
import type { ApprovalRecord, PageQuery } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { formatDateTime } from '@/utils/format';

const router = useRouter();
const dictStore = useDictStore();
const loading = ref(false);
const submitting = ref(false);
const records = ref<ApprovalRecord[]>([]);
const total = ref(0);
const actionVisible = ref(false);
const actionType = ref<'approve' | 'reject'>('approve');
const current = ref<ApprovalRecord | null>(null);
const comment = ref('');

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  type: ''
});

async function load() {
  loading.value = true;
  try {
    const page = await approvalsApi.todo(query);
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
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', type: '' });
  if (shouldLoad) load();
}

function openAction(row: ApprovalRecord, type: 'approve' | 'reject') {
  current.value = row;
  actionType.value = type;
  comment.value = '';
  actionVisible.value = true;
}

async function submitAction() {
  if (!current.value) return;
  if (actionType.value === 'reject' && !comment.value.trim()) {
    ElMessage.warning('驳回时必须填写审批意见');
    return;
  }
  submitting.value = true;
  try {
    if (actionType.value === 'approve') {
      await approvalsApi.approve(current.value.id, comment.value);
    } else {
      await approvalsApi.reject(current.value.id, comment.value);
    }
    ElMessage.success('审批已处理');
    actionVisible.value = false;
    load();
  } finally {
    submitting.value = false;
  }
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  dictStore.fetchDict('approval_type');
  load();
});
</script>
