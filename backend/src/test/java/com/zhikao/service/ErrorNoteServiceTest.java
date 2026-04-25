package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.ErrorNote;
import com.zhikao.entity.Question;
import com.zhikao.mapper.QuestionMapper;
import org.junit.jupiter.api.BeforeEach;
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
class ErrorNoteServiceTest {

    @Autowired
    private ErrorNoteService errorNoteService;

    @Autowired
    private QuestionMapper questionMapper;

    private Long questionId;

    @BeforeEach
    void setUp() {
        Question q = new Question();
        q.setSubject("言语理解与表达");
        q.setModule("片段阅读");
        q.setType("SINGLE");
        q.setDifficulty(3);
        q.setContent("测试题目");
        q.setOptions("{\"A\":\"A\",\"B\":\"B\",\"C\":\"C\",\"D\":\"D\"}");
        q.setAnswer("A");
        q.setAnalysis("测试解析");
        questionMapper.insert(q);
        questionId = q.getId();
    }

    private ErrorNote createErrorNote(Long userId) {
        ErrorNote note = new ErrorNote();
        note.setUserId(userId);
        note.setQuestionId(questionId);
        note.setErrorTypes("[1,2]");
        note.setNote("测试笔记");
        note.setReviewCount(0);
        note.setMastered(false);
        errorNoteService.save(note);
        return note;
    }

    @Test
    void testPageWithQuestion() {
        createErrorNote(100L);
        IPage<Map<String, Object>> page = errorNoteService.pageWithQuestion(100L, 1, 10, null);
        assertEquals(1, page.getRecords().size());
        Map<String, Object> item = page.getRecords().get(0);
        assertEquals("言语理解与表达", item.get("subject"));
        assertNotNull(item.get("content"));
    }

    @Test
    void testPageFilterByMastered() {
        ErrorNote note = createErrorNote(100L);
        IPage<Map<String, Object>> unmasteredPage = errorNoteService.pageWithQuestion(100L, 1, 10, false);
        assertEquals(1, unmasteredPage.getRecords().size());

        IPage<Map<String, Object>> masteredPage = errorNoteService.pageWithQuestion(100L, 1, 10, true);
        assertEquals(0, masteredPage.getRecords().size());
    }

    @Test
    void testUpdateNote() {
        ErrorNote note = createErrorNote(100L);
        errorNoteService.updateNote(100L, note.getId(), "[3]", "更新笔记");
        ErrorNote updated = errorNoteService.getById(note.getId());
        assertEquals("[3]", updated.getErrorTypes());
        assertEquals("更新笔记", updated.getNote());
    }

    @Test
    void testMarkMastered() {
        ErrorNote note = createErrorNote(100L);
        errorNoteService.markMastered(100L, note.getId(), true);
        assertTrue(errorNoteService.getById(note.getId()).getMastered());
    }

    @Test
    void testRecordReview() {
        ErrorNote note = createErrorNote(100L);
        errorNoteService.recordReview(100L, note.getId());
        ErrorNote updated = errorNoteService.getById(note.getId());
        assertEquals(1, updated.getReviewCount());
        assertNotNull(updated.getNextReviewAt());
    }

    @Test
    void testGetReviewList() {
        createErrorNote(100L);
        Map<String, Object> result = errorNoteService.getReviewList(100L, 10);
        assertEquals(1, result.get("total"));
    }
}
