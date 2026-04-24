package com.zhikao.service;

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
class PracticeServiceTest {

    @Autowired
    private PracticeService practiceService;

    @Autowired
    private QuestionMapper questionMapper;

    private Long testUserId = 1L;

    private Long insertQuestion(String subject, String module, String answer) {
        Question q = new Question();
        q.setSubject(subject);
        q.setModule(module);
        q.setType("SINGLE");
        q.setDifficulty(3);
        q.setContent("题目-" + subject + "-" + module);
        q.setOptions("{\"A\":\"选项A\",\"B\":\"选项B\",\"C\":\"选项C\",\"D\":\"选项D\"}");
        q.setAnswer(answer);
        q.setAnalysis("这是解析");
        q.setFrequency("MEDIUM");
        q.setEstimatedTime(60);
        questionMapper.insert(q);
        return q.getId();
    }

    @Test
    void testStartPractice() {
        insertQuestion("言语理解与表达", "片段阅读", "A");
        insertQuestion("言语理解与表达", "片段阅读", "B");

        Map<String, Object> result = practiceService.startPractice(testUserId, "言语理解与表达", "片段阅读", 10);
        assertNotNull(result.get("questionIds"));
        assertEquals(2, ((java.util.List<?>) result.get("questionIds")).size());
    }

    @Test
    void testStartPracticeEmpty() {
        Map<String, Object> result = practiceService.startPractice(testUserId, "不存在的科目", "", 10);
        assertEquals(0, ((java.util.List<?>) result.get("questionIds")).size());
    }

    @Test
    void testSubmitCorrectAnswer() {
        Long qId = insertQuestion("数量关系", "数学运算", "A");

        Map<String, Object> result = practiceService.submitAnswer(testUserId, qId, "A", 30);
        assertTrue((Boolean) result.get("correct"));
        assertEquals("A", result.get("answer"));
        assertNotNull(result.get("analysis"));
    }

    @Test
    void testSubmitWrongAnswer() {
        Long qId = insertQuestion("判断推理", "图形推理", "B");

        Map<String, Object> result = practiceService.submitAnswer(testUserId, qId, "A", 45);
        assertFalse((Boolean) result.get("correct"));
        assertEquals("B", result.get("answer"));
    }

    @Test
    void testSubmitNonexistentQuestion() {
        assertThrows(RuntimeException.class, () ->
                practiceService.submitAnswer(testUserId, 99999L, "A", 10));
    }
}
