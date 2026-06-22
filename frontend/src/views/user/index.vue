<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">维护员工账号、部门归属、启停状态和审批人身份。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Plus" type="primary" @click="openCreate">新增用户</el-button>
        <el-button :icon="Delete" :disabled="selectedIds.length === 0" @click="removeBatch">批量删除</el-button>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="账号/姓名/手机号" />
      </el-form-item>
      <el-form-item label="角色">
        <DictSelect v-model="query.role" type-code="user_role" />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="query.status" type-code="user_status" />
      </el-form-item>
      <el-form-item label="审批人">
        <el-select v-model="query.isApprover" clearable placeholder="请选择">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
    </SearchPanel>

    <PaginationTable
      v-model:page="query.pageNum"
      v-model:page-size="query.pageSize"
      :data="records"
      :total="total"
      :loading="loading"
      selectable
      @selection-change="handleSelection"
      @refresh="load"
    >
      <el-table-column prop="username" label="账号" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="110" />
      <el-table-column prop="departmentName" label="部门" min-width="130" show-overflow-tooltip />
      <el-table-column prop="position" label="岗位" min-width="120" show-overflow-tooltip />
      <el-table-column label="角色" width="95">
        <template #default="{ row }">{{ dictStore.getLabel('user_role', row.role) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" mode="user" /></template>
      </el-table-column>
      <el-table-column label="审批人" width="92">
        <template #default="{ row }">
          <el-tag :type="row.isApprover ? 'success' : 'info'" effect="light">{{ row.isApprover ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column fixed="right" label="操作" width="285" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 5">
            <el-button class="action-col-1" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button class="action-col-2" text @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button class="action-col-3" text @click="toggleApprover(row)">{{ row.isApprover ? '取消审批人' : '设为审批人' }}</el-button>
            <el-button class="action-col-4" text @click="resetPassword(row)">重置密码</el-button>
            <el-button class="action-col-5" text type="danger" @click="remove(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model.trim="form.username" :disabled="isEdit" placeholder="请输入登录账号" />
          </el-form-item>
          <el-form-item v-if="!isEdit" label="初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model.trim="form.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <DictSelect v-model="form.role" type-code="user_role" />
          </el-form-item>
          <el-form-item label="所属部门">
            <el-tree-select
              v-model="form.departmentId"
              :data="departmentTree"
              :props="{ label: 'name', value: 'id', children: 'children' }"
              clearable
              check-strictly
              placeholder="请选择部门"
            />
          </el-form-item>
          <el-form-item label="岗位">
            <el-input v-model.trim="form.position" placeholder="请输入岗位" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model.trim="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model.trim="form.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="审批人">
            <el-switch v-model="form.isApprover" active-text="是" inactive-text="否" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Delete, Plus } from '@element-plus/icons-vue';
import { usersApi } from '@/api/users';
import { departmentsApi } from '@/api/departments';
import type { Department, PageQuery, UserRecord } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';

type UserForm = Partial<UserRecord> & { password?: string };

const dictStore = useDictStore();
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const records = ref<UserRecord[]>([]);
const total = ref(0);
const selectedIds = ref<number[]>([]);
const departmentTree = ref<Department[]>([]);
const formRef = ref<FormInstance>();

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  role: '',
  status: '',
  isApprover: ''
});

const emptyForm = (): UserForm => ({
  username: '',
  password: '',
  realName: '',
  role: 'EMPLOYEE',
  status: 1,
  isApprover: false,
  departmentId: undefined,
  position: '',
  phone: '',
  email: ''
});

const form = reactive<UserForm>(emptyForm());

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
};

async function load() {
  loading.value = true;
  try {
    const page = await usersApi.page(query);
    records.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadDepartments() {
  departmentTree.value = await departmentsApi.tree();
}

function search() {
  if (query.pageNum === 1) load();
  else query.pageNum = 1;
}

function reset() {
  const shouldLoad = query.pageNum === 1 && query.pageSize === 10;
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', role: '', status: '', isApprover: '' });
  if (shouldLoad) load();
}

function handleSelection(rows: unknown[]) {
  selectedIds.value = (rows as UserRecord[]).map((row) => row.id);
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
}

function openEdit(row: UserRecord) {
  isEdit.value = true;
  Object.assign(form, row, { isApprover: row.isApprover === true || row.isApprover === 1, password: '' });
  dialogVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (isEdit.value && form.id) {
      const { password, ...payload } = form;
      await usersApi.update(form.id, payload);
    } else {
      await usersApi.create(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    load();
  } finally {
    submitting.value = false;
  }
}

async function toggleStatus(row: UserRecord) {
  const next = row.status === 1 ? 0 : 1;
  await ElMessageBox.confirm(`确认${next === 1 ? '启用' : '禁用'}用户「${row.realName}」吗？`, '状态变更', { type: 'warning' });
  await usersApi.updateStatus(row.id, next);
  ElMessage.success('状态已更新');
  load();
}

async function toggleApprover(row: UserRecord) {
  const next = !(row.isApprover === true || row.isApprover === 1);
  await ElMessageBox.confirm(`确认${next ? '设置' : '取消'}「${row.realName}」的审批人身份吗？`, '审批人设置', { type: 'warning' });
  await usersApi.setApprover(row.id, next);
  ElMessage.success('审批人身份已更新');
  load();
}

async function resetPassword(row: UserRecord) {
  await ElMessageBox.confirm(`确认重置「${row.realName}」的登录密码吗？`, '重置密码', { type: 'warning' });
  await usersApi.resetPassword(row.id);
  ElMessage.success('密码已重置');
}

async function remove(row: UserRecord) {
  await ElMessageBox.confirm(`确认删除用户「${row.realName}」吗？`, '删除用户', { type: 'warning' });
  await usersApi.remove(row.id);
  ElMessage.success('删除成功');
  load();
}

async function removeBatch() {
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 个用户吗？`, '批量删除', { type: 'warning' });
  await usersApi.removeBatch(selectedIds.value);
  ElMessage.success('批量删除成功');
  load();
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  dictStore.fetchDict('user_role');
  dictStore.fetchDict('user_status');
  loadDepartments();
  load();
});
</script>
