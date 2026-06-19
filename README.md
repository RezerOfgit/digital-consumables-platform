[![DCP Project CI](https://github.com/RezerOfgit/digital-consumables-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/RezerOfgit/digital-consumables-platform/actions/workflows/ci.yml)
# 数字化耗材管控平台 (DCP)

> 基于 Spring Boot + Vue 3 的实验室耗材全生命周期管理系统，集成 AI 风控与 MQ 异步解耦，实现领用审批、库存管控、安全风控的业务闭环。

## 项目背景

我之前在电池材料研发岗和洁净间实验室都工作过。那段经历让我接触到一线的物料流转流程：高危试剂的领用记录分散在纸质单据和 Excel 台账里，月底盘点经常对不上账，事后想追溯某批耗材的去向，翻记录能翻半天。

我是从化工转行做开发的，所以这个项目不是我凭空想象的 Demo，而是用代码把那些真实的业务痛点转化成了一套能落地的数字化方案。

## 技术栈

| 层级 | 技术选型 |
| :--- | :--- |
| 后端框架 | Spring Boot 2.7.18 |
| 前端框架 | Vue 3 + Element Plus + Vite 4 |
| 数据库 | MySQL 8.0 + MyBatis-Plus 3.5.3.1 |
| 缓存与并发控制 | Redis + Lettuce 连接池 + MySQL 乐观锁 |
| 消息队列 | RabbitMQ（Direct 交换机 + Spring Retry 消费保障） |
| 安全认证 | Spring Security + JWT + BCrypt + RBAC |
| AI 集成 | DeepSeek API（MQ 异步调用 + 风控提示词模板外置） |
| 参数校验 | JSR-303 (Spring Boot Validation) |
| API 文档 | Knife4j 3.0.3 / Swagger |
| 接口限流 | Redis + Lua 脚本 (自定义 `@RateLimit` 注解) |
| 审计日志 | Spring AOP + ThreadLocal + 自定义 `@AuditLog` 注解 |
| 容器化 | Docker Compose 一键编排 |

## 核心功能

### 1. 高并发下怎么防止库存超卖

领用请求进来后，先用 Redis 的 `DECRBY` 命令做原子预扣减。扣减成功之后，再去 MySQL 做乐观锁落盘。`material` 表里有一个 `version` 字段，更新时带上 `WHERE version = ?`，MyBatis-Plus 的 `@Version` 注解自动处理了版本号的累加和比较。

如果两个线程同时查到了 `version=3`，只有一个能更新成功。失败的线程会触发 Redis 补偿，把刚才扣掉的库存加回去，然后提示用户重试。为了验证这套机制，我写了一个 40 线程的并发测试用例，确认了没有超卖。

### 2. AI 风控与 MQ 异步解耦

主流程只管扣库存、生成记录，然后把风控请求发送到 RabbitMQ 队列，主线程立即返回，零阻塞。

MQ 消费者异步调用 DeepSeek API，把耗材名称和用途说明拼成外置提示词模板发过去。如果 AI 返回"高危"，消费者将记录标记为"高危待人工审批"（status=3），由管理员在审批管理页面做最终决策——同意或驳回，且必须填写审批意见。驳回后自动触发库存归还补偿。

批量领用时，AI 会一次性评估整个清单是否存在配伍禁忌——比如硝酸（强氧化剂）和乙醇（易燃有机物）同时申请，会被自动拦截。消费端配合 Spring Retry，失败自动重试 3 次，保障消息不丢失。

### 3. 审计日志无侵入设计

自定义 `@AuditLog` 注解 + AOP 切面 + ThreadLocal 上下文透传。业务方法上贴一个注解，切面自动从 ThreadLocal 里取当前用户名（JWT 过滤器提前塞好的），把操作人、操作模块、请求参数写进 `sys_log` 表。新增接口只需一行注解即可接入，业务代码零侵入。

### 4. Redis 旁路缓存

引入 Cache-Aside 模式，实现启动预热 + 懒加载 + 异常降级查库三重保障。分类列表接口耗时从 70ms（首次查库）压缩至 1ms（缓存命中）。

### 5. 接口防刷与角色级权限隔离

基于 Redis + Lua 脚本实现 `@RateLimit` 注解，限流粒度精确到用户+接口，原子性保障无竞态。结合 Spring Security + JWT 实现角色级访问控制，管理员（admin）与实验员（test01）权限完全分离。

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/RezerOfgit/digital-consumables-platform.git
cd digital-consumables-platform

# 2. 构建后端 jar 包
mvn clean package -DskipTests

# 3. 配置环境变量
cp .env.example .env
# 编辑 .env，填入 DB_PASSWORD、DEEPSEEK_API_KEY、DCP_JWT_SECRET

# 4. 一键启动
docker compose up -d
```

启动后访问：
- 后端接口文档：http://localhost:8080/doc.html
- 前端页面：http://localhost:3000

### 方式二：本地手动启动

**后端：**

1. 确保本地 MySQL 8.0、Redis、RabbitMQ 已启动
2. 执行 `src/main/resources/db/schema.sql` 建库建表
3. 在 IDE 运行配置中设置环境变量：

   | 变量名 | 说明 | 示例值 |
   |--------|------|--------|
   | `DB_PASSWORD` | MySQL 连接密码 | `your_password` |
   | `REDIS_PASSWORD` | Redis 密码（无密码留空） | |
   | `DEEPSEEK_API_KEY` | DeepSeek API Key | `sk-xxx` |
   | `DCP_JWT_SECRET` | JWT 签名密钥（≥32 字符） | `DcpSecretKey2026!` |

4. 启动 `DigitalConsumablesPlatformApplication`

**前端：**

```bash
cd dcp-frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 测试账号

| 角色 | 用户名 | 密码 |
| :--- | :--- | :--- |
| 管理员 | `admin` | `123456` |
| 实验员 | `test01` | `123456` |

## 项目结构

```
digital-consumables-platform/
├─ dcp-backend/                      # 后端
│  ├─ src/main/java/com/dcp/
│  │  ├─ annotation/                 # 自定义注解 (@AuditLog, @RateLimit)
│  │  ├─ aspect/                     # AOP 切面 (审计日志、接口限流)
│  │  ├─ config/                     # Security, Redis, RabbitMQ, RestTemplate 等配置
│  │  ├─ controller/                 # 接口层
│  │  ├─ dto/                        # 数据传输对象
│  │  ├─ entity/                     # 数据库实体
│  │  ├─ exception/                  # 全局异常处理 & 自定义业务异常
│  │  ├─ mapper/                     # MyBatis-Plus Mapper 接口
│  │  ├─ security/                   # JWT 认证、Spring Security 实现
│  │  ├─ service/                    # 业务逻辑层
│  │  └─ utils/                      # 工具类 (ThreadLocal 用户上下文)
│  ├─ src/main/resources/
│  │  ├─ db/schema.sql               # 数据库初始化脚本
│  │  ├─ lua/rate_limit.lua          # Redis 限流 Lua 脚本
│  │  └─ templates/ai_risk_prompt.txt # AI 风控提示词模板（外置可热改）
│  ├─ src/test/                      # 单元测试 & 并发压力测试
│  ├─ Dockerfile
│  └─ pom.xml
│
├─ dcp-frontend/                     # 前端
│  ├─ src/
│  │  ├─ api/
│  │  │  ├─ index.js                 # API 接口定义
│  │  │  └─ request.js               # Axios 请求封装（拦截器、Token 注入）
│  │  ├─ assets/                     # 静态资源（hero.png, vue.svg）
│  │  ├─ components/
│  │  │  └─ Layout.vue               # 公共布局组件
│  │  ├─ router/
│  │  │  └─ index.js                 # 路由配置（含角色权限守卫）
│  │  ├─ stores/
│  │  │  └─ user.js                  # Pinia 用户状态管理
│  │  ├─ views/
│  │  │  ├─ Login.vue                # 登录页
│  │  │  ├─ Home.vue                 # 耗材列表 + 领用申请
│  │  │  ├─ Records.vue              # 领用记录 + 审批弹窗
│  │  │  └─ Admin.vue                # 管理中心（耗材/分类/审批管理）
│  │  ├─ App.vue                     # 根组件
│  │  └─ main.js                     # 入口文件
│  ├─ public/                        # 公共静态文件
│  ├─ index.html                     # HTML 入口
│  ├─ vite.config.js                 # Vite 配置（代理、端口）
│  ├─ package.json                   # 项目依赖与脚本配置
│  ├─ package-lock.json              # 依赖版本锁定文件
│  └─ README.md
│
├─ docker-compose.yml                # 一键编排
├─ .env.example                      # 环境变量模板
└─ README.md
```

## 路线图 (Roadmap)

- **库存流水** — 表结构已建好，后续接入领用/归还/报废的全链路记录
- **死信队列** — 为 RabbitMQ 接入死信队列，AI 风控消费失败时进入补偿流程
- **监控告警** — 接入 Prometheus + Grafana，对 MQ 堆积、接口耗时、AI 调用成功率做实时监控