# OA_SYSTEM 全流程审查、测试与整改报告（阶段记录）

> 最新最终结论见 [`final-audit-report-2026-06-23.md`](./final-audit-report-2026-06-23.md)。本文件保留 2026-06-22 阶段证据，不再代表当前完成状态。

> 审查日期：2026-06-22。所有“通过”均来自隔离库 `oa_management_test`、Redis DB 15 或构建/自动测试证据；未取得运行证据的项目明确标为“需要进一步确认”。

## 第一部分：项目整体运行审查结论

- 后端可编译并通过现有 5 个单元测试；前端生产构建通过。
- 隔离环境中已验证管理员/员工登录、真实 HTTP 403 权限语义、MySQL 持久化、Redis TTL、RabbitMQ 通知落库和 DashScope 真流式回复。
- AI 真流式实测首个 delta 约 1.7 秒，总耗时约 4.8 秒，收到 22 个 SSE 事件并正常 `done`。
- Redis 原实现对 `Stream.toList()` 返回的不可变列表缺少根类型元数据，导致反序列化失败并回源；已修复并增加往返测试。
- 当前最严重的剩余架构风险是业务写入与 MQ 发布不是原子操作，进程在两者之间崩溃仍可能丢通知。生产化前应完成 Outbox，而不是仅依赖 RabbitMQ 重试。
- SMTP 账号/授权码已经通过环境变量接入，但真实外部投递成功仍需以最新超时配置重新验证，当前状态为“需要进一步确认”。
- 最新综合脚本实际结果为 40 项通过、3 项失败；失败项是新闻发布、系统通知发送及其后续查询，直接原因均为 RabbitMQ `localhost:5672` 拒绝连接。测试容器虽收到启动指令，但 5672 未进入监听状态，因此本轮没有把消息链路标为通过。
- Flyway 目前只有 V2 增量脚本，不能从空库完整建库；不能宣称已具备自动迁移能力。

## 第二部分：核心功能真实测试清单

| 模块 | 测试目标 | 测试步骤 | 预期结果 | 当前状态 | 通过 | 问题与建议 |
|---|---|---|---|---|---|---|
| 登录注册 | 身份签发与数据落库 | `POST /api/auth/login`，管理员/员工各一次 | HTTP 200、返回 token | 隔离库已验证登录；注册由回归脚本覆盖 | 部分 | 增加弱密码、重复账号、并发注册测试 |
| 权限校验 | 员工不能访问管理接口 | 员工 token 调用管理员用户接口 | HTTP 403 且业务码 403 | 已验证 | 是 | 保持 HTTP 状态与业务码一致 |
| 缓存 | 命中、TTL、更新失效 | 连续两次查部门/字典；检查 DB15 key/TTL；更新后重查 | 第二次不访问 DB；TTL 合理；更新即失效 | TTL 已验证；序列化缺陷已修复并单测 | 部分 | 增加命中率指标、空值短 TTL、TTL 抖动和热点互斥锁 |
| 异步任务 | 真异步、异常可见 | 触发邮件；比较请求线程与 `oa-async-*` 日志 | 请求快速返回；失败有重试/最终日志 | 有界线程池、重试和 recover 已实现 | 部分 | SMTP 最新配置需再实投；增加 Micrometer 队列指标 |
| 消息发送 | 消息可靠发布/落库 | 管理员发送单用户系统通知 | API 200，队列消费，通知表新增 | 已验证落库及 eventId | 是 | 生产化补 Outbox |
| 消息接收 | 历史与未读一致 | 员工查询列表/未读，标记已读后复查 | 列表存在且未读递减 | 接口与缓存失效代码已核对 | 部分 | 用浏览器双用户再跑完整闭环 |
| WebSocket | 实时、隔离、重连 | 获取一次性 ticket；双用户连接；断线重连 | ticket 仅用一次；不串线；重连补拉 | ticket、抖动重连、eventId 去重已实现 | 部分 | 多用户浏览器场景与多实例 Pub/Sub 未完成 |
| 邮件通知 | SMTP 真投递与失败重试 | 单收件人通知；等待成功或三次失败日志 | 成功送达，或 15 秒超时后有 recover | 配置已接环境变量 | 待确认 | 禁止群发测试；补邮件发送日志表/Outbox |
| AI 接口 | 上下文、持久化、SSE | 建会话，连续提问，观察 meta/delta/done | 真实逐段输出、消息落库 | DashScope 已实测通过 | 是 | 增加首 token 前有限重试、敏感信息治理 |
| AI 页面 | 键盘、流式、停止、复制、响应式 | 5174 登录后进行桌面/移动端流程 | 无整页阻塞，自动滚动，可停止/复制 | 生产构建通过，页面已重构 | 部分 | Playwright 视觉回归待补 |
| AI 历史 | 新建、切换、删除、清空、改名 | 操作侧栏并刷新页面 | 历史从 MySQL 恢复且归属正确 | 后端 CRUD 已实现 | 部分 | 前端显式重命名入口待补 |
| AI 异常 | 超时、断网、空回复、重试 | mock/无效 key/中止请求 | 友好错误、FAILED 状态、可重试 | SSE error 与 FAILED 持久化已实现 | 部分 | 补自动化异常用例和取消状态 |

