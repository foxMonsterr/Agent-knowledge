# SmartAgent 项目架构文档

## 一、项目概述

| 项目 | 说明 |
|------|------|
| **项目名称** | SmartAgent - 基于 Spring AI 的多功能自主决策 Agent 助手 |
| **技术栈** | Spring Boot 3.5.4 + Spring AI 1.1.5 + JDK 17 |
| **AI 模型** | Chat: DeepSeek V4 Flash / Embedding: 阿里云 DashScope text-embedding-v4 |
| **接口文档** | Knife4j 4.5.0（访问地址：http://localhost:8080/doc.html） |
| **端口** | 8080 |
| **包路径** | `com.chat.myAgent` |

---

## 二、项目目录结构

```
src/main/java/com/chat/myAgent/
├── main/                           # 启动类
│   ├── MainApplication.java        # Spring Boot 入口（scanBasePackages="com.chat.myAgent"）
│   └── MainServletInitializer.java # WAR 部署支持
│
├── config/                         # 配置层（8个配置类）
│   ├── ChatClientConfig.java       # ChatClient Bean 配置（5个ChatClient Bean）
│   ├── MemoryConfig.java           # 聊天记忆配置（InMemoryChatMemoryRepository）
│   ├── EmbeddingConfig.java        # Embedding 模型配置（DashScope OpenAI兼容）
│   ├── VectorStoreConfig.java      # 向量库配置（SimpleVectorStore + JSON持久化）
│   ├── ModelConfig.java            # 多模型动态切换配置
│   ├── SecurityConfig.java         # Spring Security + JWT 配置
│   ├── Knife4jConfig.java          # Knife4j 接口文档配置（含JWT认证按钮）
│   ├── WebConfig.java              # Web MVC 配置（CORS + 限流拦截器）
│   └── RedisChatMemoryRepository.java # Redis 记忆持久化（备用）
│
├── controller/                     # 接口层（9个Controller）
│   ├── AuthController.java         # 认证：注册/登录/初始化管理员
│   ├── ChatController.java         # 对话：简单/记忆/专家/结构化
│   ├── AgentController.java        # 工具调用：无记忆/带记忆/指定工具
│   ├── KnowledgeController.java    # 知识库：文档管理 + RAG 问答
│   ├── PlanningController.java     # 任务规划：规划执行/全能Agent
│   ├── StreamController.java       # 流式对话：SSE 实时推送
│   ├── SessionController.java      # 会话管理：历史/清除
│   ├── MonitorController.java      # 监控：Token统计/对话历史
│   ├── PageController.java         # 页面路由
│   └── RedisConfig.java            # Redis 配置（@ConditionalOnClass 按需加载）
│
├── agent/                          # Agent 层（7个Agent，业务逻辑核心）
│   ├── ChatAgent.java              # 基础对话 Agent（简单/记忆/专家）
│   ├── StructuredAgent.java        # 结构化输出 Agent（图书/任务/情感）
│   ├── ToolAgent.java              # 工具调用 Agent（无记忆/带记忆/指定工具）
│   ├── RagAgent.java               # RAG 知识库问答 Agent（自动/手动）
│   ├── PlanningAgent.java          # 任务规划 Agent（拆解→逐步执行→汇总）
│   ├── FullAgent.java              # 全能力 Agent（记忆+工具统一入口）
│   └── StreamAgent.java            # 流式对话 Agent（SSE实时推送）
│
├── tool/                           # 工具层（5个工具，AI可调用的外部能力）
│   ├── DateTimeTool.java           # 时间日期工具（5个方法）
│   ├── CalculatorTool.java         # 计算器工具（3个方法）
│   ├── TranslateTool.java          # 翻译工具（AI调用AI模式）
│   ├── DocParseTool.java           # 文档解析工具（3个方法）
│   └── DbQueryTool.java            # 数据库查询工具（3个方法，模拟数据）
│
├── rag/                            # RAG 层（知识库检索增强）
│   ├── DocumentService.java        # 文档管理：上传/解析/切片/向量化/持久化
│   └── RetrievalService.java       # 向量检索：相似度搜索/格式化输出
│
├── auth/                           # 认证层
│   ├── JwtTokenProvider.java       # JWT Token 生成/解析/验证
│   ├── JwtAuthenticationFilter.java # JWT 过滤器（OncePerRequestFilter）
│   ├── JwtProperties.java          # JWT 配置属性（密钥/过期时间）
│   └── UserDetailsServiceImpl.java # Spring Security 用户加载服务
│
├── model/                          # 数据模型层
│   ├── dto/                        # 请求 DTO（7个）
│   │   ├── ChatRequest.java        # 对话请求（message, conversationId）
│   │   ├── AgentRequest.java       # Agent 请求（message, conversationId, tools）
│   │   ├── KnowledgeRequest.java   # 知识库问答请求（question, conversationId）
│   │   ├── PlanningRequest.java    # 任务规划请求（task, conversationId, autoExecute）
│   │   ├── StructuredRequest.java  # 结构化输出请求（input）
│   │   ├── LoginRequest.java       # 登录请求（username, password）
│   │   └── RegisterRequest.java    # 注册请求（username, password, role）
│   ├── vo/                         # 响应 VO（8个）
│   │   ├── ChatResponse.java       # 对话响应（reply, conversationId, model, historySize）
│   │   ├── AgentResponse.java      # Agent 响应（reply, conversationId, agentType）
│   │   ├── KnowledgeResponse.java  # 知识库问答响应（answer, sources, retrievedChunks）
│   │   ├── PlanningResponse.java   # 任务规划响应（steps, finalAnswer, planned）
│   │   ├── StructuredResponse.java # 结构化输出响应（result, outputType）
│   │   ├── AuthResponse.java       # 认证响应（token, username, role）
│   │   ├── DocumentVO.java         # 文档信息 VO（fileName, chunkCount, fileSize）
│   │   └── StepResult.java         # 规划步骤结果（stepNumber, description, result, success）
│   └── entity/                     # 实体类（5个）
│       ├── BookInfo.java           # 图书信息（结构化输出）
│       ├── TaskAnalysis.java       # 任务分析（结构化输出）
│       ├── SentimentResult.java    # 情感分析（结构化输出）
│       ├── ChatHistoryEntity.java  # 对话历史实体（JPA持久化）
│       └── UserEntity.java         # 用户实体（JPA持久化）
│
├── common/                         # 公共层
│   ├── result/R.java               # 统一响应体（code/message/data/timestamp）
│   ├── constant/AgentConstants.java # 常量定义
│   ├── exception/BizException.java # 业务异常
│   ├── exception/GlobalExceptionHandler.java # 全局异常处理（4种异常）
│   └── ratelimit/RateLimitInterceptor.java # 限流拦截器（滑动窗口，Redis+内存双模式）
│
├── monitor/                        # 监控层
│   └── TokenUsageTracker.java      # Token 使用量追踪（内存统计+DB持久化+成本估算）
│
└── repository/                     # 数据访问层
    ├── UserRepository.java         # 用户仓库（JPA）
    └── ChatHistoryRepository.java  # 对话历史仓库（JPA，含聚合查询）

src/main/resources/
├── application.yml                 # 主配置
├── application-dev.yml             # 开发环境配置
├── application-prod.yml            # 生产环境配置
├── prompts/                        # Prompt 模板（8个 .st 文件）
│   ├── chat-system.st              # 基础对话系统提示词
│   ├── expert-system.st            # 专家角色系统提示词（{role}/{level}变量）
│   ├── tool-agent-system.st        # 工具调用系统提示词
│   ├── rag-system.st               # RAG 问答系统提示词
│   ├── planning-system.st          # 任务规划系统提示词
│   ├── full-agent-system.st        # 全能Agent系统提示词
│   ├── structured-book.st          # 图书信息提取提示词
│   └── structured-task.st          # 任务分析提示词
├── knowledge/                      # 知识库文档（4个）
│   ├── employee-handbook.md        # 员工手册（考勤/休假/薪酬）
│   ├── product-faq.txt             # 产品FAQ
│   ├── tech-standard.md            # 技术规范
│   └── question.txt                # 常见问题
├── docs/                           # 可解析文档（3个，供DocParseTool读取）
│   ├── sample.md
│   ├── company-intro.txt
│   └── SYSTEM_DOC.md
└── static/                         # 前端静态页面
    ├── index.html
    ├── css/style.css
    └── js/app.js
```

