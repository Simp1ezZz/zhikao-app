package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.SubjectConfig;

import java.util.List;
import java.util.Map;

public interface SubjectConfigService extends IService<SubjectConfig> {
    List<Map<String, Object>> getTree();
}
