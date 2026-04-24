# 智考 MVP 优先级设计与技术方案

> 日期：2026-04-24
> 状态：已确认

## 约束条件

| 维度 | 说明 |
|------|------|
| 开发周期 | 1个月上线MVP |
| 每日投入 | 业余时间 2-4小时 |
| 总工时 | 约 90小时（按每天3小时 × 30天） |
| 核心策略 | AI单点突破 —— 以"AI解析生成"为唯一AI亮点功能 |
| 开发者 | 个人，Java后端为主 |

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 用户端前端 | Flutter Web | 后续迁移Flutter Mobile |
| 管理后台前端 | Vue 3 + Element Plus | 题目管理、数据导入 |
| 后端业务服务 | Java (Spring Boot) | 全部业务逻辑 |
| 后端AI服务 | Python (FastAPI) | LLM API调用 + 缓存 |
| 数据库 | MySQL + Redis | 主数据 + 缓存 |
| LLM | 统一适配层 | 配置化切换供应商 |
| 部署 | 阿里云轻量服务器 + Docker Compose | |

## 功能优先级分层

### 第0层：基础设施（最先做，~18h）

| 功能 | 技术 | 工时 |
|------|------|------|
| 数据库设计 + 建表 | MySQL | 3h |
| 题目数据结构定义 | Java Entity | 2h |
| 管理后台 - 登录 + 题目CRUD | Vue + Element Plus | 8h |
| 题目批量导入（Excel上传） | Java + Apache POI | 5h |

### 第1层：MVP核心（必须做，~52h）

| 功能 | 技术 | 工时 |
|------|------|------|
| 用户注册/登录（JWT） | Spring Boot | 6h |
| 题库浏览（按科目/考点查询） | Flutter Web | 8h |
| 逐题刷题模式（即时反馈） | Flutter Web | 12h |
| AI解析生成（核心亮点） | Python FastAPI + LLM | 8h |
| 错题本（归档+标注+浏览） | 全栈 | 10h |
| 个人中心（数据+收藏+设置） | Flutter Web | 8h |

### 第2层：MVP增强（尽量做，~20h）

| 功能 | 技术 | 工时 |
|------|------|------|
| 试卷模式（整套做完出结果） | Flutter Web | 10h |
| AI对话助手（基础版） | Python FastAPI + LLM | 6h |
| 基础学习报告 | 全栈 | 4h |

> 如果时间不够，优先砍掉AI对话助手（省6h），确保其他功能完整。

### 第3层：V1.0 补充（MVP后迭代）

- 数字敏感度训练（速算 + 找数）
- AI学习路径规划（简化版）
- 学习成就系统
- 考试资讯基础模块
- 首页推荐

### 第4层：后续版本

- 所有PRD中的P2/P3功能
- 功能储备池中的21项

## 数据库核心表设计

### subject_config 表（科目/模块/知识点配置）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(50) | 名称（科目名/模块名/知识点名） |
| parent_id | BIGINT | 父级ID（0表示顶级科目） |
| level | TINYINT | 层级（1=科目, 2=模块, 3=知识点） |
| sort_order | INT | 排序序号 |
| enabled | BOOLEAN | 是否启用 |
| created_at | DATETIME | 创建时间 |

> 树形结构：科目（如"言语理解"）→ 模块（如"片段阅读"）→ 知识点（如"主旨概括"）

### error_type_config 表（错因类型配置）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| name | VARCHAR(50) | 错因名称（如"知识盲区"、"粗心大意"） |
| sort_order | INT | 排序序号 |
| enabled | BOOLEAN | 是否启用 |
| created_at | DATETIME | 创建时间 |

> 管理员可在后台增删改错因选项，用户端自动同步显示

### user 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像URL |
| exam_type | VARCHAR(20) | 考试类型（国考/省考） |
| target_score | INT | 目标分数 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

### question 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| subject | VARCHAR(20) | 科目（关联subject_config，level=1的name） |
| module | VARCHAR(30) | 模块（关联subject_config，level=2的name） |
| knowledge_point | VARCHAR(50) | 知识点 |
| type | VARCHAR(10) | 题型（单选/多选/问答） |
| difficulty | TINYINT | 难度（1-5） |
| content | TEXT | 题干 |
| options | JSON | 选项（A/B/C/D） |
| answer | VARCHAR(10) | 正确答案 |
| analysis | TEXT | 预设解析（可为空，由AI生成） |
| source | VARCHAR(50) | 来源（真题年份/模拟题/AI生成） |
| frequency | VARCHAR(10) | 考频（高频/中频/低频） |
| estimated_time | INT | 预估答题时间（秒） |
| created_at | DATETIME | 创建时间 |