---

## 三、系统架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                          前端（index.html）                          │
│                     EventSource / fetch / axios                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP / SSE
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    请求拦截链路                                       │
│  CorsFilter → RateLimitInterceptor → JwtAuthenticationFilter        │
│  （跨域）      （滑动窗口限流30次/分）   （JWT无状态认证）              │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Controller 层（REST API）                        │
│                                                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │   Auth   │ │   Chat   │ │  Agent   │ │Knowledge │ │ Planning │  │
│  │Controller│ │Controller│ │Controller│ │Controller│ │Controller│  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │  Stream  │ │ Session  │ │ Monitor  │ │   Page   │               │
│  │Controller│ │Controller│ │Controller│ │Controller│               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
│                                                                      │
│  所有 Controller 已添加 @Tag + @Operation + @Parameter 注解          │
│  统一返回 R<T> 响应体（code/message/data/timestamp）                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Agent 层（业务核心）                         │
│                                                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │   Chat   │ │Structured│ │   Tool   │ │   RAG    │               │
│  │  Agent   │ │  Agent   │ │  Agent   │ │  Agent   │               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                            │
│  │ Planning │ │   Full   │ │  Stream  │                            │
│  │  Agent   │ │  Agent   │ │  Agent   │                            │
│  └──────────┘ └──────────┘ └──────────┘                            │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────┐
│   Tool 层        │ │   RAG 层     │ │  Spring AI 核心   │
│                  │ │              │ │                  │
│ ┌──────────────┐ │ │ ┌──────────┐ │ │ ┌──────────────┐ │
│ │ DateTimeTool │ │ │ │Document  │ │ │ │ ChatClient   │ │
│ │CalculatorTool│ │ │ │ Service  │ │ │ │ ChatMemory   │ │
│ │TranslateTool │ │ │ └──────────┘ │ │ │ Embedding    │ │
│ │ DocParseTool │ │ │ ┌──────────┐ │ │ │ VectorStore  │ │
│ │ DbQueryTool  │ │ │ │Retrieval │ │ │ │ Advisor      │ │
│ └──────────────┘ │ │ │ Service  │ │ │ └──────────────┘ │
│                  │ │ └──────────┘ │ │                  │
└──────────────────┘ └──────────────┘ └──────────────────┘
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    ▼                      ▼                      ▼
          ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
          │  DeepSeek API│      │ DashScope API│      │  本地文件     │
          │  (Chat 模型)  │      │(Embedding模型)│      │(向量库JSON)   │
          └──────────────┘      └──────────────┘      └──────────────┘
