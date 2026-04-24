package com.zhikao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.ErrorTypeConfig;
import com.zhikao.mapper.ErrorTypeConfigMapper;
import com.zhikao.service.ErrorTypeConfigService;
import org.springframework.stereotype.Service;

@Service
public class ErrorTypeConfigServiceImpl extends ServiceImpl<ErrorTypeConfigMapper, ErrorTypeConfig> implements ErrorTypeConfigService {
}
