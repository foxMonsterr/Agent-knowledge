# LearnAgent 后续开发任务清单

> 基于 `docs/changeV1.md` 的升级方案整理。
>
> 目标：将现有 SmartAgent 重构为面向个人学习的知识库 Agent，围绕 `ReAct 推理链路 + 个人知识库 + 学习闭环 + React 前端体验` 形成完整闭环。
>
> 说明：本文档重点整理后续开发任务、接口清单、请求/响应格式与数据库结构，便于进入研发拆解、排期与实施。

---

## 1. 总体开发目标

### 1.1 当前阶段目标

当前项目不再继续扩展“通用型智能体平台”能力，而是收敛为一个明确产品：

- 以**个人学习知识库**为核心
- 以**ReAct 推理过程可视化**为主入口
- 以**笔记沉淀 / 测验生成 / 闪卡复习 / 学习统计**形成闭环
- 以**知识图谱**展示知识之间的关联
- 以**React 风格的组件化前端交互**增强体验

### 1.2 开发优先级原则

优先级从高到低：

1. **ReAct 主链路可用**
2. **知识笔记可沉淀**
3. **测验与闪卡可生成**
4. **学习数据可追踪**
5. **知识图谱可展示**
6. **前端交互体验完整化**

---

## 2. 总体任务分解

### 2.1 任务域划分

```
LearnAgent
├── T1 ReAct 推理引擎
├── T2 个人知识库
├── T3 学习辅助工具
├── T4 学习计划与任务
├── T5 闪卡与测验
├── T6 学习记录与掌握度
├── T7 知识图谱与关联发现
├── T8 认证与权限重构
├── T9 前端页面与组件
└── T10 配置、监控与部署适配
```

---

## 3. 里程碑计划

### 3.1 Milestone 1：ReAct 核心链路（最高优先级）

目标：先让系统具备“可以看见推理过程的学习对话能力”。

**包含任务**：
- 新建 LearnAgent 主入口
- ReAct 状态机设计
- 推理链路事件模型
- SSE 流式输出重构
- 推理链路持久化
- 用户中途干预
- 回放查询接口

### 3.2 Milestone 2：知识库与笔记沉淀

目标：让每次学习都能落到知识资产上。

**包含任务**：
- 笔记 CRUD
- 导入资料
- 自动摘要
- 语义检索
- 关联推荐
- 标签与分类体系

### 3.3 Milestone 3：测验与闪卡闭环

目标：从“学会”走向“记住”。

**包含任务**：
- 测验生成
- 答案评估
- 闪卡生成
- 闪卡复习
- SM-2 间隔重复
- 费曼检验

### 3.4 Milestone 4：学习记录与分析

目标：让系统真正知道用户学到了什么。

**包含任务**：
- 学习行为记录
- 掌握度模型
- 薄弱点分析
- 复习提醒
- 学习统计概览

### 3.5 Milestone 5：知识图谱与前端体验

目标：让知识结构和学习过程可视化。

**包含任务**：
- 知识图谱数据生成
- 图谱可视化
- ReAct 交互链路展示
- 闪卡/测验/笔记等页面联动
- 首页学习仪表盘

---

## 4. 后端开发任务清单

---

### 4.1 ReAct 推理引擎

#### 4.1.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-REA-001 | 新建 `LearnAgent` 主入口 | P0 | 替换当前 `FullAgent` 作为学习型 Agent 统一入口 |
| BE-REA-002 | 设计 ReAct 状态机 | P0 | 支持 `idle/thinking/acting/observing/summarizing/finalizing/completed/failed` |
| BE-REA-003 | 实现 ReAct 循环执行器 | P0 | `Thought -> Action -> Observation -> Final Answer` 迭代 |
| BE-REA-004 | 实现工具选择与解析器 | P0 | 解析模型输出的 Action，并调用对应工具 |
| BE-REA-005 | 实现 SSE 推理事件流 | P0 | 将每一步状态实时推送到前端 |
| BE-REA-006 | 实现推理链路持久化 | P0 | 保存到 MongoDB，支持回放 |
| BE-REA-007 | 实现用户中途干预 | P1 | 支持向当前推理链注入补充信息 |
| BE-REA-008 | 实现推理中断机制 | P1 | 支持用户强制停止当前推理并汇总已有上下文 |
| BE-REA-009 | 实现模型切换标记 | P1 | 主模型/兜底模型切换时在推理链中可见 |

#### 4.1.2 建议新增类

- `agent/LearnAgent.java`
- `agent/ReActEngine.java`
- `service/ReactService.java`
- `service/impl/ReactServiceImpl.java`
- `repository/mongo/ReActTraceRepository.java`
- `model/mongo/ReActTraceDocument.java`
- `model/mongo/ReActStepDocument.java`
- `model/dto/ReactChatRequest.java`
- `model/dto/InterveneRequest.java`
- `model/vo/ReactChatResponse.java`
- `model/vo/ReactStepVO.java`

#### 4.1.3 ReAct 流程建议

