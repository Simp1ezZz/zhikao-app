package com.zhikao.controller.admin;

import com.zhikao.common.Result;
import com.zhikao.entity.SystemConfig;
import com.zhikao.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/system-config")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<List<SystemConfig>> list() {
        return Result.ok(systemConfigService.list());
    }

    @PostMapping
    public Result<Void> add(@RequestBody SystemConfig config) {
        systemConfigService.save(config);
        systemConfigService.refreshCache(config);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SystemConfig config) {
        config.setId(id);
        systemConfigService.updateById(config);
        SystemConfig existing = systemConfigService.getById(id);
        if (existing != null) {
            existing.setConfigValue(config.getConfigValue());
            systemConfigService.refreshCache(existing);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SystemConfig old = systemConfigService.getById(id);
        if (old != null) {
            systemConfigService.deleteCache(old.getConfigKey());
        }
        systemConfigService.removeById(id);
        return Result.ok();
    }
}
