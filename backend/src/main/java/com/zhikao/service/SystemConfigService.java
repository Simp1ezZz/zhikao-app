package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.SystemConfig;

public interface SystemConfigService extends IService<SystemConfig> {
    SystemConfig getByKey(String configKey);
    void refreshCache(SystemConfig config);
    void deleteCache(String configKey);
}
