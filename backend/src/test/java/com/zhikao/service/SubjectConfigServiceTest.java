package com.zhikao.service;

import com.zhikao.entity.SubjectConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubjectConfigServiceTest {

    @Autowired
    private SubjectConfigService subjectConfigService;

    @Test
    void testGetTree() {
        List<Map<String, Object>> tree = subjectConfigService.getTree();
        assertFalse(tree.isEmpty(), "科目树不应为空");

        Map<String, Object> first = tree.get(0);
        assertNotNull(first.get("name"));
        if (tree.stream().anyMatch(n -> "言语理解与表达".equals(n.get("name")))) {
            Map<String, Object> yuyan = tree.stream()
                    .filter(n -> "言语理解与表达".equals(n.get("name")))
                    .findFirst().orElse(null);
            assertNotNull(yuyan.get("children"));
        }
    }

    @Test
    void testAddAndDelete() {
        SubjectConfig config = new SubjectConfig();
        config.setName("测试科目");
        config.setParentId(0L);
        config.setLevel(1);
        config.setSortOrder(99);
        config.setEnabled(true);
        subjectConfigService.save(config);
        assertNotNull(config.getId());

        subjectConfigService.removeById(config.getId());
        assertNull(subjectConfigService.getById(config.getId()));
    }
}
