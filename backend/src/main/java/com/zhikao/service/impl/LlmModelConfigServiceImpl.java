package com.zhikao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.LlmModelConfig;
import com.zhikao.mapper.LlmModelConfigMapper;
import com.zhikao.service.LlmModelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LlmModelConfigServiceImpl extends ServiceImpl<LlmModelConfigMapper, LlmModelConfig> implements LlmModelConfigService {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_KEY_ACTIVE = "zhikao:config:llm.active_config";
    private static final String REDIS_PREFIX = "zhikao:config:";

    @Override
    @Transactional
    public void setActive(Long id) {
        lambdaUpdate().set(LlmModelConfig::getIsActive, false).update();
        lambdaUpdate().eq(LlmModelConfig::getId, id).set(LlmModelConfig::getIsActive, true).update();
        LlmModelConfig config = getById(id);
        if (config != null) {
            refreshCache(config);
        }
    }

    @Override
    public void refreshCache(LlmModelConfig config) {
        if (config == null || !Boolean.TRUE.equals(config.getIsActive())) {
            return;
        }
        String json = String.format(
            "{\"api_key\":\"%s\",\"base_url\":\"%s\",\"model\":\"%s\",\"provider\":\"%s\"}",
            escapeJson(config.getApiKey()),
            escapeJson(config.getBaseUrl()),
            escapeJson(config.getModel()),
            escapeJson(config.getProvider())
        );
        redisTemplate.opsForValue().set(REDIS_KEY_ACTIVE, json);
        redisTemplate.opsForValue().set(REDIS_PREFIX + "llm.api_key", config.getApiKey());
        redisTemplate.opsForValue().set(REDIS_PREFIX + "llm.base_url", config.getBaseUrl());
        redisTemplate.opsForValue().set(REDIS_PREFIX + "llm.model", config.getModel());
        redisTemplate.opsForValue().set(REDIS_PREFIX + "llm.provider", config.getProvider());
    }

    @Override
    public void deleteCache() {
        redisTemplate.delete(REDIS_KEY_ACTIVE);
        redisTemplate.delete(REDIS_PREFIX + "llm.api_key");
        redisTemplate.delete(REDIS_PREFIX + "llm.base_url");
        redisTemplate.delete(REDIS_PREFIX + "llm.model");
        redisTemplate.delete(REDIS_PREFIX + "llm.provider");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
