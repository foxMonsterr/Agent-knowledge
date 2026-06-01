# SmartAgent 项目全量说明文档

> 本文档供 AI 辅助编程工具在新对话中快速了解项目全貌，覆盖前后端完整结构、核心流程、文件路径与改造方向。

---

## 一、项目概述

| 项目 | 说明 |
|------|------|
| **项目名称** | SmartAgent — 基于 Spring AI 的多功能自主决策 Agent 助手 |
| **项目定位** | 企业级 AI 智能体平台 Demo（计划改造为个人学习知识库 Agent） |
| **技术栈** | Spring Boot 3.3.5 + Spring AI 1.1.5 + JDK 17 + Vue 3 + Vite 6 |
| **AI 模型** | Chat: DeepSeek Chat（主）/ Qwen3.6-Plus（兜底）；Embedding: 阿里云 DashScope text-embedding-v4 |
| **接口文档** | Knife4j 4.5.0 → http://localhost:8080/doc.html |
| **后端端口** | 8080 |
| **前端端口** | 5173（Vite dev server，代理 /api → localhost:8080） |
| **包路径** | `com.chat.myAgent` |
| **项目根目录** | `d:\TestLocal\Agent-demo\MyAgent` |

---

## 二、后端目录结构（完整文件路径与职责）

### 2.1 启动类

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/com/chat/myAgent/main/MainApplication.java` | Spring Boot 入口，`@SpringBootApplication(scanBasePackages="com.chat.myAgent")`，启用 JPA + MongoDB Repository |
| `src/main/java/com/chat/myAgent/main/MainServletInitializer.java` | WAR 部署支持 |

### 2.2 配置层 `config/`

| 文件路径 | 职责 | 关键 Bean |
|---------|------|----------|
| `src/main/java/com/chat/myAgent/config/ChatClientConfig.java` | ChatClient Bean 配置（核心） | `primaryOpenAiApi` / `fallbackOpenAiApi` / `primaryChatModel` / `fallbackChatModel` / `baseChatClient` / `memoryChatClient` / `toolChatClient` / `ragChatClient` / `fullAgentClient` / `fallbackChatClient` |
| `src/main/java/com/chat/myAgent/config/ModelConfig.java` | 多模型动态切换配置 | 读取 `smart-agent.models.*`，提供 `getPrimaryModel()` / `getFallbackModelName()` / `resolveModel()` |
| `src/main/java/com/chat/myAgent/config/MemoryConfig.java` | 聊天记忆配置 | `ChatMemory` Bean → `MessageWindowChatMemory`（InMemoryChatMemoryRepository，maxMessages=50） |
| `src/main/java/com/chat/myAgent/config/EmbeddingConfig.java` | Embedding 模型配置 | `OpenAiEmbeddingModel` Bean → DashScope text-embedding-v4，`embeddingsPath="/embeddings"` |
| `src/main/java/com/chat/myAgent/config/SecurityConfig.java` | Spring Security + JWT 配置 | 无状态会话、路径权限矩阵、JWT 过滤器注册 |
| `src/main/java/com/chat/myAgent/config/Knife4jConfig.java` | Knife4j 接口文档配置 | 含 JWT 认证按钮 |
| `src/main/java/com/chat/myAgent/config/WebConfig.java` | Web MVC 配置 | CORS + 限流拦截器注册 |
| `src/main/java/com/chat/myAgent/config/MongoConfig.java` | MongoDB 配置 | `@EnableMongoAuditing` |
| `src/main/java/com/chat/myAgent/config/QdrantConfig.java` | Qdrant 向量库配置 | 读取 host/port/collection-name |
| `src/main/java/com/chat/myAgent/config/RedisConfig.java` | Redis 配置 | `@ConditionalOnClass` 按需加载 |
| `src/main/java/com/chat/myAgent/config/RedisChatMemoryRepository.java` | Redis 记忆持久化（备用） | Key: `chat:memory:{conversationId}`，TTL 24h |
| `src/main/java/com/chat/myAgent/config/ResponseHeaderAdvice.java` | 响应头增强 | 统一添加 traceId 等头信息 |
| `src/main/java/com/chat/myAgent/config/TraceConfig.java` | 链路追踪配置 | TraceContext 初始化 |

### 2.3 Agent 层 `agent/`（业务核心）

| 文件路径 | 职责 | 使用的 ChatClient | 能力 |
|---------|------|------------------|------|
| `src/main/java/com/chat/myAgent/agent/ChatAgent.java` | 基础对话 Agent | baseChatClient + memoryChatClient + fallbackChatClient | simpleChat（无记忆）/ chat（带记忆）/ expertChat（专家角色+PromptTemplate），含主模型→兜底模型容灾 |
| `src/main/java/com/chat/myAgent/agent/StructuredAgent.java` | 结构化输出 Agent | baseChatClient | extractBookInfo / analyzeTask / analyzeSentiment（`.entity()` 反序列化） |
| `src/main/java/com/chat/myAgent/agent/ToolAgent.java` | 工具调用 Agent | baseChatClient | chat（无记忆+5工具）/ chatWithMemory（带记忆+5工具）/ chatWithSpecificTools（指定工具） |
| `src/main/java/com/chat/myAgent/agent/RagAgent.java` | RAG 知识库问答 Agent | ragChatClient + baseChatClient | ask（自动模式）/ askManual（手动模式）/ askStream（流式）/ searchOnly |
| `src/main/java/com/chat/myAgent/agent/PlanningAgent.java` | 任务规划 Agent | baseChatClient + fullAgentClient | planAndExecute（规划+执行）/ planStream（流式规划），含 fallbackDirectExecution 回退 |
| `src/main/java/com/chat/myAgent/agent/FullAgent.java` | 全能力 Agent | fullAgentClient | chat（记忆+5工具）/ chatStream |
| `src/main/java/com/chat/myAgent/agent/StreamAgent.java` | 流式对话 Agent | fullAgentClient + baseChatClient | streamChat（无工具流式）/ streamChatWithTools（带工具流式） |

### 2.4 工具层 `tool/`（AI 可调用的外部能力）

| 文件路径 | 工具名 | 方法数 | 方法列表 |
|---------|--------|-------|---------|
| `src/main/java/com/chat/myAgent/tool/DateTimeTool.java` | 时间日期工具 | 5 | `getCurrentDateTime` / `getCurrentDayOfWeek` / `daysBetween` / `addDays` / `getDayOfWeek` |
| `src/main/java/com/chat/myAgent/tool/CalculatorTool.java` | 计算器工具 | 3 | `calculate`（表达式）/ `calculatePercentage`（百分比）/ `convertUnit`（单位换算） |
| `src/main/java/com/chat/myAgent/tool/TranslateTool.java` | 翻译工具 | 2 | `translate`（AI调用AI翻译）/ `detectLanguage`（语言检测） |
| `src/main/java/com/chat/myAgent/tool/DocParseTool.java` | 文档解析工具 | 3 | `readFile` / `listFiles` / `getFileInfo`，读取 `src/main/resources/docs/` 下文件 |
| `src/main/java/com/chat/myAgent/tool/DbQueryTool.java` | 数据库查询工具 | 3 | `queryEmployees` / `queryDepartments` / `statistics`，内存模拟数据 |

### 2.5 RAG 层 `rag/`

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/com/chat/myAgent/rag/DocumentService.java` | 文档管理：uploadAndIndex / indexLocalFile / indexKnowledgeDirectory / deleteDocument / listDocuments。流程：TextReader → TokenTextSplitter(chunkSize/overlap) → VectorStore.add() → SimpleVectorStore.save() |
| `src/main/java/com/chat/myAgent/rag/RetrievalService.java` | 向量检索：retrieve(query, topK, threshold) / retrieveFormatted / getSourceFiles。使用 VectorStore.similaritySearch() |

