package com.zhikao.controller.admin;

import com.zhikao.common.Result;
import com.zhikao.entity.SubjectConfig;
import com.zhikao.service.SubjectConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
public class AdminSubjectConfigController {

    private final SubjectConfigService subjectConfigService;

    @GetMapping
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(subjectConfigService.getTree());
    }

    @PostMapping
    public Result<Void> add(@RequestBody SubjectConfig config) {
        subjectConfigService.save(config);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SubjectConfig config) {
        config.setId(id);
        subjectConfigService.updateById(config);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        subjectConfigService.removeById(id);
        return Result.ok();
    }
}
