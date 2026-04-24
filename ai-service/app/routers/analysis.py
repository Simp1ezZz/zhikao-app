import json
import redis
import httpx
from fastapi import APIRouter
from app.config import REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, BACKEND_URL
from app.services.llm_service import chat

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

    prompt = f"""请对以下公务员考试题目进行详细解析：

题目：{content}
{f"选项：\n{option_text}" if option_text else ""}
正确答案：{answer}

请从以下角度进行解析：
1. 正确答案分析：为什么选这个答案
2. 各选项分析：每个选项对在哪里/错在哪里
3. 知识点总结：这道题涉及的核心知识点
4. 解题技巧：类似题目的解题思路"""

    messages = [
        {"role": "system", "content": "你是一位经验丰富的公务员考试辅导老师，擅长解析行测题目。请用清晰易懂的语言进行讲解。"},
        {"role": "user", "content": prompt},
    ]

    analysis = chat(messages, temperature=0.3)
    redis_client.setex(cache_key, 86400 * 7, analysis)

    return {"source": "llm", "analysis": analysis}
