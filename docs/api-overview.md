# REST API 概览与权限规则

所有后端接口统一使用 `/api` 前缀，返回统一 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

认证方式：

```text
Authorization: Bearer <jwt-token>
```

状态码约定：`200` 成功，`400` 业务失败，`401` 未认证，`403` 无权限，`500` 系统异常。

## 权限简写

| 简写 | 含义 |
| --- | --- |
| Anonymous | 匿名可访问 |
| Authenticated | 登录用户可访问 |
| Admin | `ADMIN` 角色 |
| Employee | `EMPLOYEE` 角色 |
| Approver | `ADMIN` 或 `is_approver = 1` 的员工 |
| Owner | 数据所有者或本人 |
| Participant | 日程参与人 |

## 全局权限规则

- `/api/auth/login` 允许匿名访问。
- 除登录外，业务接口默认需要 JWT。
- 管理类接口必须校验 `ADMIN`。
- 员工只能访问自己权限范围内的数据。
- 审批操作只允许管理员或匹配到的审批人。
- 员工提交审批时不能自由指定任意审批人，审批人来自部门负责人或部门默认审批人。
- 员工可以查看已发布新闻并评论、点赞、收藏。
- 新闻新增、编辑、发布、下架、置顶和评论管理属于管理员权限。
- AI 接口必须按功能校验角色权限。
- 文件接口需按业务类型、业务 ID、上传人和资源公开性判断访问范围。
- WebSocket 建连时必须校验 JWT。

## 认证接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 登录，返回 JWT 和用户信息 | Anonymous |
| POST | `/api/auth/logout` | 登出，可预留 Redis token 黑名单 | Authenticated |
| GET | `/api/auth/me` | 当前登录用户信息 | Authenticated |
| POST | `/api/auth/refresh` | 刷新 token | Authenticated |

登录成功响应中的用户信息至少包含 `id`、`username`、`realName`、`role`、`avatar`。

## 用户接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/users/page` | 用户分页查询，支持用户名、姓名、手机号、部门、状态筛选 | Admin |
| GET | `/api/users/{id}` | 用户详情 | Admin |
| POST | `/api/users` | 新增员工 | Admin |
| PUT | `/api/users/{id}` | 修改员工 | Admin |
| DELETE | `/api/users/{id}` | 逻辑删除员工 | Admin |
| DELETE | `/api/users/batch` | 批量逻辑删除员工 | Admin |
| PUT | `/api/users/{id}/status` | 启用或禁用员工 | Admin |
| PUT | `/api/users/{id}/password/reset` | 重置密码 | Admin |
| PUT | `/api/users/{id}/approver` | 设置或取消审批人身份 | Admin |
| GET | `/api/users/approvers` | 查询审批人列表 | Admin |
| GET | `/api/users/profile` | 当前用户资料 | Authenticated |
| PUT | `/api/users/profile` | 修改当前用户基础资料 | Authenticated |

规则：
- 管理员不能删除自己。
- 员工不能自行修改角色、状态、审批人身份。
- 用户被禁用后不能登录。
- 密码必须 BCrypt 加密。

## 部门接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/departments/tree` | 部门树 | Authenticated |
| GET | `/api/departments/page` | 部门分页查询 | Admin |
| GET | `/api/departments/{id}` | 部门详情 | Authenticated |
| POST | `/api/departments` | 新增部门 | Admin |
| PUT | `/api/departments/{id}` | 修改部门 | Admin |
| DELETE | `/api/departments/{id}` | 逻辑删除部门 | Admin |
| PUT | `/api/departments/{id}/leader` | 设置部门负责人 | Admin |
| PUT | `/api/departments/{id}/approver` | 设置部门默认审批人 | Admin |

规则：
- 删除部门前检查子部门和员工。
- 部门默认审批人必须是管理员或 `is_approver = 1` 的员工。
- 部门树需要 Redis 缓存。

