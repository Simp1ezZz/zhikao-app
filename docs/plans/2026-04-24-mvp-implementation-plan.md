# 智考 MVP 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 1个月内上线智考MVP，以AI解析生成为核心亮点，实现"注册→刷题→看AI解析→记错题"的完整闭环。

**Architecture:** Java Spring Boot 后端处理全部业务逻辑，Python FastAPI 独立服务负责 LLM 调用与缓存，Vue 3 + Element Plus 做管理后台，Flutter Web 做用户端。四者通过 REST API 通信，Docker Compose 统一部署。

**Tech Stack:** Java 17 / Spring Boot 3 / MyBatis-Plus / MySQL 8 / Redis / Python 3.11 / FastAPI / Vue 3 / Element Plus / Flutter 3 / Docker Compose / Nginx

**Design Doc:** `docs/plans/2026-04-24-mvp-priority-design.md`

---

## Task 1: 项目初始化 — Java Spring Boot 后端脚手架

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/zhikao/ZhikaoApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/.gitignore`

**Step 1: 使用 Spring Initializr 生成项目骨架**

```bash
cd C:/Users/ThinkPad/Desktop/My_app
mkdir -p backend
cd backend
# 使用 https://start.spring.io/ 生成项目，或手动创建pom.xml
```

`pom.xml` 核心依赖：
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.6</version>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.5</version>
    </dependency>
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>5.2.5</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Step 2: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhikao?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: ${JWT_SECRET:zhikao-mvp-secret-key-for-dev-only}
  expiration: 604800000
```

**Step 3: 创建 Application 主类**

```java
package com.zhikao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZhikaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhikaoApplication.class, args);
    }
}
```

**Step 4: 验证项目能启动**

Run: `cd backend && mvn spring-boot:run`
Expected: 应用启动成功，监听8080端口（数据库连不上可暂时忽略）

**Step 5: Commit**

```bash
git add backend/
git commit -m "feat: 初始化Spring Boot后端项目骨架"
```

---

## Task 2: 数据库建表 + Java Entity 定义

**Files:**
- Create: `backend/src/main/resources/sql/schema.sql`
- Create: `backend/src/main/java/com/zhikao/entity/User.java`
- Create: `backend/src/main/java/com/zhikao/entity/Question.java`
- Create: `backend/src/main/java/com/zhikao/entity/PracticeRecord.java`
- Create: `backend/src/main/java/com/zhikao/entity/ErrorNote.java`
- Create: `backend/src/main/java/com/zhikao/entity/Collection.java`
- Create: `backend/src/main/java/com/zhikao/enums/Subject.java`
- Create: `backend/src/main/java/com/zhikao/enums/ErrorType.java`

**Step 1: 创建数据库建表SQL**

`sql/schema.sql`：
```sql
CREATE DATABASE IF NOT EXISTS zhikao DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zhikao;

CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(50) DEFAULT '',
    `avatar` VARCHAR(255) DEFAULT '',
    `exam_type` VARCHAR(20) DEFAULT '',
    `target_score` INT DEFAULT 0,
    `role` VARCHAR(10) DEFAULT 'USER',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `question` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `subject` VARCHAR(20) NOT NULL COMMENT '科目',
    `module` VARCHAR(30) NOT NULL COMMENT '模块',
    `knowledge_point` VARCHAR(50) DEFAULT '' COMMENT '知识点',
    `type` VARCHAR(10) NOT NULL DEFAULT 'SINGLE' COMMENT '题型',
    `difficulty` TINYINT NOT NULL DEFAULT 3 COMMENT '难度1-5',
    `content` TEXT NOT NULL COMMENT '题干',
    `options` JSON COMMENT '选项',
    `answer` VARCHAR(10) NOT NULL COMMENT '正确答案',
    `analysis` TEXT COMMENT '预设解析',
    `source` VARCHAR(50) DEFAULT '' COMMENT '来源',
    `frequency` VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '考频',
    `estimated_time` INT DEFAULT 60 COMMENT '预估答题时间(秒)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_subject_module` (`subject`, `module`),
    INDEX `idx_difficulty` (`difficulty`)
);

