import { Divider, Grid, H1, H2, Stack, Stat, Table, Text } from 'cursor/canvas'

export default function RagMongoStoragePlan() {
  return (
    <Stack gap={20}>
      <H1>RAG 向量数据库与 MongoDB 对话存储规划</H1>
      <Text tone="secondary">
        当前规划建立在登录注册、JWT、Redis 会话、个人中心都已经具备的前提下，
        目标是把知识库检索和对话记忆存储拆成两条独立的数据链路，避免和认证体系耦合。
      </Text>

      <Grid columns={3} gap={16}>
        <Stat value="2" label="存储方向" />
        <Stat value="6" label="建议阶段" />
        <Stat value="1" label="核心目标：解耦" />
      </Grid>

      <Divider />

      <H2>总体原则</H2>
      <Text>
        1. RAG 向量数据库只负责知识检索，不负责用户认证和业务状态。
      </Text>
      <Text>
        2. MongoDB 只负责对话类半结构化数据，如消息记录、会话摘要、工具调用过程。
      </Text>
      <Text>
        3. 认证、用户资料、验证码、会话这些能力继续留在 MySQL + Redis，不混到 RAG / MongoDB 里。
      </Text>
      <Text>
        4. 先设计数据流，再选具体向量库实现；先定 MongoDB 文档结构，再做对话落库。
      </Text>

      <Divider />

      <H2>六阶段规划</H2>
      <Table
        headers={['阶段', '目标', '关键任务', '完成标准']}
        rows={[
          [
            '阶段 1：边界与场景定义',
            '明确 RAG 与 MongoDB 的职责边界',
            '梳理知识库检索场景、对话存储场景、数据保留策略、读取策略',
            '形成数据分层，不和认证/用户表混用',
          ],
          [
            '阶段 2：MongoDB 连接与集合建模',
            '先把 MongoDB 的会话与消息模型定下来',
            '建立连接、设计集合、定义索引、确认文档结构',
            '可以稳定写入/读取会话和消息',
          ],
          [
            '阶段 3：MongoDB 对话写入与查询',
            '把聊天过程落库并能回看历史',
            '消息追加、会话列表、会话摘要、历史分页查询',
            '前端可以恢复最近上下文',
          ],
          [
            '阶段 4：Qdrant 连接与 collection 规划',
            '建立向量库 collection 和 payload 规则',
            '确认维度、距离算法、collection、payload 结构',
            '可稳定入库与检索 chunk',
          ],
          [
            '阶段 5：RAG 文档切分与检索',
            '把知识文档切块后向量化并召回',
            '切分、embedding、topK、阈值过滤、重排',
            '可以给大模型提供相关上下文',
          ],
          [
            '阶段 6：RAG + Mongo 联动',
            '把知识检索和对话历史融合到聊天链路',
            '先查向量库，再取 Mongo 最近消息，最后统一组 prompt',
            '形成完整 Agent 问答链路',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB 集合结构</H2>
      <Table
        headers={['集合', '用途', '核心字段', '索引建议']}
        rows={[
          [
            'chat_sessions',
            '保存会话元信息',
            'sessionId, userId, title, summary, status, lastMessageAt, createdAt, updatedAt',
            'sessionId 唯一，userId + lastMessageAt 普通索引',
          ],
          [
            'chat_messages',
            '保存每条聊天消息',
            'messageId, sessionId, userId, role, content, traceId, model, toolCalls, latencyMs, tokenUsage, createdAt',
            'sessionId + createdAt 复合索引，messageId 唯一',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB Java 实体设计</H2>
      <Table
        headers={['实体', '集合', '字段设计重点', '代码实现建议']}
        rows={[
          [
            'ChatSessionDocument',
            'chat_sessions',
            '会话 ID、用户 ID、标题、摘要、状态、最近消息时间、创建/更新时间',
            '使用 `@Document` + `@Indexed`，会话 ID 设唯一索引，优先支持列表查询',
          ],
          [
            'ChatMessageDocument',
            'chat_messages',
            '消息 ID、会话 ID、用户 ID、角色、内容、traceId、模型、工具调用、耗时、token 使用、创建时间',
            '消息按会话分页查询，messageId 唯一，sessionId + createdAt 建复合索引',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB Java 实体字段规范</H2>
      <Text>
        1. 主键建议使用 Mongo 默认 `ObjectId`，同时保留业务 ID 字段，如 `sessionId`、`messageId`。
      </Text>
      <Text>
        2. 时间字段统一使用 `LocalDateTime` 或 `Instant`，建议后续实体层统一格式，避免序列化混乱。
      </Text>
      <Text>
        3. 文本类字段：`title`、`summary`、`content` 使用 String，必要时加长度限制。
      </Text>
      <Text>
        4. 状态类字段：`status` 建议用字符串枚举，例如 `ACTIVE`、`ARCHIVED`、`DELETED`。
      </Text>
      <Text>
        5. 追踪类字段：`traceId`、`model`、`toolCalls`、`tokenUsage` 用于后续审计和调试。
      </Text>

      <Divider />

      <H2>MongoDB 实际操作步骤</H2>
      <Text>
        1. MongoDB 已启动时，先确认能连到 `mongodb://localhost:27017`。
      </Text>
      <Text>
        2. 数据库名建议先用 `smart_agent_chat`，方便和项目语义对应。
      </Text>
      <Text>
        3. 先建集合 `chat_sessions` 和 `chat_messages`，或者直接通过首次插入自动创建。
      </Text>
      <Text>
        4. 建好集合后先加索引，再写测试数据验证读写和分页。
      </Text>
      <Text>
        5. 验证通过后，再开始写 Spring Data MongoDB 的实体和 Repository。
      </Text>

      <Divider />

      <H2>MongoDB Repository / Service 规划</H2>
      <Table
        headers={['层', '职责', '建议接口']}
        rows={[
          [
            'Repository',
            '负责 Mongo 查询与分页',
            'findBySessionId / findByUserId / findLatestByUserId / existsBySessionId',
          ],
          [
            'Service',
            '封装会话创建、消息追加、摘要更新',
            'createSession / appendMessage / updateSummary / listSessions / listMessages',
          ],
          [
            'Controller',
            '给前端提供会话与消息 API',
            'GET/POST/PUT/DELETE 会话相关接口',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB Service 具体接口方法签名</H2>
      <Text tone="secondary">
        下面是建议的 Java 接口方法签名，后面你确认后我再进入实现代码。
      </Text>
      <Table
        headers={['Service', '方法签名', '返回值', '用途']}
        rows={[
          [
            'ChatSessionService',
            'ChatSessionDocument createSession(String userId)',
            'ChatSessionDocument',
            '创建新会话并返回会话信息',
          ],
          [
            'ChatSessionService',
            'Optional<ChatSessionDocument> getSessionBySessionId(String sessionId)',
            'Optional<ChatSessionDocument>',
            '按会话 ID 查询单个会话',
          ],
          [
            'ChatSessionService',
            'List<ChatSessionDocument> listSessionsByUserId(String userId)',
            'List<ChatSessionDocument>',
            '按用户查询会话列表，按最近消息排序',
          ],
          [
            'ChatSessionService',
            'ChatSessionDocument updateSessionTitle(String sessionId, String title)',
            'ChatSessionDocument',
            '修改会话标题',
          ],
          [
            'ChatSessionService',
            'ChatSessionDocument updateSessionSummary(String sessionId, String summary)',
            'ChatSessionDocument',
            '更新会话摘要',
          ],
          [
            'ChatSessionService',
            'ChatSessionDocument updateLastMessageAt(String sessionId, LocalDateTime lastMessageAt)',
            'ChatSessionDocument',
            '更新最近消息时间',
          ],
          [
            'ChatSessionService',
            'void archiveSession(String sessionId)',
            'void',
            '归档会话，状态改为 ARCHIVED',
          ],
          [
            'ChatSessionService',
            'void deleteSession(String sessionId)',
            'void',
            '删除会话及其业务关联消息（通常配合消息服务一起调用）',
          ],
          [
            'ChatMessageService',
            'ChatMessageDocument appendMessage(ChatMessageDocument message)',
            'ChatMessageDocument',
            '追加一条消息并返回保存结果',
          ],
          [
            'ChatMessageService',
            'List<ChatMessageDocument> listMessagesBySessionId(String sessionId)',
            'List<ChatMessageDocument>',
            '按会话 ID 获取全部消息',
          ],
          [
            'ChatMessageService',
            'Page<ChatMessageDocument> pageMessagesBySessionId(String sessionId, Pageable pageable)',
            'Page<ChatMessageDocument>',
            '按会话分页获取消息历史',
          ],
          [
            'ChatMessageService',
            'List<ChatMessageDocument> listLatestMessagesByUserId(String userId, int limit)',
            'List<ChatMessageDocument>',
            '获取某用户最近的消息',
          ],
          [
            'ChatMessageService',
            'boolean existsByMessageId(String messageId)',
            'boolean',
            '判断消息是否重复',
          ],
          [
            'ChatMessageService',
            'void deleteMessagesBySessionId(String sessionId)',
            'void',
            '删除某会话的全部消息',
          ],
          [
            'ChatConversationService',
            'String sendUserMessage(String userId, String sessionId, String content)',
            'String',
            '处理用户发消息的完整流程',
          ],
          [
            'ChatConversationService',
            'List<ChatMessageDocument> loadConversationContext(String sessionId, int limit)',
            'List<ChatMessageDocument>',
            '加载上下文消息',
          ],
          [
            'ChatConversationService',
            'String buildPrompt(String userId, String sessionId, String content)',
            'String',
            '组装模型 prompt',
          ],
          [
            'ChatConversationService',
            'void saveAssistantMessage(String userId, String sessionId, String content)',
            'void',
            '保存 AI 回复',
          ],
          [
            'ChatConversationService',
            'void updateConversationSummary(String sessionId)',
            'void',
            '更新会话摘要',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB Service 实现代码草案</H2>
      <Text tone="secondary">
        下面是实现层的推荐组织方式，后续你确认后我就可以直接开始落代码。
      </Text>
      <Table
        headers={['实现类', '实现重点', '建议内部依赖', '备注']}
        rows={[
          [
            'ChatSessionServiceImpl',
            '会话创建、列表、标题、摘要、状态管理',
            'ChatSessionRepository',
            '统一维护 session 级别元数据',
          ],
          [
            'ChatMessageServiceImpl',
            '消息追加、分页查询、历史读取、消息删除',
            'ChatMessageRepository',
            '统一维护 message 级别明细',
          ],
          [
            'ChatConversationServiceImpl',
            '业务编排、上下文拼装、消息保存、摘要更新',
            'ChatSessionService + ChatMessageService + QdrantService',
            '后续接入 RAG 时主要从这里扩展',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB Service 实现草案说明</H2>
      <Text>
        1. `ChatSessionServiceImpl` 里建议统一负责会话默认标题生成，比如 `新会话` + 时间戳。
      </Text>
      <Text>
        2. `ChatMessageServiceImpl` 里建议在保存消息前补齐 `messageId`、`createdAt`、`traceId`。
      </Text>
      <Text>
        3. `ChatConversationServiceImpl` 里建议先保存用户消息，再保存 AI 消息，保证上下文连续。
      </Text>
      <Text>
        4. 会话摘要更新可以先做成简单策略，后面再升级成模型摘要。
      </Text>
      <Text>
        5. 如果会话不存在，`ChatConversationService` 应先创建会话再写消息。
      </Text>

      <Divider />

      <H2>MongoDB Service 接口正式定义</H2>
      <Text tone="secondary">
        下面是我建议你真正落代码时采用的 Service 接口定义，命名尽量保持简洁、稳定、可扩展。
      </Text>
      <Table
        headers={['接口', '方法签名', '说明']}
        rows={[
          [
            'ChatSessionService',
            'ChatSessionDocument createSession(String userId, String title)',
            '创建新会话，title 允许前端传入；为空则使用默认标题',
          ],
          [
            'ChatSessionService',
            'Optional<ChatSessionDocument> getSessionBySessionId(String sessionId)',
            '按 sessionId 获取会话详情',
          ],
          [
            'ChatSessionService',
            'List<ChatSessionDocument> listSessionsByUserId(String userId)',
            '按用户 ID 获取会话列表',
          ],
          [
            'ChatSessionService',
            'ChatSessionDocument updateSessionTitle(String sessionId, String title)',
            '更新会话标题',
          ],
          [
            'ChatSessionService',
            'ChatSessionDocument updateSessionSummary(String sessionId, String summary)',
            '更新会话摘要',
          ],
          [
            'ChatSessionService',
            'ChatSessionDocument updateLastMessageAt(String sessionId, LocalDateTime lastMessageAt)',
            '更新会话最近消息时间',
          ],
          [
            'ChatSessionService',
            'void archiveSession(String sessionId)',
            '归档会话',
          ],
          [
            'ChatSessionService',
            'void deleteSession(String sessionId)',
            '删除会话',
          ],
          [
            'ChatMessageService',
            'ChatMessageDocument appendMessage(ChatMessageDocument message)',
            '保存一条消息',
          ],
          [
            'ChatMessageService',
            'List<ChatMessageDocument> listMessagesBySessionId(String sessionId)',
            '获取某会话全部消息',
          ],
          [
            'ChatMessageService',
            'Page<ChatMessageDocument> pageMessagesBySessionId(String sessionId, Pageable pageable)',
            '按分页获取会话消息',
          ],
          [
            'ChatMessageService',
            'List<ChatMessageDocument> listLatestMessagesByUserId(String userId, int limit)',
            '获取某用户最近的消息',
          ],
          [
            'ChatMessageService',
            'boolean existsByMessageId(String messageId)',
            '判断消息是否重复',
          ],
          [
            'ChatMessageService',
            'void deleteMessagesBySessionId(String sessionId)',
            '删除某会话的全部消息',
          ],
          [
            'ChatConversationService',
            'String sendUserMessage(String userId, String sessionId, String content)',
            '处理用户发消息的完整流程',
          ],
          [
            'ChatConversationService',
            'List<ChatMessageDocument> loadConversationContext(String sessionId, int limit)',
            '加载上下文消息',
          ],
          [
            'ChatConversationService',
            'String buildPrompt(String userId, String sessionId, String content)',
            '组装模型 prompt',
          ],
          [
            'ChatConversationService',
            'void saveAssistantMessage(String userId, String sessionId, String content)',
            '保存 AI 回复',
          ],
          [
            'ChatConversationService',
            'void updateConversationSummary(String sessionId)',
            '更新会话摘要',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB Service 实现代码草案</H2>
      <Text tone="secondary">
        下面是你后面真正落代码时可直接采用的实现思路。
      </Text>
      <Table
        headers={['实现类', '核心步骤', '注意事项']}
        rows={[
          [
            'ChatSessionServiceImpl',
            '创建会话、列表查询、更新标题/摘要/时间、归档删除',
            '默认标题、状态字段、更新时间统一维护',
          ],
          [
            'ChatMessageServiceImpl',
            '补齐消息 ID、时间、traceId 后落库，并支持分页查询',
            'messageId 唯一，分页排序要稳定',
          ],
          [
            'ChatConversationServiceImpl',
            '先写用户消息，再组装 prompt，再写 AI 消息并更新摘要',
            '后续接 Qdrant 时只扩展 prompt 构建逻辑',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB 实体类代码草案</H2>
      <Text tone="secondary">
        下面是实体类后面真正落代码时的建议草案，先按这个结构定住，再进入实现。
      </Text>
      <Table
        headers={['实体类', '推荐注解', '字段草案', '备注']}
        rows={[
          [
            'ChatSessionDocument',
            '@Data, @Builder, @Document, @CompoundIndex',
            'id, sessionId, userId, title, summary, status, lastMessageAt, createdAt, updatedAt',
            '一条记录代表一个会话',
          ],
          [
            'ChatMessageDocument',
            '@Data, @Builder, @Document, @CompoundIndex',
            'id, messageId, sessionId, userId, role, content, traceId, model, toolCalls, latencyMs, tokenUsage, createdAt',
            '一条记录代表一条消息',
          ],
        ]}
      />

      <Divider />

      <H2>MongoDB 实体类字段细化</H2>
      <Text>
        1. `ChatSessionDocument` 建议把 `title`、`summary` 设计为可空，方便刚创建时只写基础信息。
      </Text>
      <Text>
        2. `ChatSessionDocument` 的 `status` 建议初始值为 `ACTIVE`。
      </Text>
      <Text>
        3. `ChatMessageDocument` 的 `role` 建议用 `user` / `assistant` / `system` / `tool`。
      </Text>
      <Text>
        4. `toolCalls` 和 `tokenUsage` 先用结构化 Map 或 List 保存，后面再按需要细化成独立 VO。
      </Text>
      <Text>
        5. `latencyMs` 用于记录单条消息生成耗时，后面排查慢响应很有用。
      </Text>

      <Divider />

      <H2>Qdrant Collection 与 Payload 结构</H2>
      <Table
        headers={['对象', '用途', '建议字段', '说明']}
        rows={[
          [
            'collection',
            '存向量 chunk',
            'docId, chunkId, title, content, source, category, tags, chunkIndex, enabled, createdAt, updatedAt',
            '维度和距离算法要与 embedding 模型匹配',
          ],
          [
            'payload',
            '检索与过滤元数据',
            'title, source, category, tags, chunkIndex, enabled',
            '用于过滤、排序、展示召回结果',
          ],
        ]}
      />

      <Divider />

      <H2>Qdrant 实际操作步骤</H2>
      <Text>
        1. 先确认 Qdrant 已启动，如果你用 Docker，可以先启动容器并暴露默认端口。
      </Text>
      <Text>
        2. 进入 Qdrant 的 Web UI 或直接通过 API 创建 collection。
      </Text>
      <Text>
        3. 创建 collection 时先确定向量维度，必须和 embedding 模型输出维度一致。
      </Text>
      <Text>
        4. 距离算法建议先用 cosine，相对更适合语义检索。
      </Text>
      <Text>
        5. 创建好 collection 后，先插入一条测试向量和 payload，确认可查询。
      </Text>
      <Text>
        6. 再把文档切 chunk、生成 embedding、批量写入，最后测试相似度召回。
      </Text>

      <Divider />

      <H2>推荐的代码落地顺序</H2>
      <Text>
        1. 先做 MongoDB 的 Java 实体和 Repository。
      </Text>
      <Text>
        2. 再做 MongoDB 的 Service 和 Controller。
      </Text>
      <Text>
        3. 然后做 Qdrant 的文档切分、入库和检索服务。
      </Text>
      <Text>
        4. 最后把 RAG 检索结果和对话历史拼到聊天流程里。
      </Text>

      <Divider />

      <H2>你接下来要做什么</H2>
      <Text>
        - 先确认 MongoDB 是否已经启动。
      </Text>
      <Text>
        - 再确认 Qdrant 是否可用。
      </Text>
      <Text>
        - 如果你要，我下一步就可以直接继续补 MongoDB 的 Java 实体类和后端接口规划。
      </Text>
      <Text tone="secondary" size="small">
        这份文件已经把规划、集合、索引、操作步骤、Service 签名、Service 实现草案、Service 接口正式定义、实体类草案都放到同一份 Canvas 里，方便你后面逐步执行。
      </Text>
    </Stack>
  )
}