浏览器补充证据：Playwright 使用系统 Edge 在 1440×900 和 390×844 下完成登录与 `/ai` 访问；桌面端、移动端均无页面级横向滚动，移动端菜单和输入器可见。受执行沙箱外网权限限制，本轮 DashScope 请求产生了可见 FAILED 提示；此前同一隔离环境已取得真实 DashScope `delta/done` 成功证据。

## 第三部分：后端问题清单与修改建议

1. `config/RedisConfig.java`：`NON_FINAL` 对最终不可变集合不写根类型，缓存读取失败。已改为 `EVERYTHING`；部署时应清理旧格式 key，再观察第二次查询 SQL 日志。
2. `schedule/service/impl/ScheduleServiceImpl.java#scanAndSendReminders`：原先“查询后发送再更新”在并发调度器中重复发送。已增加 `reminder_status: 0 -> 2` 条件抢占，异常回滚为 0。后续建议用带租约时间的独立任务表恢复永久卡死任务。
3. `notification/mq/NotificationConsumer.java`：已用 `event_id + receiver_id` 唯一键幂等并配置重试/DLQ；但数据库事务和发布仍非原子。建议新增 `sys_outbox_event(id,event_id,topic,payload,status,retry_count,next_retry_at,created_at)`，业务事务只写 Outbox，由发布器确认后标记 SENT。
4. `notification/service/impl/MailServiceImpl.java`：已异步、三次重试、最终 recover 和 SMTP 超时。建议新增 `sys_mail_delivery`，记录收件人脱敏值、模板、状态、错误摘要、重试次数，严禁记录授权码。
5. `notification/websocket/*`：一次性 Redis ticket 已取代 URL 长期 JWT；仍需 ping/pong 心跳和 Redis Pub/Sub 扇出，才能支持多实例。
6. `ai/service/impl/AiConversationServiceImpl.java`：已支持 20 条/12000 字上下文、会话归属、clientMessageId 幂等、SSE 和重新生成。建议把限制改为配置项，并在首 token 之前做最多两次指数退避；首 token 后禁止自动重试，避免重复文本。
7. `db/migration/V2__reliability_and_ai_conversations.sql`：当前不是完整 Flyway 链。建议将现有 `schema.sql` 整理为幂等 V1，在全新容器执行 `flyway:migrate`，旧库用 baseline 后再升级。
8. `common/exception/GlobalExceptionHandler.java`：已修复始终 HTTP 200 的问题，并避免将系统异常详情返回客户端。建议增加 traceId，并对参数校验错误输出字段级错误。

## 第四部分：前端问题清单与修改建议

- AI 路由已设为独立沉浸式布局，不再嵌套传统后台侧栏。
- `api/ai.ts` 使用 Fetch 解析 SSE；已支持 AbortSignal、meta/delta/done/error 和重新生成。建议抽成通用 SSE 客户端并覆盖分包、多行 data、畸形 JSON 测试。
- AI 页面已具备固定输入区、Enter 发送、Shift+Enter 换行、请求禁重复、自动高度、停止、回到底部、快捷问题、Markdown、安全净化、代码高亮、复制与移动端侧栏。
- 仍建议将会话、消息、流状态迁入 `stores/ai.ts`，避免页面组件承担请求与业务状态；切换路由后也可保留生成状态。
- 当前只有整条回答复制；代码块应增加独立复制按钮和语言标签。
- Element Plus 与 ECharts 分包后仍较大。应按路由按需加载图表和 Element Plus 组件，避免 AI 首屏下载不相关代码。

