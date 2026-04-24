package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhikao.common.Result;
import com.zhikao.entity.ErrorTypeConfig;
import com.zhikao.entity.Question;
import com.zhikao.service.ErrorTypeConfigService;
import com.zhikao.service.QuestionService;
import com.zhikao.service.SubjectConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final SubjectConfigService subjectConfigService;
    private final ErrorTypeConfigService errorTypeConfigService;

    @GetMapping
    public Result<IPage<Question>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer difficulty) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(Question::getSubject, subject);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(Question::getModule, module);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        wrapper.orderByDesc(Question::getCreatedAt);
        // 不返回答案字段
        wrapper.select(Question::getId, Question::getSubject, Question::getModule,
                Question::getKnowledgePoint, Question::getType, Question::getDifficulty,
                Question::getContent, Question::getOptions, Question::getSource,
                Question::getFrequency, Question::getEstimatedTime, Question::getCreatedAt);
        return Result.ok(questionService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/{id}")
    public Result<Question> detail(@PathVariable Long id) {
        Question q = questionService.getById(id);
        if (q != null) {
            q.setAnswer(null);
        }
        return Result.ok(q);
    }

    @GetMapping("/subjects")
    public Result<List<Map<String, Object>>> subjects() {
        return Result.ok(subjectConfigService.getTree());
    }

    @GetMapping("/error-types")
    public Result<List<ErrorTypeConfig>> errorTypes() {
        return Result.ok(errorTypeConfigService.list(
                new LambdaQueryWrapper<ErrorTypeConfig>()
                        .eq(ErrorTypeConfig::getEnabled, true)
                        .orderByAsc(ErrorTypeConfig::getSortOrder)));
    }
}
