import request from '@/utils/request';
import type { FileRecord, PageQuery, PageResult } from './types';

export const filesApi = {
  upload(data: FormData) {
    return request.post<FileRecord>('/files/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  detail(id: number) {
    return request.get<FileRecord>(`/files/${id}`);
  },
  download(id: number) {
    return request.get<Blob>(`/files/download/${id}`, { responseType: 'blob' });
  },
  business(params: { businessType: string; businessId: number }) {
    return request.get<FileRecord[]>('/files/business', { params });
  },
  page(params: PageQuery) {
    return request.get<PageResult<FileRecord>>('/files/page', { params });
  },
  remove(id: number) {
    return request.delete<void>(`/files/${id}`);
  }
};
