import { ref } from 'vue';
import { defineStore } from 'pinia';
import { ElNotification } from 'element-plus';
import { notificationsApi } from '@/api/notifications';
import type { NotificationRecord, PageResult } from '@/api/types';

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0);
  const latest = ref<NotificationRecord[]>([]);
  const loading = ref(false);
  const realtimeVersion = ref(0);

  async function fetchUnreadCount() {
    try {
      unreadCount.value = await notificationsApi.unreadCount();
    } catch {
      unreadCount.value = unreadCount.value || 0;
    }
  }

  async function fetchLatest() {
    loading.value = true;
    try {
      const page: PageResult<NotificationRecord> = await notificationsApi.page({ pageNum: 1, pageSize: 8 });
      latest.value = page.records;
      return page;
    } finally {
      loading.value = false;
    }
  }

  async function markRead(id: number) {
    await notificationsApi.read(id);
    const item = latest.value.find((row) => row.id === id);
    if (item && item.readStatus === 0) {
      item.readStatus = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  async function markAllRead(ids?: number[]) {
    await notificationsApi.readBatch(ids);
    latest.value.forEach((item) => {
      item.readStatus = 1;
    });
    unreadCount.value = 0;
  }

  function receiveRealtime(message: Partial<NotificationRecord>) {
    if (message.eventId && latest.value.some((item) => item.eventId === message.eventId)) return;
    unreadCount.value += 1;
    const record: NotificationRecord = {
      id: Number(message.id || Date.now()),
      eventId: message.eventId,
      title: message.title || '新的系统通知',
      content: message.content || '',
      type: message.type || 'SYSTEM',
      businessType: message.businessType,
      businessId: message.businessId,
      readStatus: 0,
      createdAt: message.createdAt || new Date().toLocaleString()
    };
    latest.value.unshift(record);
    latest.value = latest.value.slice(0, 8);
    realtimeVersion.value += 1;

    ElNotification({
      title: record.title,
      message: record.content || '请前往站内信中心查看详情',
      type: 'info',
      duration: 4500
    });
  }

  return {
    unreadCount,
    latest,
    loading,
    realtimeVersion,
    fetchUnreadCount,
    fetchLatest,
    markRead,
    markAllRead,
    receiveRealtime
  };
});
