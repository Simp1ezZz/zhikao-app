package com.zhikao.service;

import com.zhikao.entity.Question;
import com.zhikao.mapper.QuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExamServiceTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private QuestionMapper questionMapper;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 5; i++) {
            Question q = new Question();
            q.setSubject("言语理解与表达");
            q.setModule("片段阅读");
            q.setType("SINGLE");
            q.setDifficulty(3);
            q.setContent("考试测试题" + i);
            q.setOptions("{\"A\":\"A\",\"B\":\"B\",\"C\":\"C\",\"D\":\"D\"}");
            q.setAnswer("A");
            q.setAnalysis("解析" + i);
            questionMapper.insert(q);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testStartExam() {
        Map<String, Object> result = examService.startExam(300L, "", 5, 60);
        assertNotNull(result.get("examId"));
        assertEquals(5, result.get("total"));
        List<Map<String, Object>> questions = (List<Map<String, Object>>) result.get("questions");
        assertEquals(5, questions.size());
        assertNull(questions.get(0).get("answer"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSubmitExam() {
        Map<String, Object> exam = examService.startExam(300L, "", 5, 60);
        Long examId = ((Number) exam.get("examId")).longValue();
        List<Map<String, Object>> questions = (List<Map<String, Object>>) exam.get("questions");

        Map<String, String> answers = new HashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            String qId = String.valueOf(questions.get(i).get("id"));
            answers.put(qId, i < 3 ? "A" : "B");
        }

        Map<String, Object> result = examService.submitExam(300L, examId, answers);
        assertEquals(5, result.get("total"));
        assertEquals(3, result.get("correct"));
        assertTrue((double) result.get("score") > 0);
    }

    @Test
    void testSubmitNonexistentExam() {
        assertThrows(RuntimeException.class, () ->
                examService.submitExam(300L, 99999L, Map.of()));
    }
}
