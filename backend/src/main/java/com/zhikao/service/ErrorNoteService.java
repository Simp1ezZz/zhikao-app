package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.ErrorNote;

import java.util.Map;

public interface ErrorNoteService extends IService<ErrorNote> {
    IPage<Map<String, Object>> pageWithQuestion(Long userId, int page, int size, Boolean mastered);
    void updateNote(Long userId, Long id, String errorTypes, String note);
    void markMastered(Long userId, Long id, boolean mastered);
    Map<String, Object> getReviewList(Long userId, int count);
    void recordReview(Long userId, Long id);
}
