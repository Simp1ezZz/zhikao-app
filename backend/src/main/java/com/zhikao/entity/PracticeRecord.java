package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("practice_record")
public class PracticeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    private Boolean isCorrect;
    private Integer timeSpent;
    private String mode;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