1. 接收用户问题
2. 初始化 trace
3. 读取用户上下文、知识库、学习状态
4. 进入推理循环
5. 逐步输出 Thought / Action / Observation
6. 必要时调用知识工具、测验工具、学习工具
7. 输出最终回答
8. 自动沉淀笔记、记录学习行为

---

### 4.2 个人知识库

#### 4.2.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-NOTE-001 | 新建知识笔记实体 | P0 | 作为个人学习知识资产主表 |
| BE-NOTE-002 | 实现笔记 CRUD | P0 | 支持创建、查询、更新、删除 |
| BE-NOTE-003 | 实现笔记标签与分类 | P0 | 支持标签筛选、分类浏览 |
| BE-NOTE-004 | 实现资料导入 | P0 | 支持 txt/md/pdf 导入 |
| BE-NOTE-005 | 实现自动摘要 | P1 | 导入或创建笔记后自动生成摘要 |
| BE-NOTE-006 | 实现语义检索 | P0 | 与向量库联动 |
| BE-NOTE-007 | 实现关联笔记推荐 | P1 | 基于向量相似度和引用关系 |
| BE-NOTE-008 | 实现笔记归档 | P1 | 区分活跃学习与归档内容 |

#### 4.2.2 建议新增类

- `model/mongo/KnowledgeNoteDocument.java`
- `model/dto/NoteCreateRequest.java`
- `model/dto/NoteUpdateRequest.java`
- `model/dto/NoteSearchRequest.java`
- `model/vo/NoteVO.java`
- `repository/mongo/KnowledgeNoteRepository.java`
- `service/NoteService.java`
- `service/impl/NoteServiceImpl.java`
- `controller/NoteController.java`

---

### 4.3 学习辅助工具

#### 4.3.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-TOOL-001 | 新建 `KnowledgeTool` | P0 | 管理笔记检索、创建、更新、关联、删除 |
| BE-TOOL-002 | 新建 `QuizTool` | P0 | 负责测验生成、评估答案、费曼检验 |
| BE-TOOL-003 | 新建 `LearningTool` | P0 | 负责学习进度、薄弱点、提醒 |
| BE-TOOL-004 | 新建 `WebSearchTool` | P1 | 辅助联网搜索和资料摘要 |
| BE-TOOL-005 | 工具接入 ReAct 引擎 | P0 | 所有工具可被 ReAct 选择调用 |

#### 4.3.2 工具函数建议

**KnowledgeTool**
- `searchNotes`
- `createNote`
- `updateNote`
- `findRelated`
- `deleteNote`
- `getNoteSummary`

**QuizTool**
- `generateQuiz`
- `evaluateAnswer`
- `generateFlashcard`
- `evaluateExplanation`

**LearningTool**
- `getProgress`
- `recordStudy`
- `analyzeWeakness`
- `getReviewReminders`

---

### 4.4 学习计划与任务

#### 4.4.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-PLAN-001 | 学习计划实体建模 | P1 | 支持学习主题、目标、步骤、进度 |
| BE-PLAN-002 | 学习计划生成 | P1 | 根据主题/笔记/薄弱点自动拆解任务 |
| BE-PLAN-003 | 学习计划进度更新 | P1 | 支持标记步骤完成与整体进度 |
| BE-PLAN-004 | 学习计划列表与详情 | P1 | 提供计划管理能力 |

#### 4.4.2 建议新增类

- `model/mongo/LearningPlanDocument.java`
- `model/dto/PlanGenerateRequest.java`
- `model/dto/PlanProgressRequest.java`
- `model/vo/LearningPlanVO.java`
- `controller/PlanController.java`
- `service/PlanService.java`
- `service/impl/PlanServiceImpl.java`

---

### 4.5 测验与闪卡

#### 4.5.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-QUIZ-001 | 测验实体建模 | P0 | 保存生成的题目与答案 |
| BE-QUIZ-002 | 测验生成 | P0 | 结合笔记内容生成题目 |
| BE-QUIZ-003 | 答案评估 | P0 | 支持自动判分与反馈 |
| BE-QUIZ-004 | 测验历史记录 | P1 | 支持历史答题查询 |
| BE-FLASH-001 | 闪卡实体建模 | P0 | 保存正反面内容与复习节奏 |
| BE-FLASH-002 | 闪卡生成 | P0 | 从笔记提炼关键知识点 |
| BE-FLASH-003 | 闪卡复习提交 | P0 | 处理 quality 评分 |
| BE-FLASH-004 | 间隔重复算法 | P0 | 支持 SM-2 变种调度 |
| BE-FLASH-005 | 今日待复习列表 | P1 | 按 nextReviewAt 查询 |

#### 4.5.2 建议新增类

- `model/mongo/QuizDocument.java`
- `model/mongo/FlashcardDocument.java`
- `model/dto/QuizGenerateRequest.java`
- `model/dto/QuizEvaluateRequest.java`
- `model/dto/FlashcardReviewRequest.java`
- `model/vo/QuizVO.java`
- `model/vo/FlashcardVO.java`
- `service/QuizService.java`
- `service/FlashcardService.java`
- `service/impl/QuizServiceImpl.java`
- `service/impl/FlashcardServiceImpl.java`
- `controller/QuizController.java`
- `controller/FlashcardController.java`