CREATE TABLE `practice_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` VARCHAR(10) NOT NULL,
    `is_correct` BOOLEAN NOT NULL,
    `time_spent` INT DEFAULT 0 COMMENT '耗时(秒)',
    `mode` VARCHAR(10) NOT NULL DEFAULT 'PRACTICE' COMMENT 'PRACTICE/EXAM',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_question_id` (`question_id`)
);

CREATE TABLE `error_note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `error_types` JSON COMMENT '错因选项数组',
    `note` VARCHAR(200) DEFAULT '' COMMENT '补充说明',
    `review_count` INT DEFAULT 0,
    `mastered` BOOLEAN DEFAULT FALSE,
    `next_review_at` DATETIME COMMENT '下次复习时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_user_question` (`user_id`, `question_id`)
);

CREATE TABLE `collection` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `note` VARCHAR(500) DEFAULT '',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX `idx_user_question` (`user_id`, `question_id`)
);
```

**Step 2: 创建 Entity 类（以 Question 为例，其余类似）**

```java
package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subject;
    private String module;
    private String knowledgePoint;
    private String type;
    private Integer difficulty;
    private String content;
    private String options; // JSON字符串
    private String answer;
    private String analysis;
    private String source;
    private String frequency;
    private Integer estimatedTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

其余 Entity（User、PracticeRecord、ErrorNote、Collection）按设计文档中的字段对应创建。

**Step 3: 创建枚举类**

```java
package com.zhikao.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public enum Subject {
    SPEECH("言语理解与表达"),
    QUANTITY("数量关系"),
    JUDGMENT("判断推理"),
    DATA_ANALYSIS("资料分析"),
    COMMON_SENSE("常识判断"),
    ESSAY("申论");

    private final String label;
}
```

```java
package com.zhikao.enums;

public enum ErrorType {
    KNOWLEDGE_GAP("知识盲区"),
    CONCEPT_CONFUSION("概念混淆"),
    MISREAD("审题失误"),
    CALCULATION("计算错误"),
    CARELESS("粗心大意"),
    WRONG_METHOD("方法不对"),
    TIME_SHORTAGE("时间不够"),
    GUESS("猜的");

    private final String label;
    ErrorType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
```

**Step 4: 执行建表SQL，验证**

Run: `mysql -u root -p < backend/src/main/resources/sql/schema.sql`
Expected: 数据库和5张表创建成功

**Step 5: Commit**

```bash
git add backend/src/main/resources/sql/ backend/src/main/java/com/zhikao/entity/ backend/src/main/java/com/zhikao/enums/
git commit -m "feat: 数据库建表SQL + Entity类定义"
```

---

## Task 3: 管理后台 — Vue 3 + Element Plus 脚手架

**Files:**
- Create: `admin/` 整个Vue项目

**Step 1: 用 Vite 创建 Vue 3 项目**

```bash
cd C:/Users/ThinkPad/Desktop/My_app
npm create vite@latest admin -- --template vue
cd admin
npm install
npm install element-plus @element-plus/icons-vue vue-router@4 axios
```

**Step 2: 配置 Element Plus 和 Router**

`src/main.js`：
```javascript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
```

**Step 3: 创建基础路由和布局**

`src/router/index.js`：配置 `/login` 和 `/` （管理后台首页含题目管理）
`src/layout/AdminLayout.vue`：简单的侧边栏 + 内容区布局
`src/views/Login.vue`：登录页
`src/views/QuestionManage.vue`：题目管理页（先占位）

**Step 4: 验证项目能启动**

Run: `cd admin && npm run dev`
Expected: Vite 启动成功，浏览器能看到登录页面

**Step 5: Commit**

```bash
git add admin/
git commit -m "feat: 初始化Vue3管理后台项目(Element Plus + Router)"
```

---

## Task 4: 后端 — 题目 CRUD API + Excel 导入

**Files:**
- Create: `backend/src/main/java/com/zhikao/mapper/QuestionMapper.java`
- Create: `backend/src/main/java/com/zhikao/service/QuestionService.java`
- Create: `backend/src/main/java/com/zhikao/service/impl/QuestionServiceImpl.java`
- Create: `backend/src/main/java/com/zhikao/controller/admin/AdminQuestionController.java`
- Create: `backend/src/main/java/com/zhikao/dto/QuestionImportDTO.java`
- Test: `backend/src/test/java/com/zhikao/service/QuestionServiceTest.java`

**Step 1: 写 QuestionMapper 的测试（验证基础查询）**

**Step 2: 实现 QuestionMapper + QuestionService（CRUD）**

