# LearnAgent 详细功能需求与模块规格书

> 版本：v1.0 | 日期：2026-05-25 | 状态：待开发
>
> 本文档为 LearnAgent 项目的完整功能需求规格，覆盖 ReAct 推理链路可视化、学习场景专用工具、知识图谱化沉淀三大方向，所有需求均按可上线部署标准编写。

---

## 一、产品定义

### 1.1 产品定位

| 项目 | 说明 |
|------|------|
| **产品名称** | LearnAgent — 个人学习知识库 Agent |
| **一句话描述** | 基于 ReAct 推理模式的个人学习知识库助手，先思考再行动，推理过程全程可见 |
| **目标用户** | 学生、自学者、知识工作者 |
| **核心价值** | 推理透明（看到 AI 怎么想）+ 知识沉淀（学过的自动存）+ 学习闭环（测-学-练一体） |

### 1.2 用户角色

| 角色 | 权限范围 |
|------|---------|
| **学习者**（默认角色） | 知识笔记 CRUD、ReAct 对话、自测练习、闪卡复习、查看个人学习数据 |
| **管理员**（系统初始化时创建） | 用户管理、系统监控、全局知识库管理 |

### 1.3 核心用户旅程

```
注册/登录 → 创建学习主题 → 导入学习资料（文档/手动笔记）
    → 与 Agent 对话学习（ReAct 推理链路可见）
    → Agent 自动沉淀知识笔记 → 生成闪卡/测验
    → 间隔复习 → 薄弱点分析 → 查看知识图谱 → 继续深入学习
```

---

## 二、功能模块总览

```
LearnAgent
├── M1  ReAct 推理引擎（核心）
├── M2  知识笔记管理
├── M3  学习辅助工具集
├── M4  学习追踪与分析
├── M5  知识图谱
├── M6  用户与认证
├── M7  系统管理
└── M8  前端交互界面
```

---

## 三、模块详细需求

---

### M1：ReAct 推理引擎

#### M1.1 模块概述

ReAct 推理引擎是 LearnAgent 的核心，替代当前的黑盒式 ChatClient 调用。用户提问后，Agent 进入 Thought → Action → Observation 迭代循环，每一步的推理过程实时推送到前端可视化展示。

#### M1.2 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M1-F01 | ReAct 循环执行 | P0 | Thought → Action → Observation 迭代，最大 8 轮 |
| M1-F02 | 推理链路实时推送 | P0 | SSE 逐步推送每步 Thought/Action/Observation |
| M1-F03 | 推理链路持久化 | P1 | 完整记录 ReActTrace 到 MongoDB，支持回放 |
| M1-F04 | 用户中途干预 | P1 | 用户可在任意 Thought 步骤插入补充信息 |
| M1-F05 | 推理策略选择 | P2 | 根据问题类型自动选择策略（检索优先/生成优先/测验优先） |
| M1-F06 | 推理链路回放 | P2 | 历史对话的推理过程可完整回放 |

#### M1-F01：ReAct 循环执行

**输入**：用户问题（String）、会话ID（String）、策略模式（Enum，可选）

**输出**：最终回答 + 完整推理链路（List<ReActStep>）

**处理流程**：

```
1. 接收用户问题，初始化 ReActContext
2. 将用户问题 + 系统提示词 + 工具描述列表组装为初始 Prompt
3. 进入循环（maxIterations = 8）：
   a. 调用 LLM 生成回复
   b. 解析回复，判断是否包含 Action:
      - 包含 Action → 提取工具名和参数 → 执行工具 → 得到 Observation → 拼接上下文 → 继续循环
      - 包含 Final Answer → 提取最终回答 → 退出循环
      - 格式异常 → 尝试修复，修复失败则将原始回复作为最终回答
4. 返回最终回答 + 所有步骤记录
```

**ReAct Prompt 模板**：

```
你是一个个人学习助手 LearnAgent。你通过 ReAct（Reasoning + Acting）模式帮助用户学习和理解知识。

你可以使用以下工具：
{tool_descriptions}

请严格按照以下格式思考和行动：

Question: 用户的问题
Thought: 你的推理过程（分析用户需要什么信息，下一步该做什么）
Action: 工具名称(参数)
Observation: 工具返回的结果

...（可以多轮 Thought → Action → Observation）

Thought: 我现在有足够的信息来回答用户的问题了
Final Answer: 最终回答

规则：
1. 每次只执行一个 Action
2. 必须基于 Observation 来决定下一步，不要凭空猜测
3. 如果知识库中没有相关信息，主动创建新笔记保存到知识库
4. 优先检索已有知识，避免重复生成
5. 如果用户的问题涉及多个知识点，逐步检索和整理
6. 在 Final Answer 中引用你参考的知识来源
```

**异常处理**：

| 异常场景 | 处理方式 |
|---------|---------|
| LLM 返回格式不符合 ReAct 规范 | 尝试正则提取 Action/Final Answer，失败则将原始文本作为直接回答 |
| 工具执行超时（>10s） | 返回 Observation: "工具执行超时"，继续推理 |
| 工具执行异常 | 返回 Observation: "工具执行失败: {错误信息}"，继续推理 |
| 达到最大迭代次数 | 强制终止，将当前累积上下文发送给 LLM 生成最终回答 |
| 主模型不可用 | 切换兜底模型（Qwen3.6-Plus），在推理链路中标记模型切换 |

**数据模型**：

```java
// ReActTrace - 推理链路记录
public class ReActTrace {
    private String traceId;           // 链路唯一ID
    private String conversationId;    // 会话ID
    private String userId;            // 用户ID
    private String question;          // 用户原始问题
    private List<ReActStep> steps;    // 推理步骤列表
    private String finalAnswer;       // 最终回答
    private String model;             // 使用的模型
    private int totalIterations;      // 实际迭代次数
    private long totalTimeMs;         // 总耗时
    private LocalDateTime createdAt;  // 创建时间
}

// ReActStep - 单步推理记录
public class ReActStep {
    private int stepNumber;           // 步骤序号（从1开始）
    private String thought;           // 推理文本
    private String actionName;        // 工具名称（null 表示无 Action）
    private String actionInput;       // 工具输入参数 JSON
    private String observation;       // 工具返回结果
    private long stepTimeMs;          // 本步耗时
    private boolean isFinalStep;      // 是否为最终步骤
}
```

