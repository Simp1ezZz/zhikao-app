from openai import OpenAI
import openai
import anthropic
from app.config import get_llm_config


def chat(messages: list, temperature: float = 0.7) -> str:
    cfg = get_llm_config()
    api_key = cfg["api_key"]
    base_url = cfg["base_url"]
    model = cfg["model"]
    client_type = cfg["client_type"]

    if not api_key:
        return "错误：LLM API Key 未配置，请在管理后台 AI 配置页面设置。"

    try:
        if client_type == "anthropic":
            client = anthropic.Anthropic(api_key=api_key, base_url=base_url)
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
            client = OpenAI(api_key=api_key, base_url=base_url)
            response = client.chat.completions.create(
                model=model,
                messages=messages,
                temperature=temperature,
            )
            if isinstance(response, str):
                return response
            return response.choices[0].message.content
    except openai.RateLimitError as e:
        return f"错误：模型速率限制（429），请稍后重试或更换模型。详情：{e.body.get('error', {}).get('message', str(e))}"
    except openai.AuthenticationError as e:
        return f"错误：API Key 无效或已过期，请在管理后台检查 AI 配置。"
    except openai.NotFoundError as e:
        return f"错误：模型 '{model}' 不存在，请在管理后台检查 Model 配置。"
    except anthropic.RateLimitError as e:
        return f"错误：Anthropic 速率限制，请稍后重试。"
    except anthropic.AuthenticationError as e:
        return f"错误：Anthropic API Key 无效，请在管理后台检查 AI 配置。"
    except anthropic.NotFoundError as e:
        return f"错误：Anthropic 模型 '{model}' 不存在，请在管理后台检查 Model 配置。"
    except Exception as e:
        return f"错误：LLM 调用失败 - {str(e)}"


def chat_stream(messages: list, temperature: float = 0.7):
    """流式返回 LLM 响应，yield 纯文本 chunk。"""
    cfg = get_llm_config()
    api_key = cfg["api_key"]
    base_url = cfg["base_url"]
    model = cfg["model"]
    client_type = cfg["client_type"]

    if not api_key:
        yield "[ERROR] LLM API Key 未配置，请在管理后台 AI 配置页面设置。"
        return

    try:
        if client_type == "anthropic":
            client = anthropic.Anthropic(api_key=api_key, base_url=base_url)
            system_msg = ""
            anthropic_messages = []
            for m in messages:
                if m["role"] == "system":
                    system_msg = m["content"]
                else:
                    anthropic_messages.append({"role": m["role"], "content": m["content"]})
            with client.messages.stream(
                model=model,
                max_tokens=4096,
                messages=anthropic_messages,
                system=system_msg if system_msg else anthropic.NOT_GIVEN,
                temperature=temperature,
            ) as stream:
                for text in stream.text_stream:
                    yield text
        else:
            client = OpenAI(api_key=api_key, base_url=base_url)
            response = client.chat.completions.create(
                model=model,
                messages=messages,
                temperature=temperature,
                stream=True,
            )
            for chunk in response:
                if not chunk.choices:
                    continue
                delta = chunk.choices[0].delta.content
                if delta:
                    yield delta
    except openai.RateLimitError as e:
        msg = e.body.get("error", {}).get("message", str(e)) if hasattr(e, "body") else str(e)
        yield f"[ERROR] 模型速率限制（429），请稍后重试或更换模型。详情：{msg}"
    except openai.AuthenticationError as e:
        yield "[ERROR] API Key 无效或已过期，请在管理后台检查 AI 配置。"
    except openai.NotFoundError as e:
        yield f"[ERROR] 模型 '{model}' 不存在，请在管理后台检查 Model 配置。"
    except anthropic.RateLimitError as e:
        yield "[ERROR] Anthropic 速率限制，请稍后重试。"
    except anthropic.AuthenticationError as e:
        yield "[ERROR] Anthropic API Key 无效，请在管理后台检查 AI 配置。"
    except anthropic.NotFoundError as e:
        yield f"[ERROR] Anthropic 模型 '{model}' 不存在，请在管理后台检查 Model 配置。"
    except Exception as e:
        yield f"[ERROR] LLM 调用失败 - {str(e)}"