## 第五部分：AI 助手专项优化方案

- 接口：`GET/POST /api/ai/v2/conversations`、`PATCH/DELETE /{id}`、`GET/DELETE /{id}/messages`、`POST /{id}/messages/stream`、`POST /{id}/messages/{messageId}/regenerate`。
- SSE：`meta` 返回会话及消息 ID；`delta` 仅携带增量；`done` 返回结束原因和耗时；`error` 返回稳定错误码、友好文案和 retryable。
- 表：`sys_ai_conversation` 存用户、标题、模式、模型、最后消息时间；`sys_ai_message` 存角色、parent、clientMessageId、内容、状态、模型、耗时和错误摘要。唯一键保证客户端发送幂等。
- 上下文：数据库是事实源；Redis 只缓存最近会话摘要，TTL 30 分钟。按 token 预算从近到远裁剪，并始终保留系统指令和最后问题。
- Markdown：`markdown-it` 关闭 HTML，DOMPurify 二次净化，highlight.js 仅注册常用语言。外链增加 `rel=noopener noreferrer`。
- 错误：区分认证失败、限流、上游超时、内容为空、客户端中止；已开始输出后不可透明重试。
- 推荐组件：`ConversationSidebar`、`ChatMessage`、`MarkdownRenderer`、`ChatComposer`；后续增加 `CodeBlockToolbar`、`AiModeSelector`、`useAiStream` 与 Pinia `ai` store。
- 实施顺序：稳定 SSE 契约与数据库迁移 → 异常/幂等测试 → 状态迁 Pinia → 代码块工具条 → Playwright 桌面/移动端回归 → 指标、限流和内容治理。

## 第六部分：AI 助手新版页面设计方案

- 左侧 280px 会话栏：品牌、新建、搜索、按最近更新时间排序、删除；小屏转抽屉。
- 中间聊天区：顶部轻量状态/模式，正文最大宽度 850px；用户右侧蓝灰气泡，AI 左侧无厚重卡片的排版块。
- 底部输入：渐隐背景上的固定自适应输入器，明确发送/停止状态，下方提示“AI 生成内容仅供参考”。
- 空状态：一个主问题、能力说明与四个任务型快捷入口，不放仪表盘式统计卡。
- 加载：消息内光标与顶部状态，不用遮罩整页；失败保留已生成内容并给出重试/重新生成。
- 移动端：58px 顶栏、会话抽屉、单列快捷问题、safe-area 底部间距、44px 触控目标。

## 第七部分：最终整改优先级

- **P0**：完成空库 Flyway V1/V2 验证；完成通知/邮件 Outbox；生产环境强制非默认 JWT/数据库/RabbitMQ 密码。原因：这些问题会造成无法部署、消息永久丢失或安全事故。
- **P1**：SMTP 实投与失败回放、WebSocket 多用户/重连/心跳/PubSub、缓存旧 key 清理和命中观测、AI 取消/限流/首 token 前重试。原因：直接影响核心流程稳定可用。
- **P2**：AI Pinia 状态、代码块复制、显式改名、移动端与 Playwright 视觉回归、前端包体优化。原因：功能已有但体验和回归保障仍有缺口。
- **P3**：统一 traceId、指标仪表盘、日志保留/脱敏策略、缓存注解与统一工具、任务租约模型。原因：主要提升长期运维和维护效率。

## 可复现命令

```powershell
cd backend
mvn test
$env:DB_USERNAME='root'; $env:DB_PASSWORD='<本机隔离库密码>'; $env:OA_AI_PROVIDER='tongyi'
mvn spring-boot:run "-Dspring-boot.run.profiles=test" -DskipTests

cd ../frontend
npm run build
$env:VITE_BACKEND_TARGET='http://127.0.0.1:8082'; npm run dev -- --host 127.0.0.1 --port 5174

cd ..
powershell -ExecutionPolicy Bypass -File scripts/qa-api-regression.ps1 -BaseUrl http://127.0.0.1:8082/api
```

严禁把真实密钥、授权码、token 或完整邮件地址写入日志、报告、提交历史或截图。
