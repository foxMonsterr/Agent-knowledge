# LearnAgent 简历化项目改造 Roadmap

## 1. 项目定位

项目最终定位为：

> LearnAgent：基于 Spring AI 的个人知识库 ReAct 学习助手。

项目不再以“泛用多 Agent Demo”为核心叙事，而是围绕个人学习场景构建完整闭环：

```text
资料导入 -> 文档解析与切片 -> 向量检索 -> ReAct 学习对话 -> 来源引用
-> 笔记沉淀 -> 测验/闪卡/费曼检验 -> 掌握度更新 -> 学习统计与知识图谱
```

## 2. 简历核心卖点

### 2.1 可重点书写的能力

| 能力 | 简历表述方向 |
|---|---|
| Spring AI 接入 | 使用 `ChatClient` 接入主模型与兜底模型，封装多 Agent 对话入口 |
| RAG 检索 | 支持 Markdown、TXT、PDF、DOCX 资料导入、切片、向量化和来源引用 |
| ReAct Agent | 抽象 ReAct 执行引擎，统一管理工具调用、状态流转、SSE 输出和 Trace 持久化 |
| 个人知识库 | 以用户维度隔离笔记、文档 chunk、测验、闪卡和学习记录 |
| 学习闭环 | 支持测验、闪卡、费曼检验、掌握度更新、薄弱点分析和知识图谱 |
| 工程治理 | 集成 JWT/RBAC、审计日志、TraceId、监控统计和限流保护 |
| 前端体验 | Vue 3 展示 ReAct 推理链路、引用来源、学习工作台和知识图谱 |

### 2.2 需要避免夸大的说法

| 避免表述 | 替代表述 |
|---|---|
| 全能 Agent 编排平台 | 面向个人学习场景的 ReAct Agent 应用 |
| 精确 token 计费系统 | 运行时 token 统计与成本估算 |
| 企业级知识库系统 | 个人知识库与学习资料管理 |
| 复杂工作流引擎 | 轻量任务规划与工具调用能力 |
| 自动化智能出题系统 | 基于笔记内容的题目生成与答题评估模块 |

## 3. 当前项目诊断

### 3.1 已具备基础

1. `ConversationAgentRouter` 已经形成统一对话入口。
2. `LearnAgent` 已接入通用 `ReActEngine`。
3. `ReActEngine` 已支持 Trace 持久化、SSE 事件输出、中断和干预接口。
4. `DocumentService` 已支持 `.md`、`.txt`、`.pdf`、`.docx` 导入。
5. `RetrievalService` 已支持 `userId` 过滤和 `enabled` 过滤。
6. `FlashcardService` 已实现 SM-2 复习调度。
7. 前端已具备学习对话、笔记、测验、闪卡、图谱、统计等页面。

### 3.2 当前短板

1. 测试覆盖不足，核心服务缺少可重复验证。
2. `LearnReActToolRegistry` 目前工具选择较轻，只固定检索知识库。
3. 测验、闪卡、费曼检验部分逻辑偏规则生成，不能夸大为强智能评估。
4. `PlanningAgent` 工具选择依赖字符串匹配，工程厚度不足。
5. `TokenUsageTracker` 需要区分真实 usage 与估算数据。
6. `SecurityConfig` 中部分通用 Agent 接口仍处于开发开放状态。
7. Qdrant 与 Spring AI `VectorStore` 的实际主链路关系需要在文档中讲清楚。

## 4. 学习路线

### 4.1 P0：必须掌握

| 主题 | 学习目标 | 对应项目模块 |
|---|---|---|
| Spring AI ChatClient | 掌握同步、流式、工具调用、Advisor、Memory | `agent`、`conversation`、`react` |
| RAG 工程 | 掌握文档解析、切片、embedding、向量检索、metadata filter | `rag`、`service/qdrant` |
| ReAct 模式 | 掌握 Thought/Action/Observation、工具白名单、Trace | `react/core`、`learn/react` |
| SSE | 掌握事件协议、断线、错误、done 闭环 | `conversation/stream`、前端 composable |
| JWT/RBAC | 掌握认证、授权、用户数据隔离 | `auth`、`config/SecurityConfig` |

### 4.2 P1：增强工程深度

| 主题 | 学习目标 | 对应项目模块 |
|---|---|---|
| 单元测试与集成测试 | 覆盖核心业务规则和接口边界 | `src/test` |
| 多存储分层 | 讲清 MySQL、MongoDB、Redis、Qdrant 的职责 | `repository`、`learn/repository` |
| 可观测性 | 统一 traceId、audit、metrics、token 口径 | `common/context`、`service/AuditService`、`monitor` |
| 前端状态管理 | 管理 SSE 生命周期、Trace 回放、错误状态 | `front/src/composables` |

## 5. 改造阶段

### 阶段一：建立可信度基线

目标：让项目至少有一组能跑通的核心测试，证明不是只堆页面和接口。

改造项：

1. 补 `FlashcardService` 单元测试，验证 SM-2 复习算法。
2. 补 `RetrievalService` 单元测试，验证用户过滤和禁用 chunk 过滤。
3. 后续补 `NoteService`、`ReActEngine`、`LearnChatController` 测试。

验收标准：

```bash
mvn test
```

至少能覆盖不依赖外部模型、数据库和 Qdrant 服务的确定性逻辑。

### 阶段二：打深 LearnAgent 主链路

目标：让 LearnAgent 成为简历主线，而不是附加模块。

改造项：

