<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">字典管理</h1>
        <p class="page-subtitle">维护系统高频枚举数据，支持刷新缓存。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" @click="refreshCache">刷新缓存</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="panel dict-tabs">
      <el-tab-pane label="字典类型" name="types">
        <div class="panel-body">
          <div class="toolbar">
            <SearchPanel :model="typeQuery" :loading="typeLoading" plain @search="searchTypes" @reset="resetTypes">
              <el-form-item label="关键字">
                <el-input v-model.trim="typeQuery.keyword" clearable placeholder="编码/名称" />
              </el-form-item>
            </SearchPanel>
            <el-button :icon="Plus" type="primary" @click="openTypeCreate">新增类型</el-button>
          </div>
          <PaginationTable
            v-model:page="typeQuery.pageNum"
            v-model:page-size="typeQuery.pageSize"
            :data="types"
            :total="typeTotal"
            :loading="typeLoading"
            @refresh="loadTypes"
          >
            <el-table-column prop="typeCode" label="类型编码" min-width="180" />
            <el-table-column prop="typeName" label="类型名称" min-width="160" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" mode="user" /></template>
            </el-table-column>
            <el-table-column fixed="right" label="操作" width="160" align="center">
              <template #default="{ row }">
                <div class="table-actions-cell" style="--action-columns: 2">
                  <el-button class="action-col-1" text type="primary" @click="openTypeEdit(row)">编辑</el-button>
                  <el-button class="action-col-2" text type="danger" @click="removeType(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </PaginationTable>
        </div>
      </el-tab-pane>

      <el-tab-pane label="字典数据" name="data">
        <div class="panel-body">
          <div class="toolbar">
            <SearchPanel :model="dataQuery" :loading="dataLoading" plain @search="searchData" @reset="resetData">
              <el-form-item label="类型编码">
                <el-input v-model.trim="dataQuery.typeCode" clearable placeholder="如 approval_status" />
              </el-form-item>
              <el-form-item label="关键字">
                <el-input v-model.trim="dataQuery.keyword" clearable placeholder="标签/值" />
              </el-form-item>
            </SearchPanel>
            <el-button :icon="Plus" type="primary" @click="openDataCreate">新增数据</el-button>
          </div>
          <PaginationTable
            v-model:page="dataQuery.pageNum"
            v-model:page-size="dataQuery.pageSize"
            :data="dataList"
            :total="dataTotal"
            :loading="dataLoading"
            @refresh="loadData"
          >
            <el-table-column prop="typeCode" label="类型编码" min-width="170" />
            <el-table-column prop="dictLabel" label="标签" min-width="130" />
            <el-table-column prop="dictValue" label="值" min-width="130" />
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" mode="user" /></template>
            </el-table-column>
            <el-table-column fixed="right" label="操作" width="160" align="center">
              <template #default="{ row }">
                <div class="table-actions-cell" style="--action-columns: 2">
                  <el-button class="action-col-1" text type="primary" @click="openDataEdit(row)">编辑</el-button>
                  <el-button class="action-col-2" text type="danger" @click="removeData(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </PaginationTable>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="typeDialog" :title="typeEdit ? '编辑字典类型' : '新增字典类型'" width="560px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="96px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model.trim="typeForm.typeCode" :disabled="typeEdit" placeholder="请输入类型编码" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model.trim="typeForm.typeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="typeForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitType">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dataDialog" :title="dataEdit ? '编辑字典数据' : '新增字典数据'" width="560px">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="96px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model.trim="dataForm.typeCode" placeholder="请输入类型编码" />
        </el-form-item>
        <el-form-item label="标签" prop="dictLabel">
          <el-input v-model.trim="dataForm.dictLabel" placeholder="请输入标签" />
        </el-form-item>
        <el-form-item label="值" prop="dictValue">
          <el-input v-model.trim="dataForm.dictValue" placeholder="请输入值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="dataForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitData">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { dictApi } from '@/api/dict';
import type { DictData, DictType, PageQuery } from '@/api/types';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';

const dictStore = useDictStore();
const activeTab = ref('types');
const typeLoading = ref(false);
const dataLoading = ref(false);
const submitting = ref(false);
const types = ref<DictType[]>([]);
const dataList = ref<DictData[]>([]);
const typeTotal = ref(0);
const dataTotal = ref(0);
const typeDialog = ref(false);
const dataDialog = ref(false);
const typeEdit = ref(false);
const dataEdit = ref(false);
const typeFormRef = ref<FormInstance>();
const dataFormRef = ref<FormInstance>();

