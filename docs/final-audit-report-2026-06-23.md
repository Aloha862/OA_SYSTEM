# OA_SYSTEM 全流程审查、测试与整改报告

> 最终验证日期：2026-06-23。运行证据来自隔离数据库 `oa_management_test`、Redis DB 15、本机 RabbitMQ 测试容器、Edge 真实浏览器和自动化测试。未取得证据的项目均明确标注为“需要进一步确认”。

## 第一部分：项目整体运行审查结论

- 后端能够使用 `test` profile 在 8082 启动；`/actuator/health` 返回 HTTP 200。
- 健康检查确认数据库、Redis、RabbitMQ、磁盘、存活和就绪状态均为 `UP`。
- 前端能够在 5173 启动，登录、仪表盘、通知和 AI 助手页面可访问。
- Flyway 已形成 V1-V3 迁移链；全新空库已成功执行三版迁移，已有测试库已升级到 V3。
- 最终 API 回归结果为 `46/46`，无残留测试数据。
- 后端单元测试结果为 `10/10`，0 failure、0 error。
- 前端 Vitest 结果为 `4/4`；生产构建成功；`npm audit` 为 0 vulnerability。
- Notification Outbox 共验证 73 条历史记录全部进入 `SENT`；RabbitMQ 停止期间 API 仍能接受通知，恢复后能够续投。
- WebSocket 已真实验证：单实例不刷新时未读数从 7 实时更新为 8；断网期间消息落库、恢复网络后自动重连并从 8 更新为 9；双实例中消息由 8083 消费后可经 Redis Pub/Sub 推送到连接在 8082 的浏览器。
- AI 助手的会话、消息持久化、SSE、Enter 发送、复制、重新生成、刷新恢复和响应式布局已通过真实浏览器验证。
- 邮件配置、Mail Outbox、三次重试及 `FAILED_FINAL` 状态真实生效；但当前执行环境连接 `smtp.qq.com:587/465` 后均收不到 SMTP/TLS 握手数据，真实投递被外部网络阻断。
- 当前最严重的剩余问题不是代码启动问题，而是生产验收环境必须允许 SMTP 出站，并完成一次真实收件箱到达确认。

## 第二部分：核心功能真实测试清单

| 测试模块 | 测试目标 | 测试步骤 | 预期结果 | 当前状态 | 通过 | 问题说明 / 修复建议 |
|---|---|---|---|---|---|---|
| 登录注册 | 身份签发与用户落库 | admin、employee、lisi 分别调用 `/api/auth/login` | HTTP 200、返回 token | 三账号登录通过 | 是 | 注册边界仍建议增加专门的弱密码、并发重复账号测试 |
| 权限校验 | 用户数据隔离 | lisi 删除 employee 通知、其他用户删除文件 | HTTP 403 | 真实返回 403 | 是 | 保持 HTTP 状态与业务码一致 |
| 数据库迁移 | 空库和旧库均可升级 | 对空库执行 V1-V3；旧库 baseline 后升级 | schema 到 V3 | 两种场景均通过 | 是 | Flyway 9.22.3 对 MySQL 9.3 有兼容性警告，生产前升级 Flyway |
| 缓存功能 | TTL、序列化、失效和降级 | 字典刷新、Redis 往返、Redis 锁异常单测 | 缓存可读、更新失效、Redis 异常回源 | 序列化和降级测试通过 | 是 | 后续增加命中率指标和压测 |
| 异步任务 | 线程池、失败记录与重试 | 触发通知/邮件调度，检查状态和 retryCount | 请求线程不阻塞、失败可追踪 | Outbox 调度真实运行 | 是 | 建议增加任务队列 Micrometer 指标 |
| 消息发送 | 数据库与 MQ 可靠衔接 | RabbitMQ 停止时发送，恢复后检查 Outbox | API 接受、消息最终 SENT | 已验证 | 是 | 保留 FAILED_FINAL 告警和后台重试入口 |
| 消息接收 | 消费、落库、已读、隔离 | 系统通知后轮询列表、标记已读、跨用户删除 | 通知存在、状态正确、越权 403 | 46 项回归覆盖 | 是 | 无阻断问题 |
| WebSocket | 实时、离线恢复和跨实例推送 | 单实例推送；浏览器断网后发送再恢复；8082 禁用 Listener、8083 消费 | 不刷新更新；重连补齐；跨实例不串线 | 三种场景均通过 | 是 | 生产仍建议增加长时间抖动和大并发压测 |
| 邮件通知 | Outbox、重试、真实 SMTP | 将隔离用户邮箱临时指向测试邮箱并发送一次 | SMTP 接受并最终到达 | Outbox 与三次重试通过；握手超时 | 否 | 需要放通 SMTP 出站后复测收件箱；代码不可伪造通过 |
| AI 助手接口 | SSE、持久化和幂等 | 创建会话、发送、重新生成、刷新 | meta/delta/done、历史可恢复 | mock 全链路通过；此前 Tongyi 真流式通过 | 是/部分 | 当前网络下真实 Tongyi 复测需要进一步确认 |
| AI 页面交互 | 输入、复制、重新生成、错误提示 | Edge 桌面端操作 | 操作自然、状态明确 | 通过 | 是 | 重新生成已改为成功替换、失败保留原回答 |
| AI 历史记录 | 新建、切换、刷新恢复 | 新建会话后发送并刷新 | MySQL 恢复消息 | 通过 | 是 | 可继续增加显式重命名入口 |
| AI 异常处理 | 失败、取消与日志脱敏 | 单测日志清洗；代码检查取消和错误 SSE | 不泄露密钥、状态可恢复 | 单测通过 | 是/部分 | 上游 429/5xx 真实故障注入需要进一步确认 |
| 响应式页面 | 小屏无横向溢出 | Edge 调整为 390×844 | `scrollWidth == clientWidth` | 390 == 390 | 是 | 快捷问题横向滚动符合设计 |

