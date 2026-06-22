<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">部门管理</h1>
        <p class="page-subtitle">查看组织结构，维护负责人和默认审批人。</p>
      </div>
      <div v-if="userStore.isAdmin" class="toolbar-actions">
        <el-button :icon="Plus" type="primary" @click="openCreate">新增部门</el-button>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="部门名称" />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="query.status" type-code="user_status" />
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
      <el-table-column prop="name" label="部门名称" min-width="180" />
      <el-table-column prop="leaderName" label="负责人" min-width="120" />
      <el-table-column prop="approverName" label="默认审批人" min-width="130" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><StatusTag :status="row.status" mode="user" /></template>
      </el-table-column>
      <el-table-column v-if="userStore.isAdmin" fixed="right" label="操作" width="210" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 4">
            <el-button class="action-col-1" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button class="action-col-2" text @click="openLeader(row)">负责人</el-button>
            <el-button class="action-col-3" text @click="openApprover(row)">审批人</el-button>
            <el-button class="action-col-4" text type="danger" @click="remove(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="departmentTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            clearable
            check-strictly
            placeholder="顶级部门"
          />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.leaderId" clearable filterable placeholder="请选择负责人">
            <el-option v-for="item in approvers" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认审批人">
          <el-select v-model="form.approverId" clearable filterable placeholder="请选择默认审批人">
            <el-option v-for="item in approvers" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
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
import { Plus } from '@element-plus/icons-vue';
import { departmentsApi } from '@/api/departments';
import { usersApi } from '@/api/users';
import type { Department, PageQuery, UserRecord } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useUserStore } from '@/stores/user';

type DepartmentForm = Partial<Department>;

const userStore = useUserStore();
const loading = ref(false);
const submitting = ref(false);
const records = ref<Department[]>([]);
const total = ref(0);
const departmentTree = ref<Department[]>([]);
const approvers = ref<UserRecord[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInstance>();

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
});

const emptyForm = (): DepartmentForm => ({
  name: '',
  parentId: 0,
  leaderId: undefined,
  approverId: undefined,
  sortOrder: 0,
  status: 1
});

const form = reactive<DepartmentForm>(emptyForm());
const rules: FormRules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    const page = await departmentsApi.page(query);
    records.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  const [tree, list] = await Promise.all([departmentsApi.tree(), usersApi.approvers()]);
  departmentTree.value = tree;
  approvers.value = list;
}

function search() {
  if (query.pageNum === 1) load();
  else query.pageNum = 1;
}

function reset() {
  const shouldLoad = query.pageNum === 1 && query.pageSize === 10;
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', status: '' });
  if (shouldLoad) load();
}

function openCreate() {
  isEdit.value = false;
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
}

function openEdit(row: Department) {
  isEdit.value = true;
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (isEdit.value && form.id) {
      await departmentsApi.update(form.id, form);
    } else {
      await departmentsApi.create(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await Promise.all([load(), loadOptions()]);
  } finally {
    submitting.value = false;
  }
}

async function openLeader(row: Department) {
  const { value } = await ElMessageBox.prompt('请输入负责人用户 ID', '设置负责人', {
    inputValue: row.leaderId ? String(row.leaderId) : '',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入数字 ID'
  });
  await departmentsApi.updateLeader(row.id, Number(value));
  ElMessage.success('负责人已更新');
  load();
}

async function openApprover(row: Department) {
  const { value } = await ElMessageBox.prompt('请输入默认审批人用户 ID', '设置审批人', {
    inputValue: row.approverId ? String(row.approverId) : '',
    inputPattern: /^\d+$/,
    inputErrorMessage: '请输入数字 ID'
  });
  await departmentsApi.updateApprover(row.id, Number(value));
  ElMessage.success('默认审批人已更新');
  load();
}

async function remove(row: Department) {
  await ElMessageBox.confirm(`确认删除部门「${row.name}」吗？`, '删除部门', { type: 'warning' });
  await departmentsApi.remove(row.id);
  ElMessage.success('删除成功');
  await Promise.all([load(), loadOptions()]);
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  load();
  loadOptions();
});
</script>
