package com.zhikao.controller;

import com.zhikao.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;

    @GetMapping("/analysis/{questionId}")
    public Result<Object> analysis(@PathVariable Long questionId) {
        String url = aiServiceUrl + "/api/ai/analysis/" + questionId;
        ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
        return Result.ok(resp.getBody());
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