---

### 4.6 学习记录与掌握度

#### 4.6.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-STUDY-001 | 学习记录表建模 | P0 | 记录对话、测验、复习、导入等行为 |
| BE-STUDY-002 | 自动记录学习行为 | P0 | 对接 ReAct、测验、闪卡、笔记操作 |
| BE-STUDY-003 | 掌握度模型计算 | P1 | 综合学习行为生成掌握度 |
| BE-STUDY-004 | 薄弱点分析 | P1 | 找出低掌握主题 |
| BE-STUDY-005 | 学习提醒查询 | P1 | 今日待复习与待学习清单 |
| BE-STUDY-006 | 学习统计汇总 | P1 | 今日/本周/总览统计 |

#### 4.6.2 建议新增类

- `model/entity/StudyRecordEntity.java`
- `repository/StudyRecordRepository.java`
- `model/vo/LearningProgressVO.java`
- `model/vo/LearningStatsVO.java`
- `controller/StudyController.java`
- `service/StudyService.java`
- `service/impl/StudyServiceImpl.java`

---

### 4.7 知识图谱

#### 4.7.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-GRAPH-001 | 图谱节点/边模型 | P1 | 笔记、标签、主题、引用关系 |
| BE-GRAPH-002 | 图谱数据生成 | P1 | 基于笔记和向量相似度生成图谱 |
| BE-GRAPH-003 | 局部图谱查询 | P1 | 查询某个笔记周边关联图 |
| BE-GRAPH-004 | 图谱刷新 | P1 | 重新计算边权重与节点关系 |

#### 4.7.2 建议新增类

- `model/vo/GraphDataVO.java`
- `model/vo/GraphNodeVO.java`
- `model/vo/GraphEdgeVO.java`
- `controller/GraphController.java`
- `service/GraphService.java`
- `service/impl/GraphServiceImpl.java`

---

### 4.8 认证与权限重构

#### 4.8.1 核心任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| BE-AUTH-001 | 统一角色命名 | P0 | `USER/ADMIN` 或 `LEARNER/ADMIN` 二选一并统一 |
| BE-AUTH-002 | 权限矩阵调整 | P0 | 增加 `/api/v1/learn/**` 规则 |
| BE-AUTH-003 | 前端菜单权限更新 | P0 | 菜单与新产品定位一致 |
| BE-AUTH-004 | 用户信息接口适配 | P1 | 返回学习概览与角色信息 |

#### 4.8.2 建议修改文件

- `config/SecurityConfig.java`
- `controller/PermissionController.java`
- `front/src/store/modules/permission.ts`
- `front/src/router/index.ts`

---

## 5. 接口清单与数据格式整理

> 以下接口分为：认证、ReAct、知识库、笔记、测验、闪卡、学习记录、图谱、计划、系统管理。
>
> 所有请求/响应建议统一使用 `R<T>` 包装，字段风格尽量保持一致。

---

### 5.1 统一响应体

#### 5.1.1 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

#### 5.1.2 通用失败格式

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null,
  "timestamp": 1710000000000
}
```

---

### 5.2 认证接口

#### 5.2.1 用户注册

- **方法**：`POST`
- **路径**：`/api/v1/auth/register`

**请求体**
```json
{
  "username": "alice",
  "password": "123456",
  "role": "USER"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "jwt-token",
    "username": "alice",
    "role": "USER"
  },
  "timestamp": 1710000000000
}
```

#### 5.2.2 用户登录

- **方法**：`POST`
- **路径**：`/api/v1/auth/login`

**请求体**
```json
{
  "username": "alice",
  "password": "123456"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "jwt-token",
    "username": "alice",
    "role": "USER"
  },
  "timestamp": 1710000000000
}
```

#### 5.2.3 初始化管理员

- **方法**：`POST`
- **路径**：`/api/v1/auth/init-admin`

**请求体**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "管理员初始化成功",
  "data": {
    "username": "admin",
    "role": "ADMIN"
  },
  "timestamp": 1710000000000
}
```

---

### 5.3 ReAct 接口

#### 5.3.1 ReAct 同步对话

- **方法**：`POST`
- **路径**：`/api/v1/learn/react/chat`

**请求体**
```json
{
  "conversationId": "conv-001",
  "message": "请讲解梯度下降和反向传播的关系",
  "strategy": "auto",
  "autoCreateNote": true
}
```

