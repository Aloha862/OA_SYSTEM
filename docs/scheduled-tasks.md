# 定时任务设计与运行说明

## 1. 适合设置定时任务的位置

| 业务位置 | 设置方式 | 当前实现 | 原因 |
| --- | --- | --- | --- |
| 日程开始前提醒 | 周期扫描 | `ScheduleReminderTask` | 提醒时间由业务数据动态决定，无法为每条日程写固定 Cron |
| 过期日程自动完成 | 周期扫描 | `ScheduleReminderTask#finishExpiredSchedules` | `end_time` 是明确的生命周期边界，适合批量更新状态 |
| 待审批事项逾期提醒 | 工作日定时扫描 | `ApprovalOverdueReminderTask` | 按提交时长识别积压审批，并通过通知 Outbox 可靠投递 |
| 通知 Outbox 重试 | 固定延迟 | `NotificationOutboxServiceImpl` | 需要持续重试失败或未发送事件 |
| 邮件 Outbox 重试 | 固定延迟 | `MailServiceImpl` | 邮件是慢速外部 I/O，需要与主请求解耦并重试 |
| WebSocket 心跳 | 固定延迟 | `NotificationWebSocketHandler` | 用于识别失效连接和保持实时通道健康 |
| AI 日志保留清理 | Cron | `AiLogRetentionTask` | 数据保留策略按天执行即可，无需高频扫描 |

新增任务与原有任务共用独立的 `ThreadPoolTaskScheduler`。默认四个调度线程，避免邮件、消息或外部网络调用阻塞日程提醒。

迁移 `V4__scheduled_task_indexes.sql` 为日程生命周期扫描和审批逾期扫描增加复合索引，避免数据增长后定时任务退化为全表扫描。

## 2. 当前不应直接增加定时任务的位置

- Redis 登录票据和普通缓存：已有 TTL，到期由 Redis 管理；再增加全表扫描会重复工作。
- 审批单的 `end_time`：这是请假或出差的业务结束时间，不是审批 SLA，不能据此判断审批逾期。
- 新闻定时发布：当前表结构没有 `scheduled_publish_at` 字段。需要先补充发布时间、发布状态与幂等发布规则，再增加任务。
- 文件清理：当前缺少“文件是否被业务引用”和保留周期规则，直接定时删除可能造成数据丢失。

## 3. 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `OA_TASKS_ZONE` | `Asia/Shanghai` | 全部 Cron 任务时区 |
| `OA_TASKS_POOL_SIZE` | `4` | 调度线程数，程序限制为 2 至 16 |
| `OA_SCHEDULE_REMINDER_CRON` | `0 * * * * ?` | 每分钟扫描日程提醒 |
| `OA_SCHEDULE_LIFECYCLE_CRON` | `0 */5 * * * ?` | 每五分钟完成已过期日程 |
| `OA_APPROVAL_OVERDUE_ENABLED` | `true` | 是否启用审批逾期提醒 |
| `OA_APPROVAL_OVERDUE_CRON` | `0 0 9 * * MON-FRI` | 工作日 09:00 扫描逾期审批 |
| `OA_APPROVAL_OVERDUE_HOURS` | `24` | 提交后超过多少小时视为逾期 |
| `OA_APPROVAL_OVERDUE_BATCH_SIZE` | `200` | 单次最多扫描条数，程序上限 1000 |

生产环境只有一个应用实例时可直接使用默认配置。多实例部署时，日程状态更新本身具有条件幂等性；审批提醒使用 `approval-overdue:{approvalId}:{date}` 事件 ID，并由通知 Outbox 唯一约束防止重复投递。若以后增加非幂等任务，应引入 ShedLock、数据库租约或平台级分布式调度。

## 4. 验证方法

### 自动化测试

```powershell
cd backend
mvn "-Dtest=AsyncSchedulingConfigTest,ScheduleLifecycleTaskTest,ApprovalOverdueReminderTaskTest" test
```

测试覆盖调度线程池、过期日程批量完成、审批提醒字段以及同一天事件 ID 的幂等性。

### 本地高频验证

仅在测试环境临时设置：

```powershell
$env:OA_SCHEDULE_LIFECYCLE_CRON='*/2 * * * * ?'
$env:OA_APPROVAL_OVERDUE_CRON='*/2 * * * * ?'
$env:OA_APPROVAL_OVERDUE_HOURS='1'
```

启动后创建一条 `endTime` 已过去、状态为 `NORMAL` 的日程。两秒内应变为 `FINISHED`。准备一条提交时间超过一小时的 `PENDING` 审批后，通知 Outbox 应出现 `approval-overdue:{approvalId}:{当天日期}` 事件；重复扫描不能产生第二条事件。

完成验证后应清除临时环境变量，恢复默认频率，避免测试配置进入生产环境。