## 第三部分：后端问题清单与修改结果

1. 配置文件：`backend/src/main/resources/application.yml`
   - 数据库、Redis、RabbitMQ、JWT、邮件和 AI 均改用环境变量。
   - 增加 Actuator 和邮件配置健康检查。
   - 建议生产部署强制校验默认密码未被使用。

2. 数据库迁移：`backend/src/main/resources/db/migration/`
   - `V1__baseline_schema.sql` 提供完整基线。
   - `V2__reliability_and_ai_conversations.sql` 增加消息幂等和 AI 会话结构。
   - `V3__notification_and_mail_outbox.sql` 增加通知及邮件 Outbox。

3. Redis 缓存：`backend/src/main/java/com/example/oa/common/cache/CacheSupport.java`
   - 增加 TTL 抖动、空值短 TTL、互斥锁、Lua 安全解锁、事务提交后失效和 Redis 故障回源。
   - 修复获取缓存锁失败时异常向业务接口传播的问题。
   - 新闻详情即使命中缓存也重新检查发布状态，避免草稿越权读取。

4. Notification Outbox：`NotificationOutboxServiceImpl`
   - 业务事务只写 Outbox；调度器使用 publisher confirm、mandatory return 和指数退避。
   - 状态包括 PENDING、SENDING、SENT、FAILED、FAILED_FINAL。
   - SENDING 具有两分钟租约，进程崩溃后可自动回收；失败记录可由管理员手动重试。
   - 管理查询接口为 `/api/notifications/outbox/page`，重试接口为 `POST /api/notifications/outbox/{id}/retry`。

5. Mail Outbox：`MailServiceImpl`
   - 邮件发送从请求/消费线程移到持久化 Outbox。
   - HTML 内容转义，失败保存安全错误摘要，最多自动重试三次。
   - SENDING 租约超时可回收；`FAILED_FINAL` 管理员重试已验证返回 HTTP 200 并重置为 PENDING，SENT 记录误重试返回 HTTP 409。
   - 管理查询接口为 `/api/notifications/mail-outbox/page`，重试接口为 `POST /api/notifications/mail-outbox/{id}/retry`。

6. WebSocket：`NotificationWebSocketHandler`、`NotificationRealtimeService`
   - 增加一次性 ticket、PING/PONG、超时连接清理、并发 session 装饰器和 Redis Pub/Sub。
   - 单实例实时推送、离线恢复以及 8082/8083 双实例 Redis 转发均已通过。

7. AI 服务：`AiConversationServiceImpl`、`TongyiAiServiceImpl`
   - 支持会话归属、上下文裁剪、Redis 活跃上下文、SSE、取消、首 token 前有限重试、日志脱敏和保留期清理。
   - 重新生成成功后软删除被替换回答；失败时保留原回答。
   - 旧回答清理失败只记录警告，不把已经成功生成的新回答错误标为 FAILED。

8. 异常与日志
   - 全局异常返回真实 HTTP 状态且不回传内部异常详情。
   - AI 日志会清理 API key、Authorization、邮箱和超长内容。
   - 建议下一阶段加入统一 traceId 和结构化 JSON 日志。