**请求字段说明**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversationId | string | 否 | 会话 ID，空则后端自动生成 |
| message | string | 是 | 用户输入 |
| strategy | string | 否 | `auto/retrieval_first/generation_first/quiz_first` |
| autoCreateNote | boolean | 否 | 是否自动沉淀为笔记 |

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "traceId": "trace-001",
    "conversationId": "conv-001",
    "finalAnswer": "梯度下降是...",
    "modelName": "deepseek-chat",
    "status": "completed",
    "steps": [
      {
        "stepNumber": 1,
        "stepType": "thought",
        "content": "我需要先找到梯度下降的基础资料",
        "toolName": null,
        "toolInput": null,
        "toolOutput": null,
        "latencyMs": 1200,
        "status": "success"
      }
    ],
    "sources": ["note-001", "note-005"],
    "totalIterations": 3,
    "totalTimeMs": 5820
  },
  "timestamp": 1710000000000
}
```

#### 5.3.2 ReAct 流式对话

- **方法**：`GET`
- **路径**：`/api/v1/learn/react/chat/stream`
- **响应类型**：`text/event-stream`

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 用户问题 |
| conversationId | string | 否 | 会话 ID |
| strategy | string | 否 | 推理策略 |

**SSE 事件格式**

```json
{"type":"start","traceId":"trace-001","conversationId":"conv-001"}
```

```json
{"type":"thought","stepNumber":1,"content":"我需要先检索相关笔记"}
```

```json
{"type":"action","stepNumber":1,"toolName":"searchNotes","toolInput":"{\"query\":\"梯度下降\"}"}
```

```json
{"type":"observation","stepNumber":1,"content":"检索到 3 条相关笔记"}
```

```json
{"type":"final_answer","content":"梯度下降是...","sources":["note-001"]}
```

```json
{"type":"done","traceId":"trace-001"}
```

```json
{"type":"error","content":"工具执行失败：xxx"}
```

#### 5.3.3 获取推理链路详情

- **方法**：`GET`
- **路径**：`/api/v1/learn/react/traces/{traceId}`

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "traceId": "trace-001",
    "conversationId": "conv-001",
    "question": "请讲解梯度下降和反向传播的关系",
    "finalAnswer": "梯度下降是...",
    "steps": [],
    "modelName": "deepseek-chat",
    "totalIterations": 3,
    "totalTimeMs": 5820,
    "createdAt": "2026-06-01T10:00:00"
  },
  "timestamp": 1710000000000
}
```

#### 5.3.4 获取会话推理历史

- **方法**：`GET`
- **路径**：`/api/v1/learn/react/sessions/{conversationId}`

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "traceId": "trace-001",
      "question": "请讲解梯度下降...",
      "finalAnswer": "梯度下降是...",
      "createdAt": "2026-06-01T10:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

#### 5.3.5 中途干预

- **方法**：`POST`
- **路径**：`/api/v1/learn/react/intervene`

**请求体**
```json
{
  "traceId": "trace-001",
  "stepNumber": 2,
  "message": "我更想知道 SGD 和 Adam 的区别"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "补充信息已注入推理链路",
  "data": {
    "traceId": "trace-001",
    "stepNumber": 2,
    "status": "accepted"
  },
  "timestamp": 1710000000000
}
```

#### 5.3.6 中断推理

- **方法**：`POST`
- **路径**：`/api/v1/learn/react/stop`

**请求体**
```json
{
  "traceId": "trace-001"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "推理已中断",
  "data": {
    "traceId": "trace-001",
    "status": "interrupted"
  },
  "timestamp": 1710000000000
}
```

---

### 5.4 知识笔记接口

#### 5.4.1 创建笔记

- **方法**：`POST`
- **路径**：`/api/v1/learn/notes`

**请求体**
```json
{
  "title": "梯度下降优化器对比",
  "content": "## SGD\n...",
  "tags": ["深度学习", "优化器"],
  "category": "人工智能",
  "source": "user_created"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "noteId": "note-001",
    "title": "梯度下降优化器对比",
    "summary": "对比了 SGD、Adam、RMSprop 的特点",
    "tags": ["深度学习", "优化器"],
    "category": "人工智能",
    "masteryLevel": 0,
    "source": "user_created",
    "createdAt": "2026-06-01T10:00:00"
  },
  "timestamp": 1710000000000
}
```

#### 5.4.2 笔记列表

- **方法**：`GET`
- **路径**：`/api/v1/learn/notes`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页数量，默认 10 |
| keyword | string | 否 | 标题/内容关键词 |
| tag | string | 否 | 标签筛选 |
| category | string | 否 | 分类筛选 |
| archived | boolean | 否 | 是否只看归档 |

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "page": 1,
    "size": 10,
    "total": 42,
    "items": [
      {
        "noteId": "note-001",
        "title": "梯度下降优化器对比",
        "summary": "对比了 SGD、Adam、RMSprop 的特点",
        "tags": ["深度学习", "优化器"],
        "category": "人工智能",
        "masteryLevel": 45,
        "updatedAt": "2026-06-01T10:00:00"
      }
    ]
  },
  "timestamp": 1710000000000
}
```

#### 5.4.3 笔记详情

- **方法**：`GET`
- **路径**：`/api/v1/learn/notes/{noteId}`

#### 5.4.4 更新笔记

- **方法**：`PUT`
- **路径**：`/api/v1/learn/notes/{noteId}`

**请求体**
```json
{
  "title": "梯度下降优化器对比（修订版）",
  "content": "## SGD\n...",
  "tags": ["深度学习", "优化器", "复习"],
  "category": "人工智能",
  "archived": false
}
```

#### 5.4.5 删除笔记

- **方法**：`DELETE`
- **路径**：`/api/v1/learn/notes/{noteId}`

#### 5.4.6 语义检索

- **方法**：`GET`
- **路径**：`/api/v1/learn/notes/search`

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| query | string | 是 | 检索内容 |
| topK | number | 否 | 返回条数，默认 5 |
| threshold | number | 否 | 相似度阈值 |

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "noteId": "note-001",
      "title": "梯度下降优化器对比",
      "snippet": "SGD 是最基础的优化器...",
      "score": 0.87,
      "tags": ["深度学习", "优化器"]
    }
  ],
  "timestamp": 1710000000000
}
```

