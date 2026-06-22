export type Role = 'ADMIN' | 'EMPLOYEE';

export interface Result<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  [key: string]: unknown;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
}

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  role: Role;
  avatar?: string;
  departmentId?: number;
  departmentName?: string;
  position?: string;
  email?: string;
  phone?: string;
  gender?: number;
  status?: number;
  isApprover?: boolean | number;
  permissions?: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
  realName: string;
  phone?: string;
  email?: string;
}

export interface LoginResponse {
  token: string;
  userInfo: UserInfo;
}

export interface Department {
  id: number;
  name: string;
  parentId: number;
  leaderId?: number;
  leaderName?: string;
  approverId?: number;
  approverName?: string;
  sortOrder?: number;
  status: number;
  children?: Department[];
}

export interface UserRecord extends UserInfo {
  hireDate?: string;
  createdAt?: string;
}

export interface ApprovalRecord {
  id: number;
  approvalNo: string;
  title: string;
  type: string;
  status: string;
  applicantId: number;
  applicantName?: string;
  departmentId?: number;
  departmentName?: string;
  approverId?: number;
  approverName?: string;
  reason?: string;
  startTime?: string;
  endTime?: string;
  amount?: number;
  destination?: string;
  formData?: string | Record<string, unknown>;
  aiSummary?: string;
  aiRiskLevel?: string;
  aiRiskSuggestion?: string;
  submittedAt?: string;
  approvedAt?: string;
  createdAt?: string;
}

export interface ApprovalFlowRecord {
  id: number;
  approvalId: number;
  operatorId: number;
  operatorName?: string;
  action: string;
  comment?: string;
  createdAt: string;
}

export interface ScheduleRecord {
  id: number;
  title: string;
  content?: string;
  type: string;
  creatorId?: number;
  creatorName?: string;
  startTime: string;
  endTime: string;
  location?: string;
  reminderMinutes?: number;
  status?: string;
  aiOriginText?: string;
}

export interface NewsRecord {
  id: number;
  title: string;
  summary?: string;
  content: string;
  category?: string;
  coverImage?: string;
  status: string;
  isTop?: number;
  viewCount?: number;
  likeCount?: number;
  favoriteCount?: number;
  commentCount?: number;
  publisherId?: number;
  publisherName?: string;
  publishedAt?: string;
  createdAt?: string;
  aiGenerated?: number;
  liked?: boolean;
  favorited?: boolean;
}

export interface NewsComment {
  id: number;
  newsId: number;
  userId: number;
  userName?: string;
  content: string;
  parentId?: number;
  createdAt: string;
}

export interface DictType {
  id: number;
  typeCode: string;
  typeName: string;
  remark?: string;
  status: number;
}

export interface DictData {
  id: number;
  typeCode: string;
  dictLabel: string;
  dictValue: string;
  sortOrder?: number;
  status: number;
  remark?: string;
}

export interface FileRecord {
  id: number;
  originalName: string;
  fileName: string;
  fileUrl?: string;
  fileSize?: number;
  fileType?: string;
  extension?: string;
  businessType?: string;
  businessId?: number;
  createdAt?: string;
}

export interface NotificationRecord {
  id: number;
  title: string;
  content?: string;
  type: string;
  businessType?: string;
  businessId?: number;
  readStatus: number;
  createdAt: string;
}

export interface AiResponse {
  functionType?: string;
  provider?: string;
  data?: unknown;
  costTimeMs?: number;
  title?: string;
  summary?: string;
  content?: string;
  riskLevel?: string;
  suggestion?: string;
  answer?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  [key: string]: unknown;
}
