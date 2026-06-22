<template>
  <el-popover placement="bottom-end" width="360" trigger="click" @show="load">
    <template #reference>
      <el-badge :value="notificationStore.unreadCount" :max="99" :hidden="notificationStore.unreadCount === 0">
        <el-button :icon="Bell" circle />
      </el-badge>
    </template>

    <div class="notification-popover">
      <div class="notification-popover__head">
        <strong>{{ t('notification.inbox') }}</strong>
        <el-button type="primary" text :disabled="notificationStore.unreadCount === 0" @click="markAll">
          {{ t('notification.markAll') }}
        </el-button>
      </div>

      <el-skeleton v-if="notificationStore.loading" :rows="4" animated />
      <el-empty v-else-if="notificationStore.latest.length === 0" :description="t('notification.empty')" />
      <div v-else class="notification-list">
        <button
          v-for="item in notificationStore.latest"
          :key="item.id"
          class="notification-item"
          type="button"
          @click="open(item)"
        >
          <span class="notification-dot" :class="{ unread: item.readStatus === 0 }" />
          <span class="notification-copy">
            <strong>{{ item.title }}</strong>
            <small>{{ item.content || t('notification.detailFallback') }}</small>
          </span>
          <time>{{ item.createdAt }}</time>
        </button>
      </div>

      <el-button class="notification-more" text type="primary" @click="router.push('/notifications')">
        {{ t('notification.viewAll') }}
      </el-button>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useNotificationStore } from '@/stores/notification';
import type { NotificationRecord } from '@/api/types';

const router = useRouter();
const notificationStore = useNotificationStore();
const { t } = useI18n();

function load() {
  notificationStore.fetchLatest();
}

async function open(item: NotificationRecord) {
  if (item.readStatus === 0) {
    await notificationStore.markRead(item.id);
  }

  if (item.businessType === 'APPROVAL' && item.businessId) {
    router.push(`/approvals/${item.businessId}`);
  } else if (item.businessType === 'NEWS' && item.businessId) {
    router.push(`/news/${item.businessId}`);
  } else {
    router.push('/notifications');
  }
}

function markAll() {
  notificationStore.markAllRead();
}
</script>

<style scoped>
.notification-popover {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-popover__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notification-item {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  grid-template-rows: auto auto;
  gap: 4px 8px;
  width: 100%;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid var(--oa-border);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
}

.notification-dot {
  grid-row: 1 / 3;
  width: 7px;
  height: 7px;
  margin-top: 7px;
  border-radius: 999px;
  background: #cbd5e1;
}

.notification-dot.unread {
  background: var(--oa-primary);
}

.notification-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.notification-copy strong,
.notification-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-copy small,
.notification-item time {
  color: var(--oa-muted);
}

.notification-item time {
  grid-column: 2;
  font-size: 12px;
}

.notification-more {
  align-self: stretch;
}
</style>