#### 5.4.7 关联笔记

- **方法**：`GET`
- **路径**：`/api/v1/learn/notes/{noteId}/related`

#### 5.4.8 更新标签

- **方法**：`PUT`
- **路径**：`/api/v1/learn/notes/{noteId}/tags`

**请求体**
```json
{
  "tags": ["深度学习", "优化器", "高频考点"]
}
```

#### 5.4.9 更新掌握度

- **方法**：`PUT`
- **路径**：`/api/v1/learn/notes/{noteId}/mastery`

**请求体**
```json
{
  "masteryLevel": 72,
  "reason": "测验正确率高，闪卡复习连续通过"
}
```

---

### 5.5 文档导入接口

#### 5.5.1 上传资料导入

- **方法**：`POST`
- **路径**：`/api/v1/learn/notes/import`
- **类型**：`multipart/form-data`

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | file | 是 | 支持 txt/md/pdf |
| autoSummary | boolean | 否 | 是否自动摘要 |
| autoTag | boolean | 否 | 是否自动打标签 |

**响应体**
```json
{
  "code": 200,
  "message": "导入成功",
  "data": {
    "noteId": "note-002",
    "fileName": "deep-learning-notes.md",
    "chunkCount": 12,
    "summary": "该文档主要介绍了..."
  },
  "timestamp": 1710000000000
}
```

#### 5.5.2 批量导入目录

- **方法**：`POST`
- **路径**：`/api/v1/learn/notes/import/batch`

**请求体**
```json
{
  "dirPath": "src/main/resources/knowledge",
  "includeExtensions": ["md", "txt", "pdf"]
}
```

---

### 5.6 测验接口

#### 5.6.1 生成测验

- **方法**：`POST`
- **路径**：`/api/v1/learn/quiz/generate`

**请求体**
```json
{
  "noteId": "note-001",
  "count": 5,
  "difficulty": "medium",
  "types": ["CHOICE", "TRUE_FALSE", "SHORT_ANSWER"]
}
```

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "quizSetId": "qs-001",
    "noteId": "note-001",
    "quizzes": [
      {
        "quizId": "quiz-001",
        "question": "以下哪个优化器使用了动量（Momentum）？",
        "type": "CHOICE",
        "options": ["SGD without momentum", "Adam", "RMSprop", "以上都不对"],
        "difficulty": "medium",
        "tags": ["深度学习", "优化器"]
      }
    ],
    "totalCount": 5
  },
  "timestamp": 1710000000000
}
```

#### 5.6.2 评估答案

- **方法**：`POST`
- **路径**：`/api/v1/learn/quiz/evaluate`

**请求体**
```json
{
  "quizId": "quiz-001",
  "userAnswer": "Adam",
  "noteId": "note-001"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "quizId": "quiz-001",
    "correct": true,
    "score": 100,
    "explanation": "Adam 在 SGD 基础上引入了动量和自适应学习率...",
    "masteryDelta": 8
  },
  "timestamp": 1710000000000
}
```

#### 5.6.3 测验列表

- **方法**：`GET`
- **路径**：`/api/v1/learn/quiz/list`

#### 5.6.4 测验详情

- **方法**：`GET`
- **路径**：`/api/v1/learn/quiz/{quizId}`

---

### 5.7 闪卡接口

#### 5.7.1 根据笔记生成闪卡

- **方法**：`POST`
- **路径**：`/api/v1/learn/flashcard/generate`

**请求体**
```json
{
  "noteId": "note-001",
  "count": 10,
  "style": "anki"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "noteId": "note-001",
    "generated": 10,
    "cards": [
      {
        "cardId": "card-001",
        "front": "什么是梯度下降？",
        "back": "一种通过最小化损失函数来更新参数的优化方法...",
        "tags": ["深度学习", "优化器"],
        "interval": 1,
        "easeFactor": 2.5
      }
    ]
  },
  "timestamp": 1710000000000
}
```

#### 5.7.2 今日待复习

- **方法**：`GET`
- **路径**：`/api/v1/learn/flashcard/review`

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "count": 12,
    "items": []
  },
  "timestamp": 1710000000000
}
```

#### 5.7.3 提交复习结果

- **方法**：`POST`
- **路径**：`/api/v1/learn/flashcard/{cardId}/review`

**请求体**
```json
{
  "quality": 4,
  "reviewedAt": "2026-06-01T10:00:00"
}
```

**响应体**
```json
{
  "code": 200,
  "message": "复习记录已保存",
  "data": {
    "cardId": "card-001",
    "nextReviewAt": "2026-06-07T10:00:00",
    "interval": 6,
    "easeFactor": 2.6
  },
  "timestamp": 1710000000000
}
```