### practice_record 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 用户ID |
| question_id | BIGINT FK | 题目ID |
| user_answer | VARCHAR(10) | 用户答案 |
| is_correct | BOOLEAN | 是否正确 |
| time_spent | INT | 答题耗时（秒） |
| mode | VARCHAR(10) | 答题模式（practice/exam） |
| created_at | DATETIME | 答题时间 |

### error_note 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 用户ID |
| question_id | BIGINT FK | 题目ID |
| error_types | JSON | 错因选项（关联error_type_config，存ID数组，如[1,3,5]） |
| note | VARCHAR(200) | 用户补充说明 |
| review_count | INT | 复习次数 |
| mastered | BOOLEAN | 是否已掌握 |
| next_review_at | DATETIME | 下次复习时间 |
| created_at | DATETIME | 创建时间 |

### collection 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 用户ID |
| question_id | BIGINT FK | 题目ID |
| note | VARCHAR(500) | 收藏备注 |
| created_at | DATETIME | 收藏时间 |

## API 接口规划

### 用户模块
- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `GET /api/user/profile` - 获取个人信息
- `PUT /api/user/profile` - 更新个人信息

### 题库模块
- `GET /api/questions` - 题目列表（支持按科目/考点/难度筛选，分页）
- `GET /api/questions/{id}` - 题目详情
- `GET /api/questions/random` - 随机获取练习题
- `GET /api/subjects` - 科目列表（从subject_config读取，含模块和知识点树）
- `GET /api/error-types` - 错因类型列表（从error_type_config读取）

### 刷题模块
- `POST /api/practice/start` - 开始练习（返回一组题目）
- `POST /api/practice/submit` - 提交单题答案
- `POST /api/practice/exam/submit` - 提交整套试卷

### AI模块
- `GET /api/ai/analysis/{questionId}` - 获取AI解析（有缓存则返回缓存，否则调用LLM生成）
- `POST /api/ai/chat` - AI对话（基础版）

### 错题模块
- `GET /api/errors` - 错题列表（支持按科目/错因筛选）
- `POST /api/errors` - 标注错因
- `PUT /api/errors/{id}` - 修改错因标注
- `POST /api/errors/{id}/review` - 复习错题

### 收藏模块
- `POST /api/collections` - 收藏题目
- `DELETE /api/collections/{id}` - 取消收藏
- `GET /api/collections` - 收藏列表

### 学习数据
- `GET /api/stats/overview` - 学习数据概览（做题量/正确率/时长）
- `GET /api/stats/trend` - 成绩趋势

### 管理后台
- `POST /api/admin/questions/import` - 批量导入题目（Excel）
- `GET /api/admin/questions` - 题目管理列表
- `PUT /api/admin/questions/{id}` - 编辑题目
- `DELETE /api/admin/questions/{id}` - 删除题目
- `GET /api/admin/subjects` - 科目配置列表（树形）
- `POST /api/admin/subjects` - 新增科目/模块/知识点
- `PUT /api/admin/subjects/{id}` - 编辑科目配置
- `DELETE /api/admin/subjects/{id}` - 删除科目配置
- `GET /api/admin/error-types` - 错因类型列表
- `POST /api/admin/error-types` - 新增错因类型
- `PUT /api/admin/error-types/{id}` - 编辑错因类型
- `DELETE /api/admin/error-types/{id}` - 删除错因类型

## 开发顺序建议

```
第1周：基础设施
├── Day 1-2：数据库设计 + Java Entity + Repository
├── Day 3-4：管理后台 Vue 项目搭建 + 登录
├── Day 5-7：题目CRUD + Excel导入 + 导入一批测试数据

第2周：核心后端
├── Day 8-9：用户注册/登录（JWT）
├── Day 10-11：题库查询API + 刷题API
├── Day 12-14：Python FastAPI + LLM解析接口 + 缓存

第3周：Flutter Web前端
├── Day 15-16：Flutter项目搭建 + 登录页
├── Day 17-19：题库浏览 + 刷题页（逐题模式）
├── Day 20-21：错题本 + 个人中心

第4周：打磨上线
├── Day 22-24：试卷模式 + 学习报告
├── Day 25-26：AI对话助手（时间够则做）
├── Day 27-28：Bug修复 + 测试
├── Day 29-30：部署上线（阿里云 + Docker + Nginx）
```