**API 接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/learn/react/chat` | ReAct 对话（同步，返回完整推理链路） |
| GET | `/api/v1/learn/react/chat/stream` | ReAct 对话（SSE 流式，逐步推送） |
| GET | `/api/v1/learn/react/trace/{traceId}` | 获取推理链路详情 |
| GET | `/api/v1/learn/react/trace/list` | 获取用户推理链路列表 |
| POST | `/api/v1/learn/react/intervene` | 用户中途干预（注入补充信息） |

**SSE 事件格式**：

```json
// Thought 事件
{"type": "thought", "stepNumber": 1, "content": "用户问的是梯度下降，我需要先检索知识库", "timestamp": "..."}

// Action 事件
{"type": "action", "stepNumber": 1, "toolName": "search_notes", "toolInput": "{\"query\":\"梯度下降\"}", "timestamp": "..."}

// Observation 事件
{"type": "observation", "stepNumber": 1, "content": "找到3条相关笔记...", "timestamp": "..."}

// Final Answer 事件
{"type": "final_answer", "content": "梯度下降是...", "sources": ["note-001", "note-005"], "timestamp": "..."}

// 错误事件
{"type": "error", "content": "工具执行失败: ...", "timestamp": "..."}
```

---

#### M1-F02：推理链路实时推送

**技术方案**：

后端 ReActEngine 每完成一个步骤，立即通过 SseEmitter（Spring MVC）或 Flux（WebFlux）推送事件到前端。前端通过 `useSseStream` composable 接收并渲染。

**推送时机**：

| 事件 | 推送时机 | 数据 |
|------|---------|------|
| `thought` | LLM 返回 Thought 文本后 | stepNumber, thought 内容 |
| `action` | 解析出 Action 后、执行工具前 | stepNumber, 工具名, 工具参数 |
| `observation` | 工具执行完成后 | stepNumber, observation 内容 |
| `final_answer` | 生成最终回答后 | 最终回答, 引用来源 |
| `error` | 任何步骤异常时 | 错误信息 |

---

#### M1-F04：用户中途干预

**交互方式**：前端推理链路面板中，每个 Thought 步骤旁显示"补充信息"按钮。用户点击后弹出输入框，输入补充信息后发送到后端。

**后端处理**：将用户补充信息作为额外上下文注入到 ReAct 循环的下一步 Prompt 中：

```
[用户补充信息]: {用户输入的补充内容}

请基于以上补充信息继续推理。
```

**API**：

```
POST /api/v1/learn/react/intervene
{
  "traceId": "trace-xxx",
  "stepNumber": 2,
  "message": "我主要想了解 SGD 和 Adam 的区别"
}
```

---

### M2：知识笔记管理

#### M2.1 模块概述

知识笔记是 LearnAgent 的数据核心。用户可手动创建笔记，也可由 Agent 在 ReAct 推理过程中自动创建。笔记支持标签分类、语义检索、关联发现，同时自动向量化存入 Qdrant。

#### M2.2 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M2-F01 | 笔记 CRUD | P0 | 创建/读取/更新/删除知识笔记 |
| M2-F02 | 笔记标签管理 | P0 | 为笔记添加/移除标签，按标签筛选 |
| M2-F03 | 文档导入与切片 | P0 | 上传 PDF/Markdown/TXT，自动切片向量化 |
| M2-F04 | 语义检索 | P0 | 基于向量相似度的知识检索 |
| M2-F05 | Agent 自动创建笔记 | P0 | ReAct 推理过程中 Agent 可调用 create_note 工具 |
| M2-F06 | 笔记关联发现 | P1 | 自动发现笔记间的语义关联 |
| M2-F07 | 笔记版本历史 | P2 | 记录笔记修改历史，支持回溯 |

#### M2-F01：笔记 CRUD

**数据模型**：

```java
// KnowledgeNote - 知识笔记（MongoDB 文档）
public class KnowledgeNote {
    private String noteId;              // 笔记唯一ID（自动生成，格式：note-{timestamp}-{random}）
    private String userId;              // 所属用户ID
    private String title;               // 笔记标题（必填，最大200字）
    private String content;             // 笔记内容（必填，Markdown格式，最大50000字）
    private String summary;             // 笔记摘要（自动生成，最大500字）
    private List<String> tags;          // 标签列表（如 ["深度学习","优化器"]）
    private String category;            // 分类（如 "人工智能"、"编程语言"）
    private String source;              // 来源：user_created / imported / agent_generated
    private String sourceDocName;       // 来源文档名（imported 时有值）
    private List<String> relatedNoteIds;// 关联笔记ID列表
    private int masteryLevel;           // 掌握度 0-100（默认0）
    private int reviewCount;            // 复习次数
    private LocalDateTime nextReviewAt;  // 下次复习时间（间隔重复）
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
```

**API 接口**：

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/v1/learn/notes` | 创建笔记 | 学习者 |
| GET | `/api/v1/learn/notes` | 笔记列表（支持分页、标签筛选、关键词搜索） | 学习者 |
| GET | `/api/v1/learn/notes/{noteId}` | 笔记详情 | 学习者 |
| PUT | `/api/v1/learn/notes/{noteId}` | 更新笔记 | 学习者 |
| DELETE | `/api/v1/learn/notes/{noteId}` | 删除笔记 | 学习者 |
| GET | `/api/v1/learn/notes/search` | 语义检索笔记 | 学习者 |
| GET | `/api/v1/learn/notes/{noteId}/related` | 获取关联笔记 | 学习者 |
| PUT | `/api/v1/learn/notes/{noteId}/tags` | 更新笔记标签 | 学习者 |
| PUT | `/api/v1/learn/notes/{noteId}/mastery` | 更新掌握度 | 学习者 |

**请求/响应示例**：

```
POST /api/v1/learn/notes
请求：
{
  "title": "梯度下降优化器对比",
  "content": "## SGD\n最基础的优化器...\n\n## Adam\n自适应学习率...",
  "tags": ["深度学习", "优化器"],
  "category": "人工智能"
}

响应：
{
  "code": 200,
  "data": {
    "noteId": "note-1748150400-a3f8c2",
    "title": "梯度下降优化器对比",
    "summary": "对比了 SGD、Adam、RMSprop 三种优化器的原理和适用场景",
    "tags": ["深度学习", "优化器"],
    "category": "人工智能",
    "source": "user_created",
    "masteryLevel": 0,
    "createdAt": "2026-05-25T10:00:00"
  }
}
```

---

#### M2-F03：文档导入与切片

**支持的文件格式**：`.txt`、`.md`、`.pdf`

**切片策略**：

| 参数 | 默认值 | 说明 |
|------|-------|------|
| chunkSize | 800 | 每个切片的最大 token 数 |
| chunkOverlap | 100 | 切片之间的重叠 token 数 |
| minChunkSize | 50 | 过小的切片合并到前一个 |

**导入流程**：

```
1. 接收上传文件
2. 根据文件类型选择 Reader：
   - .txt/.md → TextReader
   - .pdf → PagePdfDocumentReader
3. TokenTextSplitter 切片
4. 为每个切片添加 metadata：
   - source: 文件名
   - noteId: 关联的笔记ID
   - chunk_index: 切片序号
   - total_chunks: 总切片数
5. VectorStore.add(chunks) → 自动向量化
6. 创建 KnowledgeNote（source=imported）
7. 持久化向量库
```

**API 接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/learn/notes/import` | 上传文件导入（multipart/form-data） |
| POST | `/api/v1/learn/notes/import/batch` | 批量导入目录下所有文件 |

---

#### M2-F05：Agent 自动创建笔记

**触发条件**：ReAct 推理过程中，Agent 判断知识库中缺少某知识点时，调用 `create_note` 工具自动创建。

**工具定义**：

```java
@Component
public class KnowledgeTool {

