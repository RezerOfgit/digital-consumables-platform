# 数字化耗材管控平台 - 前端

> 基于 Vue 3 + Element Plus 构建的实验室耗材管控平台前端，配合后端实现领用、审批、风控的完整业务流程。

## 技术栈

- Vue 3 + Composition API
- Vue Router 4
- Pinia 状态管理
- Element Plus 组件库
- Axios HTTP 客户端
- Vite 4 构建工具

## 功能特性

- 用户登录认证（JWT Token）
- 耗材列表展示与领用申请（单品/批量）
- 领用记录查看，高危待审批标红提醒
- 管理员审批管理（统一弹窗 + 必填审批意见）
- 耗材入库与分类管理（管理员）
- AI 风控标记展示（备注中 AI 风控文字红色高亮）

## 快速开始

后端服务需先启动，详见 [根目录 README](../README.md)。

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:3000

### 测试账号

| 角色 | 用户名 | 密码 |
| :--- | :--- | :--- |
| 管理员 | `admin` | `123456` |
| 实验员 | `test01` | `123456` |

### 构建生产版本

```bash
npm run build
```

## 项目结构

```
dcp-frontend/                     # 前端
├─ src/
│  ├─ api/
│  │  ├─ index.js                 # API 接口定义
│  │  └─ request.js               # Axios 请求封装（拦截器、Token 注入）
│  ├─ assets/                     # 静态资源（hero.png, vue.svg）
│  ├─ components/
│  │  └─ Layout.vue               # 公共布局组件
│  ├─ router/
│  │  └─ index.js                 # 路由配置（含角色权限守卫）
│  ├─ stores/
│  │  └─ user.js                  # Pinia 用户状态管理
│  ├─ views/
│  │  ├─ Login.vue                # 登录页
│  │  ├─ Home.vue                 # 耗材列表 + 领用申请
│  │  ├─ Records.vue              # 领用记录 + 审批弹窗
│  │  └─ Admin.vue                # 管理中心（耗材/分类/审批管理）
│  ├─ App.vue                     # 根组件
│  └─ main.js                     # 入口文件
├─ public/                        # 公共静态文件
├─ index.html                     # HTML 入口
├─ vite.config.js                 # Vite 配置（代理、端口）
├─ package.json                   # 项目依赖与脚本配置
├─ package-lock.json              # 依赖版本锁定文件
└─ README.md
```

## 常见问题

### 登录时出现 500 错误

这是因为后端服务没有启动。请按照上面的说明启动后端服务。

### 端口被占用

如果 3000 端口被占用，可以修改 `vite.config.js` 中的端口配置。

### API 请求失败

确保：
1. 后端服务已启动在 http://localhost:8080
2. MySQL 和 Redis 服务正常运行
3. 数据库已初始化
