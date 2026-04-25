package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.Collection;

import java.util.Map;

public interface CollectionService extends IService<Collection> {
    IPage<Map<String, Object>> pageWithQuestion(Long userId, int page, int size);
    void toggle(Long userId, Long questionId, String note);
    boolean isCollected(Long userId, Long questionId);
}
