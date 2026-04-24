package com.zhikao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("error_note")
public class ErrorNote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private String errorTypes;
    private String note;
    private Integer reviewCount;
    private Boolean mastered;
    private LocalDateTime nextReviewAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
