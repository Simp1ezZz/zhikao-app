package com.zhikao.service;

import com.zhikao.entity.PracticeRecord;

import java.util.List;
import java.util.Map;

public interface PracticeService {
    Map<String, Object> startPractice(Long userId, String subject, String module, int count);
    Map<String, Object> submitAnswer(Long userId, Long questionId, String userAnswer, int timeSpent);
}
