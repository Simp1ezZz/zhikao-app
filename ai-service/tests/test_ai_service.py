from fastapi.testclient import TestClient
from unittest.mock import patch, MagicMock, AsyncMock
from app.main import app

client = TestClient(app)


def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_analysis_with_cache(monkeypatch):
    monkeypatch.setattr("app.routers.analysis.redis_client.get",
                        MagicMock(return_value="缓存的解析内容"))

    resp = client.get("/api/ai/analysis/1")
    assert resp.status_code == 200
    data = resp.json()
    assert data["source"] == "cache"
    assert data["analysis"] == "缓存的解析内容"


def test_analysis_no_cache(monkeypatch):
    monkeypatch.setattr("app.routers.analysis.redis_client.get",
                        MagicMock(return_value=None))
    monkeypatch.setattr("app.routers.analysis.redis_client.setex",
                        MagicMock())

    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "data": {
            "content": "1+1=?",
            "options": '{"A":"1","B":"2","C":"3","D":"4"}',
            "answer": "B",
        }
    }

    mock_client = AsyncMock()
    mock_client.get = AsyncMock(return_value=mock_response)
    mock_client.__aenter__ = AsyncMock(return_value=mock_client)
    mock_client.__aexit__ = AsyncMock(return_value=False)

    import httpx
    monkeypatch.setattr(httpx, "AsyncClient", MagicMock(return_value=mock_client))

    with patch("app.routers.analysis.chat") as mock_chat:
        mock_chat.return_value = "选B因为1+1=2"
        resp = client.get("/api/ai/analysis/1")
        assert resp.status_code == 200
        assert resp.json()["source"] == "llm"


def test_chat_endpoint():
    with patch("app.routers.chat.chat") as mock_chat:
        mock_chat.return_value = "这是AI回复"
        resp = client.post("/api/ai/chat", json={"message": "你好", "history": []})
        assert resp.status_code == 200
        assert resp.json()["reply"] == "这是AI回复"


def test_chat_with_history():
    with patch("app.routers.chat.chat") as mock_chat:
        mock_chat.return_value = "回复内容"
        history = [{"user": "什么是行测", "assistant": "行测是..."}]
        resp = client.post("/api/ai/chat", json={"message": "详细说说", "history": history})
        assert resp.status_code == 200
        call_args = mock_chat.call_args[0][0]
        assert len(call_args) == 4  # system + history(2) + user


def test_config_provider():
    from app.config import get_provider_config
    base_url, model = get_provider_config()
    assert base_url
    assert model


def test_llm_service():
    from app.services.llm_service import get_model
    model = get_model()
    assert model
