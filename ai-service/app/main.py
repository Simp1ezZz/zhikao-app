from fastapi import FastAPI
from app.routers import analysis, chat

app = FastAPI(title="智考AI服务", version="0.1.0")

app.include_router(analysis.router)
app.include_router(chat.router)

@app.get("/health")
async def health():
    return {"status": "ok"}