### 2.6 Controller 层 `controller/`

| 文件路径 | 路径前缀 | 职责 |
|---------|---------|------|
| `src/main/java/com/chat/myAgent/controller/AuthController.java` | `/api/v1/auth` | 注册/登录/初始化管理员 |
| `src/main/java/com/chat/myAgent/controller/ChatController.java` | `/api/v1/chat` | 简单对话/记忆对话/专家对话/结构化输出(book/task/sentiment)/ping/quick |
| `src/main/java/com/chat/myAgent/controller/AgentController.java` | `/api/v1/agent` | 工具调用对话(无记忆/带记忆/指定工具) |
| `src/main/java/com/chat/myAgent/controller/KnowledgeController.java` | `/api/v1/knowledge` | 文档上传/批量加载/列表/删除/RAG问答(自动/手动)/纯检索/状态 |
| `src/main/java/com/chat/myAgent/controller/PlanningController.java` | `/api/v1/planning` | 规划执行/仅规划/全能Agent |
| `src/main/java/com/chat/myAgent/controller/StreamController.java` | `/api/v1/stream` | 流式对话(基础/带工具)，返回 `Flux<String>` + `text/event-stream` |
| `src/main/java/com/chat/myAgent/controller/SessionController.java` | `/api/v1/session` | 会话历史/清除/列表/创建 |
| `src/main/java/com/chat/myAgent/controller/MonitorController.java` | `/api/v1/monitor` | Token统计/对话历史/会话详情 |
| `src/main/java/com/chat/myAgent/controller/AuditController.java` | `/api/v1/audit` | 审计日志查询 |
| `src/main/java/com/chat/myAgent/controller/AdminController.java` | `/api/v1/admin` | 用户管理 |
| `src/main/java/com/chat/myAgent/controller/HomeController.java` | `/api/v1/home` | 首页运营指标 |
| `src/main/java/com/chat/myAgent/controller/DemoController.java` | `/api/v1/demo` | 演示中心 |
| `src/main/java/com/chat/myAgent/controller/DeployController.java` | `/api/v1/deploy` | 部署验收/健康检查 |
| `src/main/java/com/chat/myAgent/controller/ReleaseController.java` | `/api/v1/release` | 发布说明 |
| `src/main/java/com/chat/myAgent/controller/HealthController.java` | `/api/v1/health` | 健康检查 |
| `src/main/java/com/chat/myAgent/controller/PerformanceController.java` | `/api/v1/performance` | 性能指标 |
| `src/main/java/com/chat/myAgent/controller/PermissionController.java` | `/api/v1/permission` | 当前用户权限查询 |
| `src/main/java/com/chat/myAgent/controller/OpsController.java` | `/api/v1/ops` | 运维面板 |
| `src/main/java/com/chat/myAgent/controller/SystemInfoController.java` | `/api/v1/system` | 系统信息 |
| `src/main/java/com/chat/myAgent/controller/ChatMongoController.java` | `/api/v1/chat/mongo` | MongoDB 会话管理 |
| `src/main/java/com/chat/myAgent/controller/PageController.java` | 页面路由 | 转发前端页面 |

