package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.Result;
import com.zhikao.entity.ErrorTypeConfig;
import com.zhikao.service.ErrorTypeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/error-types")
@RequiredArgsConstructor
public class ErrorTypeController {

    private final ErrorTypeConfigService errorTypeConfigService;

    @GetMapping
    public Result<List<ErrorTypeConfig>> list() {
        return Result.ok(errorTypeConfigService.list(
                new LambdaQueryWrapper<ErrorTypeConfig>()
                        .eq(ErrorTypeConfig::getEnabled, true)
                        .orderByAsc(ErrorTypeConfig::getSortOrder)));
    }
}
