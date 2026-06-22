<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">新闻管理</h1>
        <p class="page-subtitle">发布公告、维护新闻正文，并通过 AI 生成或润色内容。</p>
      </div>
      <el-button :icon="Plus" type="primary" @click="openCreate">新增新闻</el-button>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item label="关键字">
        <el-input v-model.trim="query.keyword" clearable placeholder="标题/摘要" />
      </el-form-item>
      <el-form-item label="分类">
        <DictSelect v-model="query.category" type-code="news_category" />
      </el-form-item>
      <el-form-item label="状态">
        <DictSelect v-model="query.status" type-code="news_status" />
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
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ dictStore.getLabel('news_category', row.category) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><StatusTag :status="row.status" mode="news" /></template>
      </el-table-column>
      <el-table-column label="置顶" width="80">
        <template #default="{ row }">{{ row.isTop ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="275" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 5">
            <el-button class="action-col-1" text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 'PUBLISHED'" class="action-col-2" text @click="publish(row)">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" class="action-col-2" text @click="offline(row)">下架</el-button>
            <el-button class="action-col-3" text @click="toggleTop(row)">{{ row.isTop ? '取消置顶' : '置顶' }}</el-button>
            <el-button class="action-col-4" text @click="router.push(`/news/${row.id}`)">预览</el-button>
            <el-button class="action-col-5" text type="danger" @click="remove(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑新闻' : '新增新闻'" width="860px" destroy-on-close>
      <div class="dialog-toolbar">
        <el-button :icon="MagicStick" @click="aiVisible = true">AI 生成</el-button>
        <el-button :icon="EditPen" :loading="aiLoading === 'polish'" @click="polish">AI 润色</el-button>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="标题" prop="title" class="full">
            <el-input v-model.trim="form.title" placeholder="请输入新闻标题" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <DictSelect v-model="form.category" type-code="news_category" />
          </el-form-item>
          <el-form-item label="状态">
            <DictSelect v-model="form.status" type-code="news_status" />
          </el-form-item>
          <el-form-item label="摘要" class="full">
            <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入摘要" />
          </el-form-item>
          <el-form-item label="封面" class="full">
            <OaUpload
              v-model="coverFiles"
              business-type="NEWS"
              :limit="1"
              accept="image/*"
              tip="上传后自动作为新闻封面"
              @uploaded="setCover"
              @removed="clearCover"
            />
          </el-form-item>
          <el-form-item label="正文" prop="content" class="full">
            <el-input v-model="form.content" type="textarea" :rows="12" placeholder="请输入新闻正文" />
          </el-form-item>
          <el-form-item label="置顶">
            <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiVisible" title="AI 生成新闻" width="560px">
      <el-form :model="aiForm" label-width="82px">
        <el-form-item label="主题">
          <el-input v-model.trim="aiForm.topic" placeholder="请输入主题" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model.trim="aiForm.keywords" placeholder="多个关键词用逗号分隔" />
        </el-form-item>
        <el-form-item label="语气">
          <el-select v-model="aiForm.tone">
            <el-option label="正式" value="formal" />
            <el-option label="温和" value="warm" />
            <el-option label="简洁" value="concise" />
          </el-select>
        </el-form-item>
        <el-form-item label="字数">
          <el-input-number v-model="aiForm.wordCount" :min="200" :max="2000" :step="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading === 'generate'" @click="generate">生成并填入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { EditPen, MagicStick, Plus } from '@element-plus/icons-vue';
import { newsApi } from '@/api/news';
import type { FileRecord, NewsRecord, PageQuery } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import OaUpload from '@/components/OaUpload.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { formatDateTime } from '@/utils/format';

type NewsForm = Partial<NewsRecord>;

const router = useRouter();
const dictStore = useDictStore();
const loading = ref(false);
const submitting = ref(false);
const records = ref<NewsRecord[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const aiVisible = ref(false);
const aiLoading = ref('');
const isEdit = ref(false);
const formRef = ref<FormInstance>();
const coverFiles = ref<FileRecord[]>([]);

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: '',
  status: ''
});

const emptyForm = (): NewsForm => ({
  title: '',
  summary: '',
  content: '',
  category: 'NOTICE',
  status: 'DRAFT',
  coverImage: '',
  isTop: 0
});

const form = reactive<NewsForm>(emptyForm());
const aiForm = reactive({
  topic: '',
  keywords: '',
  tone: 'formal',
  wordCount: 600
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    const page = await newsApi.page(query);
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
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', category: '', status: '' });
  if (shouldLoad) load();
}

function openCreate() {
  isEdit.value = false;
  coverFiles.value = [];
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
}

function openEdit(row: NewsRecord) {
  isEdit.value = true;
  Object.assign(form, row);
  coverFiles.value = row.coverImage ? [{ id: row.id, originalName: '新闻封面', fileName: 'cover', fileUrl: row.coverImage }] as FileRecord[] : [];
  dialogVisible.value = true;
}

function setCover(file: FileRecord) {
  form.coverImage = file.fileUrl || '';
}

function clearCover() {
  form.coverImage = '';
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (isEdit.value && form.id) {
      await newsApi.update(form.id, form);
    } else {
      await newsApi.create(form);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    load();
  } finally {
    submitting.value = false;
  }
}

async function publish(row: NewsRecord) {
  await ElMessageBox.confirm(`确认发布新闻「${row.title}」吗？`, '发布新闻', { type: 'warning' });
  await newsApi.publish(row.id);
  ElMessage.success('新闻已发布');
  load();
}

async function offline(row: NewsRecord) {
  await ElMessageBox.confirm(`确认下架新闻「${row.title}」吗？`, '下架新闻', { type: 'warning' });
  await newsApi.offline(row.id);
  ElMessage.success('新闻已下架');
  load();
}

async function toggleTop(row: NewsRecord) {
  await newsApi.top(row.id, !row.isTop);
  ElMessage.success('置顶状态已更新');
  load();
}

async function remove(row: NewsRecord) {
  await ElMessageBox.confirm(`确认删除新闻「${row.title}」吗？`, '删除新闻', { type: 'warning' });
  await newsApi.remove(row.id);
  ElMessage.success('删除成功');
  load();
}

async function generate() {
  if (!aiForm.topic.trim()) {
    ElMessage.warning('请输入新闻主题');
    return;
  }
  aiLoading.value = 'generate';
  try {
    const result = await newsApi.aiGenerate({ ...aiForm, category: form.category });
    Object.assign(form, {
      title: result.title || form.title,
      summary: result.summary || form.summary,
      content: result.content || form.content,
      aiGenerated: 1
    });
    aiVisible.value = false;
  } finally {
    aiLoading.value = '';
  }
}

async function polish() {
  if (!form.title || !form.content) {
    ElMessage.warning('请先填写标题和正文');
    return;
  }
  aiLoading.value = 'polish';
  try {
    const result = await newsApi.aiPolish({ title: form.title, content: form.content, style: 'formal' });
    Object.assign(form, {
      title: result.title || form.title,
      summary: result.summary || form.summary,
      content: result.content || form.content
    });
  } finally {
    aiLoading.value = '';
  }
}

watch(() => [query.pageNum, query.pageSize], load);

onMounted(() => {
  dictStore.fetchDict('news_category');
  dictStore.fetchDict('news_status');
  load();
});
</script>

<style scoped>
.dialog-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
