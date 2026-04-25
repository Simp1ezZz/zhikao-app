import os
from dotenv import load_dotenv

load_dotenv()

LLM_PROVIDER = os.getenv("LLM_PROVIDER", "deepseek")
LLM_API_KEY = os.getenv("LLM_API_KEY", "")
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")
LLM_MODEL = os.getenv("LLM_MODEL", "deepseek-chat")

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")

BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")

PROVIDER_CONFIG = {
    "deepseek": {
        "base_url": "https://api.deepseek.com/v1",
        "model": "deepseek-chat",
        "type": "openai",
    },
    "qwen": {
        "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "model": "qwen-turbo",
        "type": "openai",
    },
    "zhipu": {
        "base_url": "https://open.bigmodel.cn/api/paas/v4",
        "model": "glm-4-flash",
        "type": "openai",
    },
    "anthropic": {
        "base_url": "https://api.anthropic.com",
        "model": "claude-3-sonnet-20240229",
        "type": "anthropic",
    },
}

def get_provider_config():
    cfg = PROVIDER_CONFIG.get(LLM_PROVIDER, PROVIDER_CONFIG["deepseek"])
    base_url = os.getenv("LLM_BASE_URL", cfg["base_url"])
    model = os.getenv("LLM_MODEL", cfg["model"])
    client_type = cfg.get("type", "openai")
    return base_url, model, client_type