const typeQuery = reactive<PageQuery>({ pageNum: 1, pageSize: 10, keyword: '' });
const dataQuery = reactive<PageQuery>({ pageNum: 1, pageSize: 10, typeCode: '', keyword: '' });

const typeForm = reactive<Partial<DictType>>({ typeCode: '', typeName: '', remark: '', status: 1 });
const dataForm = reactive<Partial<DictData>>({ typeCode: '', dictLabel: '', dictValue: '', sortOrder: 0, status: 1, remark: '' });

const typeRules: FormRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
};
const dataRules: FormRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入值', trigger: 'blur' }]
};

async function loadTypes() {
  typeLoading.value = true;
  try {
    const page = await dictApi.typePage(typeQuery);
    types.value = page.records;
    typeTotal.value = page.total;
  } finally {
    typeLoading.value = false;
  }
}

async function loadData() {
  dataLoading.value = true;
  try {
    const page = await dictApi.dataPage(dataQuery);
    dataList.value = page.records;
    dataTotal.value = page.total;
  } finally {
    dataLoading.value = false;
  }
}

function searchTypes() {
  if (typeQuery.pageNum === 1) loadTypes();
  else typeQuery.pageNum = 1;
}

function searchData() {
  if (dataQuery.pageNum === 1) loadData();
  else dataQuery.pageNum = 1;
}

function resetTypes() {
  const shouldLoad = typeQuery.pageNum === 1 && typeQuery.pageSize === 10;
  Object.assign(typeQuery, { pageNum: 1, pageSize: 10, keyword: '' });
  if (shouldLoad) loadTypes();
}

function resetData() {
  const shouldLoad = dataQuery.pageNum === 1 && dataQuery.pageSize === 10;
  Object.assign(dataQuery, { pageNum: 1, pageSize: 10, typeCode: '', keyword: '' });
  if (shouldLoad) loadData();
}

function openTypeCreate() {
  typeEdit.value = false;
  Object.assign(typeForm, { id: undefined, typeCode: '', typeName: '', remark: '', status: 1 });
  typeDialog.value = true;
}

function openTypeEdit(row: DictType) {
  typeEdit.value = true;
  Object.assign(typeForm, row);
  typeDialog.value = true;
}

function openDataCreate() {
  dataEdit.value = false;
  Object.assign(dataForm, { id: undefined, typeCode: dataQuery.typeCode || '', dictLabel: '', dictValue: '', sortOrder: 0, status: 1, remark: '' });
  dataDialog.value = true;
}

function openDataEdit(row: DictData) {
  dataEdit.value = true;
  Object.assign(dataForm, row);
  dataDialog.value = true;
}

async function submitType() {
  await typeFormRef.value?.validate();
  submitting.value = true;
  try {
    if (typeEdit.value && typeForm.id) await dictApi.updateType(typeForm.id, typeForm);
    else await dictApi.createType(typeForm);
    ElMessage.success('保存成功');
    typeDialog.value = false;
    loadTypes();
  } finally {
    submitting.value = false;
  }
}

async function submitData() {
  await dataFormRef.value?.validate();
  submitting.value = true;
  try {
    if (dataEdit.value && dataForm.id) await dictApi.updateData(dataForm.id, dataForm);
    else await dictApi.createData(dataForm);
    dictStore.clear(dataForm.typeCode);
    ElMessage.success('保存成功');
    dataDialog.value = false;
    loadData();
  } finally {
    submitting.value = false;
  }
}

async function removeType(row: DictType) {
  await ElMessageBox.confirm(`确认删除字典类型「${row.typeName}」吗？`, '删除字典类型', { type: 'warning' });
  await dictApi.removeType(row.id);
  ElMessage.success('删除成功');
  loadTypes();
}

async function removeData(row: DictData) {
  await ElMessageBox.confirm(`确认删除字典数据「${row.dictLabel}」吗？`, '删除字典数据', { type: 'warning' });
  await dictApi.removeData(row.id);
  dictStore.clear(row.typeCode);
  ElMessage.success('删除成功');
  loadData();
}

async function refreshCache() {
  await dictApi.refreshCache();
  dictStore.clear();
  ElMessage.success('字典缓存已刷新');
}

watch(() => [typeQuery.pageNum, typeQuery.pageSize], loadTypes);
watch(() => [dataQuery.pageNum, dataQuery.pageSize], loadData);

onMounted(() => {
  loadTypes();
  loadData();
});
</script>

<style scoped>
.dict-tabs {
  padding: 0 12px 12px;
}

.toolbar {
  align-items: flex-start;
}

.toolbar :deep(.search-panel) {
  flex: 1;
  padding-left: 0;
  border: 0;
}
</style>