1. `LearnReActToolRegistry` 从单工具检索升级为策略化工具选择。
2. ReAct 事件补齐 `heartbeat`、`partial_error`、工具耗时和工具状态。
3. Trace 回放接口返回更清晰的 step、source、toolCall 信息。
4. 学习对话最终回答严格引用真实来源，来源不足时明确提示。

验收标准：

1. 导入资料后可以在 `/chat` 提问。
2. 前端能看到 `thought/action/observation/source/final_answer/done`。
3. Trace 可查询、可回放。
4. 第二个用户不能检索第一个用户的资料。

### 阶段三：强化 RAG 与知识库

目标：让 RAG 能讲清楚、能演示、能防幻觉。

改造项：

1. 统一 `NoteService.search` 与 `RetrievalService.retrieveForUser` 的结果结构。
2. 检索结果增加 `score`、`sourceType`、`docId`、`chunkId`、`enabled`。
3. 增加文档 hash 去重。
4. 增加归档笔记的向量禁用校验。
5. 明确 Qdrant 是实际 VectorStore Adapter，文档中不要混淆 SimpleVectorStore 和 Qdrant。

验收标准：

1. 同一用户导入资料后可检索。
2. 不同用户数据隔离。
3. 归档笔记不会出现在默认检索结果中。
4. 回答来源可追溯到 note 或 chunk。

### 阶段四：补学习闭环智能化

目标：让测验、闪卡、费曼检验不只是规则拼接。

改造项：

1. `QuizService.generate` 可选接入 LLM，基于笔记内容生成题目。
2. `QuizService.evaluate` 对简答题返回覆盖度、准确度、遗漏点。
3. `FlashcardService.generate` 可基于笔记摘要和标签生成不同类型闪卡。
4. `StudyService` 增强薄弱点证据，包括答错题、复习失败、过期未复习。

验收标准：

1. 生成题目与来源笔记强相关。
2. 答题后掌握度变化可解释。
3. 闪卡复习后 `nextReviewAt` 符合 SM-2。
4. 工作台能展示薄弱主题和建议动作。

### 阶段五：收口平台治理

目标：让项目像一个可维护的后端平台。

改造项：

1. 收紧开发阶段开放接口。
2. 统一 `traceId/conversationId/userId`。
3. Token 统计增加 `usageSource`，区分真实 usage 和估算。
4. Audit 记录增加失败原因、Agent 类型、模式、模型、耗时。
5. 监控页面展示真实指标来源。

验收标准：

1. 未登录用户不能访问个人学习数据。
2. USER 不能访问 ADMIN 接口。
3. 失败调用能在审计日志中定位原因。
4. token 和成本统计口径清晰。

## 6. 推荐简历描述

### 6.1 简历项目名称

LearnAgent：基于 Spring AI 的个人知识库 ReAct 学习助手

### 6.2 简历项目描述

基于 Spring Boot 3、Spring AI、Vue 3 构建个人知识库学习 Agent，支持学习资料导入、向量检索、ReAct 推理链路可视化、来源引用、笔记沉淀、测验/闪卡生成、掌握度追踪和知识图谱导航。系统通过 JWT/RBAC 实现用户级数据隔离，结合 MongoDB、MySQL、Redis、Qdrant 承载文档型数据、结构化学习记录、会话记忆和向量检索能力。

### 6.3 简历职责表述

1. 设计统一 Conversation 路由层，按 Chat、RAG、Planning、ReAct、LearnAgent 等类型分发请求，并统一响应结构与流式输出。
2. 抽象 ReActEngine，封装工具选择、状态流转、SSE 事件输出、Trace 持久化和中断恢复能力。
3. 实现个人知识库 RAG 链路，支持 PDF/DOCX/Markdown/TXT 解析、TokenTextSplitter 切片、VectorStore 入库和用户级 metadata 过滤。
4. 构建学习闭环模块，实现笔记、测验、闪卡、费曼检验、掌握度更新、薄弱点分析和知识图谱展示。
5. 接入 JWT/RBAC、审计日志、traceId、限流和监控统计，提升系统安全性与可观测性。

## 7. 面试答辩重点

### 7.1 如何讲主链路

```text
用户导入资料
-> DocumentService 解析文件并切片
-> VectorStore/Qdrant 写入向量和 metadata
-> 用户在 LearnAgent 提问
-> ReActEngine 选择知识检索工具
-> RetrievalService 按 userId 检索
-> LearnReActProfile 基于来源生成回答
-> SSE 推送 Thought/Action/Observation/FinalAnswer
-> Trace 保存到 MongoDB
-> StudyService 记录学习行为并更新掌握度
```

### 7.2 如何讲数据分层

| 存储 | 职责 |
|---|---|
| MySQL | 用户、角色、会话元数据、学习行为记录 |
| MongoDB | 笔记、测验、闪卡、ReAct Trace、文档型学习数据 |
| Redis | 会话记忆和缓存能力 |
| Qdrant | 文档 chunk 与笔记 chunk 的向量检索 |

### 7.3 如何讲项目边界

这个项目不是企业协作知识库，而是个人学习场景下的 Agent 应用。重点不是多人协作权限、审批流或企业搜索，而是个人知识积累、可追溯回答和学习闭环。

## 8. 当前执行顺序

第一批执行：

1. 新增 Roadmap 文档。
2. 新增 `FlashcardService` SM-2 单元测试。
3. 新增 `RetrievalService` 用户隔离与 enabled 过滤单元测试。
4. 跑 `mvn test` 验证。

第二批执行：

1. 增强 `LearnReActToolRegistry` 工具选择。
2. 补 ReAct SSE heartbeat 与 partial error。
3. 增强 Trace 回放结构。
4. 补 LearnAgent 主链路集成测试。