```

---

## 四、核心模块详解

### 4.1 ChatClient 配置体系

ChatClient 是 Spring AI 的核心客户端，项目通过 `ChatClientConfig` 预配置了 5 个 Bean：

| Bean 名称 | 系统提示词 | Advisor | 使用的 Agent |
|-----------|-----------|---------|------------|
| `baseChatClient` | 内置默认字符串 | 无 | StructuredAgent, ToolAgent |
| `memoryChatClient` | chat-system.st | MessageChatMemoryAdvisor | ChatAgent |
| `toolChatClient` | tool-agent-system.st | MessageChatMemoryAdvisor | （备用） |
| `ragChatClient` | rag-system.st | MessageChatMemoryAdvisor + QuestionAnswerAdvisor | RagAgent |
| `fullAgentClient` | full-agent-system.st | MessageChatMemoryAdvisor | FullAgent, StreamAgent, PlanningAgent |

**Advisor 执行顺序：**

```
请求进入 → MessageChatMemoryAdvisor（加载历史）→ QuestionAnswerAdvisor（检索知识库）
         → 发送给AI模型
         → QuestionAnswerAdvisor（处理）→ MessageChatMemoryAdvisor（保存记忆）→ 返回响应
```

### 4.2 Agent 能力矩阵

| Agent | 使用的ChatClient | 记忆 | 工具 | RAG | 结构化 | 流式 | 规划 |
|-------|-----------------|:----:|:----:|:---:|:------:|:----:|:----:|
| ChatAgent | base + memory | ✅ | - | - | - | - | - |
| StructuredAgent | base | - | - | - | ✅ | - | - |
| ToolAgent | base | ✅ | ✅ | - | - | - | - |
| RagAgent | rag + base | ✅ | - | ✅ | - | - | - |
| PlanningAgent | base + full | ✅ | ✅ | - | - | - | ✅ |
| FullAgent | full | ✅ | ✅ | - | - | - | - |
| StreamAgent | full + base | ✅ | ✅ | - | - | ✅ | - |

### 4.3 工具调用流程

```
用户消息 → ChatClient.prompt()
              │
              ▼
         大模型分析（DeepSeek V4 Flash）
              │
              ├── 不需要工具 → 直接生成回答
              │
              └── 需要工具 → 生成 tool_calls JSON
                                │
                                ▼
                     Spring AI 自动执行工具方法
                     （根据 @Tool 注解描述匹配）
                                │
                                ▼
                     工具结果回传给大模型
                                │
                                ▼
                     大模型整合结果生成最终回答
