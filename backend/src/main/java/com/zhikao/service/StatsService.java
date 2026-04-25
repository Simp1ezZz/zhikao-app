package com.zhikao.service;

import java.util.List;
import java.util.Map;

public interface StatsService {
    Map<String, Object> getOverview(Long userId);
    Map<String, Object> getTrend(Long userId, int days);
    List<Map<String, Object>> getSubjectStats(Long userId);
}
