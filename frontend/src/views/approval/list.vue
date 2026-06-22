<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">审批列表</h1>
        <p class="page-subtitle">{{ userStore.isAdmin ? '查看和管理全部审批单。' : '查看自己发起的审批单。' }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Plus" type="primary" @click="router.push('/approvals/create')">发起审批</el-button>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题/编号/申请人" />
      </el-form-item>
      <el-form-item label="类型">
        <DictSelect v-model="query.type" type-code="approval_type" />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="query.status" type-code="approval_status" />
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
      <el-table-column label="标题" min-width="190" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="table-link" @click="router.push(`/approvals/${row.id}`)">{{ row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ dictStore.getLabel('approval_type', row.type) }}</template>
      </el-table-column>
      <el-table-column prop="applicantName" label="申请人" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" mode="approval" /></template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column prop="submittedAt" label="提交时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="220" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 4">
            <el-button class="action-col-1" text type="primary" @click="router.push(`/approvals/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'DRAFT'" class="action-col-2" text @click="submitApproval(row)">提交</el-button>
            <el-button v-if="row.status === 'PENDING' && !userStore.isAdmin" class="action-col-2" text @click="withdraw(row)">撤回</el-button>
            <el-button v-if="userStore.isAdmin || row.status === 'DRAFT'" class="action-col-4" text type="danger" @click="remove(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { approvalsApi } from '@/api/approvals';
import type { ApprovalRecord, PageQuery } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { useUserStore } from '@/stores/user';
import { formatDateTime } from '@/utils/format';

const router = useRouter();
const dictStore = useDictStore();
const userStore = useUserStore();
const loading = ref(false);
const records = ref<ApprovalRecord[]>([]);
const total = ref(0);

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  type: '',
  status: ''
});

async function load() {
  loading.value = true;
  try {
    const page = userStore.isAdmin ? await approvalsApi.page(query) : await approvalsApi.my(query);
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
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', type: '', status: '' });
  if (shouldLoad) load();
}

async function submitApproval(row: ApprovalRecord) {
  await ElMessageBox.confirm(`确认提交审批「${row.title}」吗？`, '提交审批', { type: 'warning' });
  await approvalsApi.submit(row.id);
  ElMessage.success('审批已提交');
  load();
}

async function withdraw(row: ApprovalRecord) {
  await ElMessageBox.confirm(`确认撤回审批「${row.title}」吗？`, '撤回审批', { type: 'warning' });
  await approvalsApi.withdraw(row.id);
  ElMessage.success('审批已撤回');
  load();
}

async function remove(row: ApprovalRecord) {
  await ElMessageBox.confirm(`确认删除审批「${row.title}」吗？`, '删除审批', { type: 'warning' });
  await approvalsApi.remove(row.id);
  ElMessage.success('删除成功');
  load();
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  dictStore.fetchDict('approval_type');
  dictStore.fetchDict('approval_status');
  load();
});
</script>