```

**可用工具列表：**

| 工具 | 方法数 | 能力说明 |
|------|-------|---------|
| DateTimeTool | 5 | getCurrentDateTime / getCurrentDayOfWeek / daysBetween / addDays / getDayOfWeek |
| CalculatorTool | 3 | calculate（表达式计算）/ calculatePercentage（百分比）/ convertUnit（单位换算） |
| TranslateTool | 2 | translate（AI调用AI翻译）/ detectLanguage（语言检测） |
| DocParseTool | 3 | readFile（读取文件）/ listFiles（列出文件）/ getFileInfo（文件信息） |
| DbQueryTool | 3 | queryEmployees（员工查询）/ queryDepartments（部门查询）/ statistics（统计） |

### 4.4 RAG 知识库流程

**文档入库流程：**

```
上传文件 / 加载本地文件
       │
       ▼
TextReader.read() 读取文本内容
       │
       ▼
TokenTextSplitter.builder()
  .withChunkSize(300)      ← 可配置
  .withKeepSeparator(true)
  .build()
       │
       ▼
为每个片段添加元数据（source, chunk_index, total_chunks）
       │
       ▼
VectorStore.add(chunks)  ← EmbeddingModel自动向量化
       │
       ▼
SimpleVectorStore.save() 持久化到 JSON 文件
```

**RAG 问答流程（自动模式）：**

```
用户问题 → EmbeddingModel 向量化 → VectorStore 相似度检索（topK=5, threshold=0.3）
        → QuestionAnswerAdvisor 注入检索结果到 Prompt
        → 大模型基于检索结果生成回答
        → MessageChatMemoryAdvisor 保存对话记忆
```

**RAG 问答流程（手动模式）：**

```
用户问题 → RetrievalService.retrieve() 手动检索
        → 拼接上下文（[参考1] 来源: xxx \n 内容: xxx）
        → baseChatClient.prompt().system(context).user(question)
        → 大模型基于手动拼接的上下文回答
```

### 4.5 任务规划流程

```
用户复杂需求
     │
     ▼
PlanningAgent.planAndExecute()
     │
     ▼
baseChatClient + planning-system.st → 模型返回规划JSON
     │
     ├── needPlanning=false → 直接返回 directAnswer
     │
     └── needPlanning=true → 拆解为多步
           │
           ├── autoExecute=false → 只返回规划步骤，不执行
           │
           └── autoExecute=true → 逐步执行
                 │
                 ├── 步骤1 → [需要工具? → executeWithTools() : baseChatClient]
                 │            → 结果1
                 ├── 步骤2 → [携带步骤1结果作为上下文] → 结果2
                 └── 步骤N → 结果N
                      │
                      ▼
               generateFinalAnswer() → 汇总所有步骤 → 最终回答
                      │
                      ▼（如果规划解析失败）
               fallbackDirectExecution() → 回退到 FullAgent 直接执行
