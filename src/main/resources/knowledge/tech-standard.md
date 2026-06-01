# SmartTech 技术开发规范

## 一、代码规范

### 1.1 命名规范
- 类名：大驼峰命名（如 UserService、OrderController）
- 方法名：小驼峰命名（如 getUserById、createOrder）
- 常量：全大写下划线（如 MAX_RETRY_COUNT）
- 包名：全小写（如 com.smarttech.service）
- 数据库表名：下划线命名（如 user_info、order_detail）

### 1.2 注释规范
- 类注释：必须包含作者、日期、功能描述
- 方法注释：公共方法必须有 JavaDoc
- 复杂逻辑：必须有行内注释说明意图
- TODO注释：格式为 // TODO [姓名] [日期] 描述

### 1.3 代码质量
- 方法长度：不超过50行
- 类长度：不超过500行
- 圈复杂度：不超过10
- 禁止：魔法数字、空 catch 块、过深嵌套（不超过3层）

## 二、Git 规范

### 2.1 分支管理
- main：生产分支，只接受 release 合并
- develop：开发分支，日常开发基于此分支
- feature/xxx：功能分支，从 develop 切出
- hotfix/xxx：紧急修复，从 main 切出
- release/x.x.x：发布分支

### 2.2 提交信息格式
<type>(<scope>): <description>

类型(type):

- feat: 新功能
- fix: 修复Bug
- docs: 文档更新
- style: 代码格式（不影响功能）
- refactor: 重构
- test: 测试相关
- chore: 构建/工具变动

示例： 
- feat(user): 新增用户注册功能
- fix(order): 修复订单金额计算错误
- docs(readme): 更新部署文档

### 2.3 Code Review
- 所有代码必须经过至少1人 Review
- 核心模块代码需要2人 Review
- Review 要点：逻辑正确性、规范遵从、性能考量、安全风险

## 三、接口设计规范

### 3.1 RESTful 规范
- GET：查询资源
- POST：创建资源
- PUT：全量更新
- PATCH：部分更新
- DELETE：删除资源

### 3.2 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2026-01-01T00:00:00"
}
```

### 3.3 错误码规范
- 200：成功
- 400：参数错误
- 401：未认证
- 403：无权限
- 404：资源不存在
- 500：服务器错误
- 503：服务不可用

## 四、数据库规范

### 4.1 表设计
- 必须有主键（建议使用自增ID或雪花ID）
- 必须有 created_at 和 updated_at 字段
- 软删除使用 deleted_at 字段
- 索引命名：idx_表名_字段名
- 外键：原则上不使用物理外键

### 4.2 SQL 规范
- 禁止 SELECT *
- 禁止不带 WHERE 的 UPDATE/DELETE
- 大表查询必须有分页
- 联表查询不超过3张表
- 慢查询阈值：200ms

## 五、部署规范

### 5.1 环境划分
- dev：开发环境（开发自测）
- test：测试环境（QA测试）
- staging：预发布环境（上线前验证）
- prod：生产环境

### 5.2 发布流程
- 代码合并到 release 分支
- 自动化测试通过
- 部署到 staging 环境
- QA 回归测试通过
- 审批后部署到 prod
- 线上验证无误后，合并到 main


