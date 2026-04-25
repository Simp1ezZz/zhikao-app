package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.LlmModelConfig;

public interface LlmModelConfigService extends IService<LlmModelConfig> {
    void setActive(Long id);
    void refreshCache(LlmModelConfig config);
    void deleteCache();
}
