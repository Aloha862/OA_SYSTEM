<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ t('notification.title') }}</h1>
        <p class="page-subtitle">{{ t('notification.subtitle') }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Check" :disabled="selectedIds.length === 0" @click="markSelected">
          {{ t('notification.markSelected') }}
        </el-button>
        <el-button :icon="Check" @click="markAll">{{ t('notification.markAll') }}</el-button>
        <el-button v-if="userStore.isAdmin" :icon="Promotion" type="primary" @click="openSend">
          {{ t('notification.sendSystem') }}
        </el-button>
      </div>
    </div>

    <SearchPanel :model="query" :loading="loading" @search="search" @reset="reset">
      <el-form-item :label="t('notification.keyword')">
        <el-input v-model.trim="query.keyword" clearable :placeholder="t('notification.keywordPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('notification.type')">
        <DictSelect v-model="query.type" type-code="notification_type" />
      </el-form-item>
      <el-form-item :label="t('notification.status')">
        <el-select v-model="query.readStatus" clearable :placeholder="t('notification.status')">
          <el-option :label="t('notification.unread')" :value="0" />
          <el-option :label="t('notification.read')" :value="1" />
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
      <el-table-column :label="t('notification.tableStatus')" width="90">
        <template #default="{ row }"><StatusTag :status="row.readStatus" mode="read" /></template>
      </el-table-column>
      <el-table-column prop="title" :label="t('notification.tableTitle')" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" :label="t('notification.tableContent')" min-width="260" show-overflow-tooltip />
      <el-table-column :label="t('notification.tableType')" width="110">
        <template #default="{ row }">{{ dictStore.getLabel('notification_type', row.type) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('notification.createdAt')" width="160">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('notification.actions')" width="160" align="center">
        <template #default="{ row }">
          <div class="table-actions-cell" style="--action-columns: 2">
            <el-button v-if="row.readStatus === 0" class="action-col-1" text type="primary" @click="markRead(row)">
              {{ t('notification.markRead') }}
            </el-button>
            <el-button class="action-col-2" text type="danger" @click="remove(row)">{{ t('common.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </PaginationTable>

    <el-dialog v-model="sendVisible" :title="t('notification.sendDialogTitle')" width="640px">
      <el-form ref="sendFormRef" :model="sendForm" :rules="sendRules" label-width="96px">
        <div class="mail-tip">
          <el-icon><Message /></el-icon>
          <div>
            <strong>{{ t('notification.mailTipTitle') }}</strong>
            <span>{{ t('notification.mailTipText') }}</span>
          </div>
        </div>
        <el-form-item :label="t('notification.receiver')">
          <el-select v-model="sendForm.receiverIds" multiple filterable :placeholder="t('notification.receiverPlaceholder')">
            <el-option v-for="item in users" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('notification.subject')" prop="title">
          <el-input v-model.trim="sendForm.title" :placeholder="t('notification.subjectPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('notification.content')" prop="content">
          <el-input v-model="sendForm.content" type="textarea" :rows="5" :placeholder="t('notification.contentPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="sending" @click="send">{{ t('common.send') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Check, Message, Promotion } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { notificationsApi } from '@/api/notifications';
import { usersApi } from '@/api/users';
import type { NotificationRecord, PageQuery, UserRecord } from '@/api/types';
import DictSelect from '@/components/DictSelect.vue';
import PaginationTable from '@/components/PaginationTable.vue';
import SearchPanel from '@/components/SearchPanel.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useDictStore } from '@/stores/dict';
import { useNotificationStore } from '@/stores/notification';
import { useUserStore } from '@/stores/user';
import { formatDateTime } from '@/utils/format';

const dictStore = useDictStore();
const userStore = useUserStore();
const notificationStore = useNotificationStore();
const { t } = useI18n();
const loading = ref(false);
const sending = ref(false);
const records = ref<NotificationRecord[]>([]);
const total = ref(0);
const selectedIds = ref<number[]>([]);
const sendVisible = ref(false);
const users = ref<UserRecord[]>([]);
const sendFormRef = ref<FormInstance>();
let syncGeneration = 0;
let realtimeRefreshTimer: number | undefined;

const query = reactive<PageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  type: '',
  readStatus: ''
});

const sendForm = reactive({ receiverIds: [] as number[], title: '', content: '' });
const sendRules: FormRules = {
  title: [{ required: true, message: t('notification.subjectPlaceholder'), trigger: 'blur' }],
  content: [{ required: true, message: t('notification.contentPlaceholder'), trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    const page = await notificationsApi.page(query);
    records.value = page.records;
    total.value = page.total;
    selectedIds.value = [];
    notificationStore.fetchUnreadCount();
  } finally {
    loading.value = false;
  }
}

async function loadUsers() {
  if (!userStore.isAdmin) return;
  const page = await usersApi.page({ pageNum: 1, pageSize: 200, status: 1 });
  users.value = page.records;
}

function search() {
  if (query.pageNum === 1) load();
  else query.pageNum = 1;
}

function reset() {
  const shouldLoad = query.pageNum === 1 && query.pageSize === 10;
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', type: '', readStatus: '' });
  if (shouldLoad) load();
}

function handleSelection(rows: unknown[]) {
  selectedIds.value = (rows as NotificationRecord[]).map((row) => row.id);
}

async function markRead(row: NotificationRecord) {
  await notificationsApi.read(row.id);
  ElMessage.success(t('notification.readSuccess'));
  load();
}

async function markSelected() {
  await notificationsApi.readBatch(selectedIds.value);
  ElMessage.success(t('notification.selectedReadSuccess'));
  load();
}

async function markAll() {
  await notificationsApi.readBatch();
  ElMessage.success(t('notification.allReadSuccess'));
  load();
}

async function remove(row: NotificationRecord) {
  await ElMessageBox.confirm(t('notification.deleteConfirm', { title: row.title }), t('notification.deleteTitle'), { type: 'warning' });
  await notificationsApi.remove(row.id);
  ElMessage.success(t('notification.deleteSuccess'));
  load();
}

function openSend() {
  Object.assign(sendForm, { receiverIds: [], title: '', content: '' });
  sendVisible.value = true;
}

async function send() {
  await sendFormRef.value?.validate();
  sending.value = true;
  try {
    const receiverIds = [...sendForm.receiverIds];
    const sentTitle = sendForm.title;
    const sentContent = sendForm.content;
    await notificationsApi.system({
      title: sentTitle,
      content: sentContent,
      type: 'SYSTEM',
      ...(receiverIds.length ? { receiverIds } : {})
    });
    sendVisible.value = false;
    const currentUserId = userStore.userInfo?.id;
    const visibleToCurrentUser = receiverIds.length === 0
      || (currentUserId != null && receiverIds.includes(currentUserId));
    if (visibleToCurrentUser) {
      ElMessage.success(t('notification.sendSuccess'));
      Object.assign(query, { pageNum: 1, keyword: '', type: '', readStatus: '' });
      void syncSentNotification(sentTitle, sentContent);
    } else {
      ElMessage.success(t('notification.sendOthersSuccess'));
    }
  } finally {
    sending.value = false;
  }
}

function wait(milliseconds: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, milliseconds));
}

async function syncSentNotification(title: string, content: string) {
  const generation = ++syncGeneration;
  for (const delay of [250, 600, 1200, 2200, 3500]) {
    await wait(delay);
    if (generation !== syncGeneration) return;
    try {
      await load();
    } catch {
      return;
    }
    if (records.value.some((item) => item.title === title && item.content === content)) return;
  }
}

function scheduleRealtimeRefresh() {
  if (realtimeRefreshTimer) window.clearTimeout(realtimeRefreshTimer);
  realtimeRefreshTimer = window.setTimeout(() => {
    realtimeRefreshTimer = undefined;
    void load().catch(() => undefined);
  }, 120);
}

watch(() => [query.pageNum, query.pageSize], load);
watch(() => notificationStore.realtimeVersion, scheduleRealtimeRefresh);

onMounted(() => {
  dictStore.fetchDict('notification_type');
  load();
  loadUsers();
});

onBeforeUnmount(() => {
  syncGeneration += 1;
  if (realtimeRefreshTimer) window.clearTimeout(realtimeRefreshTimer);
});
</script>

<style scoped>
.mail-tip {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  margin: 0 0 18px 96px;
  padding: 12px 14px;
  border: 1px solid #cfe6e2;
  border-radius: 8px;
  background: #f0fbf8;
  color: var(--oa-text);
}

.mail-tip .el-icon {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  background: #ffffff;
  color: var(--oa-accent);
  font-size: 18px;
}

.mail-tip strong,
.mail-tip span {
  display: block;
}

.mail-tip span {
  margin-top: 3px;
  color: var(--oa-muted);
}

@media (max-width: 760px) {
  .mail-tip {
    margin-left: 0;
  }
}
</style>
