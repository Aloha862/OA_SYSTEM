<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">发起审批</h1>
        <p class="page-subtitle">审批人由部门负责人或部门默认审批人自动匹配。</p>
      </div>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <section class="panel panel-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <div class="form-grid">
          <el-form-item label="审批标题" prop="title" class="full">
            <el-input v-model.trim="form.title" placeholder="请输入审批标题" />
          </el-form-item>
          <el-form-item label="审批类型" prop="type">
            <DictSelect v-model="form.type" type-code="approval_type" />
          </el-form-item>
          <el-form-item label="时间范围" prop="timeRange">
            <el-date-picker
              v-model="form.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
            />
          </el-form-item>
          <el-form-item v-if="form.type === 'REIMBURSEMENT'" label="报销金额" prop="amount">
            <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" />
          </el-form-item>
          <el-form-item v-if="form.type === 'TRAVEL'" label="目的地" prop="destination">
            <el-input v-model.trim="form.destination" placeholder="请输入出差目的地" />
          </el-form-item>
          <el-form-item label="申请原因" prop="reason" class="full">
            <el-input v-model="form.reason" type="textarea" :rows="6" placeholder="请说明原因、背景和必要信息" />
          </el-form-item>
          <el-form-item label="附件" class="full">
            <OaUpload v-model="attachments" business-type="APPROVAL" tip="支持审批附件，单个文件不超过 20MB" />
          </el-form-item>
        </div>
      </el-form>
      <div class="approval-create__footer">
        <el-button @click="router.back()">取消</el-button>
        <el-button :loading="submitting" @click="save(false)">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="save(true)">保存并提交</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { approvalsApi } from '@/api/approvals';
import type { FileRecord } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import OaUpload from '@/components/OaUpload.vue';
import { getDateTimeRange } from '@/utils/format';

const router = useRouter();
const formRef = ref<FormInstance>();
const submitting = ref(false);
const attachments = ref<FileRecord[]>([]);

const form = reactive({
  title: '',
  type: 'LEAVE',
  timeRange: [] as string[],
  amount: undefined as number | undefined,
  destination: '',
  reason: ''
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入审批标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择审批类型', trigger: 'change' }],
  reason: [{ required: true, min: 6, message: '申请原因至少 6 个字', trigger: 'blur' }]
};

async function save(submitAfter: boolean) {
  await formRef.value?.validate();
  if (form.type === 'REIMBURSEMENT' && !form.amount) {
    ElMessage.warning('请填写报销金额');
    return;
  }
  if (form.type === 'TRAVEL' && !form.destination) {
    ElMessage.warning('请填写出差目的地');
    return;
  }

  submitting.value = true;
  try {
    const time = getDateTimeRange(form.timeRange);
    const approval = await approvalsApi.create({
      title: form.title,
      type: form.type,
      reason: form.reason,
      amount: form.amount,
      destination: form.destination,
      startTime: time.startTime,
      endTime: time.endTime,
      formData: JSON.stringify({ attachmentIds: attachments.value.map((item) => item.id) })
    });
    if (submitAfter) {
      await approvalsApi.submit(approval.id);
    }
    ElMessage.success(submitAfter ? '审批已提交' : '草稿已保存');
    router.replace(`/approvals/${approval.id}`);
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.approval-create__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
}
</style>
