import request from '@/utils/request';
import type { PageQuery, PageResult, ScheduleRecord } from './types';

export const schedulesApi = {
  page(params: PageQuery) {
    return request.get<PageResult<ScheduleRecord>>('/schedules/page', { params });
  },
  calendar(params: { start?: string; end?: string }) {
    return request.get<ScheduleRecord[]>('/schedules/calendar', { params });
  },
  today() {
    return request.get<ScheduleRecord[]>('/schedules/today');
  },
  week() {
    return request.get<ScheduleRecord[]>('/schedules/week');
  },
  detail(id: number) {
    return request.get<ScheduleRecord>(`/schedules/${id}`);
  },
  create(data: Partial<ScheduleRecord> & { participantIds?: number[] }) {
    return request.post<ScheduleRecord>('/schedules', data);
  },
  update(id: number, data: Partial<ScheduleRecord> & { participantIds?: number[] }) {
    return request.put<void>(`/schedules/${id}`, data);
  },
  remove(id: number) {
    return request.delete<void>(`/schedules/${id}`);
  },
  addParticipants(id: number, userIds: number[]) {
    return request.post<void>(`/schedules/${id}/participants`, { userIds });
  },
  removeParticipant(id: number, userId: number) {
    return request.delete<void>(`/schedules/${id}/participants/${userId}`);
  },
  accept(id: number) {
    return request.post<void>(`/schedules/${id}/accept`);
  },
  reject(id: number) {
    return request.post<void>(`/schedules/${id}/reject`);
  },
  aiParse(text: string) {
    return request.post<Partial<ScheduleRecord>>('/schedules/ai-parse', { text });
  }
};
