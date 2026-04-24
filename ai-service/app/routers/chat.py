from fastapi import APIRouter
from pydantic import BaseModel
from app.services.llm_service import chat

router = APIRouter(prefix="/api/ai", tags=["chat"])

SYSTEM_PROMPT = """你是一位专业的公务员考试辅导老师，名叫"智考AI"。你的职责是：
1. 回答考生关于公务员考试的各种问题
2. 讲解行测、申论各科目的知识点和解题技巧
3. 分析错题原因，给出针对性的学习建议
4. 提供备考策略和时间规划建议

请用简洁明了的语言回答，适当举例说明。如果问题与公务员考试无关，请礼貌地引导回考试相关话题。"""

class ChatRequest(BaseModel):
    message: str
    history: list = []

@router.post("/chat")
async def chat_endpoint(req: ChatRequest):
    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    for item in req.history[-10:]:
        messages.append({"role": "user", "content": item.get("user", "")})
        messages.append({"role": "assistant", "content": item.get("assistant", "")})
    messages.append({"role": "user", "content": req.message})

    reply = chat(messages)
    return {"reply": reply}
