from openai import OpenAI
from app.config import LLM_API_KEY, get_provider_config

_client = None

def get_client():
    global _client
    if _client is None:
        base_url, _ = get_provider_config()
        _client = OpenAI(api_key=LLM_API_KEY, base_url=base_url)
    return _client

def get_model():
    _, model = get_provider_config()
    return model

def chat(messages: list, temperature: float = 0.7) -> str:
    client = get_client()
    model = get_model()
    response = client.chat.completions.create(
        model=model,
        messages=messages,
        temperature=temperature,
    )
    return response.choices[0].message.content