## 审批接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/approvals/page` | 审批分页查询，管理员查全部，员工按权限过滤 | Authenticated |
| GET | `/api/approvals/my` | 我发起的审批 | Authenticated |
| GET | `/api/approvals/todo` | 待我审批 | Approver |
| GET | `/api/approvals/done` | 我已审批 | Approver |
| GET | `/api/approvals/{id}` | 审批详情 | Admin / Owner / Approver |
| POST | `/api/approvals` | 新建审批草稿 | Authenticated |
| PUT | `/api/approvals/{id}` | 修改审批草稿 | Owner |
| DELETE | `/api/approvals/{id}` | 删除审批草稿 | Owner / Admin |
| POST | `/api/approvals/{id}/submit` | 提交审批 | Owner |
| POST | `/api/approvals/{id}/withdraw` | 撤回审批 | Owner |
| POST | `/api/approvals/{id}/approve` | 审批通过 | Approver |
| POST | `/api/approvals/{id}/reject` | 审批驳回 | Approver |
| GET | `/api/approvals/{id}/records` | 审批流转记录 | Admin / Owner / Approver |
| POST | `/api/approvals/{id}/ai-summary` | 审批内容 AI 摘要 | Admin |
| POST | `/api/approvals/{id}/ai-risk` | 审批风险提示 | Admin |

规则：
- 审批类型：`LEAVE`、`REIMBURSEMENT`、`OVERTIME`、`TRAVEL`。
- 审批状态：`DRAFT`、`PENDING`、`APPROVED`、`REJECTED`、`WITHDRAWN`。
- 单级审批。
- 草稿可修改；已提交后不能随意修改。
- 待审批状态可撤回。
- 提交、通过、驳回后通过 RabbitMQ 生成通知。

## 日程接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/schedules/page` | 日程分页查询 | Authenticated |
| GET | `/api/schedules/calendar` | 日历视图查询 | Authenticated |
| GET | `/api/schedules/today` | 今日日程 | Authenticated |
| GET | `/api/schedules/week` | 本周日程 | Authenticated |
| GET | `/api/schedules/{id}` | 日程详情 | Owner / Participant / Admin |
| POST | `/api/schedules` | 新增日程或会议 | Authenticated |
| PUT | `/api/schedules/{id}` | 修改日程 | Owner / Admin |
| DELETE | `/api/schedules/{id}` | 删除日程 | Owner / Admin |
| POST | `/api/schedules/{id}/participants` | 添加参与人 | Owner / Admin |
| DELETE | `/api/schedules/{id}/participants/{userId}` | 移除参与人 | Owner / Admin |
| POST | `/api/schedules/{id}/accept` | 接受会议 | Participant |
| POST | `/api/schedules/{id}/reject` | 拒绝会议 | Participant |
| POST | `/api/schedules/ai-parse` | 自然语言解析日程 | Authenticated |

规则：
- 日程类型：`PERSONAL`、`MEETING`。
- 支持提前 5 分钟、15 分钟、30 分钟、1 小时提醒。
- 定时任务扫描待提醒日程，提醒事件先进入 RabbitMQ。
- 消费者负责站内信、WebSocket 和邮件提醒。

## 新闻接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/news/page` | 新闻分页，员工仅看已发布新闻 | Authenticated |
| GET | `/api/news/{id}` | 新闻详情并可使用缓存 | Authenticated |
| POST | `/api/news` | 新增新闻 | Admin |
| PUT | `/api/news/{id}` | 修改新闻 | Admin |
| DELETE | `/api/news/{id}` | 删除新闻 | Admin |
| POST | `/api/news/{id}/publish` | 发布新闻 | Admin |
| POST | `/api/news/{id}/offline` | 下架新闻 | Admin |
| POST | `/api/news/{id}/top` | 置顶或取消置顶 | Admin |
| POST | `/api/news/{id}/view` | 增加阅读量 | Authenticated |
| POST | `/api/news/{id}/comments` | 评论新闻 | Authenticated |
| GET | `/api/news/{id}/comments` | 新闻评论列表 | Authenticated |
| DELETE | `/api/news/comments/{commentId}` | 删除评论 | Owner / Admin |
| POST | `/api/news/{id}/like` | 点赞 | Authenticated |
| DELETE | `/api/news/{id}/like` | 取消点赞 | Authenticated |
| POST | `/api/news/{id}/favorite` | 收藏 | Authenticated |
| DELETE | `/api/news/{id}/favorite` | 取消收藏 | Authenticated |
| GET | `/api/news/favorites/my` | 我的收藏 | Authenticated |
| POST | `/api/news/ai-generate` | 新闻 AI 生成 | Admin |
| POST | `/api/news/ai-polish` | 新闻 AI 润色 | Admin |

