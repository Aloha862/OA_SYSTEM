import { ref } from 'vue';
import { defineStore } from 'pinia';
import { dictApi } from '@/api/dict';
import type { DictData } from '@/api/types';

const fallbackDicts: Record<string, DictData[]> = {
  user_status: [
    { id: 1, typeCode: 'user_status', dictLabel: '启用', dictValue: '1', status: 1 },
    { id: 2, typeCode: 'user_status', dictLabel: '禁用', dictValue: '0', status: 1 }
  ],
  user_role: [
    { id: 1, typeCode: 'user_role', dictLabel: '管理员', dictValue: 'ADMIN', status: 1 },
    { id: 2, typeCode: 'user_role', dictLabel: '员工', dictValue: 'EMPLOYEE', status: 1 }
  ],
  approval_type: [
    { id: 1, typeCode: 'approval_type', dictLabel: '请假', dictValue: 'LEAVE', status: 1 },
    { id: 2, typeCode: 'approval_type', dictLabel: '报销', dictValue: 'REIMBURSEMENT', status: 1 },
    { id: 3, typeCode: 'approval_type', dictLabel: '加班', dictValue: 'OVERTIME', status: 1 },
    { id: 4, typeCode: 'approval_type', dictLabel: '出差', dictValue: 'TRAVEL', status: 1 }
  ],
  approval_status: [
    { id: 1, typeCode: 'approval_status', dictLabel: '草稿', dictValue: 'DRAFT', status: 1 },
    { id: 2, typeCode: 'approval_status', dictLabel: '待审批', dictValue: 'PENDING', status: 1 },
    { id: 3, typeCode: 'approval_status', dictLabel: '已通过', dictValue: 'APPROVED', status: 1 },
    { id: 4, typeCode: 'approval_status', dictLabel: '已驳回', dictValue: 'REJECTED', status: 1 },
    { id: 5, typeCode: 'approval_status', dictLabel: '已撤回', dictValue: 'WITHDRAWN', status: 1 }
  ],
  schedule_type: [
    { id: 1, typeCode: 'schedule_type', dictLabel: '个人', dictValue: 'PERSONAL', status: 1 },
    { id: 2, typeCode: 'schedule_type', dictLabel: '会议', dictValue: 'MEETING', status: 1 }
  ],
  news_status: [
    { id: 1, typeCode: 'news_status', dictLabel: '草稿', dictValue: 'DRAFT', status: 1 },
    { id: 2, typeCode: 'news_status', dictLabel: '已发布', dictValue: 'PUBLISHED', status: 1 },
    { id: 3, typeCode: 'news_status', dictLabel: '已下架', dictValue: 'OFFLINE', status: 1 }
  ],
  news_category: [
    { id: 1, typeCode: 'news_category', dictLabel: '公司公告', dictValue: 'NOTICE', status: 1 },
    { id: 2, typeCode: 'news_category', dictLabel: '制度流程', dictValue: 'POLICY', status: 1 },
    { id: 3, typeCode: 'news_category', dictLabel: '团队动态', dictValue: 'TEAM', status: 1 }
  ],
  notification_type: [
    { id: 1, typeCode: 'notification_type', dictLabel: '审批', dictValue: 'APPROVAL', status: 1 },
    { id: 2, typeCode: 'notification_type', dictLabel: '日程', dictValue: 'SCHEDULE', status: 1 },
    { id: 3, typeCode: 'notification_type', dictLabel: '新闻', dictValue: 'NEWS', status: 1 },
    { id: 4, typeCode: 'notification_type', dictLabel: '系统', dictValue: 'SYSTEM', status: 1 }
  ]
};

function normalizeDictList(list: DictData[]) {
  const seen = new Set<string>();
  return list
    .filter((item) => item.status !== 0 && String(item.status) !== '0')
    .map((item) => ({
      ...item,
      status: Number(item.status),
      dictValue: String(item.dictValue)
    }))
    .filter((item) => {
      const key = `${item.typeCode}:${item.dictValue}:${item.dictLabel}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    })
    .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || Number(a.id || 0) - Number(b.id || 0));
}

export const useDictStore = defineStore('dict', () => {
  const dictMap = ref<Record<string, DictData[]>>({});
  const loadingMap = ref<Record<string, boolean>>({});

  async function fetchDict(typeCode: string, force = false) {
    if (!force && dictMap.value[typeCode]?.length) {
      return dictMap.value[typeCode];
    }

    loadingMap.value[typeCode] = true;
    try {
      const list = await dictApi.dataByType(typeCode);
      dictMap.value[typeCode] = normalizeDictList(list);
    } catch {
      dictMap.value[typeCode] = normalizeDictList(fallbackDicts[typeCode] || []);
    } finally {
      loadingMap.value[typeCode] = false;
    }

    return dictMap.value[typeCode];
  }

  function getOptions(typeCode: string) {
    return normalizeDictList(dictMap.value[typeCode] || fallbackDicts[typeCode] || []);
  }

  function getLabel(typeCode: string, value?: string | number) {
    if (value === undefined || value === null || value === '') return '-';
    const option = getOptions(typeCode).find((item) => String(item.dictValue) === String(value));
    return option?.dictLabel || String(value);
  }

  function clear(typeCode?: string) {
    if (typeCode) {
      delete dictMap.value[typeCode];
      return;
    }
    dictMap.value = {};
  }

  return {
    dictMap,
    loadingMap,
    fetchDict,
    getOptions,
    getLabel,
    clear
  };
});
