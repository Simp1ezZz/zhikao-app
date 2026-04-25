# AI 模型配置

## 功能概述

管理后台提供 **AI 模型配置** 页面，管理员可在此维护多组大语言模型（LLM）配置，并一键切换当前使用的配置。每组配置包含 API Key、Base URL、Model 名称和 Provider 类型。配置保存在数据库 `llm_model_config` 表中，并实时同步到 Redis 缓存，AI 服务每次调用前从 Redis 读取当前激活的配置。

## 安全设计

- **API Key 不经过 HTTP 传输**：前端仅将配置保存到后端数据库，AI 服务在内部网络中通过 Redis 读取 API Key
- **配置实时生效**：切换后立即写入 Redis，AI 服务无需重启
- **密码输入框**：前端使用 `type="password"` 隐藏 API Key
- **多组配置隔离**：可同时维护多组配置（如 DeepSeek、OpenAI、Anthropic），互不干扰

## 配置项说明

| 配置项 | 说明 | 示例 |
|---|---|---|
| 配置名称 | 自定义名称，便于识别 | DeepSeek 官方 |
| API Key | LLM 服务商提供的 API Key | sk-xxx |
| Base URL | API 基础地址 | `https://api.deepseek.com/v1` |
| Model | 模型名称 | `deepseek-chat` |
| Provider | 协议类型 | `openai` / `anthropic` |
| 设为当前 | 保存后是否立即使用该配置 | Switch |

### 支持的 Provider

| Provider | type | 默认 Base URL | 默认 Model | 适用场景 |
|---|---|---|---|---|
| openai | openai | `https://api.deepseek.com/v1` | `deepseek-chat` | 兼容 OpenAI 接口格式的服务商（DeepSeek、通义千问、智谱等） |
| anthropic | anthropic | `https://api.anthropic.com` | `claude-3-sonnet-20240229` | Anthropic Claude 官方接口 |

## 使用流程

1. 登录管理后台
2. 进入左侧菜单 **AI配置**
3. 点击 **新增配置**，填写 API Key、Base URL、Model、Provider
4. 勾选 **设为当前**（或在列表页点击 **设为当前**）
5. 配置即时生效，前端用户可使用 AI 解析功能

## 技术实现

### 数据流

```
Admin 前端 → AdminLlmModelConfigController
  → LlmModelConfigService.save() → MySQL (llm_model_config 表)
  → StringRedisTemplate.set("zhikao:config:llm.active_config", JSON)

前端用户 → AiController（不带 API Key）
  → AI 服务 /api/ai/chat
  → llm_service.chat()
      → config.get_llm_config()
          → 优先读取 Redis zhikao:config:llm.active_config (JSON)
          → fallback 到单个 key (llm.api_key, llm.provider 等)
          → fallback 到环境变量
      → 创建客户端 → 调用 LLM API
```

### 关键代码

- **后端多配置管理**：`LlmModelConfigServiceImpl` 管理多组配置，切换时同步 Redis
- **Redis 缓存格式**：`zhikao:config:llm.active_config` = `{"api_key":"...","base_url":"...","model":"...","provider":"..."}`
- **AI 服务读取**：`ai-service/app/config.py` 优先读取 JSON 格式缓存
- **AI 服务调用**：`ai-service/app/services/llm_service.py` 每次调用前读取 Redis 配置

### 数据库表

```sql
CREATE TABLE `llm_model_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '配置名称',
    `api_key` VARCHAR(255) NOT NULL COMMENT 'API Key',
    `base_url` VARCHAR(255) NOT NULL COMMENT 'Base URL',
    `model` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `provider` VARCHAR(20) NOT NULL DEFAULT 'openai',
    `is_active` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否当前使用',
    `enabled` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 环境变量兜底

若 Redis 中无配置，AI 服务会回退到读取环境变量：
- `LLM_API_KEY`
- `LLM_PROVIDER`
- `LLM_BASE_URL`
- `LLM_MODEL`

这在首次启动或 Redis 不可用时提供兼容性保障。