    @Tool(description = "在知识库中创建一条新笔记。当发现用户需要的知识点在知识库中不存在时使用。")
    public String createNote(
        @ToolParam(description = "笔记标题") String title,
        @ToolParam(description = "笔记内容，使用Markdown格式") String content,
        @ToolParam(description = "标签列表，逗号分隔，如 '深度学习,优化器'") String tags) {
        // 调用 NoteService.createNote()
        // 返回创建结果
    }

    @Tool(description = "检索知识库中的笔记。输入自然语言查询，返回最相关的笔记片段。")
    public String searchNotes(
        @ToolParam(description = "检索查询，自然语言描述") String query,
        @ToolParam(description = "返回结果数量，默认5") int topK) {
        // 调用 RetrievalService.retrieve()
        // 格式化返回
    }

    @Tool(description = "更新已有笔记的内容")
    public String updateNote(
        @ToolParam(description = "笔记ID") String noteId,
        @ToolParam(description = "新的内容") String content) {
        // 调用 NoteService.updateNote()
    }

    @Tool(description = "查找与指定笔记相关的其他笔记")
    public String findRelated(
        @ToolParam(description = "笔记ID") String noteId,
        @ToolParam(description = "返回数量，默认5") int topK) {
        // 基于向量相似度查找关联笔记
    }

    @Tool(description = "删除知识库中的笔记")
    public String deleteNote(
        @ToolParam(description = "笔记ID") String noteId) {
        // 调用 NoteService.deleteNote()
    }
}
```

---

### M3：学习辅助工具集

#### M3.1 模块概述

学习辅助工具集是 ReAct 引擎可调用的专用工具，同时也是用户可独立使用的功能模块。包含测验生成、闪卡管理、费曼检验三大核心学习工具。

#### M3.2 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M3-F01 | 测验生成 | P0 | 根据知识笔记自动生成选择题/判断题/简答题 |
| M3-F02 | 答案评估 | P0 | 评估用户答案的正确性和完整度 |
| M3-F03 | 闪卡生成 | P0 | 自动生成 Anki 风格闪卡（正面问题/背面答案） |
| M3-F04 | 间隔重复复习 | P0 | 基于艾宾浩斯遗忘曲线的复习调度 |
| M3-F05 | 费曼检验 | P1 | 用户用自己的话解释概念，Agent 评估理解程度 |
| M3-F06 | 学习计划生成 | P2 | 根据知识图谱和薄弱点生成学习路径 |

#### M3-F01：测验生成

**数据模型**：

```java
// Quiz - 测验题（MongoDB 文档）
public class Quiz {
    private String quizId;              // 测验ID
    private String userId;              // 所属用户
    private String noteId;              // 关联笔记ID
    private String question;            // 题目文本
    private QuizType type;              // 题型：CHOICE / TRUE_FALSE / SHORT_ANSWER
    private List<String> options;       // 选项（选择题时有值，4个选项）
    private String correctAnswer;       // 正确答案
    private String explanation;         // 答案解析
    private String difficulty;          // 难度：easy / medium / hard
    private List<String> tags;          // 知识点标签
    private LocalDateTime createdAt;    // 创建时间
}

public enum QuizType {
    CHOICE,        // 选择题
    TRUE_FALSE,    // 判断题
    SHORT_ANSWER   // 简答题
}
```

**生成规则**：

| 规则 | 说明 |
|------|------|
| 题目来源 | 基于指定笔记的内容生成，确保题目与笔记内容一致 |
| 题型分布 | 默认 60% 选择题 + 20% 判断题 + 20% 简答题 |
| 难度控制 | 根据 masteryLevel 调整：低掌握度 → easy 为主；高掌握度 → hard 为主 |
| 数量 | 默认生成 5 题，可指定 1-20 题 |
| 去重 | 同一笔记同一难度级别不生成重复题目（基于语义相似度判断） |

**API 接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/learn/quiz/generate` | 生成测验题 |
| POST | `/api/v1/learn/quiz/evaluate` | 评估用户答案 |
| GET | `/api/v1/learn/quiz/list` | 获取测验题列表（按笔记/标签筛选） |
| GET | `/api/v1/learn/quiz/{quizId}` | 获取测验题详情 |

**请求/响应示例**：

```
POST /api/v1/learn/quiz/generate
请求：
{
  "noteId": "note-1748150400-a3f8c2",
  "count": 5,
  "difficulty": "medium",
  "types": ["CHOICE", "TRUE_FALSE", "SHORT_ANSWER"]
}

响应：
{
  "code": 200,
  "data": {
    "quizSetId": "qs-1748150500-b7d1e4",
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
  }
}
```

---

#### M3-F03：闪卡生成

**数据模型**：

```java
// Flashcard - 闪卡（MongoDB 文档）
public class Flashcard {
    private String cardId;              // 闪卡ID
    private String userId;              // 所属用户
    private String noteId;              // 关联笔记ID
    private String front;               // 正面（问题/关键词）
    private String back;                // 背面（答案/解释）
    private List<String> tags;          // 标签
    private int reviewCount;            // 已复习次数
    private double easeFactor;          // 难度因子（SM-2算法，默认2.5）
    private int interval;               // 复习间隔（天）
    private LocalDateTime nextReviewAt;  // 下次复习时间
    private LocalDateTime lastReviewAt;  // 上次复习时间
    private LocalDateTime createdAt;    // 创建时间
}
```

**间隔重复算法（SM-2 变种）**：

```
输入：用户对闪卡的评分 quality（0-5）
  5 = 完全记住
  4 = 犹豫后记住
  3 = 勉强记住
  2 = 记住但费劲
  1 = 忘了但眼熟
  0 = 完全不记得

处理：
  if quality >= 3:
    if reviewCount == 0: interval = 1天
    else if reviewCount == 1: interval = 6天
    else: interval = round(interval * easeFactor)
    easeFactor = max(1.3, easeFactor + 0.1 - (5-quality) * (0.08 + (5-quality)*0.02))
  else:
    reviewCount = 0
    interval = 1天

输出：下次复习时间 = 当前时间 + interval天
```

**API 接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/learn/flashcard/generate` | 根据笔记生成闪卡 |
| GET | `/api/v1/learn/flashcard/review` | 获取今日待复习闪卡 |
| POST | `/api/v1/learn/flashcard/{cardId}/review` | 提交闪卡复习结果（quality 0-5） |
| GET | `/api/v1/learn/flashcard/list` | 闪卡列表（按标签/笔记筛选） |
| DELETE | `/api/v1/learn/flashcard/{cardId}` | 删除闪卡 |

---

#### M3-F05：费曼检验

**交互流程**：

```
1. 用户选择一个知识主题/笔记
2. 系统提示用户："请用自己的话解释 [主题]"
3. 用户输入解释文本
4. Agent 调用 evaluate_explanation 工具
5. 工具内部调用 LLM，基于原始笔记内容评估用户解释：
   - 覆盖度：用户解释覆盖了原笔记多少关键知识点（0-100%）
   - 准确度：解释是否准确（0-100%）
   - 遗漏点：列出用户遗漏的关键概念
   - 误解点：列出用户理解错误的地方
6. 返回评估结果，同时更新笔记的 masteryLevel
```

**评估结果数据模型**：

```java
public class FeynmanEvaluation {
    private String noteId;              // 关联笔记
    private double coverage;            // 覆盖度 0-1
    private double accuracy;            // 准确度 0-1
    private List<String> missedPoints;  // 遗漏的关键概念
    private List<String> misconceptions;// 误解的概念
    private String feedback;            // AI 反馈建议
    private int suggestedMastery;       // 建议掌握度 0-100
}
```

**工具定义**：

```java
@Component
public class QuizTool {

