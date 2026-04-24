package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhikao.entity.Question;
import com.zhikao.mapper.QuestionMapper;
import com.zhikao.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    private final ObjectMapper objectMapper;

    @Override
    public IPage<Question> pageQuery(int page, int size, String subject, String module) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (subject != null && !subject.isEmpty()) {
            wrapper.eq(Question::getSubject, subject);
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(Question::getModule, module);
        }
        wrapper.orderByDesc(Question::getCreatedAt);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) {
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    Question q = parseRow(row, i);
                    save(q);
                    success++;
                } catch (Exception e) {
                    fail++;
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return Map.of("success", 0, "fail", 0, "error", "文件解析失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }

    private Question parseRow(Row row, int rowIndex) {
        Question q = new Question();
        q.setSubject(getCellString(row, 0));
        q.setModule(getCellString(row, 1));
        q.setKnowledgePoint(getCellString(row, 2));
        q.setType(getCellString(row, 3).isEmpty() ? "SINGLE" : getCellString(row, 3));
        q.setDifficulty(getCellInt(row, 4, 3));
        q.setContent(getCellString(row, 5));

        if (q.getSubject().isEmpty()) throw new RuntimeException("科目不能为空");
        if (q.getModule().isEmpty()) throw new RuntimeException("模块不能为空");
        if (q.getContent().isEmpty()) throw new RuntimeException("题干不能为空");

        try {
            Map<String, String> options = new LinkedHashMap<>();
            options.put("A", getCellString(row, 6));
            options.put("B", getCellString(row, 7));
            options.put("C", getCellString(row, 8));
            options.put("D", getCellString(row, 9));
            q.setOptions(objectMapper.writeValueAsString(options));
        } catch (Exception e) {
            throw new RuntimeException("选项解析失败");
        }

        q.setAnswer(getCellString(row, 10));
        if (q.getAnswer().isEmpty()) throw new RuntimeException("答案不能为空");

        q.setAnalysis(getCellString(row, 11));
        q.setSource(getCellString(row, 12));
        q.setFrequency(getCellString(row, 13).isEmpty() ? "MEDIUM" : getCellString(row, 13));
        q.setEstimatedTime(getCellInt(row, 14, 60));
        return q;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private int getCellInt(Row row, int col, int defaultVal) {
        Cell cell = row.getCell(col);
        if (cell == null) return defaultVal;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            return Integer.parseInt(cell.getStringCellValue().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }
}
