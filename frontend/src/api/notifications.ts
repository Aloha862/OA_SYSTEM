import request from '@/utils/request';
import type { NotificationRecord, PageQuery, PageResult } from './types';

export const notificationsApi = {
  page(params: PageQuery) {
    return request.get<PageResult<NotificationRecord>>('/notifications/page', { params });
  },
  unreadCount() {
    return request.get<number>('/notifications/unread-count');
  },
  read(id: number) {
    return request.put<void>(`/notifications/${id}/read`);
  },
  readBatch(ids?: number[]) {
    return request.put<void>('/notifications/read-batch', { ids });
  },
  remove(id: number) {
    return request.delete<void>(`/notifications/${id}`);
  },
  system(data: { receiverIds?: number[]; title: string; content: string; type?: string }) {
    return request.post<void>('/notifications/system', data);
  }
};
