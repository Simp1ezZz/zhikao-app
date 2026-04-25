package com.zhikao.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.common.Result;
import com.zhikao.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public Result<IPage<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(collectionService.pageWithQuestion(userId, page, size));
    }

    @PostMapping("/toggle")
    public Result<Void> toggle(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long questionId = ((Number) body.get("questionId")).longValue();
        String note = (String) body.getOrDefault("note", "");
        collectionService.toggle(userId, questionId, note);
        return Result.ok();
    }

    @GetMapping("/check/{questionId}")
    public Result<Map<String, Object>> check(
            @PathVariable Long questionId,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        boolean collected = collectionService.isCollected(userId, questionId);
        return Result.ok(Map.of("collected", collected));
    }
}
