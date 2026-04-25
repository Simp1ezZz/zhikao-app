package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.entity.Collection;
import com.zhikao.entity.ErrorNote;
import com.zhikao.entity.PracticeRecord;
import com.zhikao.mapper.CollectionMapper;
import com.zhikao.mapper.ErrorNoteMapper;
import com.zhikao.mapper.PracticeRecordMapper;
import com.zhikao.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final ErrorNoteMapper errorNoteMapper;
    private final CollectionMapper collectionMapper;

    @Override
    public Map<String, Object> getOverview(Long userId) {
        long totalPractice = practiceRecordMapper.selectCount(
                new LambdaQueryWrapper<PracticeRecord>().eq(PracticeRecord::getUserId, userId));
        long correctCount = practiceRecordMapper.selectCount(
                new LambdaQueryWrapper<PracticeRecord>()
                        .eq(PracticeRecord::getUserId, userId)
                        .eq(PracticeRecord::getIsCorrect, true));
        long errorCount = errorNoteMapper.selectCount(
                new LambdaQueryWrapper<ErrorNote>().eq(ErrorNote::getUserId, userId));
        long masteredCount = errorNoteMapper.selectCount(
                new LambdaQueryWrapper<ErrorNote>()
                        .eq(ErrorNote::getUserId, userId)
                        .eq(ErrorNote::getMastered, true));
        long collectionCount = collectionMapper.selectCount(
                new LambdaQueryWrapper<Collection>().eq(Collection::getUserId, userId));

        double accuracy = totalPractice > 0 ? (double) correctCount / totalPractice * 100 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPractice", totalPractice);
        result.put("correctCount", correctCount);
        result.put("accuracy", Math.round(accuracy * 10) / 10.0);
        result.put("errorCount", errorCount);
        result.put("masteredCount", masteredCount);
        result.put("collectionCount", collectionCount);
        return result;
    }

    @Override
    public Map<String, Object> getTrend(Long userId, int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            long dayTotal = practiceRecordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .eq(PracticeRecord::getUserId, userId)
                            .between(PracticeRecord::getCreatedAt, start, end));
            long dayCorrect = practiceRecordMapper.selectCount(
                    new LambdaQueryWrapper<PracticeRecord>()
                            .eq(PracticeRecord::getUserId, userId)
                            .eq(PracticeRecord::getIsCorrect, true)
                            .between(PracticeRecord::getCreatedAt, start, end));

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", date.toString());
            dayData.put("total", dayTotal);
            dayData.put("correct", dayCorrect);
            trend.add(dayData);
        }

        return Map.of("days", days, "trend", trend);
    }
}
