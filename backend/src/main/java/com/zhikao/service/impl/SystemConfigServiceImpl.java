package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.SystemConfig;
import com.zhikao.mapper.SystemConfigMapper;
import com.zhikao.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_PREFIX = "zhikao:config:";

    @Override
    public SystemConfig getByKey(String configKey) {
        return getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getEnabled, true));
    }

    @Override
    public void refreshCache(SystemConfig config) {
        if (config == null) return;
        Boolean enabled = config.getEnabled();
        if (enabled == null && config.getId() != null) {
            SystemConfig db = getById(config.getId());
            enabled = db != null ? db.getEnabled() : Boolean.TRUE;
        }
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }
        if (Boolean.TRUE.equals(enabled)) {
            redisTemplate.opsForValue().set(REDIS_PREFIX + config.getConfigKey(), config.getConfigValue());
        } else {
            deleteCache(config.getConfigKey());
        }
    }

    @Override
    public void deleteCache(String configKey) {
        redisTemplate.delete(REDIS_PREFIX + configKey);
    }
}