    @Tool(description = "生成测验题。根据指定笔记内容生成选择题、判断题或简答题。")
    public String generateQuiz(
        @ToolParam(description = "笔记ID或知识主题") String noteIdOrTopic,
        @ToolParam(description = "题目数量，默认5") int count,
        @ToolParam(description = "难度：easy/medium/hard") String difficulty) { ... }

    @Tool(description = "评估用户对测验题的回答")
    public String evaluateAnswer(
        @ToolParam(description = "测验题ID") String quizId,
        @ToolParam(description = "用户的回答") String userAnswer) { ... }

    @Tool(description = "根据笔记内容生成闪卡，正面是问题/关键词，背面是答案/解释")
    public String generateFlashcard(
        @ToolParam(description = "笔记ID") String noteId) { ... }

    @Tool(description = "费曼检验：评估用户用自己的话对某个概念的解释。返回覆盖度、准确度、遗漏点和误解点。")
    public String evaluateExplanation(
        @ToolParam(description = "笔记ID或知识主题") String noteIdOrTopic,
        @ToolParam(description = "用户的解释文本") String userExplanation) { ... }
}
```

---

### M4：学习追踪与分析

#### M4.1 模块概述

自动记录用户的学习行为，分析学习数据，识别薄弱点，提供可视化学习报告。

#### M4.2 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M4-F01 | 学习行为记录 | P0 | 自动记录对话、测验、复习等学习行为 |
| M4-F02 | 学习进度查询 | P0 | 按主题/标签查询掌握度 |
| M4-F03 | 薄弱点分析 | P1 | 自动识别掌握度低的知识点 |
| M4-F04 | 复习提醒 | P1 | 基于间隔重复的待复习提醒 |
| M4-F05 | 学习统计仪表盘 | P1 | 每日/每周学习数据可视化 |
| M4-F06 | 学习报告导出 | P2 | 导出学习报告为 PDF/Markdown |

#### M4-F01：学习行为记录

**数据模型**：

```java
// StudyRecord - 学习记录（MySQL JPA 实体）
public class StudyRecord {
    private Long id;                    // 自增主键
    private String userId;              // 用户ID
    private String noteId;              // 关联笔记ID（可选）
    private String activityType;        // 活动类型：CHAT / QUIZ / REVIEW / FEYNMAN / NOTE_CREATE
    private String topic;               // 学习主题/标签
    private int durationSeconds;        // 学习时长（秒）
    private int score;                  // 得分（测验时有值，0-100）
    private String detail;              // 详细信息 JSON
    private LocalDateTime createdAt;    // 记录时间
}
```

**自动记录时机**：

| 活动类型 | 触发时机 | 记录内容 |
|---------|---------|---------|
| CHAT | ReAct 对话完成 | 问题摘要、对话轮数、耗时 |
| QUIZ | 测验答案提交 | 题目ID、正确/错误、得分 |
| REVIEW | 闪卡复习提交 | 卡片ID、quality评分 |
| FEYNMAN | 费曼检验完成 | 笔记ID、覆盖度、准确度 |
| NOTE_CREATE | 笔记创建 | 笔记ID、来源 |

**工具定义**：

```java
@Component
public class LearningTool {

    @Tool(description = "查询用户在某个主题或标签下的学习进度和掌握度")
    public String getProgress(
        @ToolParam(description = "主题或标签，如 '深度学习'") String topic) { ... }