### 2.7 认证层 `auth/`

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/com/chat/myAgent/auth/JwtTokenProvider.java` | JWT Token 生成/解析/验证，密钥和过期时间从 `smart-agent.jwt.*` 读取 |
| `src/main/java/com/chat/myAgent/auth/JwtAuthenticationFilter.java` | JWT 过滤器（OncePerRequestFilter），提取 `Authorization: Bearer <token>`，验证后设置 SecurityContext |
| `src/main/java/com/chat/myAgent/auth/JwtProperties.java` | JWT 配置属性（secret / expirationHours） |
| `src/main/java/com/chat/myAgent/auth/UserDetailsServiceImpl.java` | Spring Security 用户加载服务，从 UserRepository 查询 |

### 2.8 数据模型层 `model/`

#### DTO（请求体）

| 文件路径 | 字段 |
|---------|------|
| `src/main/java/com/chat/myAgent/model/dto/ChatRequest.java` | conversationId, message(@NotBlank), model, role, level, thinkingMode |
| `src/main/java/com/chat/myAgent/model/dto/AgentRequest.java` | conversationId, message, tools(工具列表) |
| `src/main/java/com/chat/myAgent/model/dto/KnowledgeRequest.java` | question, conversationId |
| `src/main/java/com/chat/myAgent/model/dto/PlanningRequest.java` | task, conversationId, autoExecute |
| `src/main/java/com/chat/myAgent/model/dto/StructuredRequest.java` | input |
| `src/main/java/com/chat/myAgent/model/dto/LoginRequest.java` | username, password |
| `src/main/java/com/chat/myAgent/model/dto/RegisterRequest.java` | username, password, role |
| `src/main/java/com/chat/myAgent/model/dto/SendChatMessageRequest.java` | 发送聊天消息请求 |
| `src/main/java/com/chat/myAgent/model/dto/CreateSessionRequest.java` | 创建会话请求 |
| `src/main/java/com/chat/myAgent/model/dto/ListSessionsRequest.java` | 列出会话请求 |
| `src/main/java/com/chat/myAgent/model/dto/PageMessagesRequest.java` | 分页消息请求 |
| `src/main/java/com/chat/myAgent/model/dto/ArchiveSessionRequest.java` | 归档会话请求 |
| `src/main/java/com/chat/myAgent/model/dto/UpdateSessionTitleRequest.java` | 更新会话标题请求 |
| `src/main/java/com/chat/myAgent/model/dto/UpdateSessionSummaryRequest.java` | 更新会话摘要请求 |
| `src/main/java/com/chat/myAgent/model/dto/DeleteSessionRequest.java` | 删除会话请求 |
| `src/main/java/com/chat/myAgent/model/dto/ConversationRecord.java` | 对话记录 |

#### VO（响应体）

| 文件路径 | 字段 |
|---------|------|
| `src/main/java/com/chat/myAgent/model/vo/ChatResponse.java` | conversationId, reply, thinking, traceId, model, historySize, tokenUsage |
| `src/main/java/com/chat/myAgent/model/vo/AgentResponse.java` | conversationId, reply, traceId, model, agentType |
| `src/main/java/com/chat/myAgent/model/vo/KnowledgeResponse.java` | conversationId, answer, sources, retrievedChunks, model, traceId |
| `src/main/java/com/chat/myAgent/model/vo/PlanningResponse.java` | conversationId, taskSummary, planned, steps(List<StepResult>), directAnswer, finalAnswer, totalTimeMs, traceId |
| `src/main/java/com/chat/myAgent/model/vo/StructuredResponse.java` | result(T), outputType, originalInput |
| `src/main/java/com/chat/myAgent/model/vo/AuthResponse.java` | token, username, role |
| `src/main/java/com/chat/myAgent/model/vo/StepResult.java` | stepNumber, description, toolUsed, result, success, timeMs |
| `src/main/java/com/chat/myAgent/model/vo/DocumentVO.java` | fileName, chunkCount, fileSize |
| `src/main/java/com/chat/myAgent/model/vo/HomeOverviewVO.java` | 首页概览 |
| `src/main/java/com/chat/myAgent/model/vo/MonitorOverviewVO.java` | 监控概览 |
| `src/main/java/com/chat/myAgent/model/vo/AuditLogVO.java` | 审计日志 |
| `src/main/java/com/chat/myAgent/model/vo/PermissionVO.java` | 权限信息 |
| `src/main/java/com/chat/myAgent/model/vo/UserVO.java` | 用户信息 |
| `src/main/java/com/chat/myAgent/model/vo/DemoFlowStepVO.java` | 演示步骤 |
| `src/main/java/com/chat/myAgent/model/vo/OpsDashboardVO.java` | 运维面板 |
| `src/main/java/com/chat/myAgent/model/vo/OpsMetricVO.java` | 运维指标 |
| `src/main/java/com/chat/myAgent/model/vo/PerformanceSummaryVO.java` | 性能摘要 |
| `src/main/java/com/chat/myAgent/model/vo/TrendPointVO.java` | 趋势数据点 |
| `src/main/java/com/chat/myAgent/model/vo/SessionSummaryVO.java` | 会话摘要 |

#### Entity（持久化实体）

| 文件路径 | 存储 | 说明 |
|---------|------|------|
| `src/main/java/com/chat/myAgent/model/entity/UserEntity.java` | MySQL/H2(JPA) | 用户实体（username, password, role） |
| `src/main/java/com/chat/myAgent/model/entity/ChatHistoryEntity.java` | MySQL/H2(JPA) | 对话历史（conversationId, username, messageRole, content, agentType, model, tokens, latencyMs） |
| `src/main/java/com/chat/myAgent/model/entity/AgentInvocationEntity.java` | MySQL/H2(JPA) | Agent 调用记录（invocationId, conversationId, agentType, model, traceId, inputText, outputText, thinkingText, status, latencyMs） |
| `src/main/java/com/chat/myAgent/model/entity/BookInfo.java` | 内存（结构化输出） | 图书信息（title, author, genre, year, summary） |
| `src/main/java/com/chat/myAgent/model/entity/SentimentResult.java` | 内存（结构化输出） | 情感分析（sentiment, confidence, keywords） |
| `src/main/java/com/chat/myAgent/model/entity/TaskAnalysis.java` | 内存（结构化输出） | 任务分析（taskType, complexity, steps, requiredTools） |

#### MongoDB 文档

| 文件路径 | 说明 |
|---------|------|
| `src/main/java/com/chat/myAgent/model/mongo/ChatSessionDocument.java` | 会话文档（sessionId, userId, title, summary, status, lastMessageAt） |
| `src/main/java/com/chat/myAgent/model/mongo/ChatMessageDocument.java` | 消息文档（sessionId, userId, role, content, createdAt） |

#### Qdrant 模型

| 文件路径 | 说明 |
|---------|------|
| `src/main/java/com/chat/myAgent/model/qdrant/DocumentChunk.java` | 文档切片（id, content, metadata） |
| `src/main/java/com/chat/myAgent/model/qdrant/SearchResult.java` | 检索结果（id, content, score, metadata） |

#### 平台模型

| 文件路径 | 说明 |
|---------|------|
| `src/main/java/com/chat/myAgent/model/platform/AgentInvocation.java` | Agent 调用上下文 |
| `src/main/java/com/chat/myAgent/model/platform/AgentRunRecord.java` | Agent 运行记录 |
| `src/main/java/com/chat/myAgent/model/platform/ConversationContext.java` | 对话上下文 |
| `src/main/java/com/chat/myAgent/model/platform/ConversationMessage.java` | 对话消息 |
| `src/main/java/com/chat/myAgent/model/platform/ExecutionContext.java` | 执行上下文 |
| `src/main/java/com/chat/myAgent/model/platform/KnowledgeDocumentContext.java` | 知识文档上下文 |
| `src/main/java/com/chat/myAgent/model/platform/ModelSelection.java` | 模型选择 |
| `src/main/java/com/chat/myAgent/model/platform/TaskContext.java` | 任务上下文 |
| `src/main/java/com/chat/myAgent/model/platform/ToolInvocationRecord.java` | 工具调用记录 |

### 2.9 公共层 `common/`

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/com/chat/myAgent/common/result/R.java` | 统一响应体 `{code, message, data, timestamp}`，静态方法：ok / fail / paramError / unauthorized / forbidden / modelError |
| `src/main/java/com/chat/myAgent/common/constant/ResultCode.java` | 响应码常量：200(SUCCESS) / 400(BAD_REQUEST) / 401(UNAUTHORIZED) / 403(FORBIDDEN) / 429(RATE_LIMIT) / 500(INTERNAL_ERROR) / 503(THIRD_PARTY_ERROR) |
| `src/main/java/com/chat/myAgent/common/constant/AgentConstants.java` | Agent 常量定义 |
| `src/main/java/com/chat/myAgent/common/exception/BizException.java` | 业务异常（code + message） |
| `src/main/java/com/chat/myAgent/common/exception/ErrorCode.java` | 错误码枚举 |
| `src/main/java/com/chat/myAgent/common/exception/GlobalExceptionHandler.java` | 全局异常处理：BizException / NonTransientAiException(503) / MethodArgumentNotValidException(400) / ConstraintViolationException / AccessDeniedException(403) / Exception(500兜底) |
| `src/main/java/com/chat/myAgent/common/ratelimit/RateLimitInterceptor.java` | 限流拦截器：滑动窗口30次/分钟，Redis优先+内存兜底 |
| `src/main/java/com/chat/myAgent/common/stream/StreamEvent.java` | SSE 流式事件 record：type(start/delta/message/done/error) + content + toJson() |
| `src/main/java/com/chat/myAgent/common/context/TraceContext.java` | 链路追踪上下文（ThreadLocal traceId） |
| `src/main/java/com/chat/myAgent/common/context/TraceContextFilter.java` | 链路追踪过滤器 |
| `src/main/java/com/chat/myAgent/common/context/TraceContextMdcFilter.java` | MDC 日志追踪过滤器 |

