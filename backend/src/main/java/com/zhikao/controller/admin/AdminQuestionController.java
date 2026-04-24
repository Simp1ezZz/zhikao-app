package com.zhikao.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

    @GetMapping
    public Result<IPage<Question>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String module) {
        return Result.ok(questionService.pageQuery(page, size, subject, module));
    }

    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        return Result.ok(questionService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Question question) {
        question.setId(id);
        questionService.updateById(question);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(questionService.importFromExcel(file));
    }
}
