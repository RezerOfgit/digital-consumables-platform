[![DCP Project CI](https://github.com/RezerOfgit/digital-consumables-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/RezerOfgit/digital-consumables-platform/actions/workflows/ci.yml)
# 数字化耗材管控平台 (DCP)

## 项目背景

我之前在电池材料研发岗和洁净间实验室都工作过。那段经历让我接触到一线的物料流转流程：高危试剂的领用记录分散在纸质单据和 Excel 台账里，月底盘点经常对不上账，事后想追溯某批耗材的去向，翻记录能翻半天。

我是从化工转行做开发的，所以这个项目不是我凭空想象的 Demo，而是用代码把那些真实的业务痛点转化成了一套能落地的数字化方案。

## 技术栈

| 层级 | 技术选型 |
| :--- | :--- |
| 后端框架 | Spring Boot 2.7.18 |
| 数据库 | MySQL 8.0 + MyBatis-Plus 3.5.3.1 |
| 缓存与并发控制 | Redis + Lettuce 连接池 + MySQL 乐观锁 |
| 安全认证 | Spring Security + JWT + BCrypt + RBAC |
| 参数校验 | JSR-303 (Spring Boot Validation) |
| API 文档 | Knife4j 3.0.3 / Swagger |
| AI 集成 | DeepSeek API (异步调用 + 自动风控熔断) |
| 接口限流 | Redis + Lua 脚本 (自定义 `@RateLimit` 注解) |
| 审计日志 | Spring AOP + ThreadLocal + 自定义 `@AuditLog` 注解 |

## 核心功能

### 1. 高并发下怎么防止库存超卖

领用请求进来后，先用 Redis 的 `DECRBY` 命令做原子预扣减。扣减成功之后，再去 MySQL 做乐观锁落盘。`material` 表里有一个 `version` 字段，更新时带上 `WHERE version = ?`，MyBatis-Plus 的 `@Version` 注解自动处理了版本号的累加和比较。

如果两个线程同时查到了 `version=3`，只有一个能更新成功。失败的线程会触发 Redis 补偿，把刚才扣掉的库存加回去，然后提示用户重试。为了验证这套机制，我写了一个 40 线程的并发测试用例，确认了没有超卖。

### 2. AI 风控怎么做到非侵入

主流程只管扣库存、生成记录，不碰 AI。异步线程拿到生成好的 `recordId` 之后去调 DeepSeek，把耗材名称和用途说明拼成提示词发过去。如果 AI 返回"高危"，异步线程自动调用审批驳回逻辑，把库存退回去。

批量领用的时候，AI 会一次性评估整个清单是否存在配伍禁忌——比如硝酸（强氧化剂）和乙醇（易燃有机物）同时申请，会被自动拦截。

### 3. 审计日志为什么不用硬编码

一开始想过在 Service 里直接写日志插入代码，但发现每个方法都要重复差不多的事情。后来改成了自定义注解 + AOP 切面的方式：业务方法上贴一个 `@AuditLog` 注解，切面自动从 `ThreadLocal` 里取当前用户名（JWT 过滤器提前塞好的），把操作人、操作模块、请求参数写进 `sys_log` 表。业务代码完全不用动。

### 4. 为什么用 Redis + Lua 做限流

Java 代码先去 Redis 查次数再加 1，不是原子操作，高并发下有漏洞。Redis 执行 Lua 脚本是单线程原子操作，天然线程安全。我把它封装成了 `@RateLimit` 注解，限流粒度精确到"用户 + 接口"，防止恶意刷库存或浪费大模型 API 的额度。

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

1. 克隆项目后，先在本地构建 jar 包
   ```bash
   mvn clean package -DskipTests
   ```
2. 复制并编辑环境变量文件
   ```bash
   cp .env.example .env
   ```
   填入以下值：
    - `DB_PASSWORD` — MySQL 密码
    - `DEEPSEEK_API_KEY` — DeepSeek API Key
    - `DCP_JWT_SECRET` — JWT 签名密钥（≥32 字符）
3. 一键启动
   ```bash
   docker compose up -d
   ```
4. 访问接口文档：`http://localhost:8080/doc.html`

### 方式二：本地手动启动

1. 克隆项目
2. 执行 `src/main/resources/db/schema.sql` 建库建表
3. 在 IDE 运行配置中设置以下环境变量：

   | 变量名 | 说明 | 示例值 |
   |--------|------|--------|
   | `DB_PASSWORD` | MySQL 连接密码 | `your_password` |
   | `REDIS_PASSWORD` | Redis 密码（无密码留空） | |
   | `DEEPSEEK_API_KEY` | DeepSeek API Key | `sk-xxx` |
   | `DCP_JWT_SECRET` | JWT 签名密钥（≥32 字符） | `DcpSecretKey2026!` |

4. 确保本地 Redis 已启动（默认端口 6379）
5. 启动项目，访问 `http://localhost:8080/doc.html` 进入 Knife4j 接口文档

**测试账号：**

| 角色 | 用户名 | 密码 |
| :--- | :--- | :--- |
| 管理员 | `admin` | `123456` |
| 实验员 | `test01` | `123456` |

## 项目结构

```
digital-consumables-platform/
├─ src/main/java/com/dcp/
│  ├─ annotation/          # 自定义注解 (@AuditLog, @RateLimit)
│  ├─ aspect/              # AOP 切面 (审计日志、接口限流)
│  ├─ config/              # Security, Redis, RestTemplate 等配置
│  ├─ controller/          # 接口层
│  ├─ dto/                 # 数据传输对象 (支持单品/批量申请)
│  ├─ entity/              # 数据库实体
│  ├─ exception/           # 全局异常处理 & 自定义业务异常
│  ├─ mapper/              # MyBatis-Plus Mapper 接口
│  ├─ security/            # JWT 认证、Spring Security 实现
│  ├─ service/             # 业务逻辑层
│  └─ utils/               # 工具类 (ThreadLocal 用户上下文)
├─ src/main/resources/
│  ├─ db/schema.sql        # 数据库初始化脚本
│  ├─ lua/rate_limit.lua   # Redis 限流 Lua 脚本
│  └─ templates/ai_risk_prompt.txt  # AI 风险评估提示词模板
├─ src/test/               # 单元测试 & 并发压力测试
├─ Dockerfile              # 后端镜像构建
├─ docker-compose.yml      # MySQL + Redis + 后端一键编排
├─ .env.example            # 环境变量模板
├─ pom.xml
└─ README.md
```

## 路线图 (Roadmap)

- **库存流水** — 表结构已建好，后续接入领用/归还/报废的全链路记录
- **分布式事务** — 批量领用目前采用 Redis 补偿 + MySQL 事务回滚，后续考虑引入 TCC 或 Saga 模式
- **前端页面** — 计划开发配套前端，提供完整的可视化操作体验
