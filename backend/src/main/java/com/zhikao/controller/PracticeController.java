package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @PostMapping("/start")
    public Result<Map<String, Object>> start(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String subject = (String) params.getOrDefault("subject", "");
        String module = (String) params.getOrDefault("module", "");
        int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 20;
        return Result.ok(practiceService.startPractice(userId, subject, module, count));
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long questionId = ((Number) params.get("questionId")).longValue();
        String userAnswer = (String) params.get("userAnswer");
        int timeSpent = params.containsKey("timeSpent") ? ((Number) params.get("timeSpent")).intValue() : 0;
        return Result.ok(practiceService.submitAnswer(userId, questionId, userAnswer, timeSpent));
    }
}
