package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhikao.entity.Collection;
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
class CollectionServiceTest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private QuestionMapper questionMapper;

    private Long questionId;

    @BeforeEach
    void setUp() {
        Question q = new Question();
        q.setSubject("数量关系");
        q.setModule("数学运算");
        q.setType("SINGLE");
        q.setDifficulty(3);
        q.setContent("收藏测试题");
        q.setOptions("{\"A\":\"1\",\"B\":\"2\",\"C\":\"3\",\"D\":\"4\"}");
        q.setAnswer("B");
        questionMapper.insert(q);
        questionId = q.getId();
    }

    @Test
    void testToggleCollect() {
        assertFalse(collectionService.isCollected(100L, questionId));

        collectionService.toggle(100L, questionId, "好题");
        assertTrue(collectionService.isCollected(100L, questionId));

        collectionService.toggle(100L, questionId, "");
        assertFalse(collectionService.isCollected(100L, questionId));
    }

    @Test
    void testPageWithQuestion() {
        collectionService.toggle(100L, questionId, "笔记");
        IPage<Map<String, Object>> page = collectionService.pageWithQuestion(100L, 1, 10);
        assertEquals(1, page.getRecords().size());
        Map<String, Object> item = page.getRecords().get(0);
        assertEquals("数量关系", item.get("subject"));
        assertEquals("笔记", item.get("note"));
    }

    @Test
    void testIsCollected() {
        assertFalse(collectionService.isCollected(100L, questionId));
        collectionService.toggle(100L, questionId, "");
        assertTrue(collectionService.isCollected(100L, questionId));
    }
}
