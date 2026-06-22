<template>
  <div class="page approval-detail">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ approval?.title || '审批详情' }}</h1>
        <p class="page-subtitle">审批编号：{{ approval?.approvalNo || '-' }}</p>
      </div>
      <div class="toolbar-actions">
        <StatusTag v-if="approval" :status="approval.status" mode="approval" />
        <el-button @click="router.back()">返回</el-button>
      </div>
    </div>

    <section class="approval-detail__grid">
      <div class="panel panel-body">
        <div class="section-head">
          <h2>基础信息</h2>
          <div class="toolbar-actions">
            <el-button v-if="approval?.status === 'DRAFT'" @click="submitApproval">提交</el-button>
            <el-button v-if="approval?.status === 'PENDING' && !userStore.isAdmin" @click="withdraw">撤回</el-button>
            <el-button v-if="canApprove" type="success" @click="openAction('approve')">通过</el-button>
            <el-button v-if="canApprove" type="danger" @click="openAction('reject')">驳回</el-button>
          </div>
        </div>

        <el-skeleton v-if="loading" :rows="8" animated />
        <el-descriptions v-else-if="approval" :column="2" border>
          <el-descriptions-item label="申请人">{{ approval.applicantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ approval.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批类型">{{ dictStore.getLabel('approval_type', approval.type) }}</el-descriptions-item>
          <el-descriptions-item label="审批人">{{ approval.approverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDateTime(approval.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDateTime(approval.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ approval.amount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目的地">{{ approval.destination || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请原因" :span="2">{{ approval.reason || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <aside class="panel panel-body">
        <div class="section-head">
          <h2>AI 分析</h2>
        </div>
        <template v-if="userStore.isAdmin">
          <div class="ai-actions">
            <el-button :icon="Document" :loading="aiLoading === 'summary'" @click="loadSummary">AI 摘要</el-button>
            <el-button :icon="Warning" :loading="aiLoading === 'risk'" @click="loadRisk">风险提示</el-button>
          </div>
          <el-divider />
          <StatusTag v-if="risk.riskLevel" :status="risk.riskLevel" mode="risk" />
          <p v-if="summary" class="ai-text">{{ summary }}</p>
          <p v-if="risk.suggestion" class="ai-text">{{ risk.suggestion }}</p>
          <el-empty v-if="!summary && !risk.suggestion" description="尚未生成 AI 分析" />
        </template>
        <el-empty v-else description="AI 摘要和风险提示仅管理员可用" />
      </aside>
    </section>

    <section class="panel panel-body">
      <div class="section-head">
        <h2>流转记录</h2>
      </div>
      <el-timeline>
        <el-timeline-item v-for="item in flowRecords" :key="item.id" :timestamp="formatDateTime(item.createdAt)" placement="top">
          <strong>{{ item.operatorName || item.operatorId }}</strong>
          <span class="muted"> {{ actionLabel(item.action) }}</span>
          <p>{{ item.comment || '无审批意见' }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="flowRecords.length === 0 && !loading" description="暂无流转记录" />
    </section>

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
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Document, Warning } from '@element-plus/icons-vue';
import { approvalsApi } from '@/api/approvals';
import type { ApprovalFlowRecord, ApprovalRecord } from '@/api/types';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { useUserStore } from '@/stores/user';
import { formatDateTime } from '@/utils/format';

const route = useRoute();
const router = useRouter();
const dictStore = useDictStore();
const userStore = useUserStore();
const id = Number(route.params.id);

const loading = ref(false);
const submitting = ref(false);
const approval = ref<ApprovalRecord | null>(null);
const flowRecords = ref<ApprovalFlowRecord[]>([]);
const actionVisible = ref(false);
const actionType = ref<'approve' | 'reject'>('approve');
const comment = ref('');
const summary = ref('');
const aiLoading = ref('');
const risk = reactive({ riskLevel: '', suggestion: '' });

const canApprove = computed(() => approval.value?.status === 'PENDING' && userStore.isApprover);

async function load() {
  loading.value = true;
  try {
    const [detail, records] = await Promise.all([approvalsApi.detail(id), approvalsApi.records(id)]);
    approval.value = detail;
    flowRecords.value = records;
  } finally {
    loading.value = false;
  }
}

async function submitApproval() {
  if (!approval.value) return;
  await ElMessageBox.confirm('确认提交该审批吗？', '提交审批', { type: 'warning' });
  await approvalsApi.submit(approval.value.id);
  ElMessage.success('审批已提交');
  load();
}

async function withdraw() {
  if (!approval.value) return;
  await ElMessageBox.confirm('确认撤回该审批吗？', '撤回审批', { type: 'warning' });
  await approvalsApi.withdraw(approval.value.id);
  ElMessage.success('审批已撤回');
  load();
}

function openAction(type: 'approve' | 'reject') {
  actionType.value = type;
  comment.value = '';
  actionVisible.value = true;
}

async function submitAction() {
  if (!approval.value) return;
  if (actionType.value === 'reject' && !comment.value.trim()) {
    ElMessage.warning('驳回时必须填写审批意见');
    return;
  }
  submitting.value = true;
  try {
    if (actionType.value === 'approve') {
      await approvalsApi.approve(approval.value.id, comment.value);
    } else {
      await approvalsApi.reject(approval.value.id, comment.value);
    }
    ElMessage.success('审批已处理');
    actionVisible.value = false;
    load();
  } finally {
    submitting.value = false;
  }
}

async function loadSummary() {
  aiLoading.value = 'summary';
  try {
    const result = await approvalsApi.aiSummary(id);
    summary.value = result.summary;
  } finally {
    aiLoading.value = '';
  }
}

async function loadRisk() {
  aiLoading.value = 'risk';
  try {
    const result = await approvalsApi.aiRisk(id);
    risk.riskLevel = result.riskLevel;
    risk.suggestion = result.suggestion || result.reason || '';
  } finally {
    aiLoading.value = '';
  }
}

function actionLabel(action: string) {
  return (
    {
      SUBMIT: '提交审批',
      APPROVE: '审批通过',
      REJECT: '审批驳回',
      WITHDRAW: '撤回审批'
    }[action] || action
  );
}

onMounted(() => {
  dictStore.fetchDict('approval_type');
  load();
});
</script>

<style scoped>
.approval-detail__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 14px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.ai-actions {
  display: flex;
  gap: 8px;
}

.ai-text {
  white-space: pre-wrap;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .approval-detail__grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
