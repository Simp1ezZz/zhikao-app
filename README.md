# 智考App - 公考刷题助手

智考App是一款面向公务员考试（行测）的在线刷题学习平台，包含题库浏览、刷题练习、错题本、模拟考试、学习报告等功能。

## 技术栈

| 模块 | 技术 |
|------|------|
| 后端 | Java 21 + Spring Boot 3.2.5 + MyBatis-Plus |
| 管理后台 | Vue 3 + Vite + Element Plus |
| 用户前端 | Flutter Web |
| AI服务 | Python 3.11 + FastAPI + OpenAI-compatible LLM |
| 数据库 | MySQL 9 + Redis 8 |

## 项目结构

```
zhikao-app/
├── backend/          # Spring Boot 后端
│   ├── src/main/resources/sql/  # 数据库脚本
│   └── Dockerfile
├── admin/            # Vue3 管理后台
├── frontend/         # Flutter Web 用户端
├── ai-service/       # Python AI服务
│   └── Dockerfile
└── docker-compose.yml
```

## 快速启动

### 1. Docker Compose 一键启动

```bash
docker-compose up -d
```

服务列表：
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- 后端API: `http://localhost:8080/api`
- AI服务: `http://localhost:8000`
- 管理后台: `http://localhost:3000`

### 2. 本地开发启动

**后端:**
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**AI服务:**
```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

**管理后台:**
```bash
cd admin
npm install
npm run dev
```

**Flutter Web:**
```bash
cd frontend
flutter run -d chrome
```

## 主要功能

- 题库浏览（科目/模块筛选、分页）
- 刷题练习（逐题模式、提交答案、查看解析）
- 错题本（错因标记、复习间隔、掌握状态）
- 模拟考试（整卷模式、倒计时、答题卡）
- 学习报告（总览、趋势、科目分析）
- AI 解析（LLM 智能题目解析 + 对话问答）
- 管理后台（题目 CRUD、Excel 导入、科目配置）

## 测试

**后端测试:**
```bash
cd backend
mvn test
```

**AI服务测试:**
```bash
cd ai-service
pytest
```

**Flutter 测试:**
```bash
cd frontend
flutter test
```

## 默认账号

- 管理员: `admin` / `123456`

## API 文档

启动后端后访问: `http://localhost:8080/swagger-ui.html` (如配置了 SpringDoc)

主要接口前缀: `/api`
- `/api/auth/**` - 认证（登录/注册）
- `/api/questions/**` - 题库（公开 GET）
- `/api/practice/**` - 刷题
- `/api/exam/**` - 模拟考试
- `/api/error-notes/**` - 错题本
- `/api/collections/**` - 收藏
- `/api/stats/**` - 统计
- `/api/ai/**` - AI 代理
- `/api/admin/**` - 管理后台（需 ADMIN 角色）
