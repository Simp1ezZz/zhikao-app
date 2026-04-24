package com.zhikao.dto;

import lombok.Data;

@Data
public class QuestionImportDTO {
    private String subject;
    private String module;
    private String knowledgePoint;
    private String type;
    private Integer difficulty;
    private String content;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answer;
    private String analysis;
    private String source;
    private String frequency;
    private Integer estimatedTime;
}
