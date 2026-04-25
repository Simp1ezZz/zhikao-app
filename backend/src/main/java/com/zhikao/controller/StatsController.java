package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(statsService.getOverview(userId));
    }

    @GetMapping("/trend")
    public Result<Map<String, Object>> trend(
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(statsService.getTrend(userId, days));
    }

    @GetMapping("/subjects")
    public Result<?> subjectStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(statsService.getSubjectStats(userId));
    }
}