Controller 暴露：
- `GET /api/admin/questions` — 分页列表
- `GET /api/admin/questions/{id}` — 详情
- `PUT /api/admin/questions/{id}` — 编辑
- `DELETE /api/admin/questions/{id}` — 删除
- `POST /api/admin/questions/import` — Excel批量导入

**Step 3: 实现 Excel 导入**

使用 Apache POI 读取 Excel，每行对应一道题，字段映射到 Question Entity。导入时校验必填字段，返回成功/失败条数。

**Step 4: 管理后台前端对接题目CRUD**

`admin/src/views/QuestionManage.vue`：
- Element Plus Table 展示题目列表
- 分页 + 按科目/模块筛选
- 编辑弹窗
- 删除确认
- Excel上传导入按钮

**Step 5: 端到端验证：通过管理后台导入一批测试题目**

**Step 6: Commit**

```bash
git add backend/src/main/java/com/zhikao/mapper/ backend/src/main/java/com/zhikao/service/ backend/src/main/java/com/zhikao/controller/ backend/src/main/java/com/zhikao/dto/ admin/src/
git commit -m "feat: 题目CRUD + Excel导入(后端+管理后台)"
```

---

## Task 5: 后端 — 用户注册/登录（JWT）

**Files:**
- Create: `backend/src/main/java/com/zhikao/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/zhikao/service/AuthService.java`
- Create: `backend/src/main/java/com/zhikao/service/impl/AuthServiceImpl.java`
- Create: `backend/src/main/java/com/zhikao/controller/AuthController.java`
- Create: `backend/src/main/java/com/zhikao/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/zhikao/security/JwtTokenProvider.java`
- Create: `backend/src/main/java/com/zhikao/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/zhikao/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/zhikao/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/zhikao/dto/AuthResponse.java`

**Step 1: 实现 JwtTokenProvider**

生成 token、解析 token、验证 token 的工具类。

**Step 2: 实现 SecurityConfig**

配置 Spring Security：
- `/api/auth/**` 放行
- `/api/admin/**` 需要 ADMIN 角色
- 其他接口需要认证
- 添加 JwtAuthenticationFilter

**Step 3: 实现 AuthService + AuthController**

- `POST /api/auth/register` — 注册（用户名+密码，密码BCrypt加密）
- `POST /api/auth/login` — 登录（返回JWT token）

**Step 4: 写测试 — 注册 + 登录 + 受保护接口访问**

**Step 5: 验证：用curl/Postman测试注册登录流程**

Run: `curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{"username":"test","password":"123456"}'`
Expected: 注册成功

Run: `curl -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"test","password":"123456"}'`
Expected: 返回JWT token

**Step 6: Commit**

```bash
git add backend/src/main/java/com/zhikao/security/ backend/src/main/java/com/zhikao/config/ backend/src/main/java/com/zhikao/controller/AuthController.java backend/src/main/java/com/zhikao/service/AuthService.java
git commit -m "feat: 用户注册登录(JWT + Spring Security)"
```

---

## Task 6: 后端 — 题库浏览 + 刷题 API

**Files:**
- Create: `backend/src/main/java/com/zhikao/controller/QuestionController.java`
- Create: `backend/src/main/java/com/zhikao/controller/PracticeController.java`
- Create: `backend/src/main/java/com/zhikao/service/PracticeService.java`
- Create: `backend/src/main/java/com/zhikao/service/impl/PracticeServiceImpl.java`
- Create: `backend/src/main/java/com/zhikao/mapper/PracticeRecordMapper.java`

**Step 1: 题库浏览API**

`QuestionController` 暴露：
- `GET /api/questions` — 分页查询，支持 subject/module/difficulty 筛选
- `GET /api/questions/{id}` — 题目详情（不含answer，防止前端泄露答案）
- `GET /api/subjects` — 返回科目→模块→知识点的树形结构

**Step 2: 刷题API — 开始练习**

`PracticeController`：
- `POST /api/practice/start` — 参数：科目/模块/题目数量 → 返回一组题目ID列表
- `POST /api/practice/submit` — 参数：题目ID + 用户答案 + 耗时 → 返回正确/错误 + 正确答案

**Step 3: 做题记录自动存储**

每次 submit 时自动保存到 `practice_record` 表。如果答错，自动在 `error_note` 表创建记录（错因留空，等用户标注）。

**Step 4: 写测试**

**Step 5: Commit**

