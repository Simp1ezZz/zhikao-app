package com.zhikao.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.Result;
import com.zhikao.entity.ErrorTypeConfig;
import com.zhikao.service.ErrorTypeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/error-types")
@RequiredArgsConstructor
public class AdminErrorTypeConfigController {

    private final ErrorTypeConfigService errorTypeConfigService;

    @GetMapping
    public Result<List<ErrorTypeConfig>> list() {
        return Result.ok(errorTypeConfigService.list(
                new LambdaQueryWrapper<ErrorTypeConfig>().orderByAsc(ErrorTypeConfig::getSortOrder)));
    }

    @PostMapping
    public Result<Void> add(@RequestBody ErrorTypeConfig config) {
        errorTypeConfigService.save(config);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ErrorTypeConfig config) {
        config.setId(id);
        errorTypeConfigService.updateById(config);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        errorTypeConfigService.removeById(id);
        return Result.ok();
    }
}