    @Tool(description = "记录一次学习行为")
    public String recordStudy(
        @ToolParam(description = "活动类型：CHAT/QUIZ/REVIEW/FEYNMAN") String activityType,
        @ToolParam(description = "学习主题") String topic,
        @ToolParam(description = "学习时长（秒）") int durationSeconds,
        @ToolParam(description = "得分（0-100），无得分传0") int score) { ... }

    @Tool(description = "分析用户的知识薄弱点，返回掌握度最低的知识主题")
    public String analyzeWeakness(
        @ToolParam(description = "分析范围，如 '人工智能'，为空则分析全部") String scope) { ... }

    @Tool(description = "获取用户今日待复习的闪卡列表和数量")
    public String getReviewReminders() { ... }
}
```

**API 接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/learn/progress` | 学习进度总览 |
| GET | `/api/v1/learn/progress/by-topic` | 按主题查询进度 |
| GET | `/api/v1/learn/progress/weakness` | 薄弱点列表 |
| GET | `/api/v1/learn/stats/daily` | 每日学习统计 |
| GET | `/api/v1/learn/stats/weekly` | 每周学习统计 |
| GET | `/api/v1/learn/stats/overview` | 学习总览（笔记数/测验数/复习数/总时长） |
| GET | `/api/v1/learn/review/reminders` | 今日待复习项 |
| POST | `/api/v1/learn/study/record` | 手动记录学习行为 |

---

### M5：知识图谱

#### M5.1 模块概述

基于笔记间的语义关联和标签关系，构建知识图谱，提供可视化展示和图谱导航。

#### M5.2 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M5-F01 | 图谱数据生成 | P1 | 基于笔记关联和标签关系生成图谱数据 |
| M5-F02 | 图谱可视化 | P1 | 力导向图展示知识网络 |
| M5-F03 | 图谱导航 | P1 | 点击节点查看笔记详情，展开关联 |
| M5-F04 | 图谱筛选 | P2 | 按标签/分类/掌握度筛选显示 |

#### M5-F01：图谱数据生成

**数据模型**：

```java
// GraphData - 图谱数据
public class GraphData {
    private List<GraphNode> nodes;      // 节点列表
    private List<GraphEdge> edges;      // 边列表
}

// GraphNode - 图谱节点
public class GraphNode {
    private String id;                  // noteId
    private String label;               // 笔记标题
    private String category;            // 分类
    private int masteryLevel;           // 掌握度（影响节点颜色）
    private int noteCount;              // 该分类下的笔记数（分类节点时有值）
}

// GraphEdge - 图谱边
public class GraphEdge {
    private String source;              // 源节点ID
    private String target;              // 目标节点ID
    private double weight;              // 关联权重（0-1）
    private String type;                // 关联类型：semantic（语义关联）/ tag（标签关联）/ reference（引用关联）
}
```

**关联发现算法**：

| 关联类型 | 发现方式 | 权重计算 |
|---------|---------|---------|
| **标签关联** | 两个笔记共享标签 | weight = 共享标签数 / max(笔记A标签数, 笔记B标签数) |
| **语义关联** | 笔记向量余弦相似度 | weight = cosine_similarity(笔记A向量, 笔记B向量) |
| **引用关联** | 笔记的 relatedNoteIds 字段 | weight = 0.8（固定权重） |

**阈值**：仅保留 weight >= 0.3 的边。

**API 接口**：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/learn/graph` | 获取完整知识图谱数据 |
| GET | `/api/v1/learn/graph/around/{noteId}` | 获取指定笔记周围的局部图谱 |
| POST | `/api/v1/learn/graph/refresh` | 重新计算图谱关联 |

---

### M6：用户与认证

#### M6.1 模块概述

复用现有 SmartAgent 的 JWT + RBAC 认证体系，调整角色和权限适配学习场景。

#### M6.2 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M6-F01 | 用户注册 | P0 | 注册时默认角色为 LEARNER |
| M6-F02 | 用户登录 | P0 | JWT Token 认证 |
| M6-F03 | 初始化管理员 | P0 | 首次启动时创建 ADMIN 账号 |
| M6-F04 | 用户信息查询 | P1 | 查询当前用户信息和学习概览 |

#### M6.2 权限矩阵

| 接口组 | LEARNER | ADMIN |
|--------|:-------:|:-----:|
| `/api/v1/learn/**` | ✅ | ✅ |
| `/api/v1/auth/**` | ✅ | ✅ |
| `/api/v1/admin/**` | ❌ | ✅ |
| `/api/v1/monitor/**` | ❌ | ✅ |

---

### M7：系统管理

#### M7.1 功能清单

| 编号 | 功能名称 | 优先级 | 说明 |
|------|---------|--------|------|
| M7-F01 | 用户管理 | P1 | 管理员查看/禁用用户 |
| M7-F02 | 系统监控 | P1 | Token 用量、API 调用量、系统健康 |
| M7-F03 | 全局知识库管理 | P2 | 管理员查看/清理全局向量库 |

---

### M8：前端交互界面

#### M8.1 页面清单

| 页面 | 路由 | 优先级 | 说明 |
|------|------|--------|------|
| 登录页 | `/login` | P0 | 用户登录 |
| 注册页 | `/register` | P0 | 用户注册 |
| 学习仪表盘 | `/home` | P0 | 今日学习概览、待复习数、掌握度雷达图 |
| **ReAct 对话页** | `/chat` | P0 | **核心页面**：左侧推理链路 + 右侧对话区 |
| 知识笔记页 | `/notes` | P0 | 笔记列表/创建/编辑/搜索 |
| 笔记详情页 | `/notes/:id` | P0 | 笔记内容、关联笔记、测验入口 |
| 测验页 | `/quiz` | P1 | 测验列表/答题/结果 |
| 闪卡复习页 | `/flashcard` | P1 | 闪卡翻转复习 |
| 知识图谱页 | `/graph` | P1 | 力导向图可视化 |
| 学习统计页 | `/stats` | P1 | 学习数据可视化 |
| 用户管理页 | `/admin` | P2 | 管理员用户管理 |

#### M8.2 核心页面规格：ReAct 对话页

**布局**：

```
┌─────────────────────────────────────────────────────────────────┐
│  LearnAgent 对话                              [新建] [设置]     │
├──────────────────────┬──────────────────────────────────────────┤
│  推理链路面板 (40%)   │  对话区域 (60%)                          │
│                      │                                          │
│  ┌────────────────┐  │  用户: 梯度下降和反向传播是什么关系？      │
│  │ 💭 Thought 1   │  │                                          │
│  │ 检索知识库...   │  │  LearnAgent:                            │
│  │ [补充信息]     │  │  梯度下降和反向传播是深度学习中            │
│  └────────────────┘  │  两个紧密关联的核心概念...                │
│  ┌────────────────┐  │                                          │
│  │ 🔧 Action 1    │  │  📎 引用来源:                            │
│  │ search_notes   │  │  - 笔记"梯度下降基础"                    │
│  │ query="梯度下降"│  │  - 笔记"反向传播算法"                    │
│  └────────────────┘  │                                          │
│  ┌────────────────┐  │                                          │
│  │ 👁 Observation 1│  │                                          │
│  │ 找到3条笔记...  │  │                                          │
│  └────────────────┘  │                                          │
│  ┌────────────────┐  │                                          │
│  │ 💭 Thought 2   │  │                                          │
│  │ 补充对比卡片... │  │                                          │
│  │ [补充信息]     │  │                                          │
│  └────────────────┘  │                                          │
│  ┌────────────────┐  │                                          │
│  │ 🔧 Action 2    │  │                                          │
│  │ create_note    │  │                                          │
│  └────────────────┘  │                                          │
│  ┌────────────────┐  │                                          │
│  │ 👁 Observation 2│  │                                          │
│  │ ✅ 笔记已保存  │  │                                          │
│  └────────────────┘  │                                          │
│                      │                                          │
├──────────────────────┴──────────────────────────────────────────┤
│  [输入消息...]                              [发送] [中断推理]    │
└─────────────────────────────────────────────────────────────────┘
```

**交互规格**：

| 交互 | 说明 |
|------|------|
| 推理步骤展开/折叠 | 每个 Thought/Action/Observation 步骤可展开查看详情或折叠 |
| 补充信息 | 点击 Thought 旁的"补充信息"按钮，弹出输入框，用户可注入额外上下文 |
| 中断推理 | 点击"中断推理"按钮，Agent 在当前步骤完成后停止，基于已有信息生成回答 |
| 步骤状态指示 | 进行中：蓝色脉冲动画；完成：绿色勾；失败：红色叉 |
| 引用来源 | 最终回答下方显示引用的笔记来源，点击可跳转笔记详情 |
| 推理耗时 | 每个步骤右侧显示耗时（ms） |

#### M8.3 核心页面规格：知识图谱页

**布局**：

```
┌──────────────────────────────────────────────────────────────┐
│  知识图谱                              [刷新] [筛选]          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│              ┌─────────┐                                     │
│              │ 深度学习 │ ← 分类节点（大圆）                   │
│              └────┬────┘                                     │
│           ┌───────┼───────┐                                  │
│      ┌────┴──┐ ┌──┴──┐ ┌──┴──────┐                          │
│      │优化器 │ │CNN  │ │RNN/LSTM │ ← 笔记节点（小圆）         │
│      └──┬────┘ └─────┘ └─────────┘                          │
│    ┌────┼────┐                                              │
│ ┌──┴─┐┌─┴──┐│   颜色图例：                                  │
│ │SGD ││Adam││   🟢 掌握度 >= 80                              │
│ └────┘└────┘│   🟡 掌握度 40-79                              │
│        ┌────┴─┐ 🔴 掌握度 < 40                               │
│        │RMSprop│ 边粗细 = 关联权重                             │
│        └──────┘                                              │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  筛选: [分类▼] [标签▼] [掌握度▼]    节点数: 42  边数: 67     │
└──────────────────────────────────────────────────────────────┘
```

**交互规格**：

| 交互 | 说明 |
|------|------|
| 节点拖拽 | 支持拖拽调整节点位置 |
| 节点点击 | 点击笔记节点弹出笔记摘要浮层，含"查看详情"和"开始测验"按钮 |
| 节点双击 | 双击笔记节点跳转到笔记详情页 |
| 缩放/平移 | 鼠标滚轮缩放，拖拽画布平移 |
| 局部展开 | 点击分类节点展开/收起该分类下的笔记节点 |
| 筛选 | 按分类/标签/掌握度筛选显示的节点 |

---

## 四、后端文件改造清单

### 4.1 需要新增的文件

| 文件路径 | 说明 |
|---------|------|
| `agent/ReActEngine.java` | ReAct 推理引擎核心（Thought→Action→Observation 循环） |
| `agent/LearnAgent.java` | 学习 Agent（替代 FullAgent，集成 ReActEngine） |
| `tool/KnowledgeTool.java` | 知识库工具（search_notes / create_note / update_note / find_related / delete_note） |
| `tool/LearningTool.java` | 学习追踪工具（get_progress / record_study / analyze_weakness / get_review_reminders） |
| `tool/QuizTool.java` | 测验工具（generate_quiz / evaluate_answer / generate_flashcard / evaluate_explanation） |
| `tool/WebSearchTool.java` | 联网搜索工具（search_web / summarize_article） |
| `controller/LearnController.java` | 学习模块统一入口（ReAct 对话 + 学习相关接口） |
| `controller/NoteController.java` | 知识笔记管理接口 |
| `controller/QuizController.java` | 测验管理接口 |
| `controller/FlashcardController.java` | 闪卡管理接口 |
| `controller/GraphController.java` | 知识图谱接口 |
| `controller/StudyController.java` | 学习追踪接口 |
| `model/mongo/KnowledgeNoteDocument.java` | 知识笔记 MongoDB 文档 |
| `model/mongo/QuizDocument.java` | 测验题 MongoDB 文档 |
| `model/mongo/FlashcardDocument.java` | 闪卡 MongoDB 文档 |
| `model/mongo/ReActTraceDocument.java` | 推理链路 MongoDB 文档 |
| `model/entity/StudyRecordEntity.java` | 学习记录 JPA 实体 |
| `model/dto/ReactChatRequest.java` | ReAct 对话请求 |
| `model/dto/NoteCreateRequest.java` | 创建笔记请求 |
| `model/dto/NoteUpdateRequest.java` | 更新笔记请求 |
| `model/dto/QuizGenerateRequest.java` | 生成测验请求 |
| `model/dto/QuizEvaluateRequest.java` | 评估答案请求 |
| `model/dto/FlashcardReviewRequest.java` | 闪卡复习请求 |
| `model/dto/FeynmanEvaluateRequest.java` | 费曼检验请求 |
| `model/dto/InterveneRequest.java` | 中途干预请求 |
| `model/vo/ReactChatResponse.java` | ReAct 对话响应 |
| `model/vo/ReactStepVO.java` | 推理步骤 VO |
| `model/vo/NoteVO.java` | 笔记 VO |
| `model/vo/QuizVO.java` | 测验 VO |
| `model/vo/FlashcardVO.java` | 闪卡 VO |
| `model/vo/FeynmanEvaluationVO.java` | 费曼检验结果 VO |
| `model/vo/GraphDataVO.java` | 图谱数据 VO |
| `model/vo/LearningProgressVO.java` | 学习进度 VO |
| `model/vo/LearningStatsVO.java` | 学习统计 VO |
| `repository/mongo/KnowledgeNoteRepository.java` | 笔记 MongoDB Repository |
| `repository/mongo/QuizRepository.java` | 测验 MongoDB Repository |
| `repository/mongo/FlashcardRepository.java` | 闪卡 MongoDB Repository |
| `repository/mongo/ReActTraceRepository.java` | 推理链路 MongoDB Repository |
| `repository/StudyRecordRepository.java` | 学习记录 JPA Repository |
| `service/NoteService.java` + `impl/NoteServiceImpl.java` | 笔记服务 |
| `service/QuizService.java` + `impl/QuizServiceImpl.java` | 测验服务 |
| `service/FlashcardService.java` + `impl/FlashcardServiceImpl.java` | 闪卡服务 |
| `service/GraphService.java` + `impl/GraphServiceImpl.java` | 图谱服务 |
| `service/StudyService.java` + `impl/StudyServiceImpl.java` | 学习追踪服务 |
| `service/ReactService.java` + `impl/ReactServiceImpl.java` | ReAct 推理服务 |
| `resources/prompts/react-system.st` | ReAct 系统提示词模板 |
| `resources/prompts/quiz-generate.st` | 测验生成提示词模板 |
| `resources/prompts/feynman-evaluate.st` | 费曼检验提示词模板 |
| `resources/prompts/flashcard-generate.st` | 闪卡生成提示词模板 |
| `resources/prompts/note-summary.st` | 笔记摘要生成提示词模板 |

### 4.2 需要修改的文件

| 文件路径 | 修改内容 |
|---------|---------|
| `config/ChatClientConfig.java` | 新增 `reactChatClient` Bean（ReAct 专用 ChatClient），移除不需要的旧 Bean |
| `config/SecurityConfig.java` | 调整权限矩阵：新增 `/api/v1/learn/**` 路径规则 |
| `config/MemoryConfig.java` | 保持不变，ReAct 引擎复用 ChatMemory |
| `main/MainApplication.java` | 确保 scanBasePackages 覆盖新增包 |
| `common/constant/ResultCode.java` | 新增学习相关错误码 |
| `resources/application.yml` | 新增 LearnAgent 相关配置项 |
| `resources/application-dev.yml` | 新增开发环境 LearnAgent 配置 |

### 4.3 需要删除/弃用的文件

| 文件路径 | 处理方式 |
|---------|---------|
| `agent/FullAgent.java` | 弃用，由 LearnAgent 替代 |
| `agent/StreamAgent.java` | 弃用，流式能力集成到 LearnAgent |
| `tool/DateTimeTool.java` | 删除，学习场景不需要 |
| `tool/TranslateTool.java` | 删除，由 WebSearchTool 替代 |
| `tool/DocParseTool.java` | 删除，功能合并到 KnowledgeTool |
| `tool/DbQueryTool.java` | 删除，由 LearningTool 替代 |
| `controller/StreamController.java` | 弃用，由 LearnController 替代 |
| `controller/AgentController.java` | 弃用，由 LearnController 替代 |

---

## 五、前端文件改造清单

### 5.1 需要新增的文件

| 文件路径 | 说明 |
|---------|------|
| `src/views/chat/index.vue` | ReAct 对话页（核心，替代原 stream 页） |
| `src/views/notes/index.vue` | 知识笔记列表页 |
| `src/views/notes/detail.vue` | 笔记详情页 |
| `src/views/quiz/index.vue` | 测验页 |
| `src/views/flashcard/index.vue` | 闪卡复习页 |
| `src/views/graph/index.vue` | 知识图谱页 |
| `src/views/stats/index.vue` | 学习统计页 |
| `src/components/ReactChainPanel.vue` | 推理链路面板组件 |
| `src/components/ReactStepCard.vue` | 推理步骤卡片组件 |
| `src/components/KnowledgeGraph.vue` | 知识图谱力导向图组件 |
| `src/components/FlashcardFlip.vue` | 闪卡翻转组件 |
| `src/components/MasteryRadar.vue` | 掌握度雷达图组件 |
| `src/composables/useReactStream.ts` | ReAct 流式对话 composable |
| `src/api/learn.ts` | 学习模块 API |
| `src/api/note.ts` | 笔记 API |
| `src/api/quiz.ts` | 测验 API |
| `src/api/flashcard.ts` | 闪卡 API |
| `src/api/graph.ts` | 图谱 API |
| `src/api/study.ts` | 学习追踪 API |
| `src/types/learn.ts` | 学习模块类型定义 |
| `src/types/note.ts` | 笔记类型定义 |
| `src/types/quiz.ts` | 测验类型定义 |
| `src/types/graph.ts` | 图谱类型定义 |

### 5.2 需要修改的文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/router/index.ts` | 替换路由：/stream → /chat（ReAct对话），新增 /notes /quiz /flashcard /graph /stats |
| `src/layouts/BasicLayout.vue` | 更新品牌名称为 LearnAgent，更新菜单项 |
| `src/views/home/index.vue` | 改造为学习仪表盘 |
| `src/store/modules/permission.ts` | 更新菜单权限适配新页面 |

### 5.3 需要删除/弃用的文件

| 文件路径 | 处理方式 |
|---------|---------|
| `src/views/stream/index.vue` | 弃用，由 chat/index.vue 替代 |
| `src/views/agent/index.vue` | 弃用，功能合并到 chat |
| `src/views/planning/index.vue` | 弃用，功能合并到 chat |
| `src/views/deploy/index.vue` | 弃用 |
| `src/views/release/index.vue` | 弃用 |
| `src/views/demo/index.vue` | 弃用 |

---

## 六、配置项新增

### application.yml / application-dev.yml 新增配置

```yaml
learn-agent:
  react:
    max-iterations: 8
    step-timeout-seconds: 15
    auto-create-note: true
    strategy: auto

  note:
    max-title-length: 200
    max-content-length: 50000
    auto-summary: true
    default-chunk-size: 800
    default-chunk-overlap: 100

  quiz:
    default-count: 5
    max-count: 20
    default-difficulty: medium
    type-distribution:
      choice: 0.6
      true-false: 0.2
      short-answer: 0.2

  flashcard:
    sm2:
      default-ease-factor: 2.5
      min-ease-factor: 1.3
      first-interval-days: 1
      second-interval-days: 6

  graph:
    similarity-threshold: 0.3
    max-nodes: 200
    max-edges: 500

  study:
    review-reminder-hour: 9
    weakness-threshold: 40
```

---

## 七、数据库变更

### 7.1 MySQL 新增表

```sql
CREATE TABLE study_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    note_id VARCHAR(64),
    activity_type VARCHAR(32) NOT NULL COMMENT 'CHAT/QUIZ/REVIEW/FEYNMAN/NOTE_CREATE',
    topic VARCHAR(200),
    duration_seconds INT DEFAULT 0,
    score INT DEFAULT 0,
    detail TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_created_at (created_at),
    INDEX idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 7.2 MongoDB 新增集合

| 集合名 | 索引 | 说明 |
|--------|------|------|
| `knowledge_notes` | `{userId: 1}`, `{tags: 1}`, `{category: 1}`, `{noteId: 1}`（唯一） | 知识笔记 |
| `quizzes` | `{userId: 1}`, `{noteId: 1}`, `{tags: 1}` | 测验题 |
| `flashcards` | `{userId: 1}`, `{noteId: 1}`, `{nextReviewAt: 1}` | 闪卡 |
| `react_traces` | `{userId: 1}`, `{conversationId: 1}`, `{createdAt: -1}` | 推理链路 |

---

## 八、实施优先级与里程碑

### Milestone 1：ReAct 核心（2周）

| 任务 | 优先级 | 依赖 |
|------|--------|------|
| 实现 ReActEngine 核心类 | P0 | 无 |
| 实现 react-system.st 提示词 | P0 | 无 |
| 实现 KnowledgeTool（search_notes / create_note） | P0 | NoteService |
| 实现 LearnAgent | P0 | ReActEngine + KnowledgeTool |
| 实现 LearnController（ReAct 对话接口） | P0 | LearnAgent |
| 实现 ReAct SSE 流式推送 | P0 | LearnAgent |
| 前端 ReactChainPanel 组件 | P0 | 无 |
| 前端 ReAct 对话页 | P0 | ReactChainPanel + useReactStream |
| ReActTrace 持久化 | P1 | MongoDB |

### Milestone 2：知识笔记（1.5周）

| 任务 | 优先级 | 依赖 |
|------|--------|------|
| KnowledgeNote 数据模型 + Repository | P0 | MongoDB |
| NoteService CRUD | P0 | Repository |
| NoteController | P0 | NoteService |
| 文档导入（PDF/MD/TXT） | P0 | NoteService + DocumentService |
| 笔记自动摘要 | P1 | ChatClient |
| 前端笔记列表页 | P0 | Note API |
| 前端笔记详情页 | P0 | Note API |
| KnowledgeTool 完整实现（update/find_related/delete） | P1 | NoteService |

### Milestone 3：学习工具（1.5周）

| 任务 | 优先级 | 依赖 |
|------|--------|------|
| QuizTool + QuizService | P0 | ChatClient + NoteService |
| FlashcardTool + FlashcardService | P0 | ChatClient + NoteService |
| SM-2 间隔重复算法 | P0 | FlashcardService |
| 费曼检验 | P1 | ChatClient + NoteService |
| LearningTool + StudyService | P1 | StudyRecordRepository |
| 前端测验页 | P0 | Quiz API |
| 前端闪卡复习页 | P0 | Flashcard API |

### Milestone 4：分析与图谱（1周）

| 任务 | 优先级 | 依赖 |
|------|--------|------|
| 学习进度查询 API | P1 | StudyService |
| 薄弱点分析 | P1 | StudyService + NoteService |
| 图谱数据生成 | P1 | NoteService + VectorStore |
| 前端知识图谱页 | P1 | Graph API + D3.js/vis-network |
| 前端学习统计页 | P1 | Study API + ECharts |
| 学习仪表盘改造 | P1 | Home API |

### Milestone 5：体验优化（1周）

| 任务 | 优先级 | 依赖 |
|------|--------|------|
| 用户中途干预 | P1 | ReActEngine |
| 推理链路回放 | P2 | ReActTrace |
| WebSearchTool | P2 | 外部搜索 API |
| 学习报告导出 | P2 | StudyService |
| 模型容灾（主/兜底切换） | P1 | ChatClientConfig |

---

## 九、非功能性需求

| 维度 | 要求 |
|------|------|
| **性能** | ReAct 单轮对话响应时间 < 15s（含工具调用）；SSE 首事件延迟 < 2s |
| **并发** | 支持 50 用户同时在线对话 |
| **可用性** | 主模型不可用时自动切换兜底模型，切换时间 < 5s |
| **数据安全** | 用户笔记数据隔离，不同用户不可互相访问 |
| **存储** | 单用户笔记上限 1000 条，单条笔记最大 50KB |
| **向量库** | 单用户向量片段上限 10000 个 |
| **兼容性** | 前端支持 Chrome/Firefox/Edge 最新2个版本 |

---

## 十、技术选型补充

| 技术 | 用途 | 说明 |
|------|------|------|
| **D3.js / vis-network** | 知识图谱可视化 | 力导向图渲染 |
| **ECharts** | 学习统计图表 | 雷达图/折线图/热力图 |
| **markdown-it** | Markdown 渲染 | 笔记内容渲染 |
| **SM-2 算法** | 间隔重复 | 闪卡复习调度 |
