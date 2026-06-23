import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('element-plus', () => ({ ElNotification: vi.fn() }));
vi.mock('@/api/notifications', () => ({
  notificationsApi: {
    page: vi.fn(),
    unreadCount: vi.fn(),
    read: vi.fn(),
    readBatch: vi.fn()
  }
}));

import { useNotificationStore } from '@/stores/notification';

describe('notification store realtime updates', () => {
  beforeEach(() => setActivePinia(createPinia()));

  it('increments the realtime version once for each unique event', () => {
    const store = useNotificationStore();
    const message = {
      eventId: 'system-event-1',
      title: '系统维护',
      content: '今晚发布新版本',
      type: 'system.notice'
    };

    store.receiveRealtime(message);
    store.receiveRealtime(message);

    expect(store.realtimeVersion).toBe(1);
    expect(store.latest).toHaveLength(1);
    expect(store.latest[0].title).toBe('系统维护');
  });
});
