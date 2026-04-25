package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping("/start")
    public Result<Map<String, Object>> start(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String subject = (String) params.getOrDefault("subject", "");
        int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 40;
        int timeLimit = params.containsKey("timeLimitMinutes") ? ((Number) params.get("timeLimitMinutes")).intValue() : 60;
        return Result.ok(examService.startExam(userId, subject, count, timeLimit));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(
            @RequestBody Map<String, Object> params,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long examId = ((Number) params.get("examId")).longValue();
        Map<String, String> answers = (Map<String, String>) params.get("answers");
        return Result.ok(examService.submitExam(userId, examId, answers));
    }
}
