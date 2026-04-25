package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;
    private final QuestionService questionService;

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

    @GetMapping(value = "/analysis/{questionId}/stream")
    public void analysisStream(@PathVariable Long questionId, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        Question question = questionService.getById(questionId);
        if (question == null) {
            writeSseEvent(response, "error", "题目不存在");
            return;
        }

        String prompt = buildAnalysisPrompt(question);
        String url = aiServiceUrl + "/api/ai/chat/stream";

        Map<String, Object> body = new HashMap<>();
        body.put("message", prompt);
        body.put("history", List.of());
        proxySseStream(url, body, response);
    }

    @PostMapping(value = "/chat/stream")
    public void chatStream(@RequestBody Map<String, Object> body, HttpServletResponse response) {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        String url = aiServiceUrl + "/api/ai/chat/stream";
        proxySseStream(url, body, response);
    }

    private void proxySseStream(String targetUrl, Map<String, Object> body, HttpServletResponse response) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(300000);
        factory.setBufferRequestBody(false);

        RestTemplate streamingTemplate = new RestTemplate(factory);

        RequestCallback requestCallback = req -> {
            req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            new MappingJackson2HttpMessageConverter()
                    .write(body, MediaType.APPLICATION_JSON, req);
        };

        try {
            OutputStream out = response.getOutputStream();

            ResponseExtractor<Void> responseExtractor = resp -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resp.getBody(), StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                writeSseEvent(out, null, "[DONE]");
                                return null;
                            }
                            if (data.startsWith("[ERROR]")) {
                                writeSseEvent(out, "error", data.substring(7).trim());
                                return null;
                            }
                            writeSseEvent(out, "message", data);
                        }
                    }
                } catch (Exception e) {
                    log.error("SSE stream read error", e);
                    writeSseEvent(out, "error", "流读取错误: " + e.getMessage());
                }
                return null;
            };

            streamingTemplate.execute(targetUrl, HttpMethod.POST, requestCallback, responseExtractor);
        } catch (Exception e) {
            log.error("RestTemplate execute failed (likely connection close after stream)", e);
        }
    }

    private void writeSseEvent(OutputStream out, String eventName, String data) {
        try {
            StringBuilder sb = new StringBuilder();
            if (eventName != null && !eventName.isEmpty()) {
                sb.append("event: ").append(eventName).append("\n");
            }
            for (String line : data.split("\n", -1)) {
                sb.append("data: ").append(line).append("\n");
            }
            sb.append("\n");
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            log.debug("Write to client failed (client likely disconnected)", e);
        }
    }

    private void writeSseEvent(HttpServletResponse response, String eventName, String data) {
        try {
            writeSseEvent(response.getOutputStream(), eventName, data);
        } catch (Exception e) {
            log.debug("Write to client failed", e);
        }
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
