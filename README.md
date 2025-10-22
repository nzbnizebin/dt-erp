# Attendance Management System

该项目提供一个考勤与请假管理系统：后端为纯 Java 17 编写的轻量级 HTTP 服务，直接使用系统自带的 `sqlite3` CLI 操作数据库；前端基于 Vue 3 + Vite。无需 Nginx，使用仓库提供的脚本即可一键构建与启动。

## 功能概览

- ✅ 用户账号体系，区分管理员与普通用户，使用 HMAC 签名的自定义 Token 鉴权。
- ✅ 员工档案管理：录入中文名、英文名、入职时间，支持管理员新增与查询。
- ✅ 年假自动核算：按照“基本配额 10 天（入职满 6 个月后，从第 7 个月开始每月累计 10/12 天直至满额）+ 每月 1/12 天”的规则计算，上一年度余额会在每年 4 月自动清零。
- ✅ 请假管理：支持年假、事假、婚假、产假、病假、其他假；病假每人每月限额 1 天；年假自动扣减额度；请假记录支持条件筛选与分页。
- ✅ 一键启动脚本 `start.sh`，串联后端编译与前端开发环境。

## 目录结构

```
.
├── backend/    # Java 后端源码与构建脚本
├── frontend/   # Vue 3 + Vite 前端源码
├── data/       # SQLite 数据库与后端密钥目录
└── start.sh    # 一键启动脚本
```

## 快速开始

### 环境准备

- JDK 17+
- `sqlite3` 命令行工具
- Node.js 18+（含 npm，可选，仅在需要运行前端时使用）

### 一键启动（开发模式）

```bash
./start.sh
```

脚本会执行以下步骤：

1. 编译后端 Java 代码并打包为 `backend/target/attendance-backend.jar`。
2. 启动后端 HTTP 服务（默认端口 `8080`）。
3. 如果检测到 npm，则安装前端依赖并启动 Vite 开发服务器（默认端口 `5173`）。

启动完成后，可在浏览器访问 [http://localhost:5173](http://localhost:5173)。默认管理员账号：`admin` / `admin123`。

### 手动启动

若希望分别启动前后端：

```bash
# 构建并运行后端
cd backend
./build.sh
java -jar target/attendance-backend.jar

# 运行前端（可选）
cd frontend
npm install
npm run dev -- --host
```

## 接口摘要

后端主要 API（均以 `/api` 为前缀）：

- `POST /api/auth/login`：登陆，返回访问 Token 与角色。
- `GET /api/employees`：查询所有员工。
- `POST /api/employees`：新增员工（管理员权限）。
- `GET /api/employees/{id}/annual-leave`：查看指定员工本年度年假统计。
- `POST /api/leave-requests`：提交请假申请。
- `GET /api/leave-requests`：按条件分页查询请假记录。

## 数据库

系统默认使用 `data/attendance.db` 作为 SQLite 数据库文件。后端通过 `sqlite3` CLI 执行 SQL，首次运行会自动创建数据表并生成默认管理员账号。

后端签名密钥会存储在 `data/backend-secret.key`，若需要更换，可删除该文件后重新启动服务以生成新的密钥。

## 开发提示

- 所有 HTTP 响应均为 JSON，包含跨域所需的 CORS 头。
- 如果要在生产环境部署，可使用 `backend/build.sh` 打包后端，再配合反向代理或系统服务守护后台进程。
- 默认管理员账号：`admin` / `admin123`。上线前请务必修改数据库中的密码记录。

## 许可

该项目仅用于演示用途，可根据需要自由扩展与定制。