规则：
- 新闻状态：`DRAFT`、`PUBLISHED`、`OFFLINE`。
- 员工评论后直接显示，不做审核。
- 新闻发布后通过 RabbitMQ 给员工发送通知。
- 修改、删除、下架新闻时清理新闻缓存。

## 字典接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/dict-types/page` | 字典类型分页 | Admin |
| POST | `/api/dict-types` | 新增字典类型 | Admin |
| PUT | `/api/dict-types/{id}` | 修改字典类型 | Admin |
| DELETE | `/api/dict-types/{id}` | 删除字典类型 | Admin |
| GET | `/api/dict-data/page` | 字典数据分页 | Admin |
| GET | `/api/dict-data/type/{typeCode}` | 按类型编码查询字典项 | Authenticated |
| POST | `/api/dict-data` | 新增字典数据 | Admin |
| PUT | `/api/dict-data/{id}` | 修改字典数据 | Admin |
| DELETE | `/api/dict-data/{id}` | 删除字典数据 | Admin |
| POST | `/api/dict-data/cache/refresh` | 刷新字典缓存 | Admin |

规则：
- 字典数据是高频读、低频写数据，应使用 Redis 缓存。
- 字典变更后清理或刷新缓存。

## 文件接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/files/upload` | 上传文件 | Authenticated |
| GET | `/api/files/{id}` | 文件详情 | Owner / Admin / Resource permission |
| GET | `/api/files/download/{id}` | 文件下载 | Owner / Admin / Resource permission |
| GET | `/api/files/business` | 按业务类型和业务 ID 查询附件 | Authenticated |
| DELETE | `/api/files/{id}` | 删除文件 | Owner / Admin |

规则：
- 本地磁盘按年月目录存储。
- 文件使用 UUID 重命名。
- 文件元数据保存到 `sys_file`。
- 文件记录逻辑删除，物理删除可异步。

## 通知接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/notifications/page` | 我的站内信分页 | Authenticated |
| GET | `/api/notifications/unread-count` | 未读数量 | Authenticated |
| PUT | `/api/notifications/{id}/read` | 标记单条已读 | Owner |
| PUT | `/api/notifications/read-batch` | 批量标记已读 | Authenticated |
| DELETE | `/api/notifications/{id}` | 删除通知 | Owner / Admin |
| POST | `/api/notifications/system` | 管理员发送系统通知 | Admin |

通知消息对象至少包含：

```json
{
  "receiverId": 1,
  "senderId": 2,
  "title": "新的审批待处理",
  "content": "张三提交了一条请假审批",
  "type": "approval.submit",
  "businessType": "APPROVAL",
  "businessId": 1001,
  "createdAt": "2026-06-09T10:00:00"
}
```

RabbitMQ 约定：

| 项 | 值 |
| --- | --- |
| exchange | `oa.notification.exchange` |
| queue | `oa.notification.queue` |
| routing key | `oa.notification.*` |

事件类型：
- `approval.submit`
- `approval.approved`
- `approval.rejected`
- `schedule.reminder`
- `schedule.invite`
- `news.publish`
- `news.comment`
- `system.notice`

## WebSocket

| 项 | 值 |
| --- | --- |
| 地址 | `/ws/notification?token=xxx` |
| 鉴权 | 建连时解析并校验 JWT |
| 在线映射 | 保存 `userId -> session` |
| 离线行为 | 只保存站内信，不报错 |

推送消息格式：

```json
{
  "type": "approval.submit",
  "title": "新的审批待处理",
  "content": "张三提交了一条请假审批",
  "businessType": "APPROVAL",
  "businessId": 1001,
  "time": "2026-06-09 10:00:00"
}
```

## AI 接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/api/ai/approval-summary` | 审批内容 AI 摘要 | Admin |
| POST | `/api/ai/approval-risk` | 审批风险提示 | Admin |
| POST | `/api/ai/news-generate` | 新闻 AI 生成 | Admin |
| POST | `/api/ai/news-polish` | 新闻 AI 润色 | Admin |
| POST | `/api/ai/schedule-parse` | 日程 AI 助手 | Authenticated |
| POST | `/api/ai/qa` | 智能问答助手 | Authenticated |
| GET | `/api/ai/logs/page` | AI 调用日志分页 | Admin |

规则：
- 默认使用 `mock` provider。
- 预留通义千问 provider：`tongyi`。
- 所有 AI 调用记录 `sys_ai_log`。
- 员工只展示和使用智能问答、日程 AI 助手。

