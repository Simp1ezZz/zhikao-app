package com.zhikao.service;

import java.util.Map;

public interface ExamService {
    Map<String, Object> startExam(Long userId, String subject, int count, int timeLimitMinutes);
    Map<String, Object> submitExam(Long userId, Long examId, Map<String, String> answers);
}