### 2.10 监控层 `monitor/`

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/com/chat/myAgent/monitor/TokenUsageTracker.java` | Token 使用量追踪（内存统计 + DB 持久化 + 成本估算） |

### 2.11 服务层 `service/`

| 文件路径 | 职责 |
|---------|------|
| `src/main/java/com/chat/myAgent/service/AuditService.java` | 审计服务：saveChatHistory / saveAgentInvocation / countAgentRunsByType / countChatHistoryByUsername / sumTotalTokens |
| `src/main/java/com/chat/myAgent/service/mongo/ChatConversationService.java` | MongoDB 对话服务接口 |
| `src/main/java/com/chat/myAgent/service/mongo/ChatMessageService.java` | MongoDB 消息服务接口 |
| `src/main/java/com/chat/myAgent/service/mongo/ChatSessionService.java` | MongoDB 会话服务接口：createSession / getSessionBySessionId / listSessionsByUserId / updateSessionTitle / updateSessionSummary / updateLastMessageAt / archiveSession / deleteSession |
| `src/main/java/com/chat/myAgent/service/mongo/impl/ChatConversationServiceImpl.java` | 对话服务实现 |
| `src/main/java/com/chat/myAgent/service/mongo/impl/ChatMessageServiceImpl.java` | 消息服务实现 |
| `src/main/java/com/chat/myAgent/service/mongo/impl/ChatSessionServiceImpl.java` | 会话服务实现 |
| `src/main/java/com/chat/myAgent/service/qdrant/QdrantIndexService.java` | Qdrant 索引服务接口：createCollectionIfAbsent / upsertChunks |
| `src/main/java/com/chat/myAgent/service/qdrant/QdrantSearchService.java` | Qdrant 搜索服务接口：search(query, topK) / search(query, topK, filter) |
| `src/main/java/com/chat/myAgent/service/qdrant/impl/QdrantIndexServiceImpl.java` | Qdrant 索引服务实现 |
| `src/main/java/com/chat/myAgent/service/qdrant/impl/QdrantSearchServiceImpl.java` | Qdrant 搜索服务实现 |

### 2.12 数据访问层 `repository/`

| 文件路径 | 存储 | 职责 |
|---------|------|------|
| `src/main/java/com/chat/myAgent/repository/UserRepository.java` | JPA | 用户 CRUD |
| `src/main/java/com/chat/myAgent/repository/ChatHistoryRepository.java` | JPA | 对话历史 CRUD + 聚合查询（sumTotalTokens / countByUsername / countByAgentType） |
| `src/main/java/com/chat/myAgent/repository/AgentInvocationRepository.java` | JPA | Agent 调用记录 CRUD |
| `src/main/java/com/chat/myAgent/repository/mongo/ChatMessageRepository.java` | MongoDB | 消息文档 CRUD |
| `src/main/java/com/chat/myAgent/repository/mongo/ChatSessionRepository.java` | MongoDB | 会话文档 CRUD |

### 2.13 资源文件 `src/main/resources/`

#### 配置文件

| 文件路径 | 说明 |
|---------|------|
| `src/main/resources/application.yml` | 主配置：端口8080、Spring AI OpenAI 配置、文件上传限制、Knife4j、日志级别 |
| `src/main/resources/application-dev.yml` | 开发环境：H2数据库、JPA ddl-auto:update、Redis配置、JWT配置、DashScope Embedding配置、RAG配置、限流配置、网络超时/重试配置 |
| `src/main/resources/application-prod.yml` | 生产环境配置 |
| `src/main/resources/logback-spring.xml` | 日志配置 |

#### Prompt 模板 `src/main/resources/prompts/`

| 文件路径 | 变量 | 使用者 |
|---------|------|--------|
| `src/main/resources/prompts/chat-system.st` | 无 | memoryChatClient |
| `src/main/resources/prompts/expert-system.st` | `{role}` / `{level}` | ChatAgent.expertChat() |
| `src/main/resources/prompts/tool-agent-system.st` | 无 | toolChatClient |
| `src/main/resources/prompts/rag-system.st` | 无 | ragChatClient |
| `src/main/resources/prompts/planning-system.st` | 无 | PlanningAgent |
| `src/main/resources/prompts/full-agent-system.st` | 无 | fullAgentClient |
| `src/main/resources/prompts/structured-book.st` | 无 | StructuredAgent.extractBookInfo() |
| `src/main/resources/prompts/structured-task.st` | 无 | StructuredAgent.analyzeTask() |

#### 知识库文档 `src/main/resources/knowledge/`

| 文件路径 | 说明 |
|---------|------|
| `src/main/resources/knowledge/employee-handbook.md` | 员工手册（考勤/休假/薪酬） |
| `src/main/resources/knowledge/product-faq.txt` | 产品FAQ |
| `src/main/resources/knowledge/tech-standard.md` | 技术规范 |
| `src/main/resources/knowledge/question.txt` | 常见问题 |

#### 可解析文档 `src/main/resources/docs/`

| 文件路径 | 说明 |
|---------|------|
| `src/main/resources/docs/sample.md` | 示例 Markdown |
| `src/main/resources/docs/company-intro.txt` | 公司介绍 |
| `src/main/resources/docs/SYSTEM_DOC.md` | 系统文档 |

---

## 三、前端目录结构（完整文件路径与职责）

### 3.1 项目配置

| 文件路径 | 说明 |
|---------|------|
| `front/package.json` | 依赖：vue 3.5 / vue-router 4.5 / pinia 2.2 / element-plus 2.8 / axios 1.7 / vite 6 |
| `front/vite.config.ts` | Vite 配置：端口5173，`@` 别名，`/api` 代理到 `http://localhost:8080`（不 rewrite） |
| `front/.env.development` | `VITE_APP_TITLE=AI Agent 管理台`，`VITE_API_BASE_URL=/api` |
| `front/.env.production` | 生产环境变量 |
| `front/tsconfig.json` | TypeScript 配置 |
| `front/tsconfig.app.json` | App TS 配置 |
| `front/tsconfig.node.json` | Node TS 配置 |
| `front/.eslintrc.cjs` | ESLint 配置 |
| `front/.prettierrc` | Prettier 配置 |
| `front/index.html` | HTML 入口 |