```

### 4.6 流式对话流程

```
用户消息 → StreamAgent.streamChat()
              │
              ▼
         fullAgentClient.prompt()
              .user(message)
              .stream()           ← 关键：.stream() 替代 .call()
              .content()          ← 返回 Flux<String>
              │
              ▼
         SSE 推送到前端
         （AI 边生成边推送，用户立即看到内容）
```

### 4.7 结构化输出流程

```
用户输入 → baseChatClient.prompt()
              .system(promptTemplate)
              .user(userMessage)
              .call()
              .entity(BookInfo.class)    ← 关键：.entity() 替代 .content()
              │
              ▼
         Spring AI 自动：
         1. BeanOutputConverter 分析 BookInfo 类的字段和注解
         2. 生成 JSON Schema 格式说明，注入到 Prompt
         3. 大模型按 Schema 生成 JSON
         4. 自动反序列化为 BookInfo 对象
```

---

## 五、API 接口总览

> 所有接口均已在 Knife4j 文档中注册，访问 http://localhost:8080/doc.html 查看完整文档

### 5.1 认证接口 `/api/v1/auth` — @Tag: 认证管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/register` | 用户注册 | ❌ |
| POST | `/login` | 用户登录，返回JWT Token | ❌ |
| POST | `/init-admin` | 初始化管理员账号 | ❌ |

### 5.2 对话接口 `/api/v1/chat` — @Tag: 对话管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/ping` | 健康检查 | ❌ |
| GET | `/quick?message=xxx` | 快速对话（GET方式，浏览器测试用） | ✅ |
| POST | `/simple` | 简单对话（无记忆，baseChatClient） | ✅ |
| POST | `/memory` | 多轮记忆对话（memoryChatClient） | ✅ |
| POST | `/expert` | 专家角色对话（PromptTemplate动态变量） | ✅ |
| POST | `/structured/book` | 结构化输出-图书信息提取 | ✅ |
| POST | `/structured/task` | 结构化输出-任务分析 | ✅ |
| POST | `/structured/sentiment` | 结构化输出-情感分析 | ✅ |

### 5.3 Agent 接口 `/api/v1/agent` — @Tag: Agent工具调用

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/chat` | 工具调用对话（无记忆，5个工具全开） | ✅ |
| POST | `/chat/memory` | 工具调用对话（带记忆） | ✅ |
| POST | `/chat/specific` | 指定工具对话（只启用指定工具） | ✅ |

### 5.4 知识库接口 `/api/v1/knowledge` — @Tag: 知识库管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/upload` | 上传文档（.txt/.md） | ADMIN |
| POST | `/load-directory` | 批量加载目录下所有文档 | ADMIN |
| GET | `/documents` | 已入库文档列表 | USER+ |
| DELETE | `/documents/{fileName}` | 删除指定文档 | USER+ |
| POST | `/ask` | RAG 问答（自动模式，QuestionAnswerAdvisor） | ✅ |
| POST | `/ask/manual` | RAG 问答（手动模式，手动拼接上下文） | ✅ |
| GET | `/search?query=xxx` | 纯检索（调试用，不生成回答） | ✅ |
| GET | `/status` | 知识库状态 | ✅ |

### 5.5 规划接口 `/api/v1/planning` — @Tag: 任务规划

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| POST | `/execute` | 规划并执行（autoExecute=true） | ✅ |
| POST | `/plan-only` | 仅规划不执行（autoExecute=false） | ✅ |
| POST | `/chat` | 全能 Agent 统一入口（FullAgent） | ✅ |

