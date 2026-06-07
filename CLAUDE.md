# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 常用命令

```bash
# 后端启动 (port 18080)
mvn spring-boot:run

# 前端启动 (port 5173)
cd front && npm install && npm run dev

# 仅编译检查（不改动代码时，比完整 build 快）
cd front && npx vue-tsc --noEmit

# 代码格式化
mvn spring-javaformat:apply

# 运行所有测试
mvn test -q

# 运行单个测试
mvn test -Dtest="ClassName" -q

# 构建 JAR
mvn clean package -DskipTests
```

## 架构总览

```
用户请求
  → ConversationController (统一入口, /api/v1/conversations)
    → ConversationAgentRouter (按 mode/agentType 路由)
      → ChatAgent / RagAgent / ToolAgent / PlanningAgent / FullAgent / StreamAgent / GeneralReActAgent
        → react/core/ReActEngine (Thought → Action → Observation 循环)
          → ReActProfile (领域特化的提示词 + 工具选择 + 策略)
          → ReActTool (工具适配器, 注入 @Tool Bean 委派调用)
```

### 四层数据存储

| 存储 | 用途 |
|------|------|
| **MySQL (JPA)** | 用户认证、学习记录聚合 (`StudyRecordEntity`)、审计日志 |
| **MongoDB** | 知识笔记/测验/闪卡/学习路径文档、ReAct 推理追踪、聊天消息/会话 |
| **Redis** | 会话记忆存储 (ChatMemory)、分布式限流 |
| **向量库** (SimpleVectorStore 或 Qdrant) | RAG 文档检索, 通过 `VECTOR_STORE_TYPE` 环境变量切换 |

### ReAct 框架 (`react/core/`)

核心抽象：
- `ReActEngine` — 驱动 Thought→Action→Observation 循环，使用 3 个 ChatClient (base/memory/fallback)，每次推理完整追踪写入 MongoDB
- `ReActProfile` (接口) — 每个业务域实现不同 Profile，控制工具选择、提示词模板、推理策略
- `ReActTool` (接口) — 工具适配器，包裹 @Tool 注解类，在 ReAct 循环中调用

两个 Profile 实现：
- `agent/react/GeneralReActProfile` — 通用工具 Agent (CalculatorTool, TranslateTool 等)
- `learn/react/LearnReActProfile` — 学习场景 (检索笔记、做题测验、学习进度分析、网页搜索)

策略驱动分流：`strategy` 参数 (`quiz` / `review` / `explore` / `auto`) 决定选择哪些 ReActTool 和用哪套提示词。`StrategyInferer` 可从用户消息关键词自动推断策略。

### ChatClient 分离 (`config/ChatClientConfig.java`)

5 个独立 ChatClient Bean，各自绑定不同的 system prompt (来自 `src/main/resources/prompts/`) 和 Advisor 组合。不要合并它们——每个 Agent 依赖特定的 ChatClient 实例，合并会破坏隔离性。

### 学习模块 (`learn/`) — 独立子应用

有自己的 Controller、Service、MongoDB 文档模型、JPA 聚合查询、ReAct 循环。SM-2 间隔重复算法 (`ReviewSchedulerService`)、知识图谱 (`GraphService`)、学习路径 (`LearningPathService`)。`ApplicationEventPublisher` 用于测验答对后自动推进学习路径阶段。

### 前端类型与后端的一致性

前端 `types/react.ts` 中的 `ReActStrategy` 必须和后端 `LearnReActProfile` 支持的策略值保持同步。当前值：`'auto' | 'quiz' | 'review' | 'explore'`。

## 安全规定

- Spring Security + JWT 认证（`auth/` 包，`SecurityConfig.java`）
- `/api/v1/auth/**` 和 `/api/v1/public/**` 为公开端点
- 所有生成式 AI 调用前必须通过 JWT 校验
- 速率限制：开发环境 30 req/min，生产 60 req/min

## API 文档

Knife4j (Swagger UI): `http://localhost:18080/doc.html`

## CLAUDE.md 自维护规则

当你在本项目中执行任何代码修改、架构调整或依赖变更后，**必须**检查本文件是否需要同步更新。

### 更新原则
1.  **增量更新**：仅修改与本次变更直接相关的段落，严禁重新扫描全项目或重写整个文件。
2.  **触发条件**：当以下情况发生时，需主动更新本文件：
    -   新增/删除/重命名了核心目录或模块
    -   引入了新的依赖、框架或构建工具
    -   修改了测试命令、启动脚本或 CI/CD 流程
    -   调整了代码规范、API 契约或数据模型约定
3.  **更新方式**：直接在对话中告知用户“检测到 XX 变更，已同步更新 CLAUDE.md 中的 [具体章节]”，并使用编辑工具进行精准替换。
4.  **禁止行为**：不要在没有代码变更的情况下主动重写本文件；不要用模糊描述替代具体命令或路径。