### 3.2 入口与布局

| 文件路径 | 说明 |
|---------|------|
| `front/src/main.ts` | Vue 应用入口，注册 Pinia / Router / ElementPlus |
| `front/src/App.vue` | 根组件，`<router-view />` |
| `front/src/layouts/BasicLayout.vue` | 主布局：左侧菜单 + 顶部栏 + 内容区。菜单项按权限过滤，显示角色标签，退出登录 |

### 3.3 路由 `front/src/router/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/router/index.ts` | 路由定义 + 全局守卫。公开路由：/login / /register / /init-admin。认证路由：/home / /demo / /deploy / /release / /dashboard / /docs / /stream / /agent / /knowledge / /planning / /session / /monitor / /audit / /admin。守卫逻辑：Token检查 → 权限加载 → 菜单权限校验 |

### 3.4 状态管理 `front/src/store/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/store/index.ts` | Pinia 实例创建 |
| `front/src/store/modules/user.ts` | 用户状态：token / username / role / nickname，持久化到 localStorage（key: AI_AGENT_TOKEN / AI_AGENT_USERNAME / AI_AGENT_ROLE / AI_AGENT_NICKNAME） |
| `front/src/store/modules/permission.ts` | 权限状态：role / menus / actions / loaded，从 `/api/v1/permission/current` 加载 |
| `front/src/store/modules/app.ts` | 应用全局状态 |

### 3.5 API 层 `front/src/api/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/api/index.ts` | 统一导出：authApi / chatApi / agentApi / knowledgeApi / planningApi / streamApi / sessionApi / monitorApi |
| `front/src/api/auth.ts` | login / register / initAdmin → `/api/v1/auth/*` |
| `front/src/api/chat.ts` | createSession / listSessions / getSessionMessages / sendMemoryChat / streamMemoryChatUrl / queryPing → `/api/v1/chat/*` + `/api/v1/session/*` |
| `front/src/api/agent.ts` | Agent 相关 API |
| `front/src/api/knowledge.ts` | uploadDocument / loadDirectory / listDocuments / deleteDocument / askKnowledge / askKnowledgeManual / searchKnowledge / getKnowledgeStatus → `/api/v1/knowledge/*` |
| `front/src/api/planning.ts` | 规划相关 API |
| `front/src/api/stream.ts` | buildStreamUrl（构建 SSE URL） |
| `front/src/api/session.ts` | 会话管理 API |
| `front/src/api/monitor.ts` | 监控 API |
| `front/src/api/admin.ts` | 管理员 API |
| `front/src/api/audit.ts` | 审计 API |
| `front/src/api/demo.ts` | 演示 API |
| `front/src/api/deploy.ts` | 部署 API |
| `front/src/api/health.ts` | 健康检查 API |
| `front/src/api/home.ts` | 首页 API |
| `front/src/api/ops.ts` | 运维 API |
| `front/src/api/performance.ts` | 性能 API |
| `front/src/api/permission.ts` | 权限 API（getCurrentPermission） |
| `front/src/api/release.ts` | 发布 API |

### 3.6 类型定义 `front/src/types/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/types/auth.ts` | LoginRequest / RegisterRequest / AuthResponse / ApiResponse |
| `front/src/types/chat.ts` | ChatMode / ChatRequest / ChatResponse / SessionVO / ChatMessageVO / StructuredRequest / StructuredResponse |
| `front/src/types/stream.ts` | StreamMode / StreamStatus / StreamChatParams / StreamMessageLog / StreamEventPayload |
| `front/src/types/knowledge.ts` | KnowledgeRequest / KnowledgeResponse |
| `front/src/types/agent.ts` | Agent 相关类型 |
| `front/src/types/session.ts` | 会话相关类型 |
| `front/src/types/monitor.ts` | 监控相关类型 |
| `front/src/types/planning.ts` | 规划相关类型 |
| `front/src/types/docs.ts` | 文档相关类型 |

### 3.7 工具函数 `front/src/utils/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/utils/request.ts` | Axios 封装：baseURL 为空（由 Vite 代理），30s 超时，请求拦截注入 JWT，响应拦截处理 code!=200 和 401 跳转登录 |
| `front/src/utils/storage.ts` | localStorage 工具 |
| `front/src/utils/chatNormalize.ts` | 聊天响应标准化：normalizeAnswer / normalizeSessionId |

### 3.8 Composables `front/src/composables/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/composables/useSseStream.ts` | SSE 流式对话核心 composable。使用 fetch + ReadableStream 接收 SSE，解析 data: 行，支持 start/delta/message/done/error 事件类型。提供 status / answer / error / conversationId / logs / isRunning / elapsedMs / startFetchStream / stop / clear |

