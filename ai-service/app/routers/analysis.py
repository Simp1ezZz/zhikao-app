import json
import redis
import httpx
from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from app.config import REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, BACKEND_URL
from app.services.llm_service import chat, chat_stream

router = APIRouter(prefix="/api/ai", tags=["analysis"])

redis_client = redis.Redis(
    host=REDIS_HOST, port=REDIS_PORT,
    password=REDIS_PASSWORD or None,
    decode_responses=True,
)

@router.get("/analysis/{question_id}")
async def get_analysis(question_id: int):
    cache_key = f"analysis:{question_id}"
    cached = redis_client.get(cache_key)
    if cached:
        return {"source": "cache", "analysis": cached}

    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{BACKEND_URL}/api/admin/questions/{question_id}")
        if resp.status_code != 200:
            return {"source": "error", "analysis": "题目获取失败"}
        question = resp.json().get("data", {})

    content = question.get("content", "")
    options = question.get("options", "")
    answer = question.get("answer", "")

    if options:
        try:
            opts = json.loads(options) if isinstance(options, str) else options
            option_text = "\n".join([f"{k}. {v}" for k, v in opts.items()])
        except Exception:
            option_text = str(options)
    else:
        option_text = ""

    option_section = f"选项：\n{option_text}" if option_text else ""
    prompt = f"""请对以下公务员考试题目进行详细解析：

题目：{content}
{option_section}
正确答案：{answer}

请从以下角度进行解析：
1. 正确答案分析：为什么选这个答案
2. 各选项分析：每个选项对在哪里/错在哪里
3. 知识点总结：这道题涉及的核心知识点
4. 解题技巧：类似题目的解题思路

注意：仅对该题作出回答即可，不需要对用户有任何追问，比如：是否需要相关试题巩固练习
"""

    messages = [
        {"role": "system", "content": "你是一位经验丰富的公务员考试辅导老师，擅长解析行测题目。请用清晰易懂的语言进行讲解。"},
        {"role": "user", "content": prompt},
    ]

    analysis = chat(messages, temperature=0.3)
    redis_client.setex(cache_key, 86400 * 7, analysis)

    return {"source": "llm", "analysis": analysis}


def _sse_analysis_stream(messages: list, cache_key: str):
    buffer = ""
    for chunk in chat_stream(messages, temperature=0.3):
        if chunk.startswith("[ERROR]"):
            yield f"data: {chunk}\n\n"
            yield "data: [DONE]\n\n"
            return
        buffer += chunk
        yield f"data: {chunk}\n\n"
    redis_client.setex(cache_key, 86400 * 7, buffer)
    yield "data: [DONE]\n\n"


@router.get("/analysis/{question_id}/stream")
async def get_analysis_stream(question_id: int):
    cache_key = f"analysis:{question_id}"
    cached = redis_client.get(cache_key)
    if cached:
        async def cached_stream():
            yield f"data: {cached}\n\n"
            yield "data: [DONE]\n\n"
        return StreamingResponse(
            cached_stream(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{BACKEND_URL}/api/admin/questions/{question_id}")
        if resp.status_code != 200:
            async def error_stream():
                yield "data: [ERROR] 题目获取失败\n\n"
                yield "data: [DONE]\n\n"
            return StreamingResponse(error_stream(), media_type="text/event-stream")
        question = resp.json().get("data", {})

    content = question.get("content", "")
    options = question.get("options", "")
    answer = question.get("answer", "")

    if options:
        try:
            opts = json.loads(options) if isinstance(options, str) else options
            option_text = "\n".join([f"{k}. {v}" for k, v in opts.items()])
        except Exception:
            option_text = str(options)
    else:
        option_text = ""

    option_section = f"选项：\n{option_text}" if option_text else ""
    prompt = f"""请对以下公务员考试题目进行详细解析：

题目：{content}
{option_section}
正确答案：{answer}

请从以下角度进行解析：
1. 正确答案分析：为什么选这个答案
2. 各选项分析：每个选项对在哪里/错在哪里
3. 知识点总结：这道题涉及的核心知识点
4. 解题技巧：类似题目的解题思路

注意：仅对该题作出回答即可，不需要对用户有任何追问，比如：是否需要相关试题巩固练习
"""

    messages = [
        {"role": "system", "content": "你是一位经验丰富的公务员考试辅导老师，擅长解析行测题目。请用清晰易懂的语言进行讲解。"},
        {"role": "user", "content": prompt},
    ]

    return StreamingResponse(
        _sse_analysis_stream(messages, cache_key),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