### 5.6 流式接口 `/api/v1/stream` — @Tag: 流式对话

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/chat` | 流式对话（SSE，无工具） | ❌ |
| GET | `/chat/tools` | 流式对话（SSE，带工具） | ❌ |

### 5.7 会话接口 `/api/v1/session` — @Tag: 会话管理

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/{conversationId}/history` | 获取会话历史消息 | ✅ |
| DELETE | `/{conversationId}` | 清除会话记忆 | ✅ |

### 5.8 监控接口 `/api/v1/monitor` — @Tag: 系统监控

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|:----:|
| GET | `/stats` | 系统统计（Token用量/成本估算） | ADMIN |
| GET | `/history` | 对话历史列表 | ADMIN |
| GET | `/conversation/{id}` | 会话详情 | ADMIN |
| GET | `/sessions/{username}` | 用户会话列表 | ADMIN |

---

## 六、数据流与依赖关系

### 6.1 请求处理链路

```
HTTP Request
    │
    ▼
CorsFilter（跨域处理）
    │
    ▼
RateLimitInterceptor（滑动窗口限流，30次/分钟）
    │  ├── Redis 可用 → Redis Sorted Set 分布式限流
    │  └── Redis 不可用 → ConcurrentHashMap 内存限流
    │
    ▼
JwtAuthenticationFilter（JWT认证）
    │  ├── 提取 Authorization: Bearer <token>
    │  ├── JwtTokenProvider.validateToken() 验证
    │  └── 设置 SecurityContext（username + role）
    │
    ▼
Controller → Agent → [ChatClient + Advisor + Tool]
    │
    ▼
Spring AI → AI Model API (DeepSeek / DashScope)
    │
    ▼
统一响应体 R<T> → JSON → HTTP Response
    │
    ▼（异常时）
GlobalExceptionHandler 捕获 → R.modelError() / R.fail() / R.paramError()
```

### 6.2 ChatClient 依赖注入关系

```
ChatClient.Builder (Spring AI 自动提供，基于 application.yml 中的 openai 配置)
    │
    ├── baseChatClient = Builder + 默认系统提示词字符串
    │
    ├── memoryChatClient = Builder + chat-system.st + MessageChatMemoryAdvisor
    │
    ├── toolChatClient = Builder + tool-agent-system.st + MessageChatMemoryAdvisor
    │
    ├── ragChatClient = Builder + rag-system.st + MessageChatMemoryAdvisor
    │                       + QuestionAnswerAdvisor(VectorStore, topK=5, threshold=0.3)
    │
    └── fullAgentClient = Builder + full-agent-system.st + MessageChatMemoryAdvisor
```

### 6.3 AI 模型调用关系

```
┌──────────────────────────────────────────────────────────┐
│                    Chat 模型                               │
│  DeepSeek V4 Flash                                        │
│  Base URL: https://api.deepseek.com                       │
│  环境变量: DEEPSEEK_API_KEY                                │
│  用途: 所有对话、工具调用、结构化输出、任务规划、流式对话     │
│  注意: 需关闭思考模式(extra-body.thinking.type: disabled)   │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                  Embedding 模型                            │
│  DashScope text-embedding-v4                              │
│  Base URL: https://dashscope.aliyuncs.com/compatible-mode │
│  环境变量: DASHSCOPE_API_KEY                               │
│  用途: 文档向量化、查询向量化、相似度检索                    │
│  接入: EmbeddingConfig.java 手动注册 Bean                   │
│  注意: embeddingsPath="/embeddings" 避免路径重复            │
└──────────────────────────────────────────────────────────┘
```

### 6.4 记忆存储架构

```
┌──────────────────────────────────────────────────────────┐
│              当前：InMemoryChatMemoryRepository            │
│  存储: JVM 堆内存 ConcurrentHashMap                       │
│  隔离: 按 conversationId                                  │
│  容量: 每个会话最多 100 条消息                              │
│  持久化: ❌ 重启丢失                                       │
└──────────────────────────────────────────────────────────┘
                        │
                        ▼ 升级路径
┌──────────────────────────────────────────────────────────┐
│              未来：RedisChatMemoryRepository               │
│  存储: Redis String（JSON序列化）                          │
│  Key: chat:memory:{conversationId}                        │
│  TTL: 24小时自动过期                                       │
│  持久化: ✅ Redis 持久化                                   │
└──────────────────────────────────────────────────────────┘
```