#### 5.7.4 闪卡列表

- **方法**：`GET`
- **路径**：`/api/v1/learn/flashcard/list`

#### 5.7.5 删除闪卡

- **方法**：`DELETE`
- **路径**：`/api/v1/learn/flashcard/{cardId}`

---

### 5.8 费曼检验接口

#### 5.8.1 费曼检验

- **方法**：`POST`
- **路径**：`/api/v1/learn/quiz/evaluate-explanation`

**请求体**
```json
{
  "noteId": "note-001",
  "userExplanation": "梯度下降就是不断往损失更小的方向更新参数..."
}
```

**响应体**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "coverage": 0.78,
    "accuracy": 0.84,
    "missedPoints": ["学习率衰减", "局部最优问题"],
    "misconceptions": [],
    "feedback": "你的解释整体正确，但缺少对学习率和局部最优的说明。",
    "suggestedMastery": 76
  },
  "timestamp": 1710000000000
}
```

---

### 5.9 学习记录与统计接口

#### 5.9.1 记录学习行为

- **方法**：`POST`
- **路径**：`/api/v1/learn/study/record`

**请求体**
```json
{
  "activityType": "CHAT",
  "topic": "梯度下降",
  "noteId": "note-001",
  "durationSeconds": 320,
  "score": 0,
  "detail": {
    "conversationId": "conv-001",
    "traceId": "trace-001"
  }
}
```

#### 5.9.2 学习总览

- **方法**：`GET`
- **路径**：`/api/v1/learn/study/progress`

#### 5.9.3 薄弱点分析

- **方法**：`GET`
- **路径**：`/api/v1/learn/study/weakness`

#### 5.9.4 复习提醒

- **方法**：`GET`
- **路径**：`/api/v1/learn/study/reminders`

#### 5.9.5 每日统计

- **方法**：`GET`
- **路径**：`/api/v1/learn/stats/daily`

#### 5.9.6 每周统计

- **方法**：`GET`
- **路径**：`/api/v1/learn/stats/weekly`

#### 5.9.7 学习总览

- **方法**：`GET`
- **路径**：`/api/v1/learn/stats/overview`

---

### 5.10 知识图谱接口

#### 5.10.1 获取完整图谱

- **方法**：`GET`
- **路径**：`/api/v1/learn/graph`

#### 5.10.2 获取局部图谱

- **方法**：`GET`
- **路径**：`/api/v1/learn/graph/around/{noteId}`

#### 5.10.3 刷新图谱

- **方法**：`POST`
- **路径**：`/api/v1/learn/graph/refresh`

**请求体**
```json
{
  "threshold": 0.3,
  "maxNodes": 200
}
```

---

## 6. 数据库设计清单

---

### 6.1 MySQL / JPA 表设计

#### 6.1.1 用户表 `user_entity`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| username | varchar(64) | 用户名，唯一 |
| password | varchar(255) | BCrypt 密码 |
| role | varchar(32) | 角色 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### 6.1.2 对话历史表 `chat_history_entity`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| conversation_id | varchar(64) | 会话 ID |
| username | varchar(64) | 用户名 |
| message_role | varchar(16) | user / assistant |
| content | text | 消息内容 |
| agent_type | varchar(64) | agent 类型 |
| model | varchar(64) | 模型名 |
| tokens | int | token 数 |
| latency_ms | bigint | 耗时 |
| created_at | datetime | 创建时间 |

#### 6.1.3 Agent 调用记录表 `agent_invocation_entity`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| invocation_id | varchar(64) | 调用 ID |
| conversation_id | varchar(64) | 会话 ID |
| agent_type | varchar(64) | agent 类型 |
| model | varchar(64) | 模型名 |
| trace_id | varchar(64) | 追踪 ID |
| input_text | text | 输入 |
| output_text | text | 输出 |
| thinking_text | text | 思考内容 |
| status | varchar(32) | 状态 |
| latency_ms | bigint | 耗时 |
| created_at | datetime | 创建时间 |

#### 6.1.4 学习记录表 `study_record`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint auto increment | 主键 |
| user_id | varchar(64) | 用户 ID |
| note_id | varchar(64) | 笔记 ID，可空 |
| activity_type | varchar(32) | CHAT/QUIZ/REVIEW/FEYNMAN/NOTE_CREATE |
| topic | varchar(200) | 学习主题 |
| duration_seconds | int | 时长 |
| score | int | 得分 |
| detail | text | 扩展 JSON |
| created_at | datetime | 创建时间 |

**索引建议**
- `idx_user_id(user_id)`
- `idx_activity_type(activity_type)`
- `idx_created_at(created_at)`
- `idx_topic(topic)`

---

### 6.2 MongoDB 集合设计

#### 6.2.1 会话集合 `chat_session_document`

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |
| userId | string | 用户 ID |
| title | string | 会话标题 |
| summary | string | 会话摘要 |
| status | string | active / archived / deleted |
| lastMessageAt | datetime | 最近消息时间 |

#### 6.2.2 消息集合 `chat_message_document`

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |
| userId | string | 用户 ID |
| role | string | user / assistant / system |
| content | string | 消息内容 |
| createdAt | datetime | 创建时间 |

#### 6.2.3 知识笔记集合 `knowledge_notes`

| 字段 | 类型 | 说明 |
|------|------|------|
| noteId | string | 笔记 ID |
| userId | string | 所属用户 |
| title | string | 标题 |
| content | string | 内容 |
| summary | string | 摘要 |
| tags | array<string> | 标签 |
| category | string | 分类 |
| source | string | 来源 |
| sourceDocName | string | 来源文档名 |
| relatedNoteIds | array<string> | 关联笔记 |
| masteryLevel | int | 掌握度 |
| reviewCount | int | 复习次数 |
| nextReviewAt | datetime | 下次复习时间 |
| archived | boolean | 是否归档 |
| createdAt | datetime | 创建时间 |
| updatedAt | datetime | 更新时间 |

**索引建议**
- `noteId` 唯一索引
- `userId` 普通索引
- `tags` 多键索引
- `category` 索引
- `masteryLevel` 索引

#### 6.2.4 测验集合 `quizzes`

| 字段 | 类型 | 说明 |
|------|------|------|
| quizId | string | 测验 ID |
| quizSetId | string | 题组 ID |
| userId | string | 用户 ID |
| noteId | string | 关联笔记 |
| question | string | 题目 |
| type | string | CHOICE / TRUE_FALSE / SHORT_ANSWER |
| options | array<string> | 选项 |
| correctAnswer | string | 正确答案 |
| explanation | string | 解析 |
| difficulty | string | easy / medium / hard |
| tags | array<string> | 标签 |
| createdAt | datetime | 创建时间 |

#### 6.2.5 闪卡集合 `flashcards`

| 字段 | 类型 | 说明 |
|------|------|------|
| cardId | string | 闪卡 ID |
| userId | string | 用户 ID |
| noteId | string | 关联笔记 |
| front | string | 正面 |
| back | string | 背面 |
| tags | array<string> | 标签 |
| reviewCount | int | 已复习次数 |
| easeFactor | double | 难度因子 |
| interval | int | 复习间隔（天） |
| nextReviewAt | datetime | 下次复习时间 |
| lastReviewAt | datetime | 上次复习时间 |
| createdAt | datetime | 创建时间 |

#### 6.2.6 ReAct 推理链集合 `react_traces`

| 字段 | 类型 | 说明 |
|------|------|------|
| traceId | string | Trace ID |
| conversationId | string | 会话 ID |
| userId | string | 用户 ID |
| question | string | 原始问题 |
| finalAnswer | string | 最终答案 |
| modelName | string | 模型名 |
| status | string | completed / failed / interrupted |
| totalIterations | int | 总轮数 |
| totalTimeMs | long | 总耗时 |
| steps | array<object> | 推理步骤 |
| createdAt | datetime | 创建时间 |

#### 6.2.7 学习计划集合 `learning_plans`

| 字段 | 类型 | 说明 |
|------|------|------|
| planId | string | 计划 ID |
| userId | string | 用户 ID |
| title | string | 计划标题 |
| topic | string | 学习主题 |
| goal | string | 学习目标 |
| steps | array<object> | 计划步骤 |
| progress | int | 进度百分比 |
| dueDate | datetime | 截止时间 |
| status | string | not_started / active / paused / completed |
| createdAt | datetime | 创建时间 |
| updatedAt | datetime | 更新时间 |

---

### 6.3 Qdrant / 向量库设计

#### 6.3.1 向量集合建议

| 名称 | 说明 |
|------|------|
| `knowledge_note_chunks` | 笔记切片向量 |
| `document_chunks` | 导入文档切片向量 |
| `quiz_embeddings` | 题目/题干向量 |
| `flashcard_embeddings` | 闪卡内容向量 |

#### 6.3.2 切片元数据建议

| 字段 | 说明 |
|------|------|
| noteId | 所属笔记 ID |
| sourceType | note / import / auto_generated |
| sourceName | 来源名称 |
| chunkIndex | 切片序号 |
| totalChunks | 总切片数 |
| tagList | 标签列表 |
| category | 分类 |
| userId | 用户 ID |

---

## 7. 前端开发任务清单

### 7.1 页面级任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| FE-PAGE-001 | 重构首页为学习仪表盘 | P0 | 展示学习时长、待复习、掌握度 |
| FE-PAGE-002 | 重构 `/chat` 为 ReAct 对话页 | P0 | 核心页面 |
| FE-PAGE-003 | 新增笔记管理页 | P0 | 列表、编辑、详情、搜索 |
| FE-PAGE-004 | 新增测验页 | P1 | 题目生成与答题 |
| FE-PAGE-005 | 新增闪卡页 | P1 | 复习与翻卡交互 |
| FE-PAGE-006 | 新增图谱页 | P1 | 知识图谱可视化 |
| FE-PAGE-007 | 新增学习统计页 | P1 | 趋势图、热力图、雷达图 |
| FE-PAGE-008 | 新增学习计划页 | P2 | 任务拆解与进度 |

### 7.2 组件级任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| FE-CMP-001 | ReActChainPanel | P0 | 推理链路面板 |
| FE-CMP-002 | ReactStepCard | P0 | 单个推理步骤卡片 |
| FE-CMP-003 | KnowledgeGraph | P1 | 图谱画布 |
| FE-CMP-004 | FlashcardFlip | P1 | 闪卡翻页交互 |
| FE-CMP-005 | MasteryRadar | P1 | 掌握度雷达图 |
| FE-CMP-006 | NoteEditor | P0 | 笔记编辑器 |
| FE-CMP-007 | QuizRenderer | P1 | 题目渲染组件 |

### 7.3 前端 API 任务

| 任务编号 | 任务名称 | 优先级 | 说明 |
|---------|---------|--------|------|
| FE-API-001 | 新增 learn API 聚合层 | P0 | 聚合学习相关接口 |
| FE-API-002 | 新增 note API | P0 | 笔记 CRUD 与检索 |
| FE-API-003 | 新增 quiz API | P1 | 测验生成与评估 |
| FE-API-004 | 新增 flashcard API | P1 | 闪卡生成与复习 |
| FE-API-005 | 新增 graph API | P1 | 图谱接口 |
| FE-API-006 | 新增 study API | P1 | 统计与进度 |
| FE-API-007 | 新增 react stream composable | P0 | ReAct SSE 流 |

---

## 8. 接口与数据库的对照关系

### 8.1 接口依赖数据表/集合

| 接口组 | 依赖表/集合 |
|--------|--------------|
| `/api/v1/auth/*` | `user_entity` |
| `/api/v1/learn/react/*` | `react_traces`, `chat_history_entity`, `chat_session_document` |
| `/api/v1/learn/notes/*` | `knowledge_notes`, `document_chunks`, `knowledge_note_chunks` |
| `/api/v1/learn/quiz/*` | `quizzes`, `quiz_embeddings` |
| `/api/v1/learn/flashcard/*` | `flashcards`, `flashcard_embeddings` |
| `/api/v1/learn/study/*` | `study_record`, `knowledge_notes`, `flashcards`, `quizzes` |
| `/api/v1/learn/graph/*` | `knowledge_notes`, `knowledge_note_chunks` |
| `/api/v1/learn/plans/*` | `learning_plans`, `study_record` |

---

## 9. 建议的接口请求/响应规范

### 9.1 统一请求规范

建议所有写操作统一遵循：

- 参数名使用语义化命名
- 时间字段统一使用 ISO-8601 字符串
- 列表查询支持分页参数 `page/size`
- 检索接口支持 `query/topK/threshold`
- 复杂接口支持 `traceId/conversationId` 贯穿链路

### 9.2 统一响应规范

所有业务接口尽量返回以下结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

如果是分页结果：

```json
{
  "page": 1,
  "size": 10,
  "total": 100,
  "items": []
}
```

如果是流式 SSE：

- 使用 `type` 标记事件类型
- 使用 `content` 表达当前内容
- 使用 `traceId` / `conversationId` 串联上下文

---

## 10. 推荐实施顺序

### 10.1 第一阶段

1. 定义 `LearnAgent` 主入口
2. 完成 ReAct 状态机
3. 打通 SSE 流式推理
4. 新建推理链路数据模型
5. 完成推理链路落库

### 10.2 第二阶段

1. 笔记实体与 CRUD
2. 导入与切片
3. 语义检索
4. 自动摘要与标签
5. 笔记关联推荐

### 10.3 第三阶段

1. 测验生成
2. 答案评估
3. 闪卡生成
4. 闪卡复习
5. 掌握度初版

### 10.4 第四阶段

1. 学习记录
2. 学习统计
3. 薄弱点分析
4. 复习提醒
5. 学习计划

### 10.5 第五阶段

1. 知识图谱
2. 前端重构
3. 交互组件优化
4. 回放与导出
5. 体验打磨

---

## 11. 风险与注意事项

### 11.1 技术风险

- ReAct 输出格式不稳定，解析器要有容错
- SSE 推流中断要有恢复策略
- 向量检索与笔记同步要保证一致性
- 学习掌握度模型初期可先简单实现，避免过度复杂

### 11.2 产品风险

- 如果笔记与练习闭环不完整，产品会退化成普通聊天机器人
- 如果推理链路不可视，ReAct 就没有产品价值
- 如果前端只是列表页堆叠，无法体现“学习助手”主题

---

## 12. 最终交付物建议

建议下一步研发阶段至少交付以下内容：

- `LearnAgent` 主对话链路
- `react_traces` 推理链路存储
- 个人知识笔记 CRUD
- 问答自动生成笔记
- 测验与闪卡闭环
- 学习记录与掌握度基础统计
- React 风格的推理可视化界面

---

## 13. 结论

这个任务清单的目标不是把项目做大，而是把项目做“完整”：

- 输入有入口
- 推理有过程
- 学习有沉淀
- 练习有反馈
- 复习有节奏
- 知识有结构
- 前端有表达

当这些都连起来之后，LearnAgent 才能从一个功能型 Agent 框架，真正变成一个个人学习知识库产品。