### 3.9 页面视图 `front/src/views/`

| 文件路径 | 路由 | 说明 |
|---------|------|------|
| `front/src/views/login/index.vue` | /login | 登录页 |
| `front/src/views/register/index.vue` | /register | 注册页 |
| `front/src/views/init-admin/index.vue` | /init-admin | 初始化管理员页 |
| `front/src/views/home/index.vue` | /home | 首页 |
| `front/src/views/dashboard/index.vue` | /dashboard | 仪表盘 |
| `front/src/views/demo/index.vue` | /demo | 演示中心 |
| `front/src/views/stream/index.vue` | /stream | **基础对话页**（核心）：三栏布局（会话列表/对话区+响应结果/请求日志），使用 sendSimpleChat API |
| `front/src/views/chat/index.vue` | /chat | 重定向到 /stream |
| `front/src/views/agent/index.vue` | /agent | Agent 管理页 |
| `front/src/views/knowledge/index.vue` | /knowledge | 知识库管理页 |
| `front/src/views/planning/index.vue` | /planning | 任务规划页 |
| `front/src/views/session/index.vue` | /session | 会话管理页 |
| `front/src/views/monitor/index.vue` | /monitor | 监控面板页 |
| `front/src/views/audit/index.vue` | /audit | 审计日志页 |
| `front/src/views/admin/index.vue` | /admin | 用户管理页 |
| `front/src/views/deploy/index.vue` | /deploy | 部署验收页 |
| `front/src/views/release/index.vue` | /release | 发布说明页 |
| `front/src/views/docs/index.vue` | /docs | 接口文档页 |

### 3.10 样式 `front/src/styles/`

| 文件路径 | 说明 |
|---------|------|
| `front/src/styles/index.scss` | 全局样式入口 |
| `front/src/styles/reset.scss` | 样式重置 |
| `front/src/styles/variables.scss` | SCSS 变量 |

---

## 四、核心架构与流程

### 4.1 ChatClient 配置体系

```
ChatClient.Builder (Spring AI 自动提供)
    │
    ├── baseChatClient = primaryChatModel + 默认系统提示词（无记忆无Advisor）
    │
    ├── memoryChatClient = primaryChatModel + chat-system.st + MessageChatMemoryAdvisor
    │
    ├── toolChatClient = primaryChatModel + tool-agent-system.st + MessageChatMemoryAdvisor
    │
    ├── ragChatClient = primaryChatModel + rag-system.st
    │                    + MessageChatMemoryAdvisor
    │                    + QuestionAnswerAdvisor(VectorStore, topK=5, threshold=0.5)
    │
    ├── fullAgentClient = primaryChatModel + full-agent-system.st + MessageChatMemoryAdvisor
    │
    └── fallbackChatClient = fallbackChatModel + 默认系统提示词（兜底）
```

**Advisor 执行顺序**：
```
请求 → MessageChatMemoryAdvisor(加载历史) → QuestionAnswerAdvisor(检索知识库)
     → AI模型
     → QuestionAnswerAdvisor(处理) → MessageChatMemoryAdvisor(保存记忆) → 响应
```

### 4.2 模型容灾机制

```
主模型(DeepSeek Chat)调用
    │
    ├── 成功 → 返回结果
    │
    └── 失败 → 判断异常类型
              ├── 网络连通异常(ConnectException/SocketTimeout/UnknownHost等) → 立即切换
              └── 其他异常 → 切换兜底模型
                  │
                  ├── 兜底模型(Qwen3.6-Plus)调用
                  │   ├── 成功 → 返回结果
                  │   └── 失败 → 抛出 RuntimeException
```

### 4.3 请求处理链路

```
HTTP Request
    → CorsFilter（跨域）
    → RateLimitInterceptor（滑动窗口30次/分，Redis优先+内存ConcurrentHashMap兜底）
    → JwtAuthenticationFilter（JWT无状态认证，Bearer Token）
    → Controller → Agent → [ChatClient + Advisor + Tool]
    → Spring AI → AI Model API (DeepSeek / DashScope)
    → 统一响应体 R<T> → JSON → HTTP Response
    → （异常时）GlobalExceptionHandler 捕获 → R.modelError() / R.fail() / R.paramError()
```

### 4.4 RAG 知识库流程

**入库**：
```
上传文件/加载本地文件 → TextReader.read()
    → TokenTextSplitter(chunkSize=120, overlap=30, keepSeparator=true)
    → 为每个片段添加 metadata(source, chunk_index, total_chunks)
    → VectorStore.add(chunks) → EmbeddingModel自动向量化
    → SimpleVectorStore.save() → 持久化到 ./data/vectorstore/vector-store.json
```

**自动模式问答**：
```
用户问题 → ragChatClient.prompt()
    → MessageChatMemoryAdvisor(加载历史)
    → QuestionAnswerAdvisor(EmbeddingModel向量化 → VectorStore相似度检索 topK=5 threshold=0.5 → 注入检索结果到Prompt)
    → AI模型基于检索结果生成回答
    → MessageChatMemoryAdvisor(保存记忆)
```

**手动模式问答**：
```
用户问题 → RetrievalService.retrieve() 手动检索
    → 拼接上下文（[参考N] 来源: xxx \n 内容: xxx）
    → baseChatClient.prompt().system(context).user(question)
    → AI模型基于手动拼接的上下文回答
```

### 4.5 任务规划流程

```
用户复杂需求 → PlanningAgent.planAndExecute()
    → baseChatClient + planning-system.st → 模型返回规划JSON
    → cleanJsonResponse()（清理markdown代码块包裹）
    → 解析JSON
        ├── needPlanning=false → 直接返回 directAnswer
        ├── needPlanning=true + autoExecute=false → 仅返回步骤列表
        └── needPlanning=true + autoExecute=true → executeSteps()
            → 逐步执行：每步根据 toolNeeded 决定是否带工具
            → 上下文累积：前序步骤结果注入后续步骤
            → generateFinalAnswer() → 汇总所有步骤 → 最终回答
    → 解析失败 → fallbackDirectExecution() → FullAgent 直接执行
```

### 4.6 流式对话流程

```
用户消息 → StreamAgent.streamChat() / streamChatWithTools()
    → fullAgentClient/baseChatClient.prompt().user(message).stream().content()
    → 返回 Flux<String>
    → SSE 推送到前端（text/event-stream）
```

