package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;
    private final QuestionService questionService;

    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    // ==================== 非流式端点（兼容） ====================

    @GetMapping("/analysis/{questionId}")
    public Result<Object> analysis(@PathVariable Long questionId) {
        Question question = questionService.getById(questionId);
        if (question == null) {
            return Result.error("题目不存在");
        }

        String prompt = buildAnalysisPrompt(question);

        String url = aiServiceUrl + "/api/ai/chat";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("message", prompt);
        body.put("history", List.of());
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            String reply = resp.getBody() != null ? (String) resp.getBody().get("reply") : null;
            if (reply != null && reply.startsWith("错误：")) {
                return Result.error(reply);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("source", "llm");
            result.put("analysis", reply);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.error("AI 解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/chat")
    public Result<Object> chat(@RequestBody Map<String, Object> body) {
        String url = aiServiceUrl + "/api/ai/chat";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
        return Result.ok(resp.getBody());
    }

    // ==================== 流式端点（SSE） ====================

    @GetMapping(value = "/analysis/{questionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analysisStream(@PathVariable Long questionId) {
        Question question = questionService.getById(questionId);
        if (question == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("题目不存在"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        String prompt = buildAnalysisPrompt(question);
        String url = aiServiceUrl + "/api/ai/chat/stream";

        Map<String, Object> body = new HashMap<>();
        body.put("message", prompt);
        body.put("history", List.of());
        return proxySseStream(url, body);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        String url = aiServiceUrl + "/api/ai/chat/stream";
        return proxySseStream(url, body);
    }

    private SseEmitter proxySseStream(String targetUrl, Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 分钟超时

        sseExecutor.execute(() -> {
            try {
                SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                factory.setConnectTimeout(30000);
                factory.setReadTimeout(300000);
                factory.setBufferRequestBody(false); // 关键：不缓冲请求体，实现真流式

                RestTemplate streamingTemplate = new RestTemplate(factory);

                RequestCallback requestCallback = req -> {
                    req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter()
                            .write(body, MediaType.APPLICATION_JSON, req);
                };

                ResponseExtractor<Void> responseExtractor = resp -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resp.getBody(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) {
                                    emitter.complete();
                                    return null;
                                }
                                if (data.startsWith("[ERROR]")) {
                                    emitter.send(SseEmitter.event()
                                            .name("error")
                                            .data(data.substring(7).trim()));
                                    emitter.complete();
                                    return null;
                                }
                                emitter.send(SseEmitter.event().name("message").data(data));
                            }
                        }
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("SSE stream error", e);
                        emitter.completeWithError(e);
                    }
                    return null;
                };

                streamingTemplate.execute(targetUrl, HttpMethod.POST, requestCallback, responseExtractor);

            } catch (Exception e) {
                log.error("Proxy stream failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI 服务连接失败: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.debug("SSE completed"));
        emitter.onTimeout(() -> log.warn("SSE timeout"));
        emitter.onError((e) -> log.error("SSE error", e));

        return emitter;
    }

    private String buildAnalysisPrompt(Question question) {
        String optionText = "";
        String options = question.getOptions();
        if (options != null && !options.isEmpty()) {
            optionText = options;
        }

        return String.format(
            "请对以下公务员考试题目进行详细解析：\n\n题目：%s\n选项：%s\n正确答案：%s\n\n请从以下角度进行解析：\n1. 正确答案分析：为什么选这个答案\n2. 各选项分析：每个选项对在哪里/错在哪里\n3. 知识点总结：这道题涉及的核心知识点\n4. 解题技巧：类似题目的解题思路",
            question.getContent(), optionText, question.getAnswer()
        );
    }
}
