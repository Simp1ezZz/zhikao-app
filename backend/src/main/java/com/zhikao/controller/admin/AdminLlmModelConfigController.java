package com.zhikao.controller.admin;

import com.zhikao.common.Result;
import com.zhikao.entity.LlmModelConfig;
import com.zhikao.service.LlmModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/llm-config")
@RequiredArgsConstructor
public class AdminLlmModelConfigController {

    private final LlmModelConfigService llmModelConfigService;

    @GetMapping
    public Result<List<LlmModelConfig>> list() {
        return Result.ok(llmModelConfigService.lambdaQuery().eq(LlmModelConfig::getEnabled, true).list());
    }

    @GetMapping("/{id}")
    public Result<LlmModelConfig> getById(@PathVariable Long id) {
        return Result.ok(llmModelConfigService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@RequestBody LlmModelConfig config) {
        config.setIsActive(false);
        config.setEnabled(true);
        llmModelConfigService.save(config);
        if (Boolean.TRUE.equals(config.getIsActive())) {
            llmModelConfigService.setActive(config.getId());
        }
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LlmModelConfig config) {
        config.setId(id);
        llmModelConfigService.updateById(config);
        LlmModelConfig updated = llmModelConfigService.getById(id);
        if (updated != null && Boolean.TRUE.equals(updated.getIsActive())) {
            llmModelConfigService.refreshCache(updated);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        LlmModelConfig old = llmModelConfigService.getById(id);
        if (old != null && Boolean.TRUE.equals(old.getIsActive())) {
            llmModelConfigService.deleteCache();
        }
        llmModelConfigService.removeById(id);
        return Result.ok();
    }

    @PostMapping("/{id}/set-active")
    public Result<Void> setActive(@PathVariable Long id) {
        llmModelConfigService.setActive(id);
        return Result.ok();
    }
}
