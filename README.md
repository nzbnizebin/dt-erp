# Attendance Management System

该项目提供一个基于 Spring Boot (后端) + Vue 3 (前端) + SQLite (数据库) 的考勤与请假管理系统。无需 Nginx，直接通过提供的脚本即可在本机完成依赖安装与启动。

## 功能概览

- ✅ 用户账号体系，支持管理员与普通用户登陆（JWT 鉴权）。
- ✅ 员工档案管理：录入中文名、英文名、入职时间。
- ✅ 年假自动核算：按照“基本配额 10 天（入职 6 个月后从第 7 个月开始每月累计 10/12 天直至满额）+ 每月 1/12 天”的规则实时计算，并在每年 4 月重置上一年度余额。
- ✅ 请假管理：支持年假、事假、婚假、产假、病假、其他假；病假每月限额 1 天；年假自动扣减额度。
- ✅ 记录查询：支持按员工英文名、类型、时间范围筛选以及分页浏览。
- ✅ 一键启动脚本 `start.sh`，串联后端与前端开发环境。

## 目录结构

```
.
├── backend/    # Spring Boot 应用源码
├── frontend/   # Vue 3 + Vite 前端源码
├── data/       # SQLite 数据库存储目录
└── start.sh    # 一键启动脚本
```

## 快速开始

### 环境准备

- JDK 17+
- Apache Maven 3.9+
- Node.js 18+（含 npm）

### 一键启动（开发模式）

```bash
./start.sh
```

脚本会执行以下步骤：

1. 检查并安装后端依赖（Maven）。
2. 启动 Spring Boot 服务（默认端口 `8080`）。
3. 安装前端依赖并启动 Vite 开发服务器（默认端口 `5173`）。

启动完成后，可在浏览器访问 [http://localhost:5173](http://localhost:5173)。默认管理员账号：`admin` / `admin123`。

### 手动启动

若希望分别启动前后端：

```bash
# 后端
cd backend
mvn spring-boot:run

# 前端
cd frontend
npm install
npm run dev -- --host
```

## 接口摘要

后端主要 API（均以 `/api` 为前缀）：

- `POST /api/auth/login`：登陆，返回 JWT Token 与角色。
- `GET /api/employees`：查询所有员工。
- `POST /api/employees`：新增员工（管理员）。
- `PUT /api/employees/{id}` / `DELETE /api/employees/{id}`：编辑 / 删除员工（管理员）。
- `GET /api/employees/{id}/annual-leave`：查看指定员工本年度年假统计。
- `POST /api/leave-requests`：提交请假申请。
- `GET /api/leave-requests`：按条件分页查询请假记录。
- `DELETE /api/leave-requests/{id}`：删除请假记录（管理员）。

## 数据库

系统默认使用 `data/attendance.db` 文件作为 SQLite 数据库。首次运行会自动创建表结构。

## 开发提示

- 默认创建一个管理员账号（用户名 `admin`，密码 `admin123`）。生产环境请修改 `backend/src/main/resources/application.properties` 中的 `app.security.jwt.secret` 并更新初始密码。
- 如果需要更改端口，可编辑 `application.properties`（后端）及 `frontend/vite.config.js`（前端代理配置）。

## 许可

该项目仅用于演示用途，可根据需要自由扩展与定制。