## 第四部分：前端问题清单与修改结果

- 路由：AI 助手使用独立沉浸式布局，不再套传统后台侧栏。
- 请求封装：普通接口统一 Axios；AI SSE 使用 Fetch + AbortSignal。
- 流式状态：支持 meta、delta、done、error、停止生成和错误保留。
- Markdown：关闭原始 HTML，DOMPurify 二次净化，外链强制 `target=_blank` 与 `rel=noopener noreferrer`。
- 代码块：语言标签、独立复制按钮和高亮样式已实现。
- 输入体验：Enter 发送、Shift+Enter 换行、自适应高度、请求中防重复和停止按钮均已实现并单测。
- 重新生成：由追加重复回答改为替换旧回答；浏览器刷新后仍只保留新回答。
- 响应式：桌面 1440×900 与移动端 390×844 均通过，无页面级横向滚动。
- 构建：生产构建成功；大体积 Element Plus/ECharts chunk 仍是 P2 性能优化项。
- 工具链：当前 Node 25 下 Vitest/jsdom 很慢，CI 推荐固定 Node 20 或 22 LTS。

## 第五部分：AI 助手专项优化结果

1. 当前状态：具备完整会话产品形态，而非传统表单页。
2. 接口：采用 `/api/ai/v2/conversations` 资源模型和 SSE 消息接口。
3. 会话历史：MySQL 为事实源，Redis 只保存短期活跃上下文。
4. 流式回复：只发送增量 delta；首 token 后禁止透明重试，避免重复文本。
5. Markdown：安全净化、代码高亮、外链隔离和代码复制已完成。
6. 错误兜底：FAILED/CANCELLED 持久化；前端显示友好错误并允许重试。
7. 用户体验：新建、切换、搜索、删除、清空、复制、重新生成、快捷问题和自动滚动已具备。
8. 组件拆分：`ConversationSidebar`、`ChatMessage`、`MarkdownRenderer`、`ChatComposer`。
9. 推荐接口字段：conversationId、clientMessageId、parentMessageId、role、status、modelName、costTimeMs、errorCode、errorMessage。
10. 数据表：`sys_ai_conversation` 与 `sys_ai_message` 已落地，逻辑删除支持重新生成替换。
11. 后续步骤：生产限流与配额、内容安全策略、真实上游故障注入、指标和成本统计。

## 第六部分：AI 助手新版页面设计验收

- 左侧：品牌、新建会话、搜索、历史列表、删除和返回 OA。
- 顶部：当前会话、服务状态、问答/新闻创作/内容润色/日程助手模式和清空。
- 中间：用户右侧消息块、AI 左侧正文式排版，避免厚重卡片堆叠。
- 底部：固定自适应输入框、快捷问题、发送/停止状态及免责声明。
- 空状态：主问题、能力说明和四个任务入口。
- 加载状态：消息级流式光标，不遮罩整页。
- 错误状态：保留失败消息和错误原因，允许重试；重新生成失败保留原回答。
- 移动端：侧栏抽屉、58px 顶栏、安全底部间距、输入区与消息区均可正常操作。
- 最终截图：`output/playwright/ai-desktop-final.png`、`output/playwright/ai-mobile-final.png`。

## 第七部分：最终整改优先级

- P0：当前隔离验收环境无未解决的代码级启动阻断项。生产上线前必须确认所有默认密码和 JWT secret 已替换。
- P1：放通 SMTP 587 或 465，完成一次真实收件箱到达验证；在目标生产网络复测 Tongyi；继续补充 WebSocket 长时间抖动和大并发压测。
- P2：固定 Node 20/22 LTS；按路由进一步拆分 Element Plus 和 ECharts；增加 AI 显式改名和会话分支体验。
- P3：增加 traceId、缓存命中率、Outbox 延迟、线程池队列、WebSocket 在线数、AI 首 token 延迟和 token 成本指标。

## 可复现命令

```powershell
cd backend
mvn test

cd ../frontend
npm test
npm run build
npm audit

cd ..
powershell -ExecutionPolicy Bypass -File scripts/qa-api-regression.ps1 `
  -BaseUrl http://127.0.0.1:8082/api

curl.exe http://127.0.0.1:8082/actuator/health
```

严禁把数据库密码、RabbitMQ 密码、SMTP 授权码、模型 API key、JWT 或完整收件人地址写入提交历史、报告、日志和截图。
