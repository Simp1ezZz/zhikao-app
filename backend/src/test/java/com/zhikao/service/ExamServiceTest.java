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

        int expectedCorrect = 0;
        Map<String, String> answers = new HashMap<>();
        for (Map<String, Object> q : questions) {
            String qId = String.valueOf(q.get("id"));
            Question dbQ = questionMapper.selectById(Long.valueOf(qId));
            String answer = dbQ != null ? dbQ.getAnswer() : "A";
            answers.put(qId, answer);
            expectedCorrect++;
        }

        Map<String, Object> result = examService.submitExam(300L, examId, answers);
        assertEquals(5, result.get("total"));
        assertEquals(expectedCorrect, result.get("correct"));
        assertEquals(100.0, result.get("score"));
    }

    @Test
    void testSubmitNonexistentExam() {
        assertThrows(RuntimeException.class, () ->
                examService.submitExam(300L, 99999L, Map.of()));
    }
}
