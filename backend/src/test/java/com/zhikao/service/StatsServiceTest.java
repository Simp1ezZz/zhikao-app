package com.zhikao.service;

import com.zhikao.entity.PracticeRecord;
import com.zhikao.entity.ErrorNote;
import com.zhikao.entity.Collection;
import com.zhikao.mapper.PracticeRecordMapper;
import com.zhikao.mapper.ErrorNoteMapper;
import com.zhikao.mapper.CollectionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatsServiceTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private PracticeRecordMapper practiceRecordMapper;

    @Autowired
    private ErrorNoteMapper errorNoteMapper;

    @Autowired
    private CollectionMapper collectionMapper;

    private void insertPracticeRecord(Long userId, boolean correct) {
        PracticeRecord r = new PracticeRecord();
        r.setUserId(userId);
        r.setQuestionId(1L);
        r.setUserAnswer("A");
        r.setIsCorrect(correct);
        r.setTimeSpent(30);
        r.setMode("PRACTICE");
        practiceRecordMapper.insert(r);
    }

    @Test
    void testOverview() {
        Long userId = 200L;
        insertPracticeRecord(userId, true);
        insertPracticeRecord(userId, true);
        insertPracticeRecord(userId, false);

        ErrorNote note = new ErrorNote();
        note.setUserId(userId);
        note.setQuestionId(1L);
        note.setErrorTypes("[]");
        note.setNote("");
        note.setReviewCount(0);
        note.setMastered(false);
        errorNoteMapper.insert(note);

        Collection col = new Collection();
        col.setUserId(userId);
        col.setQuestionId(1L);
        col.setNote("");
        collectionMapper.insert(col);

        Map<String, Object> overview = statsService.getOverview(userId);
        assertEquals(3L, overview.get("totalPractice"));
        assertEquals(2L, overview.get("correctCount"));
        assertEquals(66.7, overview.get("accuracy"));
        assertEquals(1L, overview.get("errorCount"));
        assertEquals(0L, overview.get("masteredCount"));
        assertEquals(1L, overview.get("collectionCount"));
    }

    @Test
    void testOverviewEmpty() {
        Map<String, Object> overview = statsService.getOverview(999L);
        assertEquals(0L, overview.get("totalPractice"));
        assertEquals(0.0, overview.get("accuracy"));
    }

    @Test
    void testTrend() {
        Long userId = 200L;
        insertPracticeRecord(userId, true);

        Map<String, Object> trend = statsService.getTrend(userId, 7);
        assertEquals(7, trend.get("days"));
        assertNotNull(trend.get("trend"));
    }
}