### 4.7 结构化输出流程

```
用户输入 → baseChatClient.prompt().system(promptTemplate).user(userMessage).call().entity(XxxClass)
    → Spring AI 自动：
        1. BeanOutputConverter 分析类字段和注解
        2. 生成 JSON Schema 注入 Prompt
        3. 大模型按 Schema 生成 JSON
        4. 自动反序列化为 Java 对象
```

---

## 五、安全体系

### 5.1 认证方式
- JWT 无状态认证，不使用 Session
- Token 通过 `Authorization: Bearer <token>` 传递
- 密码 BCrypt 加密
- Token 有效期 24h

### 5.2 权限矩阵

| 角色 | 可访问接口 |
|------|-----------|
| 未认证 | `/auth/**`, `/chat/ping`, `/chat/quick`, `/stream/**`, 静态资源, Knife4j文档, H2控制台 |
| USER | 对话接口、Agent接口、知识库问答/检索/状态、会话管理、文档列表 |
| ADMIN | 所有接口 + 知识库上传/加载/删除 + 监控管理 |

### 5.3 前端权限控制
- 路由守卫：`requiresAuth` meta 字段 + Token 检查
- 菜单权限：`permissionStore.hasMenu(routeKey)` 过滤可见菜单
- API 拦截：401 响应自动清除 Token 并跳转登录页

---

## 六、数据存储架构

| 存储 | 用途 | 实现 | 配置 |
|------|------|------|------|
| **H2/MySQL** | 用户、对话历史、Agent调用记录 | Spring Data JPA | dev: H2文件 `./data/smartagent-db`，prod: MySQL |
| **MongoDB** | 会话文档、消息文档 | Spring Data MongoDB | `@EnableMongoRepositories` |
| **Redis** | 聊天记忆持久化(备用)、限流计数 | Spring Data Redis | localhost:6379, password=root |
| **Qdrant** | 向量存储与检索 | Spring AI Qdrant VectorStore | localhost:6334, collection=smart-agent |
| **JSON文件** | SimpleVectorStore 持久化(开发阶段) | 本地文件 | `./data/vectorstore/vector-store.json` |

---

## 七、配置体系

### 7.1 关键配置项

| 配置项 | 默认值 | 说明 |
|--------|-------|------|
| `spring.ai.openai.base-url` | `https://api.deepseek.com` | DeepSeek API 地址 |
| `spring.ai.openai.api-key` | 环境变量 `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `smart-agent.models.default-model` | `deepseek-chat` | 默认主模型 |
| `smart-agent.models.fallback-model` | `qwen3.6-plus` | 兜底模型 |
| `deepseek.api-base-url` | `https://api.deepseek.com` | 主模型 Base URL |
| `deepseek.api-key` | 环境变量 `DEEPSEEK_API_KEY` | 主模型 Key |
| `qwen.api-base-url` | `https://dashscope.aliyuncs.com/compatible-mode` | 兜底模型 Base URL |
| `qwen.api-key` | 环境变量 `QWEN_API_KEY` | 兜底模型 Key |
| `dashscope.api-key` | 环境变量 `QWEN_API_KEY` | DashScope Embedding Key |
| `dashscope.base-url` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | DashScope Embedding URL |
| `dashscope.embedding-model` | `text-embedding-v4` | Embedding 模型 |
| `smart-agent.jwt.secret` | 内置默认值 | JWT 签名密钥 |
| `smart-agent.jwt.expiration-hours` | 24 | Token 有效期 |
| `smart-agent.rate-limit.max-requests-per-minute` | 30 | API 限流阈值 |
| `smart-agent.rag.vector-store-path` | `./data/vectorstore/vector-store.json` | 向量库持久化路径 |
| `smart-agent.rag.chunk-size` | 120 | 文档切片大小 |
| `smart-agent.rag.chunk-overlap` | 30 | 切片重叠大小 |
| `smart-agent.rag.top-k` | 5 | 检索 Top-K |
| `smart-agent.rag.similarity-threshold` | 0.5 | 相似度阈值 |
| `smart-agent.network.connect-timeout` | 8s | 连接超时 |
| `smart-agent.network.read-timeout` | 25s | 读取超时 |
| `smart-agent.retry.primary.max-attempts` | 1 | 主模型重试次数 |
| `smart-agent.retry.fallback.max-attempts` | 1 | 兜底模型重试次数 |

### 7.2 环境变量

| 变量名 | 说明 |
|--------|------|
| `DEEPSEEK_API_KEY` | DeepSeek Chat 模型 API Key |
| `QWEN_API_KEY` | Qwen/DashScope API Key（兜底模型 + Embedding） |
| `JWT_SECRET` | JWT 签名密钥 |
| `MYSQL_HOST` / `MYSQL_USER` / `MYSQL_PASSWORD` | MySQL 连接信息 |
| `REDIS_HOST` / `REDIS_PASSWORD` | Redis 连接信息 |

---

## 八、API 接口总览

### 8.1 认证接口 `/api/v1/auth`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/register` | 用户注册 | ❌ |
| POST | `/login` | 用户登录，返回JWT | ❌ |
| POST | `/init-admin` | 初始化管理员 | ❌ |

### 8.2 对话接口 `/api/v1/chat`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/ping` | 健康检查 | ❌ |
| GET | `/quick?message=xxx` | 快速对话(GET) | ✅ |
| POST | `/simple` | 简单对话(无记忆) | ✅ |
| POST | `/memory` | 多轮记忆对话 | ✅ |
| POST | `/expert` | 专家角色对话 | ✅ |
| POST | `/structured/book` | 结构化-图书信息 | ✅ |
| POST | `/structured/task` | 结构化-任务分析 | ✅ |
| POST | `/structured/sentiment` | 结构化-情感分析 | ✅ |

### 8.3 Agent 接口 `/api/v1/agent`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/chat` | 工具调用(无记忆) | ✅ |
| POST | `/chat/memory` | 工具调用(带记忆) | ✅ |
| POST | `/chat/specific` | 指定工具对话 | ✅ |

### 8.4 知识库接口 `/api/v1/knowledge`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/upload` | 上传文档 | ADMIN |
| POST | `/load-directory` | 批量加载 | ADMIN |
| GET | `/documents` | 文档列表 | USER+ |
| DELETE | `/documents/{fileName}` | 删除文档 | USER+ |
| POST | `/ask` | RAG问答(自动) | ✅ |
| POST | `/ask/manual` | RAG问答(手动) | ✅ |
| GET | `/search?query=xxx` | 纯检索 | ✅ |
| GET | `/status` | 知识库状态 | ✅ |

