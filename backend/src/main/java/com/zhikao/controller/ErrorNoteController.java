package com.zhikao.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.common.Result;
import com.zhikao.service.ErrorNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/error-notes")
@RequiredArgsConstructor
public class ErrorNoteController {

    private final ErrorNoteService errorNoteService;

    @GetMapping
    public Result<IPage<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean mastered,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(errorNoteService.pageWithQuestion(userId, page, size, mastered));
    }

    @PutMapping("/{id}")
    public Result<Void> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        errorNoteService.updateNote(userId, id, body.get("errorTypes"), body.get("note"));
        return Result.ok();
    }

    @PutMapping("/{id}/mastered")
    public Result<Void> mastered(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        errorNoteService.markMastered(userId, id, body.getOrDefault("mastered", true));
        return Result.ok();
    }

    @GetMapping("/review")
    public Result<Map<String, Object>> review(
            @RequestParam(defaultValue = "10") int count,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.ok(errorNoteService.getReviewList(userId, count));
    }

    @PostMapping("/{id}/review")
    public Result<Void> recordReview(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        errorNoteService.recordReview(userId, id);
        return Result.ok();
    }
}
