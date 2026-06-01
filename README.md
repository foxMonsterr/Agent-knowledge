# Smart Agent

企业级 AI 智能体平台 Demo，基于 Spring Boot + Spring AI + Vue 3 构建。

## 核心能力
- JWT + RBAC 权限控制
- 多轮对话、专家对话、思考模式
- Agent 工具调用与编排
- RAG 知识库检索问答
- 流式输出（SSE）
- 会话管理、监控、审计
- 部署验收、发布说明、演示中心
- 前后端分离控制台

## 技术栈
- 后端：Spring Boot 3.3.x、Spring Security、Spring Data JPA、Spring AI
- 前端：Vue 3、Vite、Element Plus、Pinia、TypeScript
- 数据：MySQL / H2、Redis、SimpleVectorStore
- 部署：Docker、Docker Compose

## 快速启动
### 后端
1. 配置 `src/main/resources/application-dev.yml`
2. 准备 Redis / 数据库 / AI Key
3. 执行：
```bash
mvn spring-boot:run
```

### 前端
1. 进入 `front`
2. 安装依赖
3. 启动：
```bash
npm install
npm run dev
```

### Docker
```bash
docker compose up -d mysql redis
mvn -DskipTests package
# 然后构建后端镜像并启动
```

## 主要接口
- `/api/v1/chat/**`
- `/api/v1/agent/**`
- `/api/v1/knowledge/**`
- `/api/v1/planning/**`
- `/api/v1/session/**`
- `/api/v1/monitor/**`
- `/api/v1/admin/**`
- `/api/v1/audit/**`
- `/api/v1/demo/**`
- `/api/v1/release/**`
- `/api/v1/deploy/**`
- `/api/v1/performance/**`
- `/api/v1/home/**`

## 健康检查
- `GET /api/v1/health`
- `GET /api/v1/deploy/info`
- `GET /api/v1/deploy/check`

## 环境变量
- `DEEPSEEK_API_KEY`
- `QWEN_API_KEY`
- `JWT_SECRET`
- `MYSQL_HOST`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PASSWORD`

## 目录结构
- `src/main/java` 后端代码
- `src/main/resources` 配置、提示词、知识库
- `front` 前端控制台
- `scripts` 启动脚本
- `docker-compose.yml` 本地编排

## 部署建议
1. 先启动 Redis、数据库和后端服务
2. 再启动前端开发服务器或构建静态资源
3. 生产环境建议使用 Docker Compose 统一编排
4. 上线前先访问健康检查与部署验收页

## 演示顺序建议
1. 首页与运营指标
2. 演示中心
3. 聊天 / Agent / RAG / 规划
4. 会话 / 审计 / 监控
5. 用户管理 / 部署验收 / 发布说明

## 简历描述建议
本项目是一个企业级 AI 智能体平台，支持多 Agent 协同、RAG 知识库、工具调用、任务规划、JWT/RBAC 权限控制、审计追踪、监控看板与 Docker 部署，具备完整的演示与发布能力。

## 说明
该项目目标是做成一个适合写入简历的企业级 AI Demo，重点展示：
- 多 Agent 协同
- 知识增强
- 权限与审计
- 可观测性
- 可部署性
