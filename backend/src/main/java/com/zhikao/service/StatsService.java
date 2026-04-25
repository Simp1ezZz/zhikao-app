package com.zhikao.service;

import java.util.Map;

public interface StatsService {
    Map<String, Object> getOverview(Long userId);
    Map<String, Object> getTrend(Long userId, int days);
}
