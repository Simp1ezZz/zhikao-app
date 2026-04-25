from openai import OpenAI
import anthropic
from app.config import LLM_API_KEY, get_provider_config

_openai_client = None
_anthropic_client = None

def get_openai_client(base_url):
    global _openai_client
    if _openai_client is None:
        _openai_client = OpenAI(api_key=LLM_API_KEY, base_url=base_url)
    return _openai_client

def get_anthropic_client(base_url):
    global _anthropic_client
    if _anthropic_client is None:
        _anthropic_client = anthropic.Anthropic(api_key=LLM_API_KEY, base_url=base_url)
    return _anthropic_client

def chat(messages: list, temperature: float = 0.7) -> str:
    base_url, model, client_type = get_provider_config()

    if client_type == "anthropic":
        client = get_anthropic_client(base_url)
        system_msg = ""
        anthropic_messages = []
        for m in messages:
            if m["role"] == "system":
                system_msg = m["content"]
            else:
                anthropic_messages.append({"role": m["role"], "content": m["content"]})
        response = client.messages.create(
            model=model,
            max_tokens=4096,
            messages=anthropic_messages,
            system=system_msg if system_msg else anthropic.NOT_GIVEN,
            temperature=temperature,
        )
        return response.content[0].text
    else:
        client = get_openai_client(base_url)
        response = client.chat.completions.create(
            model=model,
            messages=messages,
            temperature=temperature,
        )
        if isinstance(response, str):
            return response
        return response.choices[0].message.content