---

## 七、配置体系

### 7.1 主配置文件 application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: smart-agent
  profiles:
    active: dev
  ai:
    openai:
      base-url: ${AI_BASE_URL:https://api.deepseek.com}
      api-key: ${Deepseek_API_KEY:your-api-key-here}
      chat:
        options:
          model: ${AI_MODEL:deepseek-chat}
          temperature: 0.7
          max-tokens: 4096
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

knife4j:
  enable: true
  setting:
    language: zh_cn
    swagger-model-name: 实体类列表

logging:
  level:
    com.chat.myAgent: debug
    org.springframework.ai: debug
```

### 7.2 外部服务配置

| 配置项 | 环境变量 | 说明 |
|--------|---------|------|
| `spring.ai.openai.base-url` | `AI_BASE_URL` | DeepSeek API 地址 |
| `spring.ai.openai.api-key` | `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `spring.ai.openai.chat.options.model` | `AI_MODEL` | Chat 模型名称 |
| `dashscope.api-key` | `DASHSCOPE_API_KEY` | 阿里云 DashScope API Key |
| `dashscope.base-url` | - | DashScope API 地址 |
| `dashscope.embedding-model` | - | Embedding 模型名称 |

### 7.3 RAG 配置

| 配置项 | 默认值 | 说明 |
|--------|-------|------|
| `smart-agent.rag.vector-store-path` | `./data/vectorstore/vector-store.json` | 向量库持久化路径 |
| `smart-agent.rag.chunk-size` | 800 | 文档切片大小（tokens） |
| `smart-agent.rag.chunk-overlap` | 100 | 切片重叠大小 |
| `smart-agent.rag.top-k` | 5 | 检索返回的 Top-K 片段数 |
| `smart-agent.rag.similarity-threshold` | 0.5 | 相似度阈值 |

### 7.4 其他配置

| 配置项 | 默认值 | 说明 |
|--------|-------|------|
| `smart-agent.jwt.secret` | 内置默认值 | JWT 签名密钥 |
| `smart-agent.jwt.expiration-hours` | 24 | Token 有效期 |
| `smart-agent.rate-limit.max-requests-per-minute` | 30 | API 限流阈值 |
| `smart-agent.models.default-model` | deepseek-chat | 默认模型 |
| `smart-agent.models.available` | - | 可用模型映射 |

---

## 八、安全体系

### 8.1 认证方式

- **JWT 无状态认证**，不使用 Session
- Token 通过 `Authorization: Bearer <token>` 请求头传递
- 密码使用 BCrypt 加密存储
- Token 有效期默认 24 小时

### 8.2 权限矩阵

| 角色 | 可访问接口 |
|------|-----------|
| 未认证 | `/auth/**`, `/chat/ping`, `/stream/**`, 静态资源, Knife4j文档 |
| USER | 对话接口、Agent 接口、知识库问答、会话管理、文档列表 |
| ADMIN | 所有接口 + 文档上传/加载/删除 + 监控管理 |

### 8.3 JWT 认证流程

```
1. 用户注册/登录 → AuthController
2. 验证用户名密码 → AuthenticationManager
3. 生成 JWT Token → JwtTokenProvider.generateToken(username, role)
4. 返回 Token 给前端 → AuthResponse(token, username, role)
5. 后续请求携带 Token → Authorization: Bearer <token>
6. JwtAuthenticationFilter 拦截 → 验证Token → 设置SecurityContext
```

---

## 九、异常处理体系

### 9.1 统一响应体

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-05-01T10:30:00"
}
```

### 9.2 错误码定义

| 错误码 | 含义 | 触发场景 |
|-------|------|---------|
| 200 | 成功 | 正常响应 |
| 400 | 参数错误 | 请求参数校验失败 |
| 401 | 未认证 | Token 缺失或无效 |
| 429 | 限流 | 请求超过30次/分钟 |
| 500 | 服务器错误 | 未知异常 |
| 503 | 模型服务异常 | AI API 调用失败 |

### 9.3 GlobalExceptionHandler 处理的异常

| 异常类型 | 处理方式 |
|---------|---------|
| `BizException` | 返回自定义错误码和消息 |
| `NonTransientAiException` | 返回 503 + 模型服务异常 |
| `MethodArgumentNotValidException` | 返回 400 + 参数不合法 |
| `Exception`（兜底） | 返回 500 + 服务器内部错误 |

---

## 十、开发阶段演进

| 阶段 | 核心能力 | 对应 Agent/模块 | 关键技术 |
|------|---------|----------------|---------|
| 阶段1 | 基础对话 | ChatAgent.simpleChat() | ChatClient + baseChatClient |
| 阶段2 | 记忆 + Prompt模板 + 结构化输出 | ChatAgent.chat() / StructuredAgent | MessageChatMemoryAdvisor + PromptTemplate + BeanOutputConverter |
| 阶段3 | 工具调用（Function Calling） | ToolAgent | @Tool注解 + .tools() + .entity() |
| 阶段4 | RAG 知识库问答 | RagAgent + DocumentService + RetrievalService | QuestionAnswerAdvisor + VectorStore + TokenTextSplitter |
| 阶段5 | 任务规划 + 全能力Agent | PlanningAgent + FullAgent | 规划JSON解析 + 逐步执行 + 上下文累积 |

---

## 十一、关键设计决策

### 11.1 为什么 Chat 和 Embedding 使用不同模型？

DeepSeek V4 Flash 不提供 Embedding API，因此选择阿里云 DashScope 的 text-embedding-v4 作为 Embedding 模型。两者通过 OpenAI 兼容协议统一接入，EmbeddingConfig 手动注册 Bean 并设置 `embeddingsPath="/embeddings"` 避免路径重复。

### 11.2 为什么使用 SimpleVectorStore？

开发阶段使用 SimpleVectorStore（基于内存 + JSON 文件持久化），无需安装额外数据库。后续可无缝切换到 Redis VectorStore / Milvus / PGVector。

### 11.3 为什么关闭 DeepSeek 的思考模式？

DeepSeek V4 系列默认开启思考模式，会返回 `reasoning_content` 字段。Spring AI 的 ChatMemory 不支持自动处理该字段，导致后续请求报错。通过 `extra-body.thinking.type: disabled` 关闭。

### 11.4 为什么 TranslateTool 使用"AI 调用 AI"模式？

避免引入第三方翻译 API 依赖，直接利用大模型自身的多语言能力。如需更高质量翻译，可替换为专业翻译 API。

### 11.5 为什么使用 Knife4j 替代原生 Swagger UI？

Knife4j 提供了更友好的中文界面、更好的离线文档支持、更丰富的接口调试功能。基于 OpenAPI3 规范，完全兼容 Spring Doc，所有 Controller 通过 `@Tag` + `@Operation` + `@Parameter` 注解提供完整的接口文档。

### 11.6 为什么 RAG 相似度阈值在代码中设为 0.3？

不同 Embedding 模型的相似度分布不同。阿里云 DashScope 的 text-embedding-v4 模型相似度分数普遍偏低（0.3-0.5），如果阈值设为 0.5 会导致正确结果被过滤。同时配合较小的分片大小（300 tokens），使每个片段语义更聚焦，检索更精准。

### 11.7 为什么限流拦截器使用双模式？

优先使用 Redis（分布式限流，支持多实例部署），Redis 不可用时自动回退到内存限流（ConcurrentHashMap + ConcurrentLinkedDeque 滑动窗口），保证系统在 Redis 故障时仍可正常运行。

### 11.8 为什么 PlanningAgent 有回退机制？

AI 模型返回的规划 JSON 可能格式不规范（如被 markdown 代码块包裹），`cleanJsonResponse()` 方法会尝试清理。如果仍然解析失败，`fallbackDirectExecution()` 会回退到 FullAgent 直接执行，保证用户始终能得到回答。