```bash
git add backend/src/main/java/com/zhikao/controller/QuestionController.java backend/src/main/java/com/zhikao/controller/PracticeController.java backend/src/main/java/com/zhikao/service/PracticeService.java
git commit -m "feat: 题库浏览 + 刷题API(逐题模式)"
```

---

## Task 7: Python AI 服务 — LLM 解析接口

**Files:**
- Create: `ai-service/requirements.txt`
- Create: `ai-service/app/main.py`
- Create: `ai-service/app/config.py`
- Create: `ai-service/app/services/llm_service.py`
- Create: `ai-service/app/routers/analysis.py`
- Create: `ai-service/app/routers/chat.py`

**Step 1: 初始化 FastAPI 项目**

```bash
mkdir -p ai-service/app/routers ai-service/app/services
```

`requirements.txt`：
```
fastapi==0.111.0
uvicorn==0.29.0
httpx==0.27.0
python-dotenv==1.0.1
redis==5.0.4
openai==1.28.0
```

**Step 2: 实现 LLM 统一适配层**

`app/services/llm_service.py`：
- 从配置文件读取当前供应商（deepseek/qwen/zhipu）
- 统一调用接口，屏蔽不同供应商的请求格式差异
- 使用 httpx 或 openai SDK（大部分国产模型兼容 OpenAI 格式）

**Step 3: 实现解析生成接口**

`app/routers/analysis.py`：
- `GET /api/ai/analysis/{question_id}` — Java后端调用此接口
- 先查 Redis 缓存，有则直接返回
- 无缓存则调用 LLM，prompt 包含题目+选项+正确答案，要求生成详细解析
- 结果写入 Redis 缓存（key: `analysis:{question_id}`）

**Step 4: 实现对话接口（基础版）**

`app/routers/chat.py`：
- `POST /api/ai/chat` — 接收用户消息，返回AI回复
- 系统提示词设定为考公辅导老师角色

**Step 5: 验证：手动调用接口测试**

Run: `cd ai-service && uvicorn app.main:app --port 8000`
Expected: FastAPI 启动成功，访问 http://localhost:8000/docs 可看 Swagger 文档

**Step 6: Commit**

```bash
git add ai-service/
git commit -m "feat: Python AI服务(LLM解析+对话+缓存)"
```

---

## Task 8: 后端 — AI 服务代理 + 错题本 API

**Files:**
- Modify: `backend/src/main/java/com/zhikao/controller/PracticeController.java`
- Create: `backend/src/main/java/com/zhikao/controller/AiController.java`
- Create: `backend/src/main/java/com/zhikao/controller/ErrorNoteController.java`
- Create: `backend/src/main/java/com/zhikao/service/ErrorNoteService.java`
- Create: `backend/src/main/java/com/zhikao/service/impl/ErrorNoteServiceImpl.java`
- Create: `backend/src/main/java/com/zhikao/mapper/ErrorNoteMapper.java`

**Step 1: AI 代理接口**

`AiController` 转发请求到 Python 服务：
- `GET /api/ai/analysis/{questionId}` → 转发到 Python 服务
- `POST /api/ai/chat` → 转发到 Python 服务

使用 RestTemplate 或 WebClient 调用 `http://ai-service:8000`。

**Step 2: 错题本 API**

`ErrorNoteController`：
- `GET /api/errors` — 错题列表，支持按科目/错因筛选，分页
- `POST /api/errors` — 标注错因（error_types数组 + note）
- `PUT /api/errors/{id}` — 修改错因标注
- `POST /api/errors/{id}/review` — 复习错题（review_count++，更新next_review_at）

**Step 3: 收藏 API**

- `POST /api/collections` — 收藏
- `DELETE /api/collections/{id}` — 取消
- `GET /api/collections` — 收藏列表

**Step 4: 学习数据 API**

- `GET /api/stats/overview` — 聚合查询：总做题量、正确率、学习时长、各科目正确率
- `GET /api/stats/trend` — 按日/周聚合的正确率趋势数据

**Step 5: 写测试**

**Step 6: Commit**

```bash
git add backend/src/main/java/com/zhikao/
git commit -m "feat: AI代理接口 + 错题本 + 收藏 + 学习数据API"
```

---

## Task 9: Flutter Web 前端 — 项目搭建 + 登录

**Files:**
- Create: `frontend/` 整个Flutter项目

**Step 1: 创建 Flutter Web 项目**

```bash
cd C:/Users/ThinkPad/Desktop/My_app
flutter create --platforms=web frontend
cd frontend
flutter pub add dio provider go_router shared_preferences
```

