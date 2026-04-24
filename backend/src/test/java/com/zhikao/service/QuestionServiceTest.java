package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.Question;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionServiceTest {

    @Autowired
    private QuestionService questionService;

    private void insertTestQuestion(String subject, String module) {
        Question q = new Question();
        q.setSubject(subject);
        q.setModule(module);
        q.setType("SINGLE");
        q.setDifficulty(3);
        q.setContent("测试题: " + subject + "-" + module);
        q.setOptions("{\"A\":\"A\",\"B\":\"B\",\"C\":\"C\",\"D\":\"D\"}");
        q.setAnswer("A");
        questionService.save(q);
    }

    @Test
    void testPageQueryAll() {
        insertTestQuestion("言语理解与表达", "片段阅读");
        insertTestQuestion("数量关系", "数学运算");

        IPage<Question> page = questionService.pageQuery(1, 10, null, null);
        assertTrue(page.getRecords().size() >= 2);
    }

    @Test
    void testPageQueryBySubject() {
        insertTestQuestion("判断推理", "图形推理");
        insertTestQuestion("数量关系", "数学运算");

        IPage<Question> page = questionService.pageQuery(1, 10, "判断推理", null);
        assertTrue(page.getRecords().size() >= 1);
        page.getRecords().forEach(q -> assertEquals("判断推理", q.getSubject()));
    }

    @Test
    void testPageQueryBySubjectAndModule() {
        insertTestQuestion("资料分析", "文字材料");
        insertTestQuestion("资料分析", "表格材料");
        insertTestQuestion("数量关系", "数学运算");

        IPage<Question> page = questionService.pageQuery(1, 10, "资料分析", "文字材料");
        page.getRecords().forEach(q -> {
            assertEquals("资料分析", q.getSubject());
            assertEquals("文字材料", q.getModule());
        });
    }
}
