package com.zhikao.mapper;

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
class QuestionMapperTest {

    @Autowired
    private QuestionMapper questionMapper;

    private Question createTestQuestion(String subject, String module) {
        Question q = new Question();
        q.setSubject(subject);
        q.setModule(module);
        q.setType("SINGLE");
        q.setDifficulty(3);
        q.setContent("测试题目内容");
        q.setOptions("{\"A\":\"选项A\",\"B\":\"选项B\",\"C\":\"选项C\",\"D\":\"选项D\"}");
        q.setAnswer("A");
        q.setAnalysis("测试解析");
        q.setFrequency("MEDIUM");
        q.setEstimatedTime(60);
        return q;
    }

    @Test
    void testInsertAndSelect() {
        Question q = createTestQuestion("言语理解与表达", "片段阅读");
        questionMapper.insert(q);
        assertNotNull(q.getId());

        Question found = questionMapper.selectById(q.getId());
        assertNotNull(found);
        assertEquals("言语理解与表达", found.getSubject());
        assertEquals("片段阅读", found.getModule());
        assertEquals("A", found.getAnswer());
    }

    @Test
    void testUpdate() {
        Question q = createTestQuestion("数量关系", "数学运算");
        questionMapper.insert(q);

        q.setDifficulty(5);
        q.setContent("更新后的题目");
        questionMapper.updateById(q);

        Question found = questionMapper.selectById(q.getId());
        assertEquals(5, found.getDifficulty());
        assertEquals("更新后的题目", found.getContent());
    }

    @Test
    void testDelete() {
        Question q = createTestQuestion("判断推理", "图形推理");
        questionMapper.insert(q);
        Long id = q.getId();

        questionMapper.deleteById(id);
        assertNull(questionMapper.selectById(id));
    }

    @Test
    void testSelectBySubject() {
        questionMapper.insert(createTestQuestion("常识判断", "政治常识"));
        questionMapper.insert(createTestQuestion("常识判断", "法律常识"));
        questionMapper.insert(createTestQuestion("数量关系", "数学运算"));

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question>()
                .eq(Question::getSubject, "常识判断");
        long count = questionMapper.selectCount(wrapper);
        assertEquals(2, count);
    }
}