**Step 2: 项目结构**

```
frontend/lib/
├── main.dart
├── app.dart
├── config/
│   ├── api_config.dart
│   └── theme.dart
├── models/
│   ├── user.dart
│   ├── question.dart
│   └── practice_record.dart
├── services/
│   ├── api_service.dart
│   ├── auth_service.dart
│   └── storage_service.dart
├── providers/
│   ├── auth_provider.dart
│   └── question_provider.dart
├── pages/
│   ├── login_page.dart
│   ├── register_page.dart
│   ├── home_page.dart
│   ├── question_list_page.dart
│   ├── practice_page.dart
│   ├── error_note_page.dart
│   └── profile_page.dart
└── widgets/
    ├── question_card.dart
    ├── answer_feedback.dart
    └── error_tag_dialog.dart
```

**Step 3: 配置主题（简约清爽风格）**

`config/theme.dart`：白色背景、浅灰卡片、蓝色主色调、清晰文字层次。

**Step 4: 实现登录/注册页**

- 简洁的表单：用户名 + 密码 + 登录/注册切换
- 调用后端 `/api/auth/login` 和 `/api/auth/register`
- 登录成功后 token 存 SharedPreferences，跳转首页

**Step 5: 验证：浏览器能打开登录页，输入账号密码能登录**

Run: `cd frontend && flutter run -d chrome`
Expected: Chrome 打开，显示登录页面，能完成注册登录流程

**Step 6: Commit**

```bash
git add frontend/
git commit -m "feat: Flutter Web前端项目(主题+登录注册)"
```

---

## Task 10: Flutter Web — 题库浏览 + 逐题刷题

**Files:**
- Create: `frontend/lib/pages/question_list_page.dart`
- Create: `frontend/lib/pages/practice_page.dart`
- Create: `frontend/lib/widgets/question_card.dart`
- Create: `frontend/lib/widgets/answer_feedback.dart`
- Create: `frontend/lib/providers/question_provider.dart`

**Step 1: 题库浏览页**

- 顶部：科目Tab（言语理解/数量关系/判断推理/资料分析/常识判断）
- 科目下显示模块列表，点击进入题目列表
- 题目列表用卡片展示：题号、题干预览、难度星级、考频标签
- 底部："开始练习"按钮

**Step 2: 逐题刷题页**

- 顶部：进度指示（第3/20题）+ 计时器
- 中部：题目卡片（题干 + ABCD选项）
- 底部：提交按钮
- 提交后：
  - 正确 → 绿色反馈 + 简短鼓励 + "下一题"按钮
  - 错误 → 红色反馈 + 显示正确答案 + AI解析区域 + 错因标注弹窗
- AI解析区域：先显示预设解析（如有），下方有"查看AI详细解析"按钮，点击调用AI接口

**Step 3: 错因标注弹窗**

- 8个预设选项（多选checkbox）
- 补充说明输入框（最多200字）
- "跳过"和"提交标注"按钮

**Step 4: 端到端验证：完整走一遍刷题流程**

**Step 5: Commit**

```bash
git add frontend/lib/
git commit -m "feat: 题库浏览 + 逐题刷题(AI解析+错因标注)"
```

---

## Task 11: Flutter Web — 错题本 + 个人中心 + 首页

**Files:**
- Create: `frontend/lib/pages/error_note_page.dart`
- Create: `frontend/lib/pages/profile_page.dart`
- Modify: `frontend/lib/pages/home_page.dart`

**Step 1: 首页**

- 今日学习数据卡片（做题量/正确率/学习时长）
- 快捷入口：继续刷题 / 错题复习 / 题库浏览
- 考试倒计时

**Step 2: 错题本页**

- 顶部筛选：按科目 / 按错因类型
- 错题卡片列表：题干预览 + 错因标签 + 做错时间
- 点击展开：完整题目 + 我的答案 vs 正确答案 + AI解析
- 支持修改错因标注
- "复习此题"按钮

**Step 3: 个人中心页**

- 个人信息展示（昵称、备考目标）
- 学习数据看板：累计天数、做题量、正确率、时长
- 收藏题目列表
- 设置（深色模式开关等占位）
- 关于页面

**Step 4: 配置底部导航栏**

首页 / 题库 / 我的 — 三个Tab的底部导航。

**Step 5: Commit**

```bash
git add frontend/lib/
git commit -m "feat: 首页 + 错题本 + 个人中心"
```