### 8.5 规划接口 `/api/v1/planning`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/execute` | 规划并执行 | ✅ |
| POST | `/plan-only` | 仅规划不执行 | ✅ |
| POST | `/chat` | 全能Agent入口 | ✅ |

### 8.6 流式接口 `/api/v1/stream`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/chat` | 流式对话(无工具) | ❌ |
| GET | `/chat/tools` | 流式对话(带工具) | ❌ |

### 8.7 会话接口 `/api/v1/session`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/{conversationId}/history` | 会话历史 | ✅ |
| DELETE | `/{conversationId}` | 清除会话 | ✅ |

### 8.8 监控接口 `/api/v1/monitor`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/stats` | Token统计/成本 | ADMIN |
| GET | `/history` | 对话历史列表 | ADMIN |
| GET | `/conversation/{id}` | 会话详情 | ADMIN |

---

## 九、Docker 与部署

### 9.1 Docker Compose

文件路径：`docker-compose.yml`

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.4 | 3306 | 数据库，root密码root123，库名smart_agent |
| redis | redis:7.4 | 6379 | 缓存，appendonly=yes |

### 9.2 Dockerfile

文件路径：`Dockerfile` — 后端镜像构建

### 9.3 启动脚本

文件路径：`scripts/start-dev.ps1` — Windows 开发环境启动脚本

---

## 十、项目改造方向：LearnAgent — 个人学习知识库 Agent

### 10.1 改造目标

将 SmartAgent 从"通用企业Agent展示"改造为**面向个人学习者的AI知识助手**，核心融入 **ReAct（Reasoning + Acting）推理模式**，使 Agent 的思考过程可视化。

### 10.2 ReAct 核心循环

```
用户问题 → Thought(推理) → Action(工具调用) → Observation(观察结果)
         → Thought(基于观察继续推理) → Action → Observation
         → ... → Final Answer(最终回答)
```

与当前项目的关键区别：
- 当前：工具调用是黑盒，用户只看到最终结果
- ReAct：每步 Thought/Action/Observation 可视化，用户可中途干预
- 当前：一次性调用，模型自行决定
- ReAct：迭代式，基于观察结果决定下一步

### 10.3 工具替换计划

| 当前工具 | 替换为 | 说明 |
|---------|--------|------|
| DateTimeTool | **KnowledgeTool** | search_notes / create_note / update_note / find_related / delete_note |
| CalculatorTool | **QuizTool** | generate_quiz / evaluate_answer / generate_flashcard / evaluate_explanation |
| TranslateTool | **WebSearchTool** | search_web / summarize_article |
| DocParseTool | 合并到 KnowledgeTool | 笔记管理统一入口 |
| DbQueryTool | **LearningTool** | get_progress / record_study / analyze_weakness / get_review_reminders |
| CalculatorTool(保留) | CalculatorTool | 数学计算保留 |

### 10.4 新增核心模块

| 模块 | 说明 |
|------|------|
| **ReActEngine** | ReAct 推理引擎核心类，Thought→Action→Observation 迭代循环，maxIterations=8 |
| **LearnAgent** | 替代 FullAgent，集成 ReAct 引擎，面向学习场景 |
| **KnowledgeNote** | 知识笔记实体（MongoDB + Qdrant 双存储） |
| **StudyRecord** | 学习记录实体（MySQL） |
| **Quiz / Flashcard** | 测验题/闪卡实体（MongoDB） |
| **ReActTrace** | 推理链路记录（MongoDB），支持回放 |

### 10.5 前端改造计划

| 页面 | 改造方向 |
|------|---------|
| 首页 | 学习仪表盘（今日学习时长、待复习数、掌握度雷达图） |
| 基础对话 | **ReAct 对话页**（左侧推理链路可视化 + 右侧对话区） |
| 知识库 | 知识笔记管理（CRUD、标签分类、知识图谱可视化） |
| Agent管理 | 学习工具箱（闪卡练习、自测模式、费曼检验） |
| 任务规划 | 学习计划（自动生成学习路径、进度追踪） |
| 会话管理 | 学习记录（历史轨迹、ReAct 推理回放） |
| 监控 | 学习分析（薄弱点热力图、知识覆盖度、学习趋势） |
| 新增 | 知识图谱页（力导向图展示笔记关联） |

### 10.6 实施阶段

1. **阶段1**：ReAct 引擎核心（ReActEngine + LearnAgent + ReAct 对话接口）
2. **阶段2**：知识库工具替换（KnowledgeTool / LearningTool / QuizTool）
3. **阶段3**：前端 ReAct 可视化（推理链路面板 + 对话区双栏布局）
4. **阶段4**：学习追踪与图谱（进度追踪 + 薄弱点分析 + 知识图谱可视化）
5. **阶段5**：体验优化（ReAct 流式推送 + 用户中途干预 + 学习报告导出）

---

## 十一、关键设计决策

| 决策 | 理由 |
|------|------|
| Chat 和 Embedding 使用不同模型 | DeepSeek 不提供 Embedding API，选择 DashScope text-embedding-v4 |
| 使用 SimpleVectorStore | 开发阶段无需安装额外数据库，后续可切换到 Qdrant/PGVector |
| 关闭 DeepSeek 思考模式 | Spring AI ChatMemory 不支持 reasoning_content 字段，会导致后续请求报错 |
| TranslateTool 使用 AI调用AI | 避免引入第三方翻译 API 依赖 |
| RAG 相似度阈值代码中设为 0.3 | DashScope text-embedding-v4 相似度分数普遍偏低（0.3-0.5） |
| 限流拦截器双模式 | Redis 优先（分布式），不可用时回退内存（ConcurrentHashMap + ConcurrentLinkedDeque） |
| PlanningAgent 有回退机制 | AI 返回的规划 JSON 可能格式不规范，cleanJsonResponse() 清理后仍失败则 fallbackDirectExecution() |
| 前端 baseURL 为空 | 后端接口已含 /api 前缀，Vite 代理 /api → localhost:8080 不 rewrite |
| normalizeBaseUrl 去除 /v1 后缀 | OpenAI 兼容协议下避免路径重复 /v1/v1/chat/completions |
