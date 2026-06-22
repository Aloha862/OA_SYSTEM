import request from '@/utils/request';
import type { DictData, DictType, PageQuery, PageResult } from './types';

export const dictApi = {
  typePage(params: PageQuery) {
    return request.get<PageResult<DictType>>('/dict-types/page', { params });
  },
  createType(data: Partial<DictType>) {
    return request.post<void>('/dict-types', data);
  },
  updateType(id: number, data: Partial<DictType>) {
    return request.put<void>(`/dict-types/${id}`, data);
  },
  removeType(id: number) {
    return request.delete<void>(`/dict-types/${id}`);
  },
  dataPage(params: PageQuery) {
    return request.get<PageResult<DictData>>('/dict-data/page', { params });
  },
  dataByType(typeCode: string) {
    return request.get<DictData[]>(`/dict-data/type/${typeCode}`);
  },
  createData(data: Partial<DictData>) {
    return request.post<void>('/dict-data', data);
  },
  updateData(id: number, data: Partial<DictData>) {
    return request.put<void>(`/dict-data/${id}`, data);
  },
  removeData(id: number) {
    return request.delete<void>(`/dict-data/${id}`);
  },
  refreshCache() {
    return request.post<void>('/dict-data/cache/refresh');
  }
};
