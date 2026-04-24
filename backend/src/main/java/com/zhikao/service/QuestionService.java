package com.zhikao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface QuestionService extends IService<Question> {
    IPage<Question> pageQuery(int page, int size, String subject, String module);
    Map<String, Object> importFromExcel(MultipartFile file);
}