---

## Task 12: 试卷模式（整套做完出结果）

**Files:**
- Modify: `frontend/lib/pages/practice_page.dart`
- Create: `frontend/lib/pages/exam_result_page.dart`
- Modify: `backend/src/main/java/com/zhikao/controller/PracticeController.java`

**Step 1: 后端新增考试模式API**

- `POST /api/practice/exam/start` — 参数：科目/套题ID → 返回完整题目列表 + 限时
- `POST /api/practice/exam/submit` — 批量提交答案 → 返回成绩报告

**Step 2: Flutter前端 — 考试界面**

- 顶部倒计时（大字显示）
- 答题卡按钮（弹出所有题目答/未答状态网格）
- 题目标记功能（标记疑难题）
- 交卷确认弹窗

**Step 3: 成绩报告页**

- 总分、正确/错误/未答题数
- 各科目正确率柱状图
- "查看逐题解析"和"标注错因"按钮

**Step 4: Commit**

```bash
git add frontend/lib/ backend/src/main/java/com/zhikao/controller/PracticeController.java
git commit -m "feat: 试卷模式(整套做完+成绩报告)"
```

---

## Task 13: 基础学习报告

**Files:**
- Create: `frontend/lib/pages/report_page.dart`
- Modify: `frontend/lib/pages/home_page.dart`

**Step 1: 调用 GET /api/stats/overview 获取数据**

**Step 2: 展示内容**

- 累计学习天数、做题量、正确率、学习时长
- 各科目正确率雷达图/柱状图
- 最近7天正确率折线趋势
- 最近错题最多的知识点TOP5

**Step 3: Commit**

```bash
git add frontend/lib/
git commit -m "feat: 基础学习报告页面"
```

---

## Task 14: Docker Compose + 部署

**Files:**
- Create: `docker-compose.yml`
- Create: `backend/Dockerfile`
- Create: `ai-service/Dockerfile`
- Create: `nginx/nginx.conf`

**Step 1: 编写各服务 Dockerfile**

**Step 2: 编写 docker-compose.yml**

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_DATABASE: zhikao
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/src/main/resources/sql/schema.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
    environment:
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}

  ai-service:
    build: ./ai-service
    ports:
      - "8000:8000"
    depends_on:
      - redis
    environment:
      LLM_PROVIDER: ${LLM_PROVIDER:-deepseek}
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf
      - ./frontend/build/web:/usr/share/nginx/html

volumes:
  mysql_data:
```

**Step 3: Nginx 配置**

- `/` → Flutter Web 静态文件
- `/api/` → 反向代理到 backend:8080
- `/admin/` → 管理后台静态文件

**Step 4: 本地验证 docker-compose up**

Run: `docker-compose up --build`
Expected: 所有服务启动成功，浏览器访问 http://localhost 能看到智考首页

**Step 5: Commit**

```bash
git add docker-compose.yml backend/Dockerfile ai-service/Dockerfile nginx/
git commit -m "feat: Docker Compose部署配置"
```

---

## Task 15: 测试数据填充 + 端到端测试

**Step 1: 准备一批测试题目（至少50道，覆盖5个科目）**

Excel格式，通过管理后台导入。

**Step 2: 端到端走通完整流程**

1. 注册新用户
2. 浏览题库，按科目筛选
3. 开始逐题练习，做10道题
4. 查看AI解析
5. 标注错因
6. 查看错题本
7. 做一套试卷模式
8. 查看学习报告
9. 收藏题目

**Step 3: 修复发现的Bug**

**Step 4: Commit**

```bash
git add .
git commit -m "feat: 测试数据 + 端到端验证 + Bug修复"
```

---

## 任务依赖关系

```
Task 1 (Spring Boot骨架)
  → Task 2 (数据库+Entity)
    → Task 4 (题目CRUD+导入)  ← Task 3 (Vue管理后台)
    → Task 5 (JWT认证)
      → Task 6 (刷题API)
        → Task 8 (AI代理+错题本)

Task 7 (Python AI服务) — 独立于Task 1-6，可并行

Task 9 (Flutter骨架) — 可在Task 5完成后开始
  → Task 10 (题库+刷题页面)
    → Task 11 (错题本+个人中心)
      → Task 12 (试卷模式)
        → Task 13 (学习报告)

Task 14 (Docker部署) — 所有功能完成后
Task 15 (测试+修Bug) — 最后
```
