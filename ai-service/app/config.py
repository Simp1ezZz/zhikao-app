import os
from dotenv import load_dotenv

load_dotenv()

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")

PROVIDER_CONFIG = {
    "openai": {
        "base_url": "https://api.deepseek.com/v1",
        "model": "deepseek-chat",
        "type": "openai",
    },
    "anthropic": {
        "base_url": "https://api.anthropic.com",
        "model": "claude-3-sonnet-20240229",
        "type": "anthropic",
    },
}

# 兼容：从环境变量读取兜底配置
_env_api_key = os.getenv("LLM_API_KEY", "")
_env_provider = os.getenv("LLM_PROVIDER", "openai")
_env_base_url = os.getenv("LLM_BASE_URL", "")
_env_model = os.getenv("LLM_MODEL", "")

_redis_client = None

def get_redis_client():
    global _redis_client
    if _redis_client is None:
        import redis
        _redis_client = redis.Redis(
            host=REDIS_HOST, port=REDIS_PORT,
            password=REDIS_PASSWORD or None,
            decode_responses=True,
        )
    return _redis_client

def get_llm_config():
    """从 Redis 读取 LLM 配置，优先读取 active_config JSON，fallback 到单个 key 或环境变量"""
    api_key = _env_api_key
    provider = _env_provider
    base_url = _env_base_url
    model = _env_model

    try:
        r = get_redis_client()
        active_json = r.get("zhikao:config:llm.active_config")
        if active_json:
            import json
            active = json.loads(active_json)
            api_key = active.get("api_key") or api_key
            provider = active.get("provider") or provider
            base_url = active.get("base_url") or base_url
            model = active.get("model") or model
        else:
            api_key = r.get("zhikao:config:llm.api_key") or api_key
            provider = r.get("zhikao:config:llm.provider") or provider
            base_url = r.get("zhikao:config:llm.base_url") or base_url
            model = r.get("zhikao:config:llm.model") or model
    except Exception:
        pass

    cfg = PROVIDER_CONFIG.get(provider, PROVIDER_CONFIG["openai"])
    if not base_url:
        base_url = cfg["base_url"]
    if not model:
        model = cfg["model"]
    client_type = cfg.get("type", "openai")

    return {
        "api_key": api_key,
        "provider": provider,
        "base_url": base_url,
        "model": model,
        "client_type": client_type,
    }

def get_provider_config():
    """兼容旧接口"""
    cfg = get_llm_config()
    return cfg["base_url"], cfg["model"], cfg["client_type"]
