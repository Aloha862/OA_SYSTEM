# 企业 OA 管理系统

企业 OA 管理系统是一个前后端分离的后台管理项目，目标是覆盖用户、部门组织架构、审批、日程、新闻、字典、文件、站内信通知、WebSocket 实时推送和 AI 助手等核心办公场景。后端提供 RESTful JSON API，前端提供 Vue 3 + Element Plus 管理端界面。

## 技术栈

后端：
- Java 17
- Spring Boot 3.x
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8
- Redis
- RabbitMQ
- WebSocket
- Spring Mail
- Spring Validation
- Lombok
- Hutool
- Knife4j / Swagger
- Maven

前端：
- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- FullCalendar 或 Element Plus 日历组件
- ECharts
- WebSocket 原生 API 或封装工具

基础设施：
- Docker Compose
- MySQL 8
- Redis 7
- RabbitMQ management

## 目录结构

```text
OA_SYSTEM/
├── backend/                 # Spring Boot 后端项目
├── frontend/                # Vue 3 前端项目
├── docs/                    # 项目需求、接口和协作说明
├── docker-compose.yml       # MySQL、Redis、RabbitMQ 本地依赖
├── README.md
└── .gitignore
```

后端推荐包结构：

```text
com.example.oa
├── common
├── config
├── security
├── module
│   ├── user
│   ├── department
│   ├── approval
│   ├── schedule
│   ├── news
│   ├── dict
│   ├── file
│   ├── notification
│   └── ai
└── OaApplication.java
```

前端推荐源码结构：

```text
src
├── api
├── assets
├── components
├── layout
├── router
├── stores
├── styles
├── utils
└── views
```

## 环境要求

- JDK 17
- Maven 3.9+ 或项目内 Maven Wrapper
- Node.js 20 LTS+
- npm、pnpm 或 yarn
- Docker Desktop / Docker Engine
- Git

建议本地端口：

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 后端 API | `http://localhost:8080/api` | Spring Boot |
| 前端开发服务 | `http://localhost:5173` | Vite |
| MySQL | `localhost:3306` | 数据库 `oa_management` |
| Redis | `localhost:6379` | 无密码 |
| RabbitMQ AMQP | `localhost:5672` | 消息队列 |
| RabbitMQ Management | `http://localhost:15672` | 管理后台 |

## 启动基础依赖

在项目根目录执行：

```powershell
docker compose up -d
```

查看服务状态：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f rabbitmq
```

停止服务：

```powershell
docker compose down
```

如需清理本地数据卷：

```powershell
docker compose down -v
```

## 数据库、Redis、RabbitMQ 配置

`docker-compose.yml` 默认创建数据库 `oa_management`，并提供以下开发账号：

| 服务 | 用户名 | 密码 | 备注 |
| --- | --- | --- | --- |
| MySQL root | `root` | `root` | 管理账号 |
| MySQL app | `oa_user` | `oa_password` | 业务连接账号 |
| Redis | 无 | 无 | 默认无密码 |
| RabbitMQ | `guest` | `guest` | Management UI 和 AMQP |

后端 `application.yml` 建议配置：

```yaml
server:
  port: 8080

spring:
  application:
    name: oa-management
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/oa_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
      password:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 3
        max-concurrency: 10
        prefetch: 1

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

oa:
  jwt:
    secret: ${OA_JWT_SECRET}
    expiration: 86400000
  file:
    upload-path: D:/oa_files
    access-prefix: /files
  ai:
    provider: mock
    tongyi:
      api-key: ${TONGYI_API_KEY:}
      model: qwen-plus
```

## 后端启动

先启动 MySQL、Redis、RabbitMQ：

```powershell
docker compose up -d
```

进入后端目录：

```powershell
cd backend
```

如果项目包含 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

否则使用本机 Maven：

```powershell
mvn spring-boot:run
```

后端启动后建议检查：

- `http://localhost:8080/api/auth/me` 未登录应返回统一 JSON 的 `401`
- `http://localhost:8080/doc.html` 或 Swagger 地址可访问
- MySQL 表结构已初始化
- Redis、RabbitMQ 连接日志正常

## 前端启动

进入前端目录：

```powershell
cd frontend
```

安装依赖：

```powershell
pnpm install
```

或：

```powershell
npm install
```

设置前端 API 地址。推荐创建 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws/notification
```

启动开发服务：

```powershell
pnpm dev
```

或：

```powershell
npm run dev
```

## 默认账号

需求文档仅明确登录返回示例中的 `admin` 用户，未指定种子数据密码。建议后端初始化脚本统一使用以下开发账号，并在首次登录后修改密码：

| 角色 | 用户名 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | `admin` | `Admin@123456` | 拥有全部管理权限 |
| 员工 | `employee` | `Employee@123456` | 普通员工 |
| 审批员工 | `approver` | `Approver@123456` | `EMPLOYEE` 角色且 `is_approver = 1` |

如果后端种子数据使用不同密码，请以后端初始化 SQL 为准，并同步更新此处。

## 联调说明

1. 启动基础依赖：`docker compose up -d`。
2. 启动后端，确认数据库迁移或初始化 SQL 已执行。
3. 启动前端，确认 `VITE_API_BASE_URL` 指向 `http://localhost:8080/api`。
4. 登录成功后，前端在请求头携带 `Authorization: Bearer <token>`。
5. WebSocket 地址为 `ws://localhost:8080/ws/notification?token=<token>`。
6. RabbitMQ 通知链路为：业务事件 -> TopicExchange -> 消费者 -> `sys_notification` 落库 -> WebSocket 推送 -> 可选邮件提醒。
7. AI 默认使用 `mock` provider；真实通义千问接入需要设置 `TONGYI_API_KEY` 并切换 `oa.ai.provider=tongyi`。

联调优先级建议：

1. 登录、JWT 鉴权、当前用户信息。
2. 用户、部门、字典基础 CRUD。
3. 审批单提交、审批、撤回和通知。
4. 日程提醒、RabbitMQ、站内信、WebSocket。
5. 新闻发布、评论、点赞、收藏和缓存刷新。
6. 文件上传和元数据落库。
7. AI mock 功能和 AI 调用日志。

## 常见问题

### 端口被占用

修改 `docker-compose.yml` 对应端口，或通过环境变量覆盖：

```powershell
$env:MYSQL_PORT=3307
docker compose up -d mysql
```

### MySQL 连接失败

确认容器健康状态：

```powershell
docker compose ps mysql
```

确认连接参数与后端 `application.yml` 一致。默认数据库为 `oa_management`，默认 root 密码为 `root`。

### RabbitMQ Management 不能登录

确认 RabbitMQ 容器已启动并通过 `http://localhost:15672` 访问。默认用户名和密码均为 `guest`。

### Redis 缓存没有生效

检查后端是否启用缓存配置，并确认字典、部门树、新闻详情、未读通知数等高频数据写入了约定 key。

### 401 或 403

`401` 表示未认证或 token 失效；`403` 表示当前角色无权限。前端应在 `401` 时跳转登录页，在 `403` 时给出无权限提示。

### WebSocket 无推送

确认前端连接地址带有 token，后端握手时能解析 JWT，并确认业务事件已先进入 RabbitMQ、消费者已将通知落库。
