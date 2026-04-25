package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;
    private final QuestionService questionService;

    @GetMapping("/analysis/{questionId}")
    public Result<Object> analysis(@PathVariable Long questionId) {
        Question question = questionService.getById(questionId);
        if (question == null) {
            return Result.error("题目不存在");
        }

        String optionText = "";
        String options = question.getOptions();
        if (options != null && !options.isEmpty()) {
            optionText = options;
        }

        String prompt = String.format(
            "请对以下公务员考试题目进行详细解析：\n\n题目：%s\n选项：%s\n正确答案：%s\n\n请从以下角度进行解析：\n1. 正确答案分析：为什么选这个答案\n2. 各选项分析：每个选项对在哪里/错在哪里\n3. 知识点总结：这道题涉及的核心知识点\n4. 解题技巧：类似题目的解题思路",
            question.getContent(), optionText, question.getAnswer()
        );

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
}
