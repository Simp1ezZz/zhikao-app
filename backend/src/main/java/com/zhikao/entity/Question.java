package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subject;
    private String module;
    private String knowledgePoint;
    private String type;
    private Integer difficulty;
    private String content;
    private String options;
    private String answer;
    private String analysis;
    private String source;
    private String frequency;
    private Integer estimatedTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
