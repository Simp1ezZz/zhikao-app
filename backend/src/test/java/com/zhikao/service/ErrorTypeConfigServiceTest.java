package com.zhikao.service;

import com.zhikao.entity.ErrorTypeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ErrorTypeConfigServiceTest {

    @Autowired
    private ErrorTypeConfigService errorTypeConfigService;

    @Test
    void testListAll() {
        List<ErrorTypeConfig> list = errorTypeConfigService.list();
        assertFalse(list.isEmpty(), "错因类型列表不应为空");
    }

    @Test
    void testAddAndDelete() {
        ErrorTypeConfig config = new ErrorTypeConfig();
        config.setName("测试错因");
        config.setSortOrder(99);
        config.setEnabled(true);
        errorTypeConfigService.save(config);
        assertNotNull(config.getId());

        errorTypeConfigService.removeById(config.getId());
        assertNull(errorTypeConfigService.getById(config.getId()));
    }
}